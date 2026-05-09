package com.clubeve.cc.ui.result

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.ClubEveApplication
import com.clubeve.cc.data.local.AppDatabase
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.data.repository.AttendanceRepository
import com.clubeve.cc.data.repository.ScanResult
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ResultUiState(
    val isLoading: Boolean = true,
    val scanResult: ScanResult? = null
)

class ScanResultViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val networkMonitor = (app as ClubEveApplication).networkMonitor
    private val repo = AttendanceRepository(db.registrationDao(), networkMonitor)
    private val _state = MutableStateFlow(ResultUiState())
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    fun process(usn: String, eventId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val uid = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: ""
            val result = repo.markPresent(usn, eventId, uid)
            _state.update { it.copy(isLoading = false, scanResult = result) }
        }
    }
}
