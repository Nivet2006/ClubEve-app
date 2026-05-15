package com.clubeve.cc.ui.faculty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.ApprovalStatus
import com.clubeve.cc.models.CcEvent
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Local models ──────────────────────────────────────────────────────────────

@Serializable
data class PrAssignmentRow(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("pr_id") val prId: String,
    @SerialName("assigned_at") val assignedAt: String? = null
)

@Serializable
data class PrProfile(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val usn: String = "",
    val department: String = ""
)

/** A single rejection reason entry stored in the `rejection_data` JSON column. */
@Serializable
data class RejectionEntry(
    val field: String,
    val reason: String
)

// ── UI state ──────────────────────────────────────────────────────────────────

data class FacultyEventDetailUiState(
    val event: CcEvent? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    // PR assignment
    val assignedPrs: List<PrProfile> = emptyList(),
    val isPrLoading: Boolean = false,
    val searchResults: List<PrProfile> = emptyList(),
    val isSearching: Boolean = false,
    val isAssigning: Boolean = false,
    val snackbar: String? = null,
    val error: String? = null,
    // Navigate back after successful submit
    val navigateBack: Boolean = false
)

class FacultyEventDetailViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(FacultyEventDetailUiState())
    val state: StateFlow<FacultyEventDetailUiState> = _state.asStateFlow()

    fun load(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val event = client.from("events")
                    .select { filter { eq("id", eventId) } }
                    .decodeSingleOrNull<CcEvent>()

                _state.update { it.copy(event = event, isLoading = false) }

                // Load PR assignments if event is approved
                if (event?.approvalStatus == ApprovalStatus.APPROVED) {
                    loadAssignedPrs(eventId)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load event.")
                }
            }
        }
    }

    // ── Review decision ───────────────────────────────────────────────────────

    fun submitDecision(eventId: String, approve: Boolean, remarks: String) {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                val role = SessionManager.currentProfile?.role
                val newStatus = when {
                    approve && role == "teacher" -> ApprovalStatus.PENDING_HOD
                    approve                      -> ApprovalStatus.APPROVED  // hod, admin, manager
                    else                         -> ApprovalStatus.REJECTED
                }

                if (!approve) {
                    client.from("events").update({
                        set("approval_status", ApprovalStatus.REJECTED)
                        set("rejection_data", listOf(RejectionEntry(field = "General", reason = remarks)))
                    }) {
                        filter { eq("id", eventId) }
                    }
                } else {
                    client.from("events").update({
                        set("approval_status", newStatus)
                        set("rejection_data", emptyList<RejectionEntry>())
                    }) {
                        filter { eq("id", eventId) }
                    }
                }

                _state.update {
                    it.copy(
                        isSubmitting = false,
                        event = it.event?.copy(approvalStatus = newStatus),
                        snackbar = if (approve) "Event authorized" else "Event declined",
                        navigateBack = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        snackbar = "Failed to submit decision: ${e.message}"
                    )
                }
            }
        }
    }

    // ── PR assignment ─────────────────────────────────────────────────────────

    private fun loadAssignedPrs(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isPrLoading = true) }
            try {
                val assigned = client.from("pr_event_assignments")
                    .select(columns = Columns.list("id", "event_id", "pr_id", "assigned_at")) {
                        filter { eq("event_id", eventId) }
                    }.decodeList<PrAssignmentRow>()

                val prIds = assigned.map { it.prId }
                val prProfiles = if (prIds.isNotEmpty()) {
                    client.from("profiles")
                        .select(columns = Columns.list("id", "full_name", "usn", "department")) {
                            filter { isIn("id", prIds) }
                        }.decodeList<PrProfile>()
                } else {
                    emptyList()
                }

                _state.update { it.copy(assignedPrs = prProfiles, isPrLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isPrLoading = false, snackbar = "Failed to load PR assignments: ${e.message}")
                }
            }
        }
    }

    fun searchPrs(query: String) {
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }
            try {
                val results = client.from("profiles")
                    .select(columns = Columns.list("id", "full_name", "usn", "department")) {
                        filter {
                            eq("role", "pr")
                            or {
                                ilike("full_name", "%$query%")
                                ilike("usn", "%$query%")
                            }
                        }
                    }.decodeList<PrProfile>()

                // Exclude already-assigned PRs
                val assignedIds = _state.value.assignedPrs.map { it.id }.toSet()
                _state.update {
                    it.copy(
                        searchResults = results.filter { pr -> pr.id !in assignedIds },
                        isSearching = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSearching = false, snackbar = "Search failed: ${e.message}")
                }
            }
        }
    }

    fun assignPr(eventId: String, prId: String) {
        if (_state.value.assignedPrs.size >= 2) {
            _state.update { it.copy(snackbar = "Maximum 2 PR officers per event") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isAssigning = true) }
            try {
                client.from("pr_event_assignments").insert(
                    mapOf("event_id" to eventId, "pr_id" to prId)
                )
                // Reload assignments
                loadAssignedPrs(eventId)
                _state.update {
                    it.copy(
                        isAssigning = false,
                        searchResults = emptyList(),
                        snackbar = "PR officer assigned"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isAssigning = false, snackbar = "Failed to assign PR: ${e.message}")
                }
            }
        }
    }

    fun removePr(eventId: String, prId: String) {
        viewModelScope.launch {
            try {
                client.from("pr_event_assignments").delete {
                    filter {
                        eq("event_id", eventId)
                        eq("pr_id", prId)
                    }
                }
                _state.update {
                    it.copy(
                        assignedPrs = it.assignedPrs.filter { pr -> pr.id != prId },
                        snackbar = "PR officer removed"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(snackbar = "Failed to remove PR: ${e.message}") }
            }
        }
    }

    fun clearSearchResults() = _state.update { it.copy(searchResults = emptyList()) }

    fun clearSnackbar() = _state.update { it.copy(snackbar = null) }

    fun consumeNavigateBack() = _state.update { it.copy(navigateBack = false) }
}
