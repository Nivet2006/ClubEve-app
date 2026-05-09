package com.clubeve.cc.data.repository

import com.clubeve.cc.data.local.dao.RegistrationDao
import com.clubeve.cc.data.local.entity.RegistrationEntity
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.data.remote.dto.RegistrationDto
import com.clubeve.cc.utils.NetworkMonitor
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(
    private val dao: RegistrationDao,
    private val networkMonitor: NetworkMonitor
) {
    private val supabase = SupabaseClientProvider.client

    fun observeAttendance(eventId: String): Flow<List<RegistrationEntity>> =
        dao.observeByEvent(eventId)

    fun observePresentCount(eventId: String): Flow<Int> =
        dao.observePresentCount(eventId)

    fun observeTotalCount(eventId: String): Flow<Int> =
        dao.observeTotalCount(eventId)

    suspend fun syncFromRemote(eventId: String): Result<Unit> = runCatching {
        if (!networkMonitor.isOnline()) return@runCatching
        val list = supabase.from("registrations")
            .select { filter { eq("event_id", eventId) } }
            .decodeList<RegistrationDto>()
        dao.upsert(list.map { it.toEntity() })
    }

    suspend fun markPresent(usn: String, eventId: String, markedBy: String): ScanResult {
        val student = dao.findByUsn(usn.uppercase(), eventId)
            ?: return ScanResult.NotRegistered(usn)

        if (student.isPresent) return ScanResult.AlreadyPresent(student)

        val now = System.currentTimeMillis()
        dao.markPresent(usn.uppercase(), eventId, now)

        if (networkMonitor.isOnline()) {
            runCatching {
                supabase.from("registrations").update({
                    set("is_present", true)
                    set("marked_at", java.time.Instant.ofEpochMilli(now).toString())
                    set("marked_by", markedBy)
                }) {
                    filter {
                        eq("usn", usn.uppercase())
                        eq("event_id", eventId)
                    }
                }
                dao.markSynced(student.id)
            }
        }

        return ScanResult.MarkedPresent(student.copy(isPresent = true, markedAt = now))
    }

    suspend fun findByUsn(usn: String, eventId: String): RegistrationEntity? =
        dao.findByUsn(usn.uppercase(), eventId)

    suspend fun getUnsynced() = dao.getUnsynced()

    suspend fun markSynced(id: String) = dao.markSynced(id)
}

sealed class ScanResult {
    data class MarkedPresent(val student: RegistrationEntity) : ScanResult()
    data class AlreadyPresent(val student: RegistrationEntity) : ScanResult()
    data class NotRegistered(val usn: String) : ScanResult()
}
