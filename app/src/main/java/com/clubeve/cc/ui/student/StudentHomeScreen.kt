package com.clubeve.cc.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.SessionManager
import com.clubeve.cc.models.Event
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onEventClick: (registrationId: String) -> Unit,
    onLogout: () -> Unit,
    // Hoisted from MainActivity so the attendance FAB lives in the same Box as ThemeToggleFab
    showAttendanceSheet: Boolean = false,
    onAttendanceDismiss: () -> Unit = {},
    vm: StudentHomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme
    val isGlass = GlassState.isGlass

    // When the external FAB opens the sheet, kick off a fresh attendance load
    LaunchedEffect(showAttendanceSheet) {
        if (showAttendanceSheet) vm.loadAttendance()
    }

    // ── Glassmorphism easter egg ───────────────────────────────────────────────
    var titleTapCount by remember { mutableStateOf(0) }
    var lastTapTime   by remember { mutableStateOf(0L) }
    var glassToastTrigger by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(glassToastTrigger) {
        glassToastTrigger?.let { enabled ->
            snackbarHostState.showSnackbar(
                if (enabled) "✦ Glassmorphism enabled" else "✦ Glassmorphism disabled"
            )
        }
    }

    val dialogBg = if (isGlass) Color(0xCC0D0D2B) else cs.surface
    val profile = SessionManager.currentProfile

    LaunchedEffect(Unit) {
        if (state.registrations.isEmpty() && !state.isLoading) vm.load()
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    // ── Attendance bottom sheet (opened by MainActivity FAB) ──────────────────
    if (showAttendanceSheet) {
        ModalBottomSheet(
            onDismissRequest = onAttendanceDismiss,
            containerColor = dialogBg,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            AttendanceSheetContent(
                records = state.attendanceRecords,
                isLoading = state.isAttendanceLoading,
                cs = cs
            )
        }
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
            containerColor = dialogBg,
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
                            color = if (isGlass) GlassAccent else cs.onBackground
                        )
                        if (!profile?.fullName.isNullOrBlank()) {
                            Text(
                                profile!!.fullName.uppercase(),
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
                    state.isLoading && state.registrations.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp))
                        }
                    }
                    state.registrations.isEmpty() -> {
                        Column(
                            Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("—", fontSize = 32.sp, color = cs.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            Text("You haven't registered for any events yet.",
                                fontFamily = Mono, fontSize = 12.sp, color = cs.onSurfaceVariant)
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(state.registrations, key = { it.id }) { reg ->
                                val event = state.events[reg.eventId]
                                StudentEventCard(
                                    registration = reg,
                                    event = event,
                                    onClick = { onEventClick(reg.id) }
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

// ── Attendance bottom sheet content ──────────────────────────────────────────

@Composable
internal fun AttendanceSheetContent(
    records: List<AttendanceRecord>,
    isLoading: Boolean,
    cs: ColorScheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("MY ATTENDANCE", fontFamily = Mono, fontWeight = FontWeight.Black,
                fontSize = 13.sp, letterSpacing = 2.sp, color = cs.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AttendanceLegendPill("PRESENT", StatusSuccess)
                AttendanceLegendPill("ATTENDED", Color(0xFF1565C0))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "PRESENT = attended + feedback given  ·  ATTENDED = QR scanned",
            fontFamily = Mono, fontSize = 9.sp, color = cs.onSurfaceVariant, letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = cs.outline)
        Spacer(Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp))
                }
            }
            records.isEmpty() -> {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("—", fontSize = 28.sp, color = cs.onSurfaceVariant)
                        Text("No attendance records yet.", fontFamily = Mono,
                            fontSize = 12.sp, color = cs.onSurfaceVariant)
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    items(records, key = { it.eventId }) { record ->
                        AttendanceRow(record = record, cs = cs)
                        HorizontalDivider(color = cs.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceRow(record: AttendanceRecord, cs: ColorScheme) {
    val dateStr = remember(record.eventDate) {
        try { ZonedDateTime.parse(record.eventDate)
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.getDefault()))
        } catch (_: Exception) { record.eventDate }
    }
    val checkedInStr = remember(record.checkedInAt) {
        record.checkedInAt?.let {
            try { ZonedDateTime.parse(it)
                .format(DateTimeFormatter.ofPattern("HH:mm, dd MMM", Locale.getDefault()))
            } catch (_: Exception) { null }
        }
    }
    val (statusColor, statusLabel) = when (record.status) {
        AttendanceStatus.PRESENT  -> StatusSuccess to "PRESENT"
        AttendanceStatus.ATTENDED -> Color(0xFF1565C0) to "ATTENDED"
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(cs.background)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(record.eventTitle, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = cs.onBackground)
            if (record.clubName.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(record.clubName.uppercase(), fontFamily = Mono, fontSize = 9.sp,
                    letterSpacing = 1.sp, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(dateStr, fontFamily = Mono, fontSize = 10.sp, color = cs.onSurfaceVariant)
            }
            checkedInStr?.let {
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = statusColor.copy(alpha = 0.7f), modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Checked in at $it", fontFamily = Mono,
                        fontSize = 10.sp, color = cs.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                .background(statusColor.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(statusLabel, fontFamily = Mono, fontWeight = FontWeight.Bold,
                fontSize = 9.sp, letterSpacing = 1.sp, color = statusColor)
        }
    }
}

@Composable
private fun AttendanceLegendPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(label, fontFamily = Mono, fontSize = 8.sp,
            letterSpacing = 0.5.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

// ── Event card ────────────────────────────────────────────────────────────────

@Composable
private fun StudentEventCard(
    registration: StudentRegistration,
    event: Event?,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val title = event?.title ?: "Event"
    val clubName = event?.clubName ?: ""
    val dateStr = remember(event?.eventDate) {
        try { ZonedDateTime.parse(event?.eventDate ?: "")
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.getDefault()))
        } catch (_: Exception) { event?.eventDate ?: "" }
    }

    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .background(cs.background).padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = cs.onBackground, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            val (pillBg, pillFg, pillLabel) = if (registration.checkedIn)
                Triple(cs.primary, cs.onPrimary, "✓ CHECKED IN")
            else
                Triple(cs.background, cs.onBackground, "REGISTERED")
            Box(modifier = Modifier
                .border(1.dp, if (registration.checkedIn) cs.primary else cs.outline,
                    RoundedCornerShape(4.dp))
                .background(pillBg, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text(pillLabel, fontFamily = Mono, fontSize = 9.sp,
                    letterSpacing = 1.sp, color = pillFg, fontWeight = FontWeight.Bold)
            }
        }
        if (clubName.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(clubName.uppercase(), fontFamily = Mono, fontSize = 10.sp,
                letterSpacing = 1.sp, color = cs.onSurfaceVariant)
        }
        if (dateStr.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(dateStr, fontFamily = Mono, fontSize = 11.sp, color = cs.onSurfaceVariant)
            }
        }
        if (!event?.location.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(event!!.location!!, fontFamily = Mono,
                    fontSize = 11.sp, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QrCode, null, tint = cs.primary, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text("Tap to view your QR code", fontFamily = Mono,
                fontSize = 10.sp, color = cs.primary)
        }
    }
}
