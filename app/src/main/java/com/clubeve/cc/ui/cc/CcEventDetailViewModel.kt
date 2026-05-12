package com.clubeve.cc.ui.cc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.CcEvent
import com.clubeve.cc.models.CcReport
import com.clubeve.cc.models.IdOnly
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CcEventDetailUiState(
    val event: CcEvent? = null,
    val report: CcReport? = null,
    val registrationCount: Int = 0,
    val isLoading: Boolean = false,
    val isTogglingFeedback: Boolean = false,
    val isSubmitting: Boolean = false,
    val snackbar: String? = null,
    val error: String? = null
)

class CcEventDetailViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(CcEventDetailUiState())
    val state: StateFlow<CcEventDetailUiState> = _state.asStateFlow()

    fun load(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch event
                val event = client.from("events")
                    .select { filter { eq("id", eventId) } }
                    .decodeSingleOrNull<CcEvent>()

                // Fetch registration count
                val regCount = client.from("registrations")
                    .select(columns = Columns.list("id")) {
                        filter { eq("event_id", eventId) }
                    }
                    .decodeList<IdOnly>()
                    .size

                // Fetch report if exists
                val report = try {
                    client.from("reports")
                        .select { filter { eq("event_id", eventId) } }
                        .decodeSingleOrNull<CcReport>()
                } catch (_: Exception) { null }

                _state.update {
                    it.copy(
                        event = event,
                        report = report,
                        registrationCount = regCount,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load event.") }
            }
        }
    }

    /** Toggle feedback_open on the event. */
    fun toggleFeedback(eventId: String, currentValue: Boolean) {
        val newValue = !currentValue
        // Optimistic update
        _state.update { it.copy(
            event = it.event?.copy(feedbackOpen = newValue),
            isTogglingFeedback = true
        ) }
        viewModelScope.launch {
            try {
                client.from("events").update({
                    set("feedback_open", newValue)
                }) {
                    filter { eq("id", eventId) }
                }
                _state.update { it.copy(
                    isTogglingFeedback = false,
                    snackbar = if (newValue) "Feedback collection enabled" else "Feedback collection disabled"
                ) }
            } catch (e: Exception) {
                // Revert on failure
                _state.update { it.copy(
                    event = it.event?.copy(feedbackOpen = currentValue),
                    isTogglingFeedback = false,
                    snackbar = "Failed to toggle feedback: ${e.message}"
                ) }
            }
        }
    }

    /** Submit the event draft for faculty review. */
    fun submitForReview(eventId: String) {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                client.from("events").update({
                    set("approval_status", "pending_teacher")
                }) {
                    filter {
                        eq("id", eventId)
                        eq("created_by", SessionManager.currentUserId)
                    }
                }
                _state.update { it.copy(
                    event = it.event?.copy(approvalStatus = "pending_teacher"),
                    isSubmitting = false,
                    snackbar = "Submitted for faculty review"
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isSubmitting = false,
                    snackbar = "Submission failed: ${e.message}"
                ) }
            }
        }
    }

    fun clearSnackbar() = _state.update { it.copy(snackbar = null) }
}
