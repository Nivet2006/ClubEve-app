package com.clubeve.cc.ui.cc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.SessionManager
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.ApprovalStatus
import com.clubeve.cc.models.CcEvent
import com.clubeve.cc.models.PipelineStats
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CcDashboardUiState(
    val events: List<CcEvent> = emptyList(),
    val stats: PipelineStats = PipelineStats(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CcDashboardViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(CcDashboardUiState())
    val state: StateFlow<CcDashboardUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = resolveUserId()

                val events = client.from("events")
                    .select {
                        filter { eq("created_by", userId) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<CcEvent>()

                val stats = PipelineStats(
                    drafts   = events.count { it.approvalStatus == ApprovalStatus.DRAFT },
                    pending  = events.count { it.approvalStatus in ApprovalStatus.PENDING_STATUSES },
                    approved = events.count { it.approvalStatus == ApprovalStatus.APPROVED },
                    rejected = events.count { it.approvalStatus == ApprovalStatus.REJECTED }
                )

                _state.update { it.copy(events = events, stats = stats, isLoading = false) }
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

    private suspend fun resolveUserId(): String =
        SessionManager.currentUserId.ifBlank {
            client.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in.")
        }
}
