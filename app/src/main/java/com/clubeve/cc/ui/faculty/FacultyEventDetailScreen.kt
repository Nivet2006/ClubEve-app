package com.clubeve.cc.ui.faculty

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.SessionManager
import com.clubeve.cc.models.ApprovalStatus
import com.clubeve.cc.models.CcEvent
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.theme.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyEventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    vm: FacultyEventDetailViewModel = viewModel()
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
    LaunchedEffect(state.navigateBack) {
        if (state.navigateBack) {
            vm.consumeNavigateBack()
            onBack()
        }
    }

    Scaffold(
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 80.dp))
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.event?.title?.uppercase() ?: "EVENT REVIEW",
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
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = cs.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            state.event == null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Event not found.",
                        fontFamily = Mono,
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
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
                        // ── Event info ────────────────────────────────────────
                        FacultyEventInfoSection(event = event, cs = cs)

                        HorizontalDivider(color = cs.outlineVariant)

                        // ── Review panel (only if pending for this role) ───────
                        val role = SessionManager.currentProfile?.role
                        val isPendingForRole = when (role) {
                            "teacher" -> event.approvalStatus == ApprovalStatus.PENDING_TEACHER
                            "hod"     -> event.approvalStatus == ApprovalStatus.PENDING_HOD
                            "admin", "manager" -> event.approvalStatus in listOf(
                                ApprovalStatus.PENDING_TEACHER,
                                ApprovalStatus.PENDING_HOD
                            )
                            else -> false
                        }

                        if (isPendingForRole) {
                            FacultyReviewPanel(
                                isSubmitting = state.isSubmitting,
                                onSubmit = { approve, remarks ->
                                    vm.submitDecision(event.id, approve, remarks)
                                },
                                cs = cs
                            )
                            HorizontalDivider(color = cs.outlineVariant)
                        } else if (event.approvalStatus !in listOf(
                                ApprovalStatus.PENDING_TEACHER,
                                ApprovalStatus.PENDING_HOD
                            )
                        ) {
                            // Show read-only status badge for already-decided events
                            AlreadyDecidedBadge(status = event.approvalStatus, cs = cs)
                            HorizontalDivider(color = cs.outlineVariant)
                        }

                        // ── PR Assignment panel (approved events only) ─────────
                        if (event.approvalStatus == ApprovalStatus.APPROVED) {
                            PrAssignmentPanel(
                                eventId = event.id,
                                assignedPrs = state.assignedPrs,
                                isPrLoading = state.isPrLoading,
                                searchResults = state.searchResults,
                                isSearching = state.isSearching,
                                isAssigning = state.isAssigning,
                                onSearch = vm::searchPrs,
                                onAssign = { prId -> vm.assignPr(event.id, prId) },
                                onRemove = { prId -> vm.removePr(event.id, prId) },
                                onClearSearch = vm::clearSearchResults,
                                cs = cs
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Event info section ────────────────────────────────────────────────────────

@Composable
private fun FacultyEventInfoSection(event: CcEvent, cs: ColorScheme) {
    val dateStr = remember(event.eventDate) {
        try {
            ZonedDateTime.parse(event.eventDate)
                .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy · HH:mm", Locale.getDefault()))
        } catch (_: Exception) { event.eventDate }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            event.title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = cs.onBackground
        )
        Text(
            event.clubName.uppercase(),
            fontFamily = Mono,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            color = cs.onSurfaceVariant
        )

        HorizontalDivider(color = cs.outlineVariant)

        FacultyInfoRow(Icons.Default.Schedule, dateStr)
        if (!event.location.isNullOrBlank()) {
            FacultyInfoRow(Icons.Default.LocationOn, event.location)
        }
        if (!event.targetedDepartment.isNullOrBlank()) {
            FacultyInfoRow(Icons.Default.School, "Department: ${event.targetedDepartment}")
        }
        if (!event.description.isNullOrBlank()) {
            FacultyInfoRow(Icons.Default.Info, event.description)
        }
    }
}

@Composable
private fun FacultyInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(14.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = Mono, fontSize = 12.sp, color = cs.onSurface)
    }
}

