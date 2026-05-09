package com.clubeve.cc.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.data.local.entity.RegistrationEntity
import com.clubeve.cc.data.repository.ScanResult
import com.clubeve.cc.ui.components.*
import com.clubeve.cc.ui.theme.*

@Composable
fun ScanResultScreen(
    eventId: String,
    usn: String,
    onRescan: () -> Unit,
    onViewList: () -> Unit,
    vm: ScanResultViewModel = viewModel()
) {
    LaunchedEffect(usn) { vm.process(usn, eventId) }
    val state by vm.state.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
        contentAlignment = Alignment.Center
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(color = AccentPrimary)
        } else {
            when (val result = state.scanResult) {
                is ScanResult.MarkedPresent -> ResultCard(
                    student = result.student,
                    type = PillType.SUCCESS,
                    label = "✓ Marked Present",
                    onRescan = onRescan,
                    onViewList = onViewList
                )
                is ScanResult.AlreadyPresent -> ResultCard(
                    student = result.student,
                    type = PillType.WARNING,
                    label = "Already Marked Present",
                    onRescan = onRescan,
                    onViewList = onViewList
                )
                is ScanResult.NotRegistered -> NotRegisteredCard(usn = result.usn, onRescan = onRescan)
                null -> {}
            }
        }
    }
}

@Composable
fun ResultCard(
    student: RegistrationEntity,
    type: PillType,
    label: String,
    onRescan: () -> Unit,
    onViewList: () -> Unit
) {
    ClubEveCard(Modifier.fillMaxWidth().padding(24.dp)) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusPill(label, type)
            Spacer(Modifier.height(20.dp))
            Text(student.studentName, style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                student.usn,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color = AccentPrimary
            )
            Text(student.email, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(28.dp))
            PrimaryButton("Scan Next", onRescan)
            Spacer(Modifier.height(8.dp))
            OutlinedAccentButton("View Attendance List", onViewList)
        }
    }
}

@Composable
fun NotRegisteredCard(usn: String, onRescan: () -> Unit) {
    ClubEveCard(Modifier.fillMaxWidth().padding(24.dp)) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusPill("Not Registered", PillType.ERROR)
            Spacer(Modifier.height(16.dp))
            Icon(
                Icons.Default.PersonOff,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                usn,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                color = StatusError
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "This USN is not registered for this event.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Scan Again", onRescan)
        }
    }
}
