package com.clubeve.cc.ui.cc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.CcReport
import com.clubeve.cc.models.ReportContent
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class CcReportUiState(
    val summary: String = "",
    val outcomes: List<String> = listOf(""),   // at least one empty row
    val photos: List<String> = listOf(""),
    val existingStatus: String? = null,        // null = no report yet
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val snackbar: String? = null
)

class CcReportViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(CcReportUiState())
    val state: StateFlow<CcReportUiState> = _state.asStateFlow()

    fun load(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val report = client.from("reports")
                    .select { filter { eq("event_id", eventId) } }
                    .decodeSingleOrNull<CcReport>()

                if (report != null) {
                    _state.update {
                        it.copy(
                            summary = report.content.summary,
                            outcomes = report.content.outcomes.ifEmpty { listOf("") },
                            photos = report.content.photos.ifEmpty { listOf("") },
                            existingStatus = report.status,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSummaryChange(value: String) = _state.update { it.copy(summary = value) }

    fun onOutcomeChange(index: Int, value: String) {
        _state.update {
            val list = it.outcomes.toMutableList()
            list[index] = value
            it.copy(outcomes = list)
        }
    }

    fun addOutcome() = _state.update { it.copy(outcomes = it.outcomes + "") }

    fun removeOutcome(index: Int) {
        _state.update {
            if (it.outcomes.size <= 1) return@update it
            it.copy(outcomes = it.outcomes.toMutableList().also { l -> l.removeAt(index) })
        }
    }

    fun onPhotoChange(index: Int, value: String) {
        _state.update {
            val list = it.photos.toMutableList()
            list[index] = value
            it.copy(photos = list)
        }
    }

    fun addPhoto() = _state.update { it.copy(photos = it.photos + "") }

    fun removePhoto(index: Int) {
        _state.update {
            if (it.photos.size <= 1) return@update it
            it.copy(photos = it.photos.toMutableList().also { l -> l.removeAt(index) })
        }
    }

    fun save(eventId: String, submit: Boolean) {
        val s = _state.value
        if (s.summary.isBlank()) {
            _state.update { it.copy(snackbar = "Summary cannot be empty.") }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val content = ReportContent(
                    summary = s.summary.trim(),
                    outcomes = s.outcomes.filter { it.isNotBlank() },
                    photos = s.photos.filter { it.isNotBlank() }
                )
                val newStatus = if (submit) "pending_pr" else "draft"

                client.from("reports").upsert(
                    CcReport(
                        eventId = eventId,
                        content = content,
                        status = newStatus
                    )
                ) {
                    onConflict = "event_id"
                }

                _state.update {
                    it.copy(
                        isSaving = false,
                        existingStatus = newStatus,
                        snackbar = if (submit) "Report submitted for PR audit" else "Draft saved"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, snackbar = "Save failed: ${e.message}") }
            }
        }
    }

    fun clearSnackbar() = _state.update { it.copy(snackbar = null) }
}
