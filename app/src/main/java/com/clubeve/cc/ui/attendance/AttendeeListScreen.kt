package com.clubeve.cc.ui.attendance

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.models.Attendee
import com.clubeve.cc.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendeeListScreen(
    eventId: String,
    eventTitle: String = "",
    onBack: () -> Unit,
    vm: AttendeeListViewModel = viewModel()
) {
    LaunchedEffect(eventId) { vm.init(eventId, eventTitle) }

    val state    by vm.state.collectAsState()
    val filtered by vm.filteredAttendees.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val cs = MaterialTheme.colorScheme

    val presentCount = state.attendees.count { it.checkedIn }
    val absentCount  = state.attendees.size - presentCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ATTENDEES", fontFamily = Mono, fontWeight = FontWeight.Black,
                            fontSize = 13.sp, letterSpacing = 2.sp, color = cs.onBackground)
                        if (state.eventTitle.isNotBlank())
                            Text(state.eventTitle, fontFamily = Mono, fontSize = 10.sp,
                                color = cs.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = cs.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = cs.onSurfaceVariant)
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
            HorizontalDivider(color = cs.outline)

            SyncStatusBadge(syncStatus = state.syncStatus)

            // Stats row
            Row(
                Modifier.fillMaxWidth().background(cs.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox("TOTAL", state.attendees.size.toString(), Modifier.weight(1f))
                StatBox("PRESENT", presentCount.toString(), Modifier.weight(1f), highlight = true)
                StatBox("ABSENT", absentCount.toString(), Modifier.weight(1f))
            }

            HorizontalDivider(color = cs.outline)

            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = vm::setSearch,
                placeholder = { Text("Search name or USN…", fontFamily = Mono, fontSize = 12.sp,
                    color = cs.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outline,
                    focusedContainerColor = cs.surface,
                    unfocusedContainerColor = cs.surface,
                    focusedTextColor = cs.onSurface,
                    unfocusedTextColor = cs.onSurface,
                    cursorColor = cs.primary
                )
            )

            // Filter chips
            Row(
                Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttendeeFilter.entries.forEach { f ->
                    val selected = state.filter == f
                    FilterChip(
                        selected = selected,
                        onClick = { vm.setFilter(f) },
                        label = {
                            Text(f.name, fontFamily = Mono, fontWeight = FontWeight.Bold,
                                fontSize = 10.sp, letterSpacing = 1.sp,
                                color = if (selected) cs.onPrimary else cs.onSurface)
                        },
                        shape = RoundedCornerShape(4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.primary,
                            selectedLabelColor = cs.onPrimary,
                            containerColor = cs.surface,
                            labelColor = cs.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = selected,
                            selectedBorderColor = cs.primary,
                            borderColor = cs.outline,
                            borderWidth = 1.dp, selectedBorderWidth = 1.dp
                        )
                    )
                }
            }

            HorizontalDivider(color = cs.outline)

            if (state.error != null) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "", fontFamily = Mono, fontSize = 12.sp, color = cs.error)
                }
                return@Column
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = vm::refresh,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.isLoading && state.attendees.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp))
                    }
                } else if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (state.attendees.isEmpty()) "No registrations yet." else "No students match.",
                            fontFamily = Mono, fontSize = 12.sp, color = cs.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filtered, key = { it.id }) { attendee ->
                            AttendeeRow(attendee)
                            HorizontalDivider(color = cs.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .border(1.dp, if (highlight) cs.primary else cs.outline, RoundedCornerShape(6.dp))
            .background(if (highlight) cs.primary else cs.surface, RoundedCornerShape(6.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = Mono, fontWeight = FontWeight.Black, fontSize = 20.sp,
            color = if (highlight) cs.onPrimary else cs.onSurface)
        Text(label, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp,
            color = if (highlight) cs.onPrimary.copy(alpha = 0.7f) else cs.onSurfaceVariant)
    }
}

@Composable
fun AttendeeRow(attendee: Attendee) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().background(cs.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(attendee.fullName, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, color = cs.onBackground)
            Spacer(Modifier.height(2.dp))
            Text("${attendee.usn} · ${attendee.department}, Sem ${attendee.semester}",
                fontFamily = Mono, fontSize = 10.sp, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (attendee.checkedIn) {
                Box(
                    modifier = Modifier
                        .border(1.dp, cs.primary, RoundedCornerShape(4.dp))
                        .background(cs.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("✓ PRESENT", fontFamily = Mono, fontWeight = FontWeight.Bold,
                        fontSize = 9.sp, letterSpacing = 1.sp, color = cs.onPrimary)
                }
                attendee.checkedInAt?.let { ts ->
                    Spacer(Modifier.height(3.dp))
                    Text("at ${formatTime(ts)}", fontFamily = Mono, fontSize = 9.sp,
                        color = cs.onSurfaceVariant)
                }
            } else {
                Box(
                    modifier = Modifier
                        .border(1.dp, cs.outline, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("ABSENT", fontFamily = Mono, fontSize = 9.sp,
                        letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatTime(iso: String): String = try {
    DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(iso))
} catch (_: Exception) { iso }

@Composable
fun SyncStatusBadge(syncStatus: SyncStatus) {
    val cs = MaterialTheme.colorScheme
    val bgColor = when (syncStatus) {
        SyncStatus.SYNCED  -> cs.surfaceVariant
        SyncStatus.SYNCING -> cs.surfaceVariant
        SyncStatus.OFFLINE -> Color(0xFFFF9500)
    }
    val icon = when (syncStatus) {
        SyncStatus.SYNCED  -> Icons.Default.CheckCircle
        SyncStatus.SYNCING -> Icons.Default.Sync
        SyncStatus.OFFLINE -> Icons.Default.WifiOff
    }
    val label = when (syncStatus) {
        SyncStatus.SYNCED  -> "Live"
        SyncStatus.SYNCING -> "Syncing…"
        SyncStatus.OFFLINE -> "Offline — cached data"
    }
    val textColor = when (syncStatus) {
        SyncStatus.OFFLINE -> Color.White
        else               -> cs.onSurfaceVariant
    }
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    Surface(color = bgColor) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = textColor,
                modifier = Modifier.size(12.dp)
                    .then(if (syncStatus == SyncStatus.SYNCING) Modifier.rotate(rotation) else Modifier))
            Spacer(Modifier.width(6.dp))
            Text(label, fontFamily = Mono, fontSize = 10.sp,
                letterSpacing = 1.sp, color = textColor)
        }
    }
}
