package com.clubeve.cc.ui.cc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.FeedbackQuestion
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

// ── Question types matching the web component ─────────────────────────────────
enum class QuestionType(val displayName: String) {
    SHORT_TEXT("Short Text"),
    LONG_TEXT("Long Text"),
    RATING("Rating (1–5)"),
    MULTIPLE_CHOICE("Multiple Choice"),
    CHECKBOXES("Checkboxes"),
    BOOLEAN("Yes / No"),
    DROPDOWN("Dropdown");

    val serialKey: String get() = when (this) {
        SHORT_TEXT      -> "short_text"
        LONG_TEXT       -> "long_text"
        RATING          -> "rating"
        MULTIPLE_CHOICE -> "multiple_choice"
        CHECKBOXES      -> "checkboxes"
        BOOLEAN         -> "boolean"
        DROPDOWN        -> "dropdown"
    }

    companion object {
        fun fromKey(key: String): QuestionType = entries.firstOrNull {
            it.serialKey == key
        } ?: when (key) {
            "text"   -> SHORT_TEXT
            "choice" -> MULTIPLE_CHOICE
            else     -> SHORT_TEXT
        }
    }
}

data class EditableQuestion(
    val id: String = UUID.randomUUID().toString(),
    val type: QuestionType = QuestionType.SHORT_TEXT,
    val label: String = "",
    val options: List<String> = emptyList(),   // for choice-based types
    val required: Boolean = true
) {
    val hasOptions: Boolean get() = type in listOf(
        QuestionType.MULTIPLE_CHOICE,
        QuestionType.CHECKBOXES,
        QuestionType.DROPDOWN
    )

    fun toFeedbackQuestion() = FeedbackQuestion(
        type = type.serialKey,
        label = label,
        options = if (hasOptions && options.isNotEmpty()) options else null
    )
}

fun FeedbackQuestion.toEditable() = EditableQuestion(
    id = UUID.randomUUID().toString(),
    type = QuestionType.fromKey(type),
    label = label,
    options = options ?: emptyList(),
    required = true
)

// ── Minimal event DTO for this screen ────────────────────────────────────────
@Serializable
private data class EventFeedbackRow(
    val id: String,
    val title: String,
    @SerialName("feedback_config") val feedbackConfig: List<FeedbackQuestion>? = null
)

data class CcFeedbackEditorUiState(
    val eventTitle: String = "",
    val questions: List<EditableQuestion> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val snackbar: String? = null,
    val isDirty: Boolean = false
)

class CcFeedbackEditorViewModel : ViewModel() {

    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(CcFeedbackEditorUiState())
    val state: StateFlow<CcFeedbackEditorUiState> = _state.asStateFlow()

    fun load(eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val row = client.from("events")
                    .select { filter { eq("id", eventId) } }
                    .decodeSingleOrNull<EventFeedbackRow>()

                val questions = row?.feedbackConfig
                    ?.map { it.toEditable() }
                    ?: emptyList()

                _state.update {
                    it.copy(
                        eventTitle = row?.title ?: "",
                        questions = questions,
                        isLoading = false,
                        isDirty = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, snackbar = "Failed to load: ${e.message}") }
            }
        }
    }

    fun addQuestion() {
        _state.update {
            it.copy(
                questions = it.questions + EditableQuestion(),
                isDirty = true
            )
        }
    }

    fun removeQuestion(id: String) {
        _state.update {
            it.copy(questions = it.questions.filter { q -> q.id != id }, isDirty = true)
        }
    }

    fun updateQuestion(id: String, updated: EditableQuestion) {
        _state.update {
            it.copy(
                questions = it.questions.map { q -> if (q.id == id) updated else q },
                isDirty = true
            )
        }
    }

    fun moveQuestion(fromIndex: Int, toIndex: Int) {
        val list = _state.value.questions.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _state.update { it.copy(questions = list, isDirty = true) }
    }

    fun save(eventId: String) {
        val questions = _state.value.questions
        if (questions.size < 3) {
            _state.update { it.copy(snackbar = "At least 3 questions are required.") }
            return
        }
        val blanks = questions.count { it.label.isBlank() }
        if (blanks > 0) {
            _state.update { it.copy(snackbar = "All questions must have a label.") }
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val config = questions.map { it.toFeedbackQuestion() }
                client.from("events").update({
                    set("feedback_config", config)
                }) {
                    filter { eq("id", eventId) }
                }
                _state.update { it.copy(isSaving = false, isDirty = false, snackbar = "Feedback questions saved") }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, snackbar = "Save failed: ${e.message}") }
            }
        }
    }

    fun clearSnackbar() = _state.update { it.copy(snackbar = null) }
}
