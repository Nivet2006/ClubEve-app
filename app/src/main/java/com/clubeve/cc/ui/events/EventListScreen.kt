package com.clubeve.cc.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.data.local.entity.EventEntity
import com.clubeve.cc.ui.components.*
import com.clubeve.cc.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onEventSelected: (String) -> Unit,
    onViewAttendance: (String) -> Unit,
    vm: EventListViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Events", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        containerColor = BackgroundPrimary
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SyncStatusBar(isOnline = state.isOnline, pendingCount = 0)

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = vm::refresh,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (state.events.isEmpty() && !state.isLoading) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No approved events found", color = TextMuted)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = vm::refresh) {
                            Text("Refresh", color = AccentPrimary)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.events, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                onScan = { onEventSelected(event.id) },
                                onViewList = { onViewAttendance(event.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(event: EventEntity, onScan: () -> Unit, onViewList: () -> Unit) {
    val dateStr = remember(event.date) {
        if (event.date > 0L)
            SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(event.date))
        else "Date TBD"
    }

    ClubEveCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Schedule, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Text(dateStr, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
            if (event.venue.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Text(event.venue, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Scan QR", onScan, Modifier.weight(1f))
                OutlinedAccentButton("View List", onViewList, Modifier.weight(1f))
            }
        }
    }
}
