package com.clubeve.cc.ui.student

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Event
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A registration row — used in the student events list. */
@Serializable
data class StudentRegistration(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("qr_token") val qrToken: String? = null,
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
    @SerialName("registered_at") val registeredAt: String? = null
)

/** Minimal feedback row — only need to know if one exists for this student+event. */
@Serializable
private data class FeedbackRow(
    val id: String,
    @SerialName("event_id") val eventId: String
)

/**
 * Attendance status for a single event.
 * PRESENT  = checked in AND feedback submitted
 * ATTENDED = checked in but no feedback yet
 */
enum class AttendanceStatus { ATTENDED, PRESENT }

data class AttendanceRecord(
    val eventId: String,
    val eventTitle: String,
    val eventDate: String,   // ISO timestamp string
    val clubName: String,
    val checkedInAt: String?,
    val status: AttendanceStatus
)

data class StudentHomeUiState(
    val registrations: List<StudentRegistration> = emptyList(),
    val events: Map<String, Event> = emptyMap(),        // eventId → Event
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isAttendanceLoading: Boolean = false,
    val error: String? = null
)

class StudentHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(StudentHomeUiState())
    val state: StateFlow<StudentHomeUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = resolveUserId()

                // Fetch all registrations for this student
                val registrations = client.from("registrations")
                    .select(columns = Columns.list(
                        "id", "event_id", "qr_token",
                        "checked_in", "checked_in_at", "registered_at"
                    )) {
                        filter { eq("student_id", userId) }
                    }
                    .decodeList<StudentRegistration>()

                if (registrations.isEmpty()) {
                    _state.update {
                        it.copy(registrations = emptyList(), events = emptyMap(), isLoading = false)
                    }
                    return@launch
                }

                // Fetch the corresponding events
                val eventIds = registrations.map { it.eventId }.distinct()
                val events = client.from("events")
                    .select(columns = Columns.list(
                        "id", "title", "description", "club_name",
                        "location", "event_date", "status", "banner_url", "max_capacity"
                    )) {
                        filter { isIn("id", eventIds) }
                    }
                    .decodeList<Event>()
                    .associateBy { it.id }

                _state.update {
                    it.copy(registrations = registrations, events = events, isLoading = false)
                }

                // Also refresh attendance in the background
                loadAttendance(userId, registrations, events)

            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load events.")
                }
            }
        }
    }

    fun loadAttendance() {
        viewModelScope.launch {
            _state.update { it.copy(isAttendanceLoading = true) }
            try {
                val userId = resolveUserId()
                val regs = _state.value.registrations
                val evts = _state.value.events
                loadAttendance(userId, regs, evts)
            } catch (e: Exception) {
                _state.update { it.copy(isAttendanceLoading = false) }
            }
        }
    }

    private suspend fun loadAttendance(
        userId: String,
        registrations: List<StudentRegistration>,
        events: Map<String, Event>
    ) {
        _state.update { it.copy(isAttendanceLoading = true) }
        try {
            // Only care about checked-in registrations
            val checkedIn = registrations.filter { it.checkedIn }
            if (checkedIn.isEmpty()) {
                _state.update { it.copy(attendanceRecords = emptyList(), isAttendanceLoading = false) }
                return
            }

            val checkedInEventIds = checkedIn.map { it.eventId }.distinct()

            // Fetch feedback rows for this student across those events
            val feedbacks = client.from("feedbacks")
                .select(columns = Columns.list("id", "event_id")) {
                    filter {
                        eq("student_id", userId)
                        isIn("event_id", checkedInEventIds)
                    }
                }
                .decodeList<FeedbackRow>()

            val feedbackEventIds = feedbacks.map { it.eventId }.toSet()

            val records = checkedIn.mapNotNull { reg ->
                val event = events[reg.eventId] ?: return@mapNotNull null
                AttendanceRecord(
                    eventId = reg.eventId,
                    eventTitle = event.title,
                    eventDate = event.eventDate,
                    clubName = event.clubName,
                    checkedInAt = reg.checkedInAt,
                    status = if (reg.eventId in feedbackEventIds)
                        AttendanceStatus.PRESENT
                    else
                        AttendanceStatus.ATTENDED
                )
            }.sortedByDescending { it.eventDate }

            _state.update { it.copy(attendanceRecords = records, isAttendanceLoading = false) }
        } catch (e: Exception) {
            _state.update { it.copy(isAttendanceLoading = false) }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { client.auth.signOut() } catch (_: Exception) {}
            SessionManager.clear()
            onDone()
        }
    }

    private suspend fun resolveUserId(): String =
        SessionManager.currentUserId.ifBlank {
            client.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in.")
        }
}