// ── Already-decided badge ─────────────────────────────────────────────────────

@Composable
private fun AlreadyDecidedBadge(status: String, cs: ColorScheme) {
    val (bg, fg, label, icon) = when (status) {
        ApprovalStatus.APPROVED -> Quad(
            StatusSuccess.copy(alpha = 0.08f),
            StatusSuccess,
            "APPROVED — This event has been published",
            Icons.Default.CheckCircle
        )
        ApprovalStatus.REJECTED -> Quad(
            StatusError.copy(alpha = 0.08f),
            StatusError,
            "REJECTED — This event was declined",
            Icons.Default.Cancel
        )
        else -> Quad(
            Color(0xFFFEF3C7),
            Color(0xFFB45309),
            "IN REVIEW — Awaiting another reviewer",
            Icons.Default.HourglassEmpty
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, fg.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(label, fontFamily = Mono, fontSize = 11.sp, color = fg, fontWeight = FontWeight.Bold)
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

// ── Faculty review panel ──────────────────────────────────────────────────────

@Composable
private fun FacultyReviewPanel(
    isSubmitting: Boolean,
    onSubmit: (approve: Boolean, remarks: String) -> Unit,
    cs: ColorScheme
) {
    var decision by remember { mutableStateOf<Boolean?>(null) }  // true=approve, false=decline
    var remarks by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "REVIEW DECISION",
            fontFamily = Mono,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            color = cs.onSurfaceVariant
        )

        // AUTHORIZE / DECLINE toggle buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // AUTHORIZE button
            val authorizeSelected = decision == true
            OutlinedButton(
                onClick = { decision = true },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (authorizeSelected) 2.dp else 1.dp,
                    color = if (authorizeSelected) StatusSuccess else cs.outline
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (authorizeSelected) StatusSuccess.copy(alpha = 0.08f)
                                     else cs.background,
                    contentColor = if (authorizeSelected) StatusSuccess else cs.onSurfaceVariant
                )
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = if (authorizeSelected) StatusSuccess else cs.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "AUTHORIZE",
                    fontFamily = Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = if (authorizeSelected) StatusSuccess else cs.onSurface
                )
            }

            // DECLINE button
            val declineSelected = decision == false
            OutlinedButton(
                onClick = { decision = false },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (declineSelected) 2.dp else 1.dp,
                    color = if (declineSelected) StatusError else cs.outline
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (declineSelected) StatusError.copy(alpha = 0.08f)
                                     else cs.background,
                    contentColor = if (declineSelected) StatusError else cs.onSurfaceVariant
                )
            ) {
                Icon(
                    Icons.Default.Cancel,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = if (declineSelected) StatusError else cs.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "DECLINE",
                    fontFamily = Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = if (declineSelected) StatusError else cs.onSurface
                )
            }
        }

        // Remarks textarea
        OutlinedTextField(
            value = remarks,
            onValueChange = { remarks = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            placeholder = {
                Text(
                    "Remarks (optional for approval, recommended for decline)…",
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    color = cs.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary,
                unfocusedBorderColor = cs.outline,
                focusedContainerColor = cs.surface,
                unfocusedContainerColor = cs.surface,
                focusedTextColor = cs.onSurface,
                unfocusedTextColor = cs.onSurface,
                cursorColor = cs.primary
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = Mono,
                fontSize = 13.sp
            )
        )

        // Submit button
        Button(
            onClick = { decision?.let { onSubmit(it, remarks) } },
            enabled = decision != null && !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (decision) {
                    true  -> StatusSuccess
                    false -> StatusError
                    null  -> cs.surfaceVariant
                },
                disabledContainerColor = cs.surfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = cs.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "SUBMIT VERIFICATION",
                    fontFamily = Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = if (decision != null) Color.White else cs.onSurfaceVariant
                )
            }
        }
    }
}

