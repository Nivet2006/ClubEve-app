package com.clubeve.cc

import android.app.Application
import com.clubeve.cc.notifications.AssignmentNotifier
import com.clubeve.cc.sync.AttendanceSyncWorker
import com.clubeve.cc.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ClubEveApplication : Application() {
    val networkMonitor by lazy { NetworkMonitor(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AssignmentNotifier.createChannel(this)
        networkMonitor.isOnlineFlow
            .onEach { isOnline ->
                if (isOnline) AttendanceSyncWorker.schedule(this)
            }
            .launchIn(appScope)
    }
}
