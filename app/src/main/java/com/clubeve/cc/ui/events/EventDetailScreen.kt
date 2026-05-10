package com.clubeve.cc.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.clubeve.cc.models.Event
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onScanQR: () -> Unit,
    onViewAttendees: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val event = remember(state.events, eventId) { state.events.find { it.id == eventId } }
    val isFetching = state.fetchingEventId == eventId
    val snackbarHostState = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(state.fetchSuccess) {
        state.fetchSuccess?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearFetchSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(event?.title?.uppercase() ?: "EVENT", fontFamily = Mono,
                        fontWeight = FontWeight.Black, fontSize = 13.sp,
                        letterSpacing = 1.sp, maxLines = 1, color = cs.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = cs.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background, titleContentColor = cs.onBackground)
            )
        },
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 80.dp))
        },
        containerColor = cs.background
    ) { padding ->
        if (event == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            HorizontalDivider(color = cs.outline)

            // Banner
            if (!event.bannerUrl.isNullOrBlank()) {
                AsyncImage(model = event.bannerUrl, contentDescription = "Banner",
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(cs.surface),
                    contentAlignment = Alignment.Center) {
                    Text(event.title.take(2).uppercase(), fontFamily = Mono,
                        fontWeight = FontWeight.Black, fontSize = 40.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.3f))
                }
            }

            HorizontalDivider(color = cs.outline)

            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Row(verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(event.title, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, color = cs.onBackground)
                        Spacer(Modifier.height(2.dp))
                        Text(event.clubName.uppercase(), fontFamily = Mono, fontSize = 10.sp,
                            letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    EventStatusBadge(status = event.status)
                }

                HorizontalDivider(color = cs.outlineVariant)

                InfoLine(Icons.Default.Schedule, formatEventDate(event.eventDate))
                if (!event.location.isNullOrBlank())
                    InfoLine(Icons.Default.LocationOn, event.location)
                if (!event.description.isNullOrBlank())
                    InfoLine(Icons.Default.Info, event.description)

                HorizontalDivider(color = cs.outlineVariant)

                // Stats bar
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .border(1.dp, cs.outline, RoundedCornerShape(8.dp)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${event.registrationCount}", fontFamily = Mono,
                            fontWeight = FontWeight.Black, fontSize = 22.sp, color = cs.onBackground)
                        Text("REGISTERED", fontFamily = Mono, fontSize = 9.sp,
                            letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                    }
                    Box(Modifier.width(1.dp).height(36.dp).background(cs.outline))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${event.attendanceCount}", fontFamily = Mono,
                            fontWeight = FontWeight.Black, fontSize = 22.sp, color = cs.onBackground)
                        Text("PRESENT", fontFamily = Mono, fontSize = 9.sp,
                            letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                    }
                    Box(Modifier.width(1.dp).height(36.dp).background(cs.outline))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val pct = if (event.registrationCount > 0)
                            (event.attendanceCount * 100 / event.registrationCount) else 0L
                        Text("$pct%", fontFamily = Mono, fontWeight = FontWeight.Black,
                            fontSize = 22.sp, color = cs.onBackground)
                        Text("RATE", fontFamily = Mono, fontSize = 9.sp,
                            letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                    }
                }

                LinearProgressIndicator(
                    progress = {
                        if (event.registrationCount > 0)
                            (event.attendanceCount.toFloat() / event.registrationCount.toFloat()).coerceIn(0f, 1f)
                        else 0f
                    },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = cs.primary,
                    trackColor = cs.surfaceVariant
                )

                HorizontalDivider(color = cs.outlineVariant)

                // Scan QR + Attendees
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onScanQR,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null,
                            modifier = Modifier.size(16.dp), tint = cs.onPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("SCAN QR", fontFamily = Mono, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, letterSpacing = 1.sp, color = cs.onPrimary)
                    }
                    OutlinedButton(
                        onClick = onViewAttendees,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cs.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onBackground)
                    ) {
                        Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ATTENDEES", fontFamily = Mono, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }

                // Fetch for offline
                OutlinedButton(
                    onClick = { vm.fetchAndCacheForOffline(eventId) },
                    enabled = !isFetching,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (isFetching) cs.outline else cs.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onBackground)
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp),
                            color = cs.primary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("FETCHING…", fontFamily = Mono, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                    } else {
                        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("FETCH REGISTERED LIST", fontFamily = Mono,
                            fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = cs.onSurfaceVariant,
            modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = Mono, fontSize = 12.sp, color = cs.onSurface)
    }
}

private fun formatEventDate(dateStr: String): String = try {
    ZonedDateTime.parse(dateStr)
        .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy · HH:mm", Locale.getDefault()))
} catch (_: Exception) { dateStr }
