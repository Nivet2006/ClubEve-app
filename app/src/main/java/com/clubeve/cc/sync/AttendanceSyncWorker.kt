package com.clubeve.cc.sync

import android.content.Context
import androidx.work.*
import com.clubeve.cc.data.local.AppDatabase
import com.clubeve.cc.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import java.util.concurrent.TimeUnit

class AttendanceSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private val dao = AppDatabase.getInstance(ctx).registrationDao()
    private val supabase = SupabaseClientProvider.client

    override suspend fun doWork(): Result {
        val unsynced = dao.getUnsynced()
        if (unsynced.isEmpty()) return Result.success()

        var allSucceeded = true
        unsynced.forEach { record ->
            runCatching {
                supabase.from("registrations").update({
                    set("is_present", record.isPresent)
                    record.markedAt?.let {
                        set("marked_at", java.time.Instant.ofEpochMilli(it).toString())
                    }
                }) { filter { eq("id", record.id) } }
                dao.markSynced(record.id)
            }.onFailure { allSucceeded = false }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }

    companion object {
        const val TAG = "attendance_sync"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
