package com.clubeve.cc.ui.student

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.data.remote.SupabaseClientProvider
import com.clubeve.cc.models.Event
import com.clubeve.cc.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── ViewModel ─────────────────────────────────────────────────────────────────

@Serializable
private data class RegistrationWithToken(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("qr_token") val qrToken: String? = null,
    @SerialName("checked_in") val checkedIn: Boolean = false,
    @SerialName("checked_in_at") val checkedInAt: String? = null
)

data class StudentQrUiState(
    val qrToken: String? = null,
    val event: Event? = null,
    val checkedIn: Boolean = false,
    val checkedInAt: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class StudentQrViewModel : ViewModel() {
    private val client = SupabaseClientProvider.client
    private val _state = MutableStateFlow(StudentQrUiState())
    val state: StateFlow<StudentQrUiState> = _state.asStateFlow()

    fun load(registrationId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val reg = client.from("registrations")
                    .select(columns = Columns.list(
                        "id", "event_id", "qr_token", "checked_in", "checked_in_at"
                    )) {
                        filter { eq("id", registrationId) }
                    }
                    .decodeSingle<RegistrationWithToken>()

                val event = client.from("events")
                    .select(columns = Columns.list(
                        "id", "title", "description", "club_name",
                        "location", "event_date", "status", "banner_url", "max_capacity"
                    )) {
                        filter { eq("id", reg.eventId) }
                    }
                    .decodeSingle<Event>()

                _state.update {
                    it.copy(
                        qrToken = reg.qrToken,
                        event = event,
                        checkedIn = reg.checkedIn,
                        checkedInAt = reg.checkedInAt,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load QR code.")
                }
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQrScreen(
    registrationId: String,
    onBack: () -> Unit,
    vm: StudentQrViewModel = viewModel()
) {
    LaunchedEffect(registrationId) { vm.load(registrationId) }

    val state by vm.state.collectAsState()
    val cs = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.event?.title?.uppercase() ?: "MY QR CODE",
                        fontFamily = Mono,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        color = cs.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            tint = cs.onBackground)
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
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ERROR", fontFamily = Mono, fontWeight = FontWeight.Black,
                            fontSize = 11.sp, letterSpacing = 2.sp, color = StatusError)
                        Text(state.error ?: "", fontFamily = Mono, fontSize = 12.sp,
                            color = cs.onSurface)
                        OutlinedButton(
                            onClick = { vm.load(registrationId) },
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cs.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onBackground)
                        ) {
                            Text("RETRY", fontFamily = Mono, fontSize = 11.sp,
                                color = cs.onBackground)
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(color = cs.outline)

                    Spacer(Modifier.height(32.dp))

                    // ── Check-in status banner ────────────────────────────────
                    if (state.checkedIn) {
                        Surface(
                            color = StatusSuccess.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .border(1.dp, StatusSuccess.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = StatusSuccess, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("CHECKED IN", fontFamily = Mono,
                                        fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                        letterSpacing = 1.sp, color = StatusSuccess)
                                    state.checkedInAt?.let { ts ->
                                        Text(
                                            formatTimestamp(ts),
                                            fontFamily = Mono,
                                            fontSize = 10.sp,
                                            color = StatusSuccess.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── QR code ───────────────────────────────────────────────
                    val token = state.qrToken
                    if (token.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
                                .background(cs.surface, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.QrCode, null,
                                    tint = cs.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp))
                                Text("QR code not available", fontFamily = Mono,
                                    fontSize = 11.sp, color = cs.onSurfaceVariant)
                            }
                        }
                    } else {
                        val qrBitmap = remember(token) { generateQrBitmap(token, 512) }
                        if (qrBitmap != null) {
                            Box(
                                modifier = Modifier
                                    .size(260.dp)
                                    .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
                                    .background(
                                        androidx.compose.ui.graphics.Color.White,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Your QR code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Instruction label ─────────────────────────────────────
                    Text(
                        "SHOW THIS TO THE PR OFFICER",
                        fontFamily = Mono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        color = cs.onSurfaceVariant
                    )

                    Spacer(Modifier.height(32.dp))

                    // ── Event info card ───────────────────────────────────────
                    state.event?.let { event ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
                                .background(cs.surface, RoundedCornerShape(12.dp))
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                event.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = cs.onSurface
                            )
                            if (event.clubName.isNotBlank()) {
                                Text(
                                    event.clubName.uppercase(),
                                    fontFamily = Mono,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    color = cs.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(color = cs.outlineVariant)
                            if (event.eventDate.isNotBlank()) {
                                EventInfoRow(
                                    icon = Icons.Default.Schedule,
                                    text = formatEventDate(event.eventDate)
                                )
                            }
                            if (!event.location.isNullOrBlank()) {
                                EventInfoRow(
                                    icon = Icons.Default.LocationOn,
                                    text = event.location
                                )
                            }
                            if (!event.description.isNullOrBlank()) {
                                EventInfoRow(
                                    icon = Icons.Default.Info,
                                    text = event.description
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun EventInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
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

private fun formatTimestamp(iso: String): String = try {
    ZonedDateTime.parse(iso)
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.getDefault()))
} catch (_: Exception) { iso }

/**
 * Generates a QR code bitmap from [content] at [sizePx] × [sizePx].
 * Returns null if encoding fails.
 */
private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? = try {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bmp.setPixel(x, y, if (bits[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    bmp
} catch (_: Exception) { null }
