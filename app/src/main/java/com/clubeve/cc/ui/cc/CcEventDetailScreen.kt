package com.clubeve.cc.ui.cc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.models.ApprovalStatus
import com.clubeve.cc.models.CcEvent
import com.clubeve.cc.models.RejectionRemark
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CcEventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    onLiveView: (eventId: String) -> Unit,
    onFeedbackEditor: (eventId: String) -> Unit,
    vm: CcEventDetailViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(eventId) { vm.load(eventId) }
    LaunchedEffect(state.snackbar) {
        state.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 80.dp)) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.event?.title?.uppercase() ?: "EVENT",
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = cs.onBackground)
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
            state.event == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Event not found.", fontFamily = Mono, fontSize = 13.sp, color = cs.onSurfaceVariant)
                }
            }
            else -> {
                val event = state.event!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    HorizontalDivider(color = cs.outline)

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // ── Status stepper ────────────────────────────────────
                        StatusStepper(status = event.approvalStatus, cs = cs)

                        // ── Rejection remarks ─────────────────────────────────
                        if (event.approvalStatus == ApprovalStatus.REJECTED &&
                            !event.rejectionData.isNullOrEmpty()) {
                            RejectionRemarksCard(remarks = event.rejectionData, cs = cs)
                        }

                        // ── Event info ────────────────────────────────────────
                        EventInfoSection(event = event, registrationCount = state.registrationCount, cs = cs)

                        HorizontalDivider(color = cs.outlineVariant)

                        // ── Feedback toggle (approved events only) ────────────
                        if (event.approvalStatus == ApprovalStatus.APPROVED) {
                            FeedbackToggleRow(
                                isEnabled = event.feedbackOpen,
                                isToggling = state.isTogglingFeedback,
                                onToggle = { vm.toggleFeedback(event.id, event.feedbackOpen) },
                                cs = cs
                            )
                            HorizontalDivider(color = cs.outlineVariant)
                        }

                        // ── Feedback questions editor (draft, rejected, approved) ──
                        OutlinedButton(
                            onClick = { onFeedbackEditor(event.id) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cs.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onBackground)
                        ) {
                            Icon(Icons.Default.Quiz, null,
                                modifier = Modifier.size(16.dp), tint = cs.onBackground)
                            Spacer(Modifier.width(8.dp))
                            val qCount = event.feedbackConfig?.size ?: 0
                            Text(
                                "FEEDBACK QUESTIONS ($qCount)",
                                fontFamily = Mono, fontWeight = FontWeight.Bold,
                                fontSize = 11.sp, letterSpacing = 1.sp, color = cs.onBackground
                            )
                        }

                        HorizontalDivider(color = cs.outlineVariant)

                        // ── Submit for review (draft only) ────────────────────
                        if (event.approvalStatus == ApprovalStatus.DRAFT ||
                            event.approvalStatus == ApprovalStatus.REJECTED) {
                            Button(
                                onClick = { vm.submitForReview(event.id) },
                                enabled = !state.isSubmitting,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                if (state.isSubmitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                        color = cs.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, null,
                                        modifier = Modifier.size(16.dp), tint = cs.onPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (event.approvalStatus == ApprovalStatus.REJECTED)
                                            "RESUBMIT FOR REVIEW" else "SUBMIT FOR REVIEW",
                                        fontFamily = Mono,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.sp,
                                        color = cs.onPrimary
                                    )
                                }
                            }
                        }

                        // ── Live View button (approved events only) ──────────
                        if (event.approvalStatus == ApprovalStatus.APPROVED) {
                            Button(
                                onClick = { onLiveView(event.id) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Icon(Icons.Default.Visibility, null,
                                    modifier = Modifier.size(16.dp), tint = cs.onPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "LIVE VIEW",
                                    fontFamily = Mono,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = cs.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Status stepper ────────────────────────────────────────────────────────────

@Composable
private fun StatusStepper(status: String, cs: ColorScheme) {
    val steps = listOf("Draft", "PR Review", "Teacher", "HOD", "Approved")
    val currentStep = ApprovalStatus.stepIndex(status)
    val isRejected = status == ApprovalStatus.REJECTED

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "APPROVAL PIPELINE",
            fontFamily = Mono,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = cs.onSurfaceVariant
        )
        if (isRejected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StatusError.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .background(StatusError.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Cancel, null, tint = StatusError, modifier = Modifier.size(16.dp))
                Text("REJECTED — Review remarks below and resubmit",
                    fontFamily = Mono, fontSize = 11.sp, color = StatusError,
                    fontWeight = FontWeight.Bold)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, label ->
                    val isDone = index < currentStep
                    val isCurrent = index == currentStep
                    val dotColor = when {
                        isDone    -> StatusSuccess
                        isCurrent -> cs.primary
                        else      -> cs.outline
                    }
                    val textColor = when {
                        isDone    -> StatusSuccess
                        isCurrent -> cs.onBackground
                        else      -> cs.onSurfaceVariant
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 10.dp else 8.dp)
                                .background(dotColor, RoundedCornerShape(50))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label,
                            fontFamily = Mono,
                            fontSize = 7.sp,
                            color = textColor,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    if (index < steps.lastIndex) {
                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .height(1.dp)
                                .background(if (isDone) StatusSuccess else cs.outline)
                        )
                    }
                }
            }
        }
    }
}