// ── PR Assignment panel ───────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PrAssignmentPanel(
    eventId: String,
    assignedPrs: List<PrProfile>,
    isPrLoading: Boolean,
    searchResults: List<PrProfile>,
    isSearching: Boolean,
    isAssigning: Boolean,
    onSearch: (String) -> Unit,
    onAssign: (prId: String) -> Unit,
    onRemove: (prId: String) -> Unit,
    onClearSearch: () -> Unit,
    cs: ColorScheme
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PR ASSIGNMENT",
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = cs.onSurfaceVariant
            )
            Text(
                "${assignedPrs.size}/2",
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (assignedPrs.size >= 2) StatusSuccess else cs.onSurfaceVariant
            )
        }

        // Assigned PR list
        if (isPrLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = cs.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else if (assignedPrs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(8.dp))
                    .background(cs.surface, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No PR officers assigned yet.",
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    color = cs.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline, RoundedCornerShape(8.dp))
                    .background(cs.surface, RoundedCornerShape(8.dp))
            ) {
                assignedPrs.forEachIndexed { index, pr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(cs.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                pr.fullName.take(1).uppercase(),
                                fontFamily = Mono,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = cs.primary
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                pr.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = cs.onBackground
                            )
                            Text(
                                pr.usn.uppercase(),
                                fontFamily = Mono,
                                fontSize = 10.sp,
                                color = cs.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onRemove(pr.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Remove",
                                tint = StatusError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (index < assignedPrs.lastIndex) {
                        HorizontalDivider(
                            color = cs.outlineVariant,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                }
            }
        }

        // Assign button (only if < 2 assigned)
        if (assignedPrs.size < 2) {
            OutlinedButton(
                onClick = {
                    showSearch = !showSearch
                    if (!showSearch) {
                        searchQuery = ""
                        onClearSearch()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, cs.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onBackground)
            ) {
                Icon(
                    if (showSearch) Icons.Default.Close else Icons.Default.PersonAdd,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = cs.onBackground
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (showSearch) "CANCEL SEARCH" else "ASSIGN PR OFFICER",
                    fontFamily = Mono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Search picker
        if (showSearch) {
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val coroutineScope = rememberCoroutineScope()

            // Scroll the search field + results into view whenever results change
            LaunchedEffect(searchResults) {
                if (searchResults.isNotEmpty()) {
                    coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearch(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                        }
                    },
                placeholder = {
                    Text(
                        "Search by name or USN…",
                        fontFamily = Mono,
                        fontSize = 12.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp).padding(2.dp),
                            color = cs.primary,
                            strokeWidth = 2.dp
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; onClearSearch() }) {
                            Icon(
                                Icons.Default.Clear,
                                "Clear",
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
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

            // Search results
            if (searchResults.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline, RoundedCornerShape(8.dp))
                        .background(cs.surface, RoundedCornerShape(8.dp))
                ) {
                    searchResults.forEachIndexed { index, pr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAssigning) {
                                    onAssign(pr.id)
                                    showSearch = false
                                    searchQuery = ""
                                    onClearSearch()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        cs.primary.copy(alpha = 0.08f),
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    pr.fullName.take(1).uppercase(),
                                    fontFamily = Mono,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = cs.primary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    pr.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = cs.onBackground
                                )
                                Text(
                                    pr.usn.uppercase(),
                                    fontFamily = Mono,
                                    fontSize = 10.sp,
                                    color = cs.onSurfaceVariant
                                )
                            }
                            if (isAssigning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = cs.primary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    "Assign",
                                    tint = cs.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < searchResults.lastIndex) {
                            HorizontalDivider(
                                color = cs.outlineVariant,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            } else if (searchQuery.isNotBlank() && !isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline, RoundedCornerShape(8.dp))
                        .background(cs.surface, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No PR officers found for \"$searchQuery\"",
                        fontFamily = Mono,
                        fontSize = 12.sp,
                        color = cs.onSurfaceVariant
                    )
                }
            }
            } // end search Column
        }
    }
}
