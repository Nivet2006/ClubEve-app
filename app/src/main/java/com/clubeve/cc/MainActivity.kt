package com.clubeve.cc

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Profile
import com.clubeve.cc.ui.components.ThemeToggleFab
import com.clubeve.cc.ui.navigation.AppNavGraph
import com.clubeve.cc.ui.navigation.Screen
import com.clubeve.cc.ui.theme.ClubEveTheme
import com.clubeve.cc.update.UpdateChecker
import com.clubeve.cc.update.UpdateDialog
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClubEveTheme {
                val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                Surface(Modifier.fillMaxSize(), color = bgColor) {
                    Box(Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        var startDestination by remember { mutableStateOf<String?>(null) }
                        var pendingRelease by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }

                        // ── 1. Check for update immediately on every launch ──
                        LaunchedEffect(Unit) {
                            CoroutineScope(Dispatchers.IO).launch {
                                val release = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                                if (release != null) pendingRelease = release
                            }
                        }

                        // ── 2. Determine start destination ───────────────────
                        LaunchedEffect(Unit) {
                            val client = SupabaseClientProvider.client
                            val user = client.auth.currentUserOrNull()
                            if (user == null) {
                                startDestination = Screen.Login.route
                                return@LaunchedEffect
                            }

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val profile = client.from("profiles")
                                        .select { filter { eq("id", user.id) } }
                                        .decodeSingle<Profile>()

                                    if (profile.role == "pr") {
                                        SessionManager.currentUserId = user.id
                                        SessionManager.currentProfile = profile
                                        startDestination = Screen.Home.route
                                    } else {
                                        try { client.auth.signOut() } catch (_: Exception) {}
                                        SessionManager.clear()
                                        startDestination = Screen.Login.route
                                    }
                                } catch (_: Exception) {
                                    startDestination = Screen.Login.route
                                }
                            }
                        }

                        startDestination?.let { dest ->
                            AppNavGraph(
                                navController = navController,
                                startDestination = dest
                            )
                        }

                        // ── Update dialog — shown as soon as release is found ─
                        pendingRelease?.let { release ->
                            UpdateDialog(
                                release = release,
                                onDismiss = { pendingRelease = null }
                            )
                        }

                        // Floating theme toggle — always on top
                        ThemeToggleFab()
                    }
                }
            }
        }
    }
}
