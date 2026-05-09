package com.clubeve.cc.ui.events

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.models.Event
import com.clubeve.cc.ui.theme.*
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
    val pullRefreshState = rememberPullToRefreshState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Load events when screen first appears — session is guaranteed set at this point
    LaunchedEffect(Unit) {
        if (vm.state.value.events.isEmpty() && !vm.state.value.isLoading) {
            vm.loadEvents()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Sign Out", fontFamily = Mono, fontWeight = FontWeight.Bold, color = Black)
            },
            text = {
                Text("Are you sure you want to sign out?", fontFamily = Mono, fontSize = 13.sp, color = DarkGray)
            },
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
            containerColor = White,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MY EVENTS", fontFamily = Mono, fontWeight = FontWeight.Black,
                            fontSize = 14.sp, letterSpacing = 2.sp, color = Black)
                        Text("PR Attendance", fontFamily = Mono, fontSize = 10.sp, color = MidGray)
                    }
                },
                actions = {
                    IconButton(onClick = vm::loadEvents) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = DarkGray)
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout", tint = DarkGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = White,
                    titleContentColor = Black
                )
            )
        },
        containerColor = White
    ) { padding ->
        // Thin divider under top bar
        Column(Modifier.padding(padding)) {
            HorizontalDivider(color = BorderDefault, thickness = 1.dp)

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = vm::loadEvents,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && state.events.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Black, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
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
                            Text("No events assigned to you.", fontFamily = Mono, fontSize = 12.sp, color = MidGray)
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

@Composable
fun EventCard(event: Event, onClick: () -> Unit) {
    val dateStr = remember(event.eventDate) {
        try {
            ZonedDateTime.parse(event.eventDate)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.getDefault()))
        } catch (_: Exception) { event.eventDate }
    }

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
            Text(
                text = event.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Black,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            EventStatusBadge(status = event.status)
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = event.clubName.uppercase(),
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = MidGray
        )

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

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MonoBadge("${event.registrationCount} registered")
            MonoBadge("${event.attendanceCount} present", filled = event.attendanceCount > 0)
        }
    }
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
        Text(label, fontFamily = Mono, fontSize = 9.sp, letterSpacing = 1.sp, color = fg, fontWeight = FontWeight.Bold)
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
