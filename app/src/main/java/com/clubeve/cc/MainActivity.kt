package com.clubeve.cc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Profile
import com.clubeve.cc.notifications.AssignmentPollWorker
import com.clubeve.cc.notifications.AssignmentWatcher
import com.clubeve.cc.notifications.SessionStore
import com.clubeve.cc.ui.components.ThemeToggleFab
import com.clubeve.cc.ui.navigation.AppNavGraph
import com.clubeve.cc.ui.navigation.Screen
import com.clubeve.cc.ui.theme.ClubEveTheme
import com.clubeve.cc.update.UpdateChecker
import com.clubeve.cc.update.UpdateDialog
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — watcher runs either way, notify() handles SecurityException */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            ClubEveTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val navController = rememberNavController()
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var pendingRelease by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }

                    // Check for update on every launch
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            val release = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                            if (release != null) withContext(Dispatchers.Main) {
                                pendingRelease = release
                            }
                        }
                    }

                    // Determine start destination + start assignment watcher after login
                    LaunchedEffect(Unit) {
                        val client = SupabaseClientProvider.client
                        val user = client.auth.currentUserOrNull()
                        if (user == null) {
                            startDestination = Screen.Login.route
                            return@LaunchedEffect
                        }
                        withContext(Dispatchers.IO) {
                            try {
                                val profile = client.from("profiles")
                                    .select { filter { eq("id", user.id) } }
                                    .decodeSingle<Profile>()
                                withContext(Dispatchers.Main) {
                                    if (profile.role == "pr") {
                                        SessionManager.currentUserId = user.id
                                        SessionManager.currentProfile = profile
                                        startDestination = Screen.Home.route
                                        // Persist and schedule background polling
                                        SessionStore.savePrId(applicationContext, user.id)
                                        AssignmentPollWorker.schedule(applicationContext)
                                        // Also start realtime watcher while app is open
                                        AssignmentWatcher.start(
                                            context = applicationContext,
                                            prUserId = user.id,
                                            scope = lifecycleScope
                                        )
                                    } else {
                                        try { client.auth.signOut() } catch (_: Exception) {}
                                        SessionManager.clear()
                                        startDestination = Screen.Login.route
                                    }
                                }
                            } catch (_: Exception) {
                                withContext(Dispatchers.Main) {
                                    startDestination = Screen.Login.route
                                }
                            }
                        }
                    }

                    startDestination?.let { dest ->
                        AppNavGraph(navController = navController, startDestination = dest)
                    }

                    pendingRelease?.let { release ->
                        UpdateDialog(release = release, onDismiss = { pendingRelease = null })
                    }

                    ThemeToggleFab()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AssignmentWatcher.stop()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
