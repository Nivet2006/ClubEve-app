package com.clubeve.cc.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Event
import com.clubeve.cc.models.PrAssignment
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // Called from HomeScreen's LaunchedEffect — guaranteed after session is set
    fun loadEvents() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Resolve userId — SessionManager is always populated before navigation
                val userId = SessionManager.currentUserId.ifBlank {
                    client.auth.currentUserOrNull()?.id
                        ?: throw Exception("Not logged in. Please sign in again.")
                }

                // Also keep SessionManager in sync if it was empty (e.g. app restart)
                if (SessionManager.currentUserId.isBlank()) {
                    SessionManager.currentUserId = userId
                }

                // Step 1: Get assigned event IDs
                val assignments = client.from("pr_event_assignments")
                    .select(columns = Columns.list("event_id")) {
                        filter { eq("pr_id", userId) }
                    }
                    .decodeList<PrAssignment>()

                val eventIds = assignments.map { it.eventId }
                if (eventIds.isEmpty()) {
                    _state.update { it.copy(events = emptyList(), isLoading = false) }
                    return@launch
                }

                // Step 2: Fetch events
                val events = client.from("events")
                    .select {
                        filter { isIn("id", eventIds) }
                        order("event_date", Order.DESCENDING)
                    }
                    .decodeList<Event>()
                    .toMutableList()

                // Step 3: Fetch counts in parallel
                val countJobs = events.mapIndexed { index, event ->
                    async {
                        try {
                            val regCount = client.from("registrations")
                                .select(columns = Columns.list("id")) {
                                    filter { eq("event_id", event.id) }
                                }
                                .decodeList<com.clubeve.cc.models.IdOnly>()
                                .size.toLong()

                            val attCount = client.from("registrations")
                                .select(columns = Columns.list("id")) {
                                    filter {
                                        eq("event_id", event.id)
                                        eq("checked_in", true)
                                    }
                                }
                                .decodeList<com.clubeve.cc.models.IdOnly>()
                                .size.toLong()

                            index to Pair(regCount, attCount)
                        } catch (_: Exception) {
                            index to Pair(0L, 0L)
                        }
                    }
                }

                countJobs.awaitAll().forEach { (index, counts) ->
                    events[index] = events[index].copy(
                        registrationCount = counts.first,
                        attendanceCount = counts.second
                    )
                }

                _state.update { it.copy(events = events, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load events.") }
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            try { client.auth.signOut() } catch (_: Exception) {}
            SessionManager.clear()
            onLoggedOut()
        }
    }
}
