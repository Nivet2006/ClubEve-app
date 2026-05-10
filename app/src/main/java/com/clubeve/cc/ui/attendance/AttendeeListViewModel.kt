package com.clubeve.cc.ui.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.local.AppDatabase
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AttendeeFilter { ALL, PRESENT, ABSENT, REGISTERED }

enum class SyncStatus { SYNCED, SYNCING, OFFLINE }

data class AttendeeListUiState(
    val attendees: List<Attendee> = emptyList(),
    val filter: AttendeeFilter = AttendeeFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventTitle: String = "",
    val isOffline: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.SYNCING,
    val lastUpdated: Long = 0L
)

class AttendeeListViewModel(application: Application) : AndroidViewModel(application) {

    private val client = SupabaseClientProvider.client
    private val db = AppDatabase.getInstance(application)

    private val _state = MutableStateFlow(AttendeeListUiState())
    val state: StateFlow<AttendeeListUiState> = _state.asStateFlow()

    private var currentEventId: String = ""
    private var realtimeChannel: RealtimeChannel? = null
    private var autoRefreshJob: Job? = null

    val filteredAttendees: StateFlow<List<Attendee>>
        get() = _filteredFlow

    private val _filteredFlow = MutableStateFlow<List<Attendee>>(emptyList())

    init {
        _state.onEach { s ->
            _filteredFlow.value = s.attendees
                .filter { a ->
                    when (s.filter) {
                        AttendeeFilter.ALL        -> true
                        AttendeeFilter.PRESENT    -> a.checkedIn
                        AttendeeFilter.ABSENT     -> !a.checkedIn
                        AttendeeFilter.REGISTERED -> true   // all registrants, same as ALL but labelled differently
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
        startAutoRefresh()
    }

    fun setFilter(f: AttendeeFilter) = _state.update { it.copy(filter = f) }
    fun setSearch(q: String) = _state.update { it.copy(searchQuery = q) }

    fun refresh() { loadAttendees() }

    /** Polls every 10 seconds regardless of online/offline state. */
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(10_000)
                loadAttendees(silent = true)
            }
        }
    }

    private fun loadAttendees(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.update { it.copy(isLoading = true, error = null) }
            _state.update { it.copy(syncStatus = SyncStatus.SYNCING) }

            try {
                val userId = SessionManager.currentUserId
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
                            syncStatus = SyncStatus.SYNCED,
                            error = "Access denied: you are not assigned to this event."
                        )
                    }
                    return@launch
                }

                val registrations = client.from("registrations")
                    .select(columns = Columns.list("id", "checked_in", "checked_in_at", "registered_at", "student_id")) {
                        filter { eq("event_id", currentEventId) }
                        order("registered_at", Order.ASCENDING)
                    }
                    .decodeList<Registration>()

                if (registrations.isEmpty()) {
                    _state.update {
                        it.copy(
                            attendees = emptyList(),
                            isLoading = false,
                            isOffline = false,
                            syncStatus = SyncStatus.SYNCED,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                    return@launch
                }

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

                _state.update {
                    it.copy(
                        attendees = attendees,
                        isLoading = false,
                        isOffline = false,
                        syncStatus = SyncStatus.SYNCED,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                subscribeRealtime(currentEventId)

            } catch (e: Exception) {
                // Remote call failed — fall back to local cache and mark as offline
                loadAttendeesFromCache()
            }
        }
    }

    private suspend fun loadAttendeesFromCache() {
        try {
            val registrations = db.registrationDao().getByEvent(currentEventId)
            val profileMap = registrations
                .filter { it.studentId.isNotBlank() }
                .mapNotNull { reg -> db.profileDao().getById(reg.studentId)?.let { reg.studentId to it } }
                .toMap()

            val attendees = registrations.map { reg ->
                val profile = profileMap[reg.studentId]
                Attendee(
                    id = reg.id,
                    fullName = profile?.fullName ?: reg.studentName.ifBlank { "Unknown" },
                    usn = profile?.usn ?: reg.usn.ifBlank { "Unknown" },
                    department = profile?.department ?: "Unknown",
                    semester = profile?.semester ?: 0,
                    year = profile?.year ?: 0,
                    checkedIn = reg.isPresent,
                    checkedInAt = reg.checkedInAt,
                    registeredAt = reg.registeredAt
                )
            }

            _state.update {
                it.copy(
                    attendees = attendees,
                    isLoading = false,
                    isOffline = true,
                    syncStatus = SyncStatus.OFFLINE,
                    error = null,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    isOffline = true,
                    syncStatus = SyncStatus.OFFLINE,
                    error = "Failed to load cached data."
                )
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
                        is PostgresAction.Update -> loadAttendees(silent = true)
                        is PostgresAction.Insert -> loadAttendees(silent = true)
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
        autoRefreshJob?.cancel()
        viewModelScope.launch {
            try { realtimeChannel?.unsubscribe() } catch (_: Exception) {}
        }
    }
}
