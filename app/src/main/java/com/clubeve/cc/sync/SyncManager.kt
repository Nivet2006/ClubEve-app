package com.clubeve.cc.sync

import android.content.Context
import com.clubeve.cc.data.local.AppDatabase
import com.clubeve.cc.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class SyncReport(val total: Int, val synced: Int, val failed: Int)

object SyncManager {

    suspend fun syncPendingCheckIns(context: Context): SyncReport {
        val db = AppDatabase.getInstance(context)
        val supabase = SupabaseClientProvider.client
        val pending = db.registrationDao().getPendingSync()

        var successCount = 0
        val failedIds = mutableListOf<String>()

        for (reg in pending) {
            runCatching {
                val checkedInAt = reg.checkedInAt ?: Instant.now().toString()
                supabase.from("registrations").update({
                    set("checked_in", true)
                    set("checked_in_at", checkedInAt)
                }) {
                    filter { eq("id", reg.id) }
                }
                db.registrationDao().markSynced(reg.id)
                successCount++
            }.onFailure {
                failedIds.add(reg.id)
            }
        }

        return SyncReport(
            total = pending.size,
            synced = successCount,
            failed = failedIds.size
        )
    }

    fun observePendingCount(context: Context): Flow<Int> =
        AppDatabase.getInstance(context).registrationDao().observePendingSyncCount()
}
