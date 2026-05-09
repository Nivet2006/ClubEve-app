package com.clubeve.cc.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.ClubEveApplication
import com.clubeve.cc.data.local.AppDatabase
import com.clubeve.cc.data.local.entity.EventEntity
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.data.repository.EventRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EventListUiState(
    val events: List<EventEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isOnline: Boolean = true,
    val error: String? = null
)

class EventListViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val networkMonitor = (app as ClubEveApplication).networkMonitor
    private val repo = EventRepository(db.eventDao(), networkMonitor)
    private val _state = MutableStateFlow(EventListUiState())
    val state: StateFlow<EventListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnlineFlow.collect { isOnline ->
                _state.update { it.copy(isOnline = isOnline) }
            }
        }
        viewModelScope.launch {
            repo.observeEvents().collect { events ->
                _state.update { it.copy(events = events) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val uid = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                if (uid != null) {
                    repo.refreshEvents(uid)
                } else {
                    // fallback: load all approved events
                    repo.refreshAllApprovedEvents()
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
            _state.update { it.copy(isLoading = false) }
        }
    }
}
