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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.clubeve.cc.ui.theme.DarkBorder
import com.clubeve.cc.ui.theme.DarkSurface
import com.clubeve.cc.ui.theme.GlassBorderColor
import com.clubeve.cc.ui.theme.GlassColorStore
import com.clubeve.cc.ui.theme.GlassSurface
import com.clubeve.cc.ui.theme.GlassState
import com.clubeve.cc.ui.theme.ThemeState
import com.clubeve.cc.ui.theme.White
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
                val isGlass = GlassState.isGlass

                // Glass mode: render a deep purple-blue radial gradient as the persistent
                // background layer. All screen backgrounds are translucent in glass mode so
                // this gradient shows through every surface.
                val bgModifier = if (isGlass) {
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to androidx.compose.ui.graphics.Color(0xFF1A0A3D),
                                    0.4f to androidx.compose.ui.graphics.Color(0xFF0D0D2B),
                                    0.7f to androidx.compose.ui.graphics.Color(0xFF050518),
                                    1.0f to androidx.compose.ui.graphics.Color(0xFF000008)
                                )
                            )
                        )
                } else {
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                }

                Box(bgModifier) {
                    val navController = rememberNavController()
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var pendingRelease by remember { mutableStateOf<UpdateChecker.ReleaseInfo?>(null) }
                    // Attendance sheet state — owned here so FAB is in the same Box as ThemeToggleFab
                    var showStudentAttendance by remember { mutableStateOf(false) }
                    val currentBackStack by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStack?.destination?.route
                    val isStudentHome = currentRoute == Screen.StudentHome.route
                    val isLoginScreen = currentRoute == Screen.Login.route

                    // Check for update on every launch
                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            val release = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
                            if (release != null) withContext(Dispatchers.Main) {
                                pendingRelease = release
                            }
                        }
                    }

                    // Load saved glass accent color from DataStore
                    LaunchedEffect(Unit) {
                        GlassColorStore.accentColorFlow(applicationContext).collect { argb ->
                            GlassState.glassAccentColor = androidx.compose.ui.graphics.Color(argb.toInt())
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
                                        // Persist pr_id for background workers
                                        SessionStore.savePrId(applicationContext, user.id)
                                    } else if (profile.role == "student") {
                                        SessionManager.currentUserId = user.id
                                        SessionManager.currentProfile = profile
                                        startDestination = Screen.StudentHome.route
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
                        AppNavGraph(
                            navController = navController,
                            startDestination = dest,
                            showStudentAttendance = showStudentAttendance,
                            onStudentAttendanceDismiss = { showStudentAttendance = false }
                        )
                    }

                    // Theme toggle FAB (bottom-right).
                    // In normal mode: dark/light toggle with wipe animation.
                    // In glass mode: palette button to customize accent color.
                    // Hidden on the login screen in both modes.
                    if (!isLoginScreen) {
                        ThemeToggleFab()
                    }

                    // Attendance FAB — bottom-left, symmetric with ThemeToggleFab (bottom-right)
                    // Shown on the student home screen in all modes (normal + glass)
                    if (isStudentHome) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 20.dp, bottom = 24.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isGlass) GlassSurface
                                        else if (ThemeState.isDark) DarkSurface
                                        else White,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .size(52.dp)
                                    .border(
                                        1.dp,
                                        if (isGlass) GlassBorderColor
                                        else if (ThemeState.isDark) DarkBorder
                                        else androidx.compose.ui.graphics.Color(0x1F000000),
                                        CircleShape
                                    )
                                    .clickable { showStudentAttendance = true }
                            ) {
                                Box(contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.BarChart,
                                        contentDescription = "My Attendance",
                                        tint = if (isGlass) GlassState.glassAccentColor
                                               else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    pendingRelease?.let { release ->
                        UpdateDialog(release = release, onDismiss = { pendingRelease = null })
                    }
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
