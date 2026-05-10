package com.clubeve.cc.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import com.clubeve.cc.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
private data class AssignmentRow(
    val id: String,
    val event_id: String
)

@Serializable
private data class EventRow(
    val title: String
)

class AssignmentPollWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // Notifications disabled — skip all polling
        return Result.success()
    }

    companion object {
        const val TAG = "AssignmentPoll"

        fun schedule(context: Context) {
            // Immediate one-shot — catches all missed assignments right on login
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<AssignmentPollWorker>()
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            )
            // Periodic every 15 min for future assignments
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AssignmentPollWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }
    }
}
