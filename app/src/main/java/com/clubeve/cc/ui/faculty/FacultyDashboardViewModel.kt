package com.clubeve.cc.ui.faculty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.ApprovalStatus
import com.clubeve.cc.models.CcEvent
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FacultyDashboardUiState(
    val pendingEvents: List<CcEvent> = emptyList(),
    val verifiedEvents: List<CcEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FacultyDashboardViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(FacultyDashboardUiState())
    val state: StateFlow<FacultyDashboardUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val role = SessionManager.currentProfile?.role
                val dept = SessionManager.currentProfile?.department

                val pendingStatus = when (role) {
                    "teacher" -> ApprovalStatus.PENDING_TEACHER
                    "hod"     -> ApprovalStatus.PENDING_HOD
                    else      -> null  // admin/manager: show all pending
                }

                // Pending events — awaiting this role's action
                val pending = if (pendingStatus != null && !dept.isNullOrBlank()) {
                    client.from("events").select {
                        filter {
                            eq("approval_status", pendingStatus)
                            eq("targeted_department", dept)
                        }
                        order("created_at", Order.ASCENDING)
                    }.decodeList<CcEvent>()
                } else {
                    // admin / manager: show all pending_teacher + pending_hod
                    client.from("events").select {
                        filter {
                            isIn("approval_status", listOf(
                                ApprovalStatus.PENDING_TEACHER,
                                ApprovalStatus.PENDING_HOD
                            ))
                        }
                        order("created_at", Order.ASCENDING)
                    }.decodeList<CcEvent>()
                }

                // Verified / live events — already passed through this role
                val verified = if (!dept.isNullOrBlank() && role in listOf("teacher", "hod")) {
                    client.from("events").select {
                        filter {
                            isIn("approval_status", listOf(
                                ApprovalStatus.PENDING_HOD,
                                ApprovalStatus.APPROVED
                            ))
                            eq("targeted_department", dept)
                        }
                        order("event_date", Order.DESCENDING)
                    }.decodeList<CcEvent>()
                } else {
                    client.from("events").select {
                        filter {
                            isIn("approval_status", listOf(
                                ApprovalStatus.PENDING_HOD,
                                ApprovalStatus.APPROVED
                            ))
                        }
                        order("event_date", Order.DESCENDING)
                    }.decodeList<CcEvent>()
                }

                _state.update {
                    it.copy(
                        pendingEvents = pending,
                        verifiedEvents = verified,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load events.")
                }
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
