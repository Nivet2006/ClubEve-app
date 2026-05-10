package com.clubeve.cc.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.models.Event
import com.clubeve.cc.sync.SyncConflict
import com.clubeve.cc.sync.SyncManager
import com.clubeve.cc.ui.theme.*
import com.clubeve.cc.utils.NetworkMonitor
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

    val isOnline by produceState(initialValue = true) {
        NetworkMonitor(context).isOnlineFlow.collect { value = it }
    }

    // Ticker for "last synced X ago" — updates every 30s
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(30_000); now = System.currentTimeMillis() }
    }

    LaunchedEffect(Unit) {
        if (vm.state.value.events.isEmpty() && !vm.state.value.isLoading) {
            vm.loadEvents()
        }
    }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            syncMessage = null
        }
    }

    // ── Logout dialog ─────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontFamily = Mono, fontWeight = FontWeight.Bold, color = Black) },
            text = { Text("Are you sure you want to sign out?", fontFamily = Mono, fontSize = 13.sp, color = DarkGray) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; vm.logout(onLogout) }) {
                    Text("SIGN OUT", fontFamily = Mono, fontWeight = FontWeight.Bold, color = StatusError, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL", fontFamily = Mono, fontSize = 12.sp, color = MidGray)
                }
            },
            containerColor = White, shape = RoundedCornerShape(12.dp)
        )
    }

    // ── Sync dialog ───────────────────────────────────────────────────────────
    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("Sync Offline Check-ins", fontFamily = Mono, fontWeight = FontWeight.Bold, color = Black) },
            text = {
                Text(
                    "You have $pendingCount offline check-in(s) pending. Sync now?",
                    fontFamily = Mono, fontSize = 13.sp, color = DarkGray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSyncDialog = false
                    coroutineScope.launch {
                        val report = SyncManager.syncPendingCheckIns(context)
                        if (report.conflicts.isNotEmpty()) {
                            conflicts = report.conflicts
                        }
                        syncMessage = buildString {
                            if (report.synced > 0) append("✓ Synced ${report.synced}")
                            if (report.failed > 0) append(" · ⚠ ${report.failed} failed")
                            if (report.conflicts.isNotEmpty()) append(" · ${report.conflicts.size} conflict(s)")
                        }.ifBlank { "Nothing to sync" }
                    }
                }) {
                    Text("SYNC NOW", fontFamily = Mono, fontWeight = FontWeight.Bold, color = Black, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("CANCEL", fontFamily = Mono, fontSize = 12.sp, color = MidGray)
                }
            },
            containerColor = White, shape = RoundedCornerShape(12.dp)
        )
    }

    // ── Conflict resolution dialog ────────────────────────────────────────────
    if (conflicts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { conflicts = emptyList() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = StatusWarning,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Conflicts", fontFamily = Mono, fontWeight = FontWeight.Bold, color = Black)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "These students were already checked in remotely with a different timestamp. Remote data was kept.",
                        fontFamily = Mono, fontSize = 11.sp, color = DarkGray
                    )
                    HorizontalDivider(color = BorderDefault)
                    conflicts.forEach { conflict ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderDefault, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(conflict.studentName, fontWeight = FontWeight.Bold,
                                fontSize = 13.sp, color = Black)
                            Text(conflict.studentUsn, fontFamily = Mono,
                                fontSize = 10.sp, color = MidGray)
                            HorizontalDivider(color = BorderSubtle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("YOUR RECORD", fontFamily = Mono,
                                        fontSize = 9.sp, letterSpacing = 1.sp, color = MidGray)
                                    Text(
                                        formatConflictTime(conflict.localCheckedInAt),
                                        fontFamily = Mono, fontSize = 11.sp, color = StatusError
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("REMOTE", fontFamily = Mono,
                                        fontSize = 9.sp, letterSpacing = 1.sp, color = MidGray)
                                    Text(
                                        formatConflictTime(conflict.remoteCheckedInAt),
                                        fontFamily = Mono, fontSize = 11.sp, color = StatusSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { conflicts = emptyList() }) {
                    Text("GOT IT", fontFamily = Mono, fontWeight = FontWeight.Bold,
                        color = Black, fontSize = 12.sp)
                }
            },
            containerColor = White, shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MY EVENTS", fontFamily = Mono, fontWeight = FontWeight.Black,
                            fontSize = 14.sp, letterSpacing = 2.sp, color = Black)
                        // Last synced timestamp
                        if (state.lastSyncedAt > 0L) {
                            Text(
                                "Synced ${formatRelativeTime(state.lastSyncedAt, now)}",
                                fontFamily = Mono, fontSize = 9.sp, color = MidGray
                            )
                        }
                    }
                },
                actions = {
                    if (pendingCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = Color(0xFFFF3B30), contentColor = White) {
                                    Text(pendingCount.toString(), fontFamily = Mono, fontSize = 9.sp)
                                }
                            }
                        ) {
                            IconButton(onClick = { showSyncDialog = true }) {
                                Icon(Icons.Default.CloudUpload, "Sync pending check-ins", tint = DarkGray)
                            }
                        }
                    }
                    IconButton(onClick = vm::loadEvents) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = DarkGray)
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = DarkGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White, titleContentColor = Black)
            )
        },
        containerColor = White
    ) { padding ->
        Column(Modifier.padding(padding)) {
            HorizontalDivider(color = BorderDefault, thickness = 1.dp)

            if (!isOnline) {
                Surface(color = Color(0xFFFF9500)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WifiOff, null, tint = White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Offline — check-ins will sync when online",
                            color = White, fontFamily = Mono, fontSize = 11.sp)
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = vm::loadEvents,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && state.events.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Black, strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp))
                        }
                    }
                    state.error != null && state.events.isEmpty() -> {
                        Column(
                            Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("ERROR", fontFamily = Mono, fontWeight = FontWeight.Black,
                                fontSize = 11.sp, letterSpacing = 2.sp, color = StatusError)
                            Spacer(Modifier.height(8.dp))
                            Text(state.error ?: "", fontFamily = Mono, fontSize = 12.sp, color = DarkGray)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = vm::loadEvents,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Black),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Black)
                            ) {
                                Text("RETRY", fontFamily = Mono, fontSize = 11.sp)
                            }
                        }
                    }
                    state.events.isEmpty() -> {
                        Column(
                            Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("—", fontSize = 32.sp, color = LightGray)
                            Spacer(Modifier.height(12.dp))
                            Text("No events assigned to you.", fontFamily = Mono,
                                fontSize = 12.sp, color = MidGray)
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(state.events, key = { it.id }) { event ->
                                EventCard(event = event, onClick = { onEventClick(event.id) })
                                HorizontalDivider(color = BorderSubtle)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Event card with countdown ─────────────────────────────────────────────────

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    val dateStr = remember(event.eventDate) {
        try {
            ZonedDateTime.parse(event.eventDate)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.getDefault()))
        } catch (_: Exception) { event.eventDate }
    }

    // Live countdown — ticks every second for upcoming events
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val eventMs = remember(event.eventDate) {
        try { ZonedDateTime.parse(event.eventDate).toInstant().toEpochMilli() }
        catch (_: Exception) { 0L }
    }
    val isUpcoming = eventMs > nowMs
    LaunchedEffect(event.id) {
        while (isUpcoming) { delay(1_000); nowMs = System.currentTimeMillis() }
    }
    val countdown = remember(nowMs, eventMs) { buildCountdown(eventMs, nowMs) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(event.title, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = Black, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            EventStatusBadge(status = event.status)
        }

        Spacer(Modifier.height(4.dp))
        Text(event.clubName.uppercase(), fontFamily = Mono, fontSize = 10.sp,
            letterSpacing = 1.sp, color = MidGray)
        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, tint = MidGray, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(dateStr, fontFamily = Mono, fontSize = 11.sp, color = MidGray)
        }

        if (!event.location.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = MidGray, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(event.location, fontFamily = Mono, fontSize = 11.sp, color = MidGray)
            }
        }

        // Countdown pill — only for upcoming events
        if (countdown != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = Black, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    countdown,
                    fontFamily = Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Black
                )
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

