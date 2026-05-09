package com.clubeve.cc.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Attendee
import com.clubeve.cc.models.IdOnly
import com.clubeve.cc.models.Profile
import com.clubeve.cc.models.Registration
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AttendeeFilter { ALL, PRESENT, ABSENT }

data class AttendeeListUiState(
    val attendees: List<Attendee> = emptyList(),
    val filter: AttendeeFilter = AttendeeFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventTitle: String = ""
)

class AttendeeListViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(AttendeeListUiState())
    val state: StateFlow<AttendeeListUiState> = _state.asStateFlow()

    private var currentEventId: String = ""
    private var realtimeChannel: RealtimeChannel? = null

    val filteredAttendees: StateFlow<List<Attendee>>
        get() = _filteredFlow

    private val _filteredFlow = MutableStateFlow<List<Attendee>>(emptyList())

    init {
        // Recompute filtered list whenever state changes
        _state.onEach { s ->
            _filteredFlow.value = s.attendees
                .filter { a ->
                    when (s.filter) {
                        AttendeeFilter.ALL -> true
                        AttendeeFilter.PRESENT -> a.checkedIn
                        AttendeeFilter.ABSENT -> !a.checkedIn
                    }
                }
                .filter { a ->
                    s.searchQuery.isBlank() ||
                    a.fullName.contains(s.searchQuery, ignoreCase = true) ||
                    a.usn.contains(s.searchQuery, ignoreCase = true)
                }
        }.launchIn(viewModelScope)
    }

    fun init(eventId: String, eventTitle: String = "") {
        if (currentEventId == eventId) return
        currentEventId = eventId
        _state.update { it.copy(eventTitle = eventTitle) }
        loadAttendees()
        subscribeRealtime(eventId)
    }

    fun setFilter(f: AttendeeFilter) = _state.update { it.copy(filter = f) }
    fun setSearch(q: String) = _state.update { it.copy(searchQuery = q) }

    fun refresh() {
        loadAttendees()
    }

    private fun loadAttendees() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = SessionManager.currentUserId
                // Validate assignment
                val assigned = client.from("pr_event_assignments")
                    .select(columns = Columns.list("id")) {
                        filter {
                            eq("pr_id", userId)
                            eq("event_id", currentEventId)
                        }
                    }
                    .decodeList<IdOnly>()

                if (assigned.isEmpty()) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Access denied: you are not assigned to this event."
                        )
                    }
                    return@launch
                }

                // Fetch registrations
                val registrations = client.from("registrations")
                    .select(columns = Columns.list("id", "checked_in", "checked_in_at", "registered_at", "student_id")) {
                        filter { eq("event_id", currentEventId) }
                        order("registered_at", Order.ASCENDING)
                    }
                    .decodeList<Registration>()

                if (registrations.isEmpty()) {
                    _state.update { it.copy(attendees = emptyList(), isLoading = false) }
                    return@launch
                }

                // Bulk fetch profiles
                val studentIds = registrations.map { it.studentId }
                val profiles = client.from("profiles")
                    .select(columns = Columns.list("id", "full_name", "usn", "department", "semester", "year")) {
                        filter { isIn("id", studentIds) }
                    }
                    .decodeList<Profile>()

                val profileMap = profiles.associateBy { it.id }

                val attendees = registrations.map { reg ->
                    val profile = profileMap[reg.studentId]
                    Attendee(
                        id = reg.id,
                        fullName = profile?.fullName ?: "Unknown",
                        usn = profile?.usn ?: "Unknown",
                        department = profile?.department ?: "Unknown",
                        semester = profile?.semester ?: 0,
                        year = profile?.year ?: 0,
                        checkedIn = reg.checkedIn,
                        checkedInAt = reg.checkedInAt,
                        registeredAt = reg.registeredAt
                    )
                }

                _state.update { it.copy(attendees = attendees, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load attendees.") }
            }
        }
    }

    private fun subscribeRealtime(eventId: String) {
        viewModelScope.launch {
            try {
                val channel = client.channel("attendance-$eventId")
                realtimeChannel = channel

                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "registrations"
                    filter("event_id", FilterOperator.EQ, eventId)
                }

                channel.subscribe()

                changes.collect { action ->
                    when (action) {
                        is PostgresAction.Update -> loadAttendees()
                        is PostgresAction.Insert -> loadAttendees()
                        else -> {}
                    }
                }
            } catch (_: Exception) {
                // Realtime is best-effort; silently ignore subscription failures
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try { realtimeChannel?.unsubscribe() } catch (_: Exception) {}
        }
    }
}
