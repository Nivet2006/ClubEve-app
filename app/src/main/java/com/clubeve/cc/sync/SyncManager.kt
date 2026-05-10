package com.clubeve.cc.sync

import android.content.Context
import com.clubeve.cc.data.local.AppDatabase
import com.clubeve.cc.data.local.entity.RegistrationEntity
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Registration
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class SyncReport(
    val total: Int,
    val synced: Int,
    val failed: Int,
    val conflicts: List<SyncConflict> = emptyList()
)

/** A conflict: local says checked-in but remote already has a different checked_in_at. */
data class SyncConflict(
    val registrationId: String,
    val studentName: String,
    val studentUsn: String,
    val localCheckedInAt: String?,
    val remoteCheckedInAt: String?,
    val remoteAlreadyCheckedIn: Boolean
)

object SyncManager {

    suspend fun syncPendingCheckIns(context: Context): SyncReport {
        val db = AppDatabase.getInstance(context)
        val supabase = SupabaseClientProvider.client
        val pending = db.registrationDao().getPendingSync()

        var successCount = 0
        val failedIds = mutableListOf<String>()
        val conflicts = mutableListOf<SyncConflict>()

        for (reg in pending) {
            runCatching {
                // Check remote state before pushing
                val remote = supabase.from("registrations")
                    .select(columns = Columns.list("id", "checked_in", "checked_in_at", "student_id")) {
                        filter { eq("id", reg.id) }
                    }
                    .decodeSingleOrNull<Registration>()

                if (remote != null && remote.checkedIn && remote.checkedInAt != null
                    && remote.checkedInAt != reg.checkedInAt) {
                    // Remote was already checked in with a different timestamp — conflict
                    val profile = db.profileDao().getById(reg.studentId)
                    conflicts.add(
                        SyncConflict(
                            registrationId = reg.id,
                            studentName = profile?.fullName ?: reg.studentName.ifBlank { "Unknown" },
                            studentUsn = profile?.usn ?: reg.usn.ifBlank { "Unknown" },
                            localCheckedInAt = reg.checkedInAt,
                            remoteCheckedInAt = remote.checkedInAt,
                            remoteAlreadyCheckedIn = true
                        )
                    )
                    // Accept remote as source of truth — mark local as synced
                    db.registrationDao().markSynced(reg.id)
                    successCount++
                } else {
                    // No conflict — push local check-in to remote
                    val checkedInAt = reg.checkedInAt ?: Instant.now().toString()
                    supabase.from("registrations").update({
                        set("checked_in", true)
                        set("checked_in_at", checkedInAt)
                    }) {
                        filter { eq("id", reg.id) }
                    }
                    db.registrationDao().markSynced(reg.id)
                    successCount++
                }
            }.onFailure {
                failedIds.add(reg.id)
            }
        }

        return SyncReport(
            total = pending.size,
            synced = successCount,
            failed = failedIds.size,
            conflicts = conflicts
        )
    }

    fun observePendingCount(context: Context): Flow<Int> =
        AppDatabase.getInstance(context).registrationDao().observePendingSyncCount()
}
