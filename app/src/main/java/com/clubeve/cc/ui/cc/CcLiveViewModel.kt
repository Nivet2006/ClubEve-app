package com.clubeve.cc.ui.cc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonPrimitive

// ── Status enum ───────────────────────────────────────────────────────────────
enum class LiveAttendeeStatus {
    REGISTERED,  // signed up, not yet scanned
    SCANNED,     // QR scanned (checked_in = true) but no feedback
    PRESENT      // checked_in = true AND feedback submitted
}

data class LiveAttendee(
    val registrationId: String,
    val studentId: String,
    val fullName: String,
    val usn: String,
    val checkedIn: Boolean,
    val hasFeedback: Boolean
) {
    val status: LiveAttendeeStatus get() = when {
        checkedIn && hasFeedback -> LiveAttendeeStatus.PRESENT
        checkedIn                -> LiveAttendeeStatus.SCANNED
        else                     -> LiveAttendeeStatus.REGISTERED
    }
}

data class CcLiveUiState(
    val attendees: List<LiveAttendee> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventTitle: String = ""
)

// ── Minimal DTOs for Supabase queries ─────────────────────────────────────────
@Serializable
private data class RegRow(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("checked_in") val checkedIn: Boolean = false
)

@Serializable
private data class ProfileRow(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val usn: String = ""
)

@Serializable
private data class FeedbackRow(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("event_id") val eventId: String
)

@Serializable
private data class EventTitleRow(val id: String, val title: String)

class CcLiveViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(CcLiveUiState())
    val state: StateFlow<CcLiveUiState> = _state.asStateFlow()

    private var currentEventId: String = ""
    private var realtimeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    fun load(eventId: String) {
        currentEventId = eventId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch event title
                val eventTitle = client.from("events")
                    .select(columns = Columns.list("id", "title")) {
                        filter { eq("id", eventId) }
                    }
                    .decodeSingleOrNull<EventTitleRow>()?.title ?: ""

                fetchAndRebuild(eventId, eventTitle)
                subscribeRealtime(eventId, eventTitle)
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load.") }
            }
        }
    }

    private suspend fun fetchAndRebuild(eventId: String, eventTitle: String) {
        // 1. All registrations for this event
        val regs = client.from("registrations")
            .select(columns = Columns.list("id", "student_id", "checked_in")) {
                filter { eq("event_id", eventId) }
            }
            .decodeList<RegRow>()

        if (regs.isEmpty()) {
            _state.update { it.copy(attendees = emptyList(), isLoading = false, eventTitle = eventTitle) }
            return
        }

        // 2. Student profiles
        val studentIds = regs.map { it.studentId }.distinct()
        val profiles = client.from("profiles")
            .select(columns = Columns.list("id", "full_name", "usn")) {
                filter { isIn("id", studentIds) }
            }
            .decodeList<ProfileRow>()
            .associateBy { it.id }

        // 3. Feedback rows for this event
        val feedbackStudentIds = client.from("feedbacks")
            .select(columns = Columns.list("id", "student_id", "event_id")) {
                filter {
                    eq("event_id", eventId)
                    isIn("student_id", studentIds)
                }
            }
            .decodeList<FeedbackRow>()
            .map { it.studentId }
            .toSet()

        // 4. Build attendee list sorted: PRESENT → SCANNED → REGISTERED, then alpha
        val attendees = regs.map { reg ->
            val profile = profiles[reg.studentId]
            LiveAttendee(
                registrationId = reg.id,
                studentId = reg.studentId,
                fullName = profile?.fullName ?: "Unknown",
                usn = profile?.usn ?: "",
                checkedIn = reg.checkedIn,
                hasFeedback = reg.studentId in feedbackStudentIds
            )
        }.sortedWith(
            compareByDescending<LiveAttendee> { it.status.ordinal }
                .thenBy { it.fullName }
        )

        _state.update { it.copy(attendees = attendees, isLoading = false, eventTitle = eventTitle) }
    }

    private fun subscribeRealtime(eventId: String, eventTitle: String) {
        viewModelScope.launch {
            try {
                val ch = client.channel("cc_live:$eventId")
                realtimeChannel = ch

                // Listen for registration changes (new registrations + check-ins)
                launch {
                    ch.postgresChangeFlow<PostgresAction>("public") {
                        table = "registrations"
                        filter("event_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, eventId)
                    }.collect { action ->
                        when (action) {
                            is PostgresAction.Insert, is PostgresAction.Update -> {
                                // Refetch the full list to keep it consistent
                                fetchAndRebuild(eventId, eventTitle)
                            }
                            else -> {}
                        }
                    }
                }

                // Listen for new feedback submissions
                launch {
                    ch.postgresChangeFlow<PostgresAction>("public") {
                        table = "feedbacks"
                        filter("event_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, eventId)
                    }.collect { action ->
                        if (action is PostgresAction.Insert) {
                            // Get the student_id from the new row and flip their hasFeedback
                            val studentId = action.record["student_id"]?.jsonPrimitive?.content
                            if (studentId != null) {
                                _state.update { s ->
                                    val updated = s.attendees.map { a ->
                                        if (a.studentId == studentId) a.copy(hasFeedback = true) else a
                                    }.sortedWith(
                                        compareByDescending<LiveAttendee> { it.status.ordinal }
                                            .thenBy { it.fullName }
                                    )
                                    s.copy(attendees = updated)
                                }
                            }
                        }
                    }
                }

                ch.subscribe()
            } catch (_: Exception) {
                // Realtime is best-effort — list still works without it
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            try {
                realtimeChannel?.let {
                    it.unsubscribe()
                    client.realtime.removeChannel(it)
                }
            } catch (_: Exception) {}
        }
    }
}
