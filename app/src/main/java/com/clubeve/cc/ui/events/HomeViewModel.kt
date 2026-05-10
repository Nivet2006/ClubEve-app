package com.clubeve.cc.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.local.AppDatabase
import com.clubeve.cc.data.local.entity.EventEntity
import com.clubeve.cc.data.local.entity.ProfileEntity
import com.clubeve.cc.data.local.entity.RegistrationEntity
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Event
import com.clubeve.cc.models.IdOnly
import com.clubeve.cc.models.PrAssignment
import com.clubeve.cc.models.Profile
import com.clubeve.cc.models.Registration
import com.clubeve.cc.sync.SyncManager
import com.clubeve.cc.utils.NetworkMonitor
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val client = SupabaseClientProvider.client
    private val db = AppDatabase.getInstance(application)
    private val networkMonitor = NetworkMonitor(application)

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Live count of offline check-ins waiting to be synced. */
    val pendingSyncCount: StateFlow<Int> = SyncManager.observePendingCount(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Called from HomeScreen's LaunchedEffect — guaranteed after session is set
    fun loadEvents() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = SessionManager.currentUserId.ifBlank {
                    client.auth.currentUserOrNull()?.id
                        ?: throw Exception("Not logged in. Please sign in again.")
                }
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
                                .decodeList<IdOnly>()
                                .size.toLong()

                            val attCount = client.from("registrations")
                                .select(columns = Columns.list("id")) {
                                    filter {
                                        eq("event_id", event.id)
                                        eq("checked_in", true)
                                    }
                                }
                                .decodeList<IdOnly>()
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

                // Step 4: Cache everything locally for offline use
                cacheEventsAndRegistrations(events)

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load events.") }
            }
        }
    }

    private suspend fun cacheEventsAndRegistrations(events: List<Event>) {
        try {
            // Cache events
            db.eventDao().upsert(events.map { e ->
                EventEntity(
                    id = e.id,
                    title = e.title,
                    description = e.description ?: "",
                    date = runCatching {
                        java.time.ZonedDateTime.parse(e.eventDate).toInstant().toEpochMilli()
                    }.getOrDefault(0L),
                    venue = e.location ?: "",
                    status = e.status ?: "",
                    maxCapacity = e.maxCapacity ?: 0,
                    registeredCount = e.registrationCount.toInt()
                )
            })

            // For each event, fetch and cache all registrations + student profiles
            for (event in events) {
                val registrations = client.from("registrations")
                    .select(
                        columns = Columns.list(
                            "id", "event_id", "student_id", "qr_token",
                            "checked_in", "checked_in_at", "registered_at"
                        )
                    ) {
                        filter { eq("event_id", event.id) }
                    }
                    .decodeList<Registration>()

                // Upsert registrations — preserve pendingSync flag for any locally-modified rows
                val existingPending = db.registrationDao().getPendingSync().map { it.id }.toSet()
                db.registrationDao().upsert(registrations.map { r ->
                    RegistrationEntity(
                        id = r.id,
                        eventId = r.eventId,
                        studentId = r.studentId,
                        qrToken = r.qrToken,
                        isPresent = r.checkedIn,
                        checkedInAt = r.checkedInAt,
                        registeredAt = r.registeredAt,
                        isSynced = true,
                        // Don't overwrite a pending local check-in with stale remote data
                        pendingSync = r.id in existingPending
                    )
                })

                // Fetch and cache student profiles
                val studentIds = registrations.map { it.studentId }.filter { it.isNotBlank() }
                if (studentIds.isNotEmpty()) {
                    val profiles = client.from("profiles")
                        .select(
                            columns = Columns.list(
                                "id", "full_name", "usn", "department", "semester", "year", "role"
                            )
                        ) {
                            filter { isIn("id", studentIds) }
                        }
                        .decodeList<Profile>()

                    db.profileDao().upsert(profiles.map { p ->
                        ProfileEntity(
                            id = p.id,
                            fullName = p.fullName,
                            usn = p.usn,
                            department = p.department,
                            semester = p.semester,
                            year = p.year,
                            role = p.role
                        )
                    })
                }
            }
        } catch (_: Exception) {
            // Caching is best-effort; don't surface errors to the user
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
