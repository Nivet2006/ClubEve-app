package com.clubeve.cc.ui.faculty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.SessionManager
import com.clubeve.cc.models.ApprovalStatus
import com.clubeve.cc.models.CcEvent
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.theme.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyDashboardScreen(
    onEventClick: (eventId: String) -> Unit,
    onLogout: () -> Unit,
    vm: FacultyDashboardViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme
    val profile = SessionManager.currentProfile

    // Greeting based on time of day
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else      -> "Good Evening"
        }
    }
    val firstName = remember(profile) {
        profile?.fullName?.split(" ")?.firstOrNull() ?: ""
    }
    val roleLabel = remember(profile) {
        when (profile?.role) {
            "teacher" -> "Teacher"
            "hod"     -> "Head of Department"
            "admin"   -> "Admin"
            "manager" -> "Manager"
            else      -> profile?.role?.replaceFirstChar { it.uppercase() } ?: ""
        }
    }

    // ── Glassmorphism easter egg — 6 taps on title ────────────────────────────
    var titleTapCount by remember { mutableStateOf(0) }
    var lastTapTime   by remember { mutableStateOf(0L) }
    val isGlass = GlassState.isGlass
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var glassToastTrigger by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(glassToastTrigger) {
        glassToastTrigger?.let { enabled ->
            snackbarHostState.showSnackbar(
                if (enabled) "✦ Glassmorphism enabled" else "✦ Glassmorphism disabled"
            )
        }
    }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Sign Out",
                    fontFamily = Mono,
                    fontWeight = FontWeight.Bold,
                    color = cs.onBackground
                )
            },
            text = {
                Text(
                    "Are you sure you want to sign out?",
                    fontFamily = Mono,
                    fontSize = 13.sp,
                    color = cs.onSurface
                )
            },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; vm.logout(onLogout) }) {
                    Text(
                        "SIGN OUT",
                        fontFamily = Mono,
                        fontWeight = FontWeight.Bold,
                        color = StatusError,
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL", fontFamily = Mono, fontSize = 12.sp, color = cs.onSurfaceVariant)
                }
            },
            containerColor = cs.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 80.dp))
        },
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime > 2_000L) titleTapCount = 0
                                lastTapTime = now
                                titleTapCount++
                                if (titleTapCount >= 6) {
                                    titleTapCount = 0
                                    GlassState.toggle()
                                    glassToastTrigger = GlassState.isGlass
                                    coroutineScope.launch {
                                        ThemePrefsStore.saveGlass(context, GlassState.isGlass)
                                    }
                                }
                            }
                        }
                    ) {
                        Text(
                            "FACULTY REVIEW",
                            fontFamily = Mono,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                            color = if (isGlass) GlassState.glassAccentColor else cs.onBackground
                        )
                        if (!profile?.fullName.isNullOrBlank()) {
                            Text(
                                "${profile!!.fullName.uppercase()} · ${roleLabel.uppercase()}",
                                fontFamily = Mono,
                                fontSize = 9.sp,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = vm::load) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = cs.onSurfaceVariant)
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = cs.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground
                )
            )
        },
        containerColor = cs.background
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            HorizontalDivider(color = cs.outline, thickness = 1.dp)

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = vm::load,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && state.pendingEvents.isEmpty() && state.verifiedEvents.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = cs.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // Greeting
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp)
                                ) {
                                    Text(
                                        "$greeting, $firstName",
                                        fontFamily = Mono,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = cs.onBackground
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        profile?.department?.uppercase() ?: "",
                                        fontFamily = Mono,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                            }

                            // ── Pending Actions section ───────────────────────
                            item {
                                SectionHeader(
                                    title = "PENDING ACTIONS (${state.pendingEvents.size})",
                                    accentColor = Color(0xFFF59E0B)
                                )
                            }

                            if (state.pendingEvents.isEmpty()) {
                                item {
                                    EmptyState(
                                        message = "No pending events to review.",
                                        modifier = Modifier.padding(vertical = 24.dp)
                                    )
                                }
                            } else {
                                items(state.pendingEvents, key = { "pending_${it.id}" }) { event ->
                                    FacultyEventCard(
                                        event = event,
                                        isPending = true,
                                        onClick = { onEventClick(event.id) }
                                    )
                                    HorizontalDivider(color = cs.outlineVariant)
                                }
                            }

                            item { Spacer(Modifier.height(20.dp)) }

                            // ── Verified & Live section ───────────────────────
                            item {
                                SectionHeader(
                                    title = "VERIFIED & LIVE",
                                    accentColor = StatusSuccess
                                )
                            }

                            if (state.verifiedEvents.isEmpty()) {
                                item {
                                    EmptyState(
                                        message = "No verified events yet.",
                                        modifier = Modifier.padding(vertical = 24.dp)
                                    )
                                }
                            } else {
                                items(state.verifiedEvents, key = { "verified_${it.id}" }) { event ->
                                    FacultyEventCard(
                                        event = event,
                                        isPending = false,
                                        onClick = { onEventClick(event.id) }
                                    )
                                    HorizontalDivider(color = cs.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, accentColor: Color) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            title,
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = accentColor
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = accentColor.copy(alpha = 0.3f))
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("—", fontSize = 24.sp, color = cs.onSurfaceVariant)
            Text(message, fontFamily = Mono, fontSize = 12.sp, color = cs.onSurfaceVariant)
        }
    }
}

// ── Event card ────────────────────────────────────────────────────────────────

@Composable
private fun FacultyEventCard(
    event: CcEvent,
    isPending: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val dateStr = remember(event.eventDate) {
        try {
            ZonedDateTime.parse(event.eventDate)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.getDefault()))
        } catch (_: Exception) { event.eventDate }
    }

    val accentColor = if (isPending) Color(0xFFF59E0B) else StatusSuccess
    val (statusLabel, statusBg, statusFg) = when (event.approvalStatus) {
        ApprovalStatus.APPROVED     -> Triple("PUBLISHED", StatusSuccess.copy(alpha = 0.10f), StatusSuccess)
        ApprovalStatus.PENDING_HOD  -> Triple("HOD PENDING", Color(0xFFFEF3C7), Color(0xFFB45309))
        else                        -> Triple("PENDING", Color(0xFFFEF3C7), Color(0xFFB45309))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(cs.background)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Amber left border for pending, green for verified
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(44.dp)
                .background(accentColor, RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = cs.onBackground,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(dateStr, fontFamily = Mono, fontSize = 10.sp, color = cs.onSurfaceVariant)
            }
            if (!event.targetedDepartment.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    event.targetedDepartment,
                    fontFamily = Mono,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    color = cs.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Status pill
        Box(
            modifier = Modifier
                .border(1.dp, statusFg.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .background(statusBg, RoundedCornerShape(4.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Text(
                statusLabel,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp,
                color = statusFg
            )
        }

        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
