package com.clubeve.cc.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.clubeve.cc.models.Event
import com.clubeve.cc.sync.SyncConflict
import com.clubeve.cc.sync.SyncManager
import com.clubeve.cc.notifications.AssignmentWatcher
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.theme.*
import com.clubeve.cc.utils.NetworkMonitor
import com.clubeve.cc.utils.ShakeDetector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEventClick: (String) -> Unit,
    onLogout: () -> Unit,
    onScanEvent: (String) -> Unit = {},   // navigate directly to scanner for an event
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val pendingCount by vm.pendingSyncCount.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var conflicts by remember { mutableStateOf<List<SyncConflict>>(emptyList()) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    // ── Shake-to-scan (PR only) ───────────────────────────────────────────────
    // If one event → go straight to scanner.
    // If multiple events → show a quick-pick bottom sheet.
    var showShakeEventPicker by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val detector = ShakeDetector(context) {
            val events = vm.state.value.events
            when {
                events.isEmpty() -> { /* nothing assigned, ignore */ }
                events.size == 1 -> onScanEvent(events.first().id)
                else             -> showShakeEventPicker = true
            }
        }
        detector.start()
        onDispose { detector.stop() }
    }

    // ── Shake event picker bottom sheet ──────────────────────────────────────
    if (showShakeEventPicker) {
        ModalBottomSheet(
            onDismissRequest = { showShakeEventPicker = false },
            containerColor = cs.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "SCAN FOR WHICH EVENT?",
                        fontFamily = Mono,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        color = cs.onSurface
                    )
                }
                HorizontalDivider(color = cs.outline)
                Spacer(Modifier.height(8.dp))
                state.events.forEach { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShakeEventPicker = false
                                onScanEvent(event.id)
                            }
                            .padding(vertical = 14.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                event.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = cs.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                event.clubName.uppercase(),
                                fontFamily = Mono,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = cs.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    HorizontalDivider(color = cs.outlineVariant)
                }
            }
        }
    }

    // ── Glassmorphism easter egg — 6 taps on "MY EVENTS" title ───────────────
    var titleTapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    val isGlass = GlassState.isGlass

    // Dialog background: translucent frosted in glass mode, normal surface otherwise
    val dialogBg = if (isGlass) Color(0xCC0D0D2B) else cs.surface

    LaunchedEffect(isGlass) {
        // Show feedback snackbar whenever glass mode changes (but not on first composition)
    }
    var glassToastTrigger by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(glassToastTrigger) {
        glassToastTrigger?.let { enabled ->
            snackbarHostState.showSnackbar(
                if (enabled) "✦ Glassmorphism enabled" else "✦ Glassmorphism disabled"
            )
        }
    }

    val isOnline by produceState(initialValue = true) {
        NetworkMonitor(context).isOnlineFlow.collect { value = it }
    }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); now = System.currentTimeMillis() }
    }

    LaunchedEffect(Unit) {
        if (vm.state.value.events.isEmpty() && !vm.state.value.isLoading) vm.loadEvents()
    }

    LaunchedEffect(syncMessage) {
        syncMessage?.let { snackbarHostState.showSnackbar(it); syncMessage = null }
    }

    // ── Logout dialog ─────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontFamily = Mono, fontWeight = FontWeight.Bold, color = cs.onBackground) },
            text = { Text("Are you sure you want to sign out?", fontFamily = Mono, fontSize = 13.sp, color = cs.onSurface) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; vm.logout(onLogout) }) {
                    Text("SIGN OUT", fontFamily = Mono, fontWeight = FontWeight.Bold, color = StatusError, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL", fontFamily = Mono, fontSize = 12.sp, color = cs.onSurfaceVariant)
                }
            },
            containerColor = dialogBg, tonalElevation = 0.dp, shape = RoundedCornerShape(12.dp)
        )
    }

    // ── Sync dialog ───────────────────────────────────────────────────────────
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync Offline Check-ins", fontFamily = Mono, fontWeight = FontWeight.Bold, color = cs.onBackground) },
            text = {
                Text("You have $pendingCount offline check-in(s) pending. Sync now?",
                    fontFamily = Mono, fontSize = 13.sp, color = cs.onSurface)
            },
            confirmButton = {
                TextButton(onClick = {
                    showSyncDialog = false
                    coroutineScope.launch {
                        val report = SyncManager.syncPendingCheckIns(context)
                        if (report.conflicts.isNotEmpty()) conflicts = report.conflicts
                        syncMessage = buildString {
                            if (report.synced > 0) append("✓ Synced ${report.synced}")
                            if (report.failed > 0) append(" · ⚠ ${report.failed} failed")
                            if (report.conflicts.isNotEmpty()) append(" · ${report.conflicts.size} conflict(s)")
                        }.ifBlank { "Nothing to sync" }
                    }
                }) {
                    Text("SYNC NOW", fontFamily = Mono, fontWeight = FontWeight.Bold, color = cs.primary, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("CANCEL", fontFamily = Mono, fontSize = 12.sp, color = cs.onSurfaceVariant)
                }
            },
            containerColor = dialogBg, tonalElevation = 0.dp, shape = RoundedCornerShape(12.dp)
        )
    }

    // ── Conflict resolution dialog ────────────────────────────────────────────
    if (conflicts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { conflicts = emptyList() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Conflicts", fontFamily = Mono, fontWeight = FontWeight.Bold, color = cs.onBackground)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("These students were already checked in remotely with a different timestamp. Remote data was kept.",
                        fontFamily = Mono, fontSize = 11.sp, color = cs.onSurface)
                    HorizontalDivider(color = cs.outline)
                    conflicts.forEach { conflict ->
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .border(1.dp, cs.outline, RoundedCornerShape(8.dp)).padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(conflict.studentName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = cs.onSurface)
                            Text(conflict.studentUsn, fontFamily = Mono, fontSize = 10.sp, color = cs.onSurfaceVariant)
                            HorizontalDivider(color = cs.outlineVariant)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("YOUR RECORD", fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                                    Text(formatConflictTime(conflict.localCheckedInAt), fontFamily = Mono, fontSize = 11.sp, color = StatusError)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("REMOTE", fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                                    Text(formatConflictTime(conflict.remoteCheckedInAt), fontFamily = Mono, fontSize = 11.sp, color = StatusSuccess)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { conflicts = emptyList() }) {
                    Text("GOT IT", fontFamily = Mono, fontWeight = FontWeight.Bold, color = cs.primary, fontSize = 12.sp)
                }
            },
            containerColor = dialogBg, tonalElevation = 0.dp, shape = RoundedCornerShape(12.dp)
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
                                // Reset counter if more than 2 seconds between taps
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
                            "MY EVENTS",
                            fontFamily = Mono,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                            color = if (isGlass) GlassState.glassAccentColor else cs.onBackground
                        )
                        if (state.lastSyncedAt > 0L) {
                            Text(
                                "Synced ${formatRelativeTime(state.lastSyncedAt, now)}",
                                fontFamily = Mono,
                                fontSize = 9.sp,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (pendingCount > 0) {
                        BadgedBox(badge = {
                            Badge(containerColor = Color(0xFFFF3B30), contentColor = Color.White) {
                                Text(pendingCount.toString(), fontFamily = Mono, fontSize = 9.sp)
                            }
                        }) {
                            IconButton(onClick = { showSyncDialog = true }) {
                                Icon(Icons.Default.CloudUpload, "Sync", tint = cs.onSurfaceVariant)
                            }
                        }
                    }
                    IconButton(onClick = vm::loadEvents) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = cs.onSurfaceVariant)
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = cs.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background, titleContentColor = cs.onBackground)
            )
        },
        containerColor = cs.background
    ) { padding ->
        Column(Modifier.padding(padding)) {
            HorizontalDivider(color = cs.outline, thickness = 1.dp)

            if (!isOnline) {
                Surface(color = Color(0xFFFF9500)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Offline — check-ins will sync when online", color = Color.White, fontFamily = Mono, fontSize = 11.sp)
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading, onRefresh = vm::loadEvents,
                state = pullRefreshState, modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && state.events.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        }
                    }
                    state.error != null && state.events.isEmpty() -> {
                        Column(Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center) {
                            Text("ERROR", fontFamily = Mono, fontWeight = FontWeight.Black,
                                fontSize = 11.sp, letterSpacing = 2.sp, color = StatusError)
                            Spacer(Modifier.height(8.dp))
                            Text(state.error ?: "", fontFamily = Mono, fontSize = 12.sp, color = cs.onSurface)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = vm::loadEvents, shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, cs.primary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.primary)) {
                                Text("RETRY", fontFamily = Mono, fontSize = 11.sp)
                            }
                        }
                    }
                    state.events.isEmpty() -> {
                        Column(Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center) {
                            Text("—", fontSize = 32.sp, color = cs.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Text("No events assigned to you.", fontFamily = Mono, fontSize = 12.sp, color = cs.onSurfaceVariant)
                        }
                    }
                    else -> {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            items(state.events, key = { it.id }) { event ->
                                EventCard(event = event, onClick = { onEventClick(event.id) })
                                HorizontalDivider(color = cs.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Event card ────────────────────────────────────────────────────────────────

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dateStr = remember(event.eventDate) {
        try { ZonedDateTime.parse(event.eventDate)
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.getDefault()))
        } catch (_: Exception) { event.eventDate }
    }

    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val eventMs = remember(event.eventDate) {
        try { ZonedDateTime.parse(event.eventDate).toInstant().toEpochMilli() }
        catch (_: Exception) { 0L }
    }
    LaunchedEffect(event.id) {
        while (eventMs > System.currentTimeMillis()) { delay(1_000); nowMs = System.currentTimeMillis() }
    }
    val countdown = remember(nowMs, eventMs) { buildCountdown(eventMs, nowMs) }

    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .background(cs.background).padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text(event.title, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = cs.onBackground, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            EventStatusBadge(status = event.status)
        }
        Spacer(Modifier.height(4.dp))
        Text(event.clubName.uppercase(), fontFamily = Mono, fontSize = 10.sp,
            letterSpacing = 1.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(dateStr, fontFamily = Mono, fontSize = 11.sp, color = cs.onSurfaceVariant)
        }
        if (!event.location.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(event.location, fontFamily = Mono, fontSize = 11.sp, color = cs.onSurfaceVariant)
            }
        }
        if (countdown != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = cs.primary, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(countdown, fontFamily = Mono, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, color = cs.primary)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonoBadge("${event.registrationCount} registered")
            MonoBadge("${event.attendanceCount} present", filled = event.attendanceCount > 0)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun buildCountdown(eventMs: Long, nowMs: Long): String? {
    val diff = eventMs - nowMs
    if (diff <= 0) return null
    val days    = diff / 86_400_000L
    val hours   = (diff % 86_400_000L) / 3_600_000L
    val minutes = (diff % 3_600_000L) / 60_000L
    val seconds = (diff % 60_000L) / 1_000L
    return when {
        days > 0    -> "in ${days}d ${hours}h"
        hours > 0   -> "in ${hours}h ${minutes}m"
        minutes > 0 -> "in ${minutes}m ${seconds}s"
        else        -> "in ${seconds}s"
    }
}

private fun formatRelativeTime(syncedAt: Long, now: Long): String {
    val diff = now - syncedAt
    return when {
        diff < 60_000L     -> "just now"
        diff < 3_600_000L  -> "${diff / 60_000L} min ago"
        diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
        else               -> "${diff / 86_400_000L} days ago"
    }
}

private fun formatConflictTime(iso: String?): String {
    if (iso == null) return "—"
    return try {
        DateTimeFormatter.ofPattern("dd MMM HH:mm:ss")
            .withZone(ZoneId.systemDefault()).format(Instant.parse(iso))
    } catch (_: Exception) { iso }
}

@Composable
fun EventStatusBadge(status: String?) {
    val cs = MaterialTheme.colorScheme
    val label = when (status?.lowercase()) {
        "ongoing"   -> "ONGOING"
        "completed" -> "DONE"
        else        -> "UPCOMING"
    }
    val (bg, fg) = when (status?.lowercase()) {
        "ongoing"   -> cs.primary to cs.onPrimary
        "completed" -> cs.surfaceVariant to cs.onSurfaceVariant
        else        -> cs.background to cs.onBackground
    }
    Box(
        modifier = Modifier
            .border(1.dp, if (status?.lowercase() == "ongoing") cs.primary else cs.outline, RoundedCornerShape(4.dp))
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp,
            color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MonoBadge(text: String, filled: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .border(1.dp, if (filled) cs.primary else cs.outline, RoundedCornerShape(4.dp))
            .background(if (filled) cs.primary else cs.background, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontFamily = Mono, fontSize = 10.sp,
            color = if (filled) cs.onPrimary else cs.onSurface)
    }
}
