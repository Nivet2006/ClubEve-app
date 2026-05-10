package com.clubeve.cc.notifications

import android.content.Context
import androidx.work.*
import com.clubeve.cc.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that polls Supabase every 15 minutes for new
 * pr_event_assignments rows. Fires a local notification for each new assignment.
 * Runs even when the app is closed, as long as the device has internet.
 */
class AssignmentPollWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val prId = SessionStore.getPrId(applicationContext) ?: return Result.success()

        return try {
            val client = SupabaseClientProvider.client
            val lastSeen = SessionStore.getLastSeen(applicationContext)

            // Fetch assignments for this PR user, ordered by created_at desc
            val response = client.from("pr_event_assignments")
                .select(columns = Columns.list("id", "event_id", "created_at")) {
                    filter { eq("pr_id", prId) }
                    order("created_at", Order.DESCENDING)
                    limit(10)
                }
                .data

            val assignments = Json.parseToJsonElement(response).jsonArray

            var newestTimestamp = lastSeen

            for (item in assignments) {
                val obj = item.jsonObject
                val createdAt = obj["created_at"]?.jsonPrimitive?.content ?: continue
                val eventId   = obj["event_id"]?.jsonPrimitive?.content ?: continue

                // Skip if we've already notified about this or older assignments
                if (lastSeen != null && !isNewer(createdAt, lastSeen)) continue

                // Fetch event title
                val eventTitle = try {
                    val eventResp = client.from("events")
                        .select(columns = Columns.list("title")) {
                            filter { eq("id", eventId) }
                        }
                        .data
                    Json.parseToJsonElement(eventResp)
                        .jsonArray.firstOrNull()
                        ?.jsonObject?.get("title")?.jsonPrimitive?.content
                        ?: "New Event"
                } catch (_: Exception) { "New Event" }

                AssignmentNotifier.notify(applicationContext, eventTitle)

                // Track the newest timestamp we've notified about
                if (newestTimestamp == null || isNewer(createdAt, newestTimestamp)) {
                    newestTimestamp = createdAt
                }
            }

            // Save the newest seen timestamp so we don't re-notify
            newestTimestamp?.let { SessionStore.saveLastSeen(applicationContext, it) }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    /** Returns true if [a] is strictly after [b] (ISO-8601 comparison). */
    private fun isNewer(a: String, b: String): Boolean = try {
        Instant.parse(a).isAfter(Instant.parse(b))
    } catch (_: Exception) { false }

    companion object {
        const val TAG = "assignment_poll"

        /** Schedule a periodic poll every 15 minutes. Safe to call multiple times. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AssignmentPollWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,   // don't reset timer if already scheduled
                request
            )
        }

        /** Cancel polling — called on logout. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }
    }
}
