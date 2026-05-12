package com.clubeve.cc.ui.scanner

import com.clubeve.cc.data.remote.SupabaseClientProvider
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.presenceDataFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Manages a Supabase Realtime Presence channel for a single event.
 *
 * Each PR officer broadcasts their own [PrPresenceState] (userId + scanCount).
 * [peerScans] emits the latest map of userId → scanCount for all OTHER connected PRs.
 *
 * If a phone disconnects, Supabase removes that user's presence automatically
 * after the heartbeat timeout — zero disruption to the remaining PR.
 */
object PrPresenceManager {

    @Serializable
    data class PrPresenceState(
        val userId: String,
        val scanCount: Int
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = SupabaseClientProvider.client

    private var channel: RealtimeChannel? = null
    private var currentEventId: String? = null
    private var myUserId: String = ""
    private var myScanCount: Int = 0

    /**
     * userId → scanCount for every OTHER PR currently online for this event.
     * Empty when offline or no peers are present.
     */
    private val _peerScans = MutableStateFlow<Map<String, Int>>(emptyMap())
    val peerScans: StateFlow<Map<String, Int>> = _peerScans.asStateFlow()

    /**
     * Join the presence channel for [eventId].
     * Safe to call multiple times — re-joins only if the event changed.
     */
    fun join(eventId: String, userId: String) {
        if (currentEventId == eventId && channel != null) return
        leave()

        currentEventId = eventId
        myUserId = userId
        myScanCount = 0
        _peerScans.value = emptyMap()

        scope.launch {
            try {
                val ch = client.channel("presence:event:$eventId")
                channel = ch

                // presenceDataFlow must be set up BEFORE subscribe()
                // It returns Flow<List<PrPresenceState>> — the full current snapshot on every change
                launch {
                    ch.presenceDataFlow<PrPresenceState>().collect { allPresences ->
                        // Filter out our own entry and build the peer map
                        val peers = allPresences
                            .filter { it.userId != myUserId }
                            .associate { it.userId to it.scanCount }
                        _peerScans.value = peers
                    }
                }

                // Subscribe (connects Realtime WebSocket if not already connected)
                ch.subscribe(blockUntilSubscribed = true)

                // Broadcast our initial state now that we're subscribed
                ch.track(PrPresenceState(userId = myUserId, scanCount = myScanCount))

            } catch (_: Exception) {
                // Presence is best-effort — scanner works fine without it
            }
        }
    }

    /** Increment our local scan count and broadcast the update to peers. */
    fun incrementScan() {
        myScanCount++
        scope.launch {
            try {
                channel?.track(PrPresenceState(userId = myUserId, scanCount = myScanCount))
            } catch (_: Exception) {}
        }
    }

    /** Leave the current channel and reset all state. */
    fun leave() {
        val ch = channel ?: return
        channel = null
        currentEventId = null
        myScanCount = 0
        _peerScans.value = emptyMap()

        scope.launch {
            try {
                ch.unsubscribe()
                client.realtime.removeChannel(ch)
            } catch (_: Exception) {}
        }
    }
}
