package com.clubeve.cc.ui.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clubeve.cc.ClubEveApplication
import com.clubeve.cc.utils.NetworkMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ScannerViewModel(app: Application) : AndroidViewModel(app) {
    val networkMonitor: NetworkMonitor = (app as ClubEveApplication).networkMonitor
    val isOnlineFlow: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.isOnline())
}
