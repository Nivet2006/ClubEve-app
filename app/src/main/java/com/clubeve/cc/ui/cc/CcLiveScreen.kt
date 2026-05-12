package com.clubeve.cc.ui.cc

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CcLiveScreen(
    eventId: String,
    onBack: () -> Unit,
    vm: CcLiveViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(eventId) { vm.load(eventId) }

    // Live counts
    val totalCount      = state.attendees.size
    val registeredCount = state.attendees.count { it.status == LiveAttendeeStatus.REGISTERED }
    val scannedCount    = state.attendees.count { it.status == LiveAttendeeStatus.SCANNED }
    val presentCount    = state.attendees.count { it.status == LiveAttendeeStatus.PRESENT }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "LIVE VIEW",
                            fontFamily = Mono,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp,
                            color = cs.onBackground
                        )
                        if (state.eventTitle.isNotBlank()) {
                            Text(
                                state.eventTitle.uppercase(),
                                fontFamily = Mono,
                                fontSize = 9.sp,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = cs.onBackground)
                    }
                },
                actions = {
                    // Live indicator dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(StatusSuccess, RoundedCornerShape(50))
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("LIVE", fontFamily = Mono, fontSize = 9.sp,
                            letterSpacing = 1.sp, color = StatusSuccess,
                            fontWeight = FontWeight.Bold)
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
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp))
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ── Live summary bar ──────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, cs.outline, RoundedCornerShape(10.dp))
                                .background(cs.surface, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            LiveCountChip(label = "TOTAL", count = totalCount,
                                color = cs.onSurfaceVariant)
                            VerticalDivider(modifier = Modifier.height(32.dp), color = cs.outline)
                            LiveCountChip(label = "REGISTERED", count = registeredCount,
                                color = cs.onSurfaceVariant)
                            VerticalDivider(modifier = Modifier.height(32.dp), color = cs.outline)
                            LiveCountChip(label = "SCANNED", count = scannedCount,
                                color = Color(0xFFF59E0B))
                            VerticalDivider(modifier = Modifier.height(32.dp), color = cs.outline)
                            LiveCountChip(label = "PRESENT", count = presentCount,
                                color = StatusSuccess)
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = cs.outline)
                    }

                    if (state.attendees.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("—", fontSize = 28.sp, color = cs.onSurfaceVariant)
                                    Text("No registrations yet.",
                                        fontFamily = Mono, fontSize = 12.sp,
                                        color = cs.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(state.attendees, key = { it.registrationId }) { attendee ->
                            LiveAttendeeRow(attendee = attendee, cs = cs)
                            HorizontalDivider(color = cs.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

// ── Live count chip ───────────────────────────────────────────────────────────

@Composable
private fun LiveCountChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            fontFamily = Mono,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = color
        )
        Text(
            label,
            fontFamily = Mono,
            fontSize = 7.sp,
            letterSpacing = 0.5.sp,
            color = color.copy(alpha = 0.7f)
        )
    }
}

// ── Attendee row ──────────────────────────────────────────────────────────────

@Composable
private fun LiveAttendeeRow(attendee: LiveAttendee, cs: ColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.background)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar initial
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(cs.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                attendee.fullName.take(1).uppercase(),
                fontFamily = Mono,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = cs.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                attendee.fullName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = cs.onBackground
            )
            if (attendee.usn.isNotBlank()) {
                Text(
                    attendee.usn,
                    fontFamily = Mono,
                    fontSize = 10.sp,
                    color = cs.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Status badge
        val (badgeBg, badgeFg, badgeLabel) = when (attendee.status) {
            LiveAttendeeStatus.PRESENT    -> Triple(
                StatusSuccess.copy(alpha = 0.12f), StatusSuccess, "PRESENT"
            )
            LiveAttendeeStatus.SCANNED    -> Triple(
                Color(0xFFFEF3C7), Color(0xFFB45309), "SCANNED"
            )
            LiveAttendeeStatus.REGISTERED -> Triple(
                cs.surfaceVariant, cs.onSurfaceVariant, "REGISTERED"
            )
        }

        Box(
            modifier = Modifier
                .border(1.dp, badgeFg.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .background(badgeBg, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                badgeLabel,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp,
                color = badgeFg
            )
        }
    }
}
