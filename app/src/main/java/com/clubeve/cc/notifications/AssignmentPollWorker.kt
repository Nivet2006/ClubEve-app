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
        val prId = SessionStore.getPrId(applicationContext)
        if (prId.isNullOrBlank()) {
            Log.d(TAG, "No pr_id — skipping")
            return Result.success()
        }

        return try {
            val client = SupabaseClientProvider.client
            val seenIds = SessionStore.getSeenAssignmentIds(applicationContext).toMutableSet()

            // Fetch all assignments for this PR
            val assignments = client.from("pr_event_assignments")
                .select(columns = Columns.list("id", "event_id")) {
                    filter { eq("pr_id", prId) }
                }
                .decodeList<AssignmentRow>()

            Log.d(TAG, "Found ${assignments.size} assignments, ${seenIds.size} already seen")

            for (assignment in assignments) {
                if (assignment.id in seenIds) continue

                // Fetch event title
                val eventTitle = try {
                    client.from("events")
                        .select(columns = Columns.list("title")) {
                            filter { eq("id", assignment.event_id) }
                        }
                        .decodeList<EventRow>()
                        .firstOrNull()?.title ?: "New Event"
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch event: ${e.message}")
                    "New Event"
                }

                Log.d(TAG, "Notifying: $eventTitle (id=${assignment.id})")
                AssignmentNotifier.notify(applicationContext, eventTitle)
                seenIds.add(assignment.id)
            }

            SessionStore.saveSeenAssignmentIds(applicationContext, seenIds)
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Poll error: ${e.message}")
            Result.retry()
        }
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
