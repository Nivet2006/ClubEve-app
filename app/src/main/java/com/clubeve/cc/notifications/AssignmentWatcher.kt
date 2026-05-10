package com.clubeve.cc.notifications

import android.content.Context
import com.clubeve.cc.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Subscribes to Supabase Realtime on the `pr_event_assignments` table.
 * When a new row is inserted for the current PR user, fires a local notification.
 */
object AssignmentWatcher {

    private var realtimeChannel: RealtimeChannel? = null
    private var job: Job? = null

    fun start(context: Context, prUserId: String, scope: CoroutineScope) {
        if (job?.isActive == true) return   // already watching

        job = scope.launch(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val ch = client.channel("assignment-watch-$prUserId")
                realtimeChannel = ch

                val flow = ch.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "pr_event_assignments"
                }

                ch.subscribe()

                flow.collect { action ->
                    if (action !is PostgresAction.Insert) return@collect

                    val record = action.record
                    val assignedPrId = record["pr_id"]?.jsonPrimitive?.content ?: return@collect
                    if (assignedPrId != prUserId) return@collect

                    val eventId = record["event_id"]?.jsonPrimitive?.content ?: return@collect

                    // Fetch event title for the notification
                    val eventTitle = try {
                        val result = client.from("events")
                            .select(columns = Columns.list("title")) {
                                filter { eq("id", eventId) }
                            }
                            .data
                        kotlinx.serialization.json.Json
                            .parseToJsonElement(result)
                            .jsonObject["title"]?.jsonPrimitive?.content
                            ?: "New Event"
                    } catch (_: Exception) { "New Event" }

                    AssignmentNotifier.notify(context, eventTitle)
                }

            } catch (_: Exception) {
                // Realtime unavailable — silently skip
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        CoroutineScope(Dispatchers.IO).launch {
            try { realtimeChannel?.unsubscribe() } catch (_: Exception) {}
        }
        realtimeChannel = null
    }
}
