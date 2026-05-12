package com.clubeve.cc.ui.cc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.SessionManager
import com.clubeve.cc.models.ApprovalStatus
import com.clubeve.cc.models.CcEvent
import com.clubeve.cc.models.PipelineStats
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CcDashboardScreen(
    onEventClick: (eventId: String) -> Unit,
    onLogout: () -> Unit,
    vm: CcDashboardViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme
    val profile = SessionManager.currentProfile

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val filteredEvents = remember(state.events, searchQuery) {
        if (searchQuery.isBlank()) state.events
        else state.events.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.clubName.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) { if (state.events.isEmpty()) vm.load() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

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
            containerColor = cs.surface,
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 80.dp)) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "MY PIPELINE",
                            fontFamily = Mono,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                            color = cs.onBackground
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
                    IconButton(onClick = { searchActive = !searchActive; if (!searchActive) searchQuery = "" }) {
                        Icon(
                            if (searchActive) Icons.Default.SearchOff else Icons.Default.Search,
                            "Search",
                            tint = cs.onSurfaceVariant
                        )
                    }
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

            // Search bar — shown when active
            if (searchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text("Search events…", fontFamily = Mono, fontSize = 13.sp,
                            color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear", tint = cs.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
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
                HorizontalDivider(color = cs.outline)
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = vm::load,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && state.events.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp))
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Stats grid
                            item {
                                PipelineStatsGrid(stats = state.stats, cs = cs)
                            }

                            // Section header
                            item {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "RECENT PIPELINE ACTIVITY",
                                    fontFamily = Mono,
                                    fontSize = 9.sp,
                                    letterSpacing = 2.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                HorizontalDivider(color = cs.outline)
                            }

                            if (filteredEvents.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("—", fontSize = 28.sp, color = cs.onSurfaceVariant)
                                            Text(
                                                if (searchQuery.isNotBlank()) "No events match \"$searchQuery\""
                                                else "No events in your pipeline yet.",
                                                fontFamily = Mono,
                                                fontSize = 12.sp,
                                                color = cs.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(filteredEvents, key = { it.id }) { event ->
                                    CcEventRow(event = event, onClick = { onEventClick(event.id) })
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

// ── Stats grid ────────────────────────────────────────────────────────────────

@Composable
private fun PipelineStatsGrid(stats: PipelineStats, cs: ColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "DRAFTS",
            value = stats.drafts,
            icon = Icons.Default.Edit,
            iconTint = cs.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "IN REVIEW",
            value = stats.pending,
            icon = Icons.Default.HourglassEmpty,
            iconTint = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "APPROVED",
            value = stats.approved,
            icon = Icons.Default.CheckCircle,
            iconTint = StatusSuccess,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "REJECTED",
            value = stats.rejected,
            icon = Icons.Default.Cancel,
            iconTint = StatusError,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Int,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .border(1.dp, cs.outline, RoundedCornerShape(10.dp))
            .background(cs.surface, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontFamily = Mono,
                fontSize = 7.sp,
                letterSpacing = 1.sp,
                color = cs.onSurfaceVariant
            )
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            value.toString(),
            fontFamily = Mono,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = cs.onBackground
        )
    }
}

// ── Event row ─────────────────────────────────────────────────────────────────

@Composable
private fun CcEventRow(event: CcEvent, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dateStr = remember(event.eventDate) {
        try {
            ZonedDateTime.parse(event.eventDate)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.getDefault()))
        } catch (_: Exception) { event.eventDate }
    }

    val (statusBg, statusFg, statusLabel) = statusColors(event.approvalStatus, cs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(cs.background)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Initial avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(statusBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                event.title.take(1).uppercase(),
                fontFamily = Mono,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = statusFg
            )
        }

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
                Icon(Icons.Default.Schedule, null, tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(dateStr, fontFamily = Mono, fontSize = 10.sp, color = cs.onSurfaceVariant)
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
        Icon(Icons.Default.ChevronRight, null, tint = cs.onSurfaceVariant,
            modifier = Modifier.size(16.dp))
    }
}

private data class StatusColors(val bg: Color, val fg: Color, val label: String)

@Composable
private fun statusColors(status: String, cs: ColorScheme): StatusColors = when (status) {
    ApprovalStatus.APPROVED -> StatusColors(
        bg = StatusSuccess.copy(alpha = 0.10f),
        fg = StatusSuccess,
        label = "APPROVED"
    )
    ApprovalStatus.REJECTED -> StatusColors(
        bg = StatusError.copy(alpha = 0.10f),
        fg = StatusError,
        label = "REJECTED"
    )
    ApprovalStatus.DRAFT -> StatusColors(
        bg = cs.surfaceVariant,
        fg = cs.onSurfaceVariant,
        label = "DRAFT"
    )
    else -> StatusColors(
        bg = Color(0xFFFEF3C7),
        fg = Color(0xFFB45309),
        label = "IN REVIEW"
    )
}