/** Returns a human-readable countdown string, or null if event is in the past. */
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

/** "just now", "2 min ago", "1 hr ago", etc. */
private fun formatRelativeTime(syncedAt: Long, now: Long): String {
    val diff = now - syncedAt
    return when {
        diff < 60_000L              -> "just now"
        diff < 3_600_000L           -> "${diff / 60_000L} min ago"
        diff < 86_400_000L          -> "${diff / 3_600_000L} hr ago"
        else                        -> "${diff / 86_400_000L} days ago"
    }
}

private fun formatConflictTime(iso: String?): String {
    if (iso == null) return "—"
    return try {
        DateTimeFormatter.ofPattern("dd MMM HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    } catch (_: Exception) { iso }
}

@Composable
fun EventStatusBadge(status: String?) {
    val label = when (status?.lowercase()) {
        "ongoing"   -> "ONGOING"
        "completed" -> "DONE"
        else        -> "UPCOMING"
    }
    val (bg, fg) = when (status?.lowercase()) {
        "ongoing"   -> Black to White
        "completed" -> LightGray to MidGray
        else        -> White to Black
    }
    Box(
        modifier = Modifier
            .border(1.dp, if (status?.lowercase() == "ongoing") Black else BorderDefault, RoundedCornerShape(4.dp))
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp,
            color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MonoBadge(text: String, filled: Boolean = false) {
    Box(
        modifier = Modifier
            .border(1.dp, if (filled) Black else BorderDefault, RoundedCornerShape(4.dp))
            .background(if (filled) Black else White, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontFamily = Mono, fontSize = 10.sp, color = if (filled) White else DarkGray)
    }
}
