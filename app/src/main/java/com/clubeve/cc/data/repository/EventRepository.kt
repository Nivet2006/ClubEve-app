package com.clubeve.cc.data.repository

import com.clubeve.cc.data.local.dao.EventDao
import com.clubeve.cc.data.local.entity.EventEntity
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.data.remote.dto.EventDto
import com.clubeve.cc.utils.NetworkMonitor
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

class EventRepository(
    private val eventDao: EventDao,
    private val networkMonitor: NetworkMonitor
) {
    fun observeEvents(): Flow<List<EventEntity>> = eventDao.observeAll()

    suspend fun refreshEvents(coordinatorId: String) {
        if (!networkMonitor.isOnline()) return
        try {
            val supabase = SupabaseClientProvider.client
            val events = supabase.from("events")
                .select {
                    filter {
                        eq("created_by", coordinatorId)
                        eq("status", "approved")
                    }
                }
                .decodeList<EventDto>()
            eventDao.upsert(events.map { it.toEntity() })
        } catch (_: Exception) {}
    }

    suspend fun refreshAllApprovedEvents() {
        if (!networkMonitor.isOnline()) return
        try {
            val supabase = SupabaseClientProvider.client
            val events = supabase.from("events")
                .select {
                    filter { eq("status", "approved") }
                }
                .decodeList<EventDto>()
            eventDao.upsert(events.map { it.toEntity() })
        } catch (_: Exception) {}
    }
}
