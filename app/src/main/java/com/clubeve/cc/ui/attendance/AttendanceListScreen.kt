package com.clubeve.cc.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.data.local.entity.RegistrationEntity
import com.clubeve.cc.ui.components.*
import com.clubeve.cc.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceListScreen(
    eventId: String,
    onScan: () -> Unit,
    onBack: () -> Unit,
    vm: AttendanceListViewModel = viewModel()
) {
    LaunchedEffect(eventId) { vm.init(eventId) }
    val state by vm.state.collectAsState()
    val displayed by vm.displayed.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Default.Refresh, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScan,
                containerColor = AccentPrimary,
                contentColor = TextPrimary
            ) {
                Icon(Icons.Default.QrCodeScanner, "Scan QR")
            }
        },
        containerColor = BackgroundPrimary
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SyncStatusBar(isOnline = state.isOnline, pendingCount = 0)

            // Stats bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip("Total", state.totalCount.toString(), AccentPrimary, Modifier.weight(1f))
                StatChip("Present", state.presentCount.toString(), StatusSuccess, Modifier.weight(1f))
                StatChip(
                    "Absent",
                    (state.totalCount - state.presentCount).toString(),
                    StatusError,
                    Modifier.weight(1f)
                )
            }

            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = vm::setSearch,
                placeholder = { Text("Search name or USN…", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    unfocusedBorderColor = BorderDefault,
                    focusedContainerColor = BackgroundElevated,
                    unfocusedContainerColor = BackgroundElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(Modifier.height(8.dp))

            // Filter tabs
            Row(
                Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AttendanceFilter.values().forEach { f ->
                    FilterChip(
                        selected = state.filter == f,
                        onClick = { vm.setFilter(f) },
                        label = {
                            Text(f.name.lowercase().replaceFirstChar { it.uppercase() })
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGlow,
                            selectedLabelColor = AccentPrimary
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (state.isLoading && displayed.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayed, key = { it.id }) { reg ->
                        RegistrationRow(reg)
                    }
                    if (displayed.isEmpty()) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No students match filter", color = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ClubEveCard(modifier) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
fun RegistrationRow(reg: RegistrationEntity) {
    ClubEveCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(reg.studentName, style = MaterialTheme.typography.labelLarge)
                Text(
                    reg.usn,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = AccentPrimary
                )
                Text(reg.email, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
            StatusPill(
                label = if (reg.isPresent) "Present" else "Absent",
                type = if (reg.isPresent) PillType.SUCCESS else PillType.ERROR
            )
        }
    }
}