// ── Rejection remarks ─────────────────────────────────────────────────────────

@Composable
private fun RejectionRemarksCard(remarks: List<RejectionRemark>, cs: ColorScheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StatusError.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .background(StatusError.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "REVISION REMARKS",
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = StatusError
        )
        remarks.forEach { remark ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    remark.field.uppercase(),
                    fontFamily = Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = cs.onBackground
                )
                Text(
                    remark.reason,
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    color = cs.onSurface
                )
            }
            HorizontalDivider(color = StatusError.copy(alpha = 0.15f))
        }
    }
}

// ── Event info ────────────────────────────────────────────────────────────────

@Composable
private fun EventInfoSection(event: CcEvent, registrationCount: Int, cs: ColorScheme) {
    val dateStr = remember(event.eventDate) {
        try {
            ZonedDateTime.parse(event.eventDate)
                .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy · HH:mm", Locale.getDefault()))
        } catch (_: Exception) { event.eventDate }
    }
    val deadlineStr = remember(event.registrationDeadline) {
        event.registrationDeadline?.let {
            try {
                ZonedDateTime.parse(it)
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm", Locale.getDefault()))
            } catch (_: Exception) { it }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(event.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = cs.onBackground)
        Text(event.clubName.uppercase(), fontFamily = Mono, fontSize = 10.sp,
            letterSpacing = 1.sp, color = cs.onSurfaceVariant)

        HorizontalDivider(color = cs.outlineVariant)

        InfoRow(Icons.Default.Schedule, dateStr)
        if (!event.location.isNullOrBlank()) InfoRow(Icons.Default.LocationOn, event.location)
        if (deadlineStr != null) InfoRow(Icons.Default.EventBusy, "Deadline: $deadlineStr")
        if (!event.description.isNullOrBlank()) InfoRow(Icons.Default.Info, event.description)

        HorizontalDivider(color = cs.outlineVariant)

        // Registration count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cs.outline, RoundedCornerShape(8.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(registrationCount.toString(), fontFamily = Mono,
                    fontWeight = FontWeight.Black, fontSize = 22.sp, color = cs.onBackground)
                Text("REGISTERED", fontFamily = Mono, fontSize = 9.sp,
                    letterSpacing = 1.sp, color = cs.onSurfaceVariant)
            }
            Box(Modifier.width(1.dp).height(36.dp).background(cs.outline))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    event.maxCapacity?.toString() ?: "∞",
                    fontFamily = Mono, fontWeight = FontWeight.Black,
                    fontSize = 22.sp, color = cs.onBackground
                )
                Text("CAPACITY", fontFamily = Mono, fontSize = 9.sp,
                    letterSpacing = 1.sp, color = cs.onSurfaceVariant)
            }
            Box(Modifier.width(1.dp).height(36.dp).background(cs.outline))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                val questions = event.feedbackConfig?.size ?: 0
                Text(questions.toString(), fontFamily = Mono,
                    fontWeight = FontWeight.Black, fontSize = 22.sp,
                    color = if (questions >= 3) cs.onBackground else StatusError)
                Text("QUESTIONS", fontFamily = Mono, fontSize = 9.sp,
                    letterSpacing = 1.sp, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = cs.onSurfaceVariant,
            modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = Mono, fontSize = 12.sp, color = cs.onSurface)
    }
}

// ── Feedback toggle ───────────────────────────────────────────────────────────

@Composable
private fun FeedbackToggleRow(
    isEnabled: Boolean,
    isToggling: Boolean,
    onToggle: () -> Unit,
    cs: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("FEEDBACK COLLECTION", fontFamily = Mono, fontWeight = FontWeight.Bold,
                fontSize = 11.sp, letterSpacing = 1.sp, color = cs.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(
                if (isEnabled) "Students can submit feedback" else "Feedback is closed",
                fontFamily = Mono, fontSize = 10.sp, color = cs.onSurfaceVariant
            )
        }
        if (isToggling) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp),
                color = cs.primary, strokeWidth = 2.dp)
        } else {
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = cs.onPrimary,
                    checkedTrackColor = StatusSuccess,
                    uncheckedThumbColor = cs.onSurfaceVariant,
                    uncheckedTrackColor = cs.surfaceVariant
                )
            )
        }
    }
}



