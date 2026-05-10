package com.clubeve.cc.ui.student

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Event
import com.clubeve.cc.models.Registration
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

/** A registration row joined with its event details — used only in the student flow. */
@Serializable
data class StudentRegistration(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("qr_token") val qrToken: String? = null,
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("checked_in_at") val checkedInAt: String? = null,
    @SerialName("registered_at") val registeredAt: String? = null
)

data class StudentHomeUiState(
    val registrations: List<StudentRegistration> = emptyList(),
    val events: Map<String, Event> = emptyMap(),   // eventId → Event
    val isLoading: Boolean = false,
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
                val userId = SessionManager.currentUserId.ifBlank {
                    client.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in.")
                }

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
                    _state.update { it.copy(registrations = emptyList(), events = emptyMap(), isLoading = false) }
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
                    it.copy(
                        registrations = registrations,
                        events = events,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load events.") }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            try { client.auth.signOut() } catch (_: Exception) {}
            SessionManager.clear()
            onDone()
        }
    }
}
