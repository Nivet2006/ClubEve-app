package com.clubeve.cc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubeve.cc.models.ScanResult
import com.clubeve.cc.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ConfirmCard(
    result: ScanResult,
    onConfirmCheckIn: (registrationId: String, studentName: String) -> Unit,
    onCancel: () -> Unit,
    onScanNext: () -> Unit,
    isOffline: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.surface)
            .border(1.dp, cs.outline, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier.width(36.dp).height(3.dp)
                .background(cs.surfaceVariant, RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(20.dp))
        when (result) {
            is ScanResult.Success -> {
                if (result.alreadyCheckedIn)
                    AlreadyCheckedInContent(result, onScanNext)
                else
                    NewCheckInContent(result,
                        onConfirm = { onConfirmCheckIn(result.registrationId, result.studentName) },
                        onCancel, isOffline)
            }
            is ScanResult.Error -> ErrorContent(result.message, onScanNext)
        }
    }
}

@Composable
private fun AlreadyCheckedInContent(result: ScanResult.Success, onScanNext: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(StatusWarning, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(8.dp))
        Text("ALREADY CHECKED IN", fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 10.sp, letterSpacing = 1.5.sp, color = StatusWarning)
    }
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = cs.outlineVariant)
    Spacer(Modifier.height(16.dp))
    Text(result.studentName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = cs.onSurface)
    Spacer(Modifier.height(4.dp))
    Text(result.studentUsn, fontFamily = Mono, fontSize = 12.sp, color = cs.onSurface)
    Text(result.studentDepartment, fontFamily = Mono, fontSize = 11.sp, color = cs.onSurfaceVariant)
    if (!result.checkedInAt.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Text("Checked in at ${formatTimestamp(result.checkedInAt)}",
            fontFamily = Mono, fontSize = 11.sp, color = cs.onSurfaceVariant)
    }
    Spacer(Modifier.height(20.dp))
    Button(onClick = onScanNext, modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
        elevation = ButtonDefaults.buttonElevation(0.dp)) {
        Text("SCAN NEXT", fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.sp, color = cs.onPrimary)
    }
}

@Composable
private fun NewCheckInContent(
    result: ScanResult.Success, onConfirm: () -> Unit,
    onCancel: () -> Unit, isOffline: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(StatusSuccess, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(8.dp))
        Text("STUDENT FOUND", fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 10.sp, letterSpacing = 1.5.sp, color = StatusSuccess)
    }
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = cs.outlineVariant)
    Spacer(Modifier.height(16.dp))
    Text(result.studentName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = cs.onSurface)
    Spacer(Modifier.height(4.dp))
    Text(result.studentUsn, fontFamily = Mono, fontSize = 12.sp, color = cs.onSurface)
    Text("${result.studentDepartment} · Sem ${result.studentSemester}",
        fontFamily = Mono, fontSize = 11.sp, color = cs.onSurfaceVariant)
    if (result.eventTitle.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(result.eventTitle, fontFamily = Mono, fontSize = 11.sp, color = cs.onSurfaceVariant)
    }
    Spacer(Modifier.height(20.dp))
    if (isOffline) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)) {
            Icon(Icons.Default.CloudOff, null, tint = Color(0xFFFF9500),
                modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Saved offline — will sync automatically when online",
                fontFamily = Mono, fontSize = 10.sp, color = Color(0xFFFF9500))
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cs.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface)) {
            Text("CANCEL", fontFamily = Mono, fontWeight = FontWeight.Bold,
                fontSize = 11.sp, letterSpacing = 1.sp)
        }
        Button(onClick = onConfirm, modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
            elevation = ButtonDefaults.buttonElevation(0.dp)) {
            Text("CHECK IN ✓", fontFamily = Mono, fontWeight = FontWeight.Bold,
                fontSize = 11.sp, letterSpacing = 1.sp, color = cs.onPrimary)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onScanAgain: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(cs.error, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(8.dp))
        Text("ERROR", fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 10.sp, letterSpacing = 1.5.sp, color = cs.error)
    }
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = cs.outlineVariant)
    Spacer(Modifier.height(16.dp))
    Text(message, fontFamily = Mono, fontSize = 13.sp, color = cs.onSurface)
    Spacer(Modifier.height(20.dp))
    Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
        elevation = ButtonDefaults.buttonElevation(0.dp)) {
        Text("SCAN AGAIN", fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.sp, color = cs.onPrimary)
    }
}

private fun formatTimestamp(iso: String): String = try {
    DateTimeFormatter.ofPattern("HH:mm, dd MMM")
        .withZone(ZoneId.systemDefault()).format(Instant.parse(iso))
} catch (_: Exception) { iso }
