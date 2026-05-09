package com.clubeve.cc.ui.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.ClubEveApplication
import com.clubeve.cc.data.local.AppDatabase
import com.clubeve.cc.data.local.entity.RegistrationEntity
import com.clubeve.cc.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AttendanceFilter { ALL, PRESENT, ABSENT }

data class AttendanceListUiState(
    val all: List<RegistrationEntity> = emptyList(),
    val filter: AttendanceFilter = AttendanceFilter.ALL,
    val isLoading: Boolean = false,
    val isOnline: Boolean = true,
    val presentCount: Int = 0,
    val totalCount: Int = 0,
    val searchQuery: String = ""
)

class AttendanceListViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val networkMonitor = (app as ClubEveApplication).networkMonitor
    private val repo = AttendanceRepository(db.registrationDao(), networkMonitor)
    private val _state = MutableStateFlow(AttendanceListUiState())
    val state: StateFlow<AttendanceListUiState> = _state.asStateFlow()

    private var eventId: String = ""

    fun init(eventId: String) {
        if (this.eventId == eventId) return
        this.eventId = eventId
        viewModelScope.launch {
            repo.observeAttendance(eventId).collect { list ->
                _state.update { it.copy(all = list) }
            }
        }
        viewModelScope.launch {
            repo.observePresentCount(eventId).collect { count ->
                _state.update { it.copy(presentCount = count) }
            }
        }
        viewModelScope.launch {
            repo.observeTotalCount(eventId).collect { count ->
                _state.update { it.copy(totalCount = count) }
            }
        }
        viewModelScope.launch {
            networkMonitor.isOnlineFlow.collect { online ->
                _state.update { it.copy(isOnline = online) }
            }
        }
        refresh()
    }

    fun setFilter(f: AttendanceFilter) = _state.update { it.copy(filter = f) }
    fun setSearch(q: String) = _state.update { it.copy(searchQuery = q) }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repo.syncFromRemote(eventId)
            _state.update { it.copy(isLoading = false) }
        }
    }

    val displayed: StateFlow<List<RegistrationEntity>> = state.map { s ->
        s.all
            .filter { r ->
                when (s.filter) {
                    AttendanceFilter.ALL -> true
                    AttendanceFilter.PRESENT -> r.isPresent
                    AttendanceFilter.ABSENT -> !r.isPresent
                }
            }
            .filter { r ->
                s.searchQuery.isBlank() ||
                r.studentName.contains(s.searchQuery, ignoreCase = true) ||
                r.usn.contains(s.searchQuery, ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
}
