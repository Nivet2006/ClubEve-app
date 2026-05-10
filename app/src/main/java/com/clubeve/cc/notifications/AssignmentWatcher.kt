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
 * Assignment notifications are currently DISABLED.
 * Subscribes to Supabase Realtime on the `pr_event_assignments` table.
 * When a new row is inserted for the current PR user, fires a local notification.
 */
object AssignmentWatcher {

    private var realtimeChannel: RealtimeChannel? = null
    private var job: Job? = null

    fun start(context: Context, prUserId: String, scope: CoroutineScope) {
        // Notifications disabled — do nothing
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
