package com.clubeve.cc.ui.cc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CcReportScreen(
    eventId: String,
    onBack: () -> Unit,
    vm: CcReportViewModel = viewModel()
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
                    Text("ACTIVITY REPORT", fontFamily = Mono, fontWeight = FontWeight.Black,
                        fontSize = 13.sp, letterSpacing = 1.sp, color = cs.onBackground)
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
        containerColor = cs.background
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.primary, strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp))
            }
            return@Scaffold
        }

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
                // Status badge if already submitted
                if (state.existingStatus == "pending_pr") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cs.outline, RoundedCornerShape(8.dp))
                            .background(cs.surface, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.HourglassEmpty, null,
                            tint = cs.primary, modifier = Modifier.size(14.dp))
                        Text("Submitted — awaiting PR audit",
                            fontFamily = Mono, fontSize = 11.sp, color = cs.primary)
                    }
                }

                // ── Summary ───────────────────────────────────────────────────
                SectionLabel("EXECUTIVE SUMMARY")
                OutlinedTextField(
                    value = state.summary,
                    onValueChange = vm::onSummaryChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    placeholder = {
                        Text("Describe what happened at the event, key highlights, and overall outcome…",
                            fontFamily = Mono, fontSize = 12.sp,
                            color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                    },
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    colors = reportFieldColors(cs)
                )

                // ── Outcomes ──────────────────────────────────────────────────
                SectionLabel("OUTCOMES ACHIEVED")
                state.outcomes.forEachIndexed { index, outcome ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = outcome,
                            onValueChange = { vm.onOutcomeChange(index, it) },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text("Outcome ${index + 1}…", fontFamily = Mono, fontSize = 12.sp,
                                    color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            colors = reportFieldColors(cs)
                        )
                        if (state.outcomes.size > 1) {
                            IconButton(
                                onClick = { vm.removeOutcome(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, "Remove",
                                    tint = StatusError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                TextButton(
                    onClick = vm::addOutcome,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.AddCircleOutline, null,
                        modifier = Modifier.size(14.dp), tint = cs.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Add outcome", fontFamily = Mono, fontSize = 11.sp, color = cs.primary)
                }

                // ── Photo URLs ────────────────────────────────────────────────
                SectionLabel("PHOTO URLS (optional)")
                state.photos.forEachIndexed { index, url ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { vm.onPhotoChange(index, it) },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text("https://…", fontFamily = Mono, fontSize = 12.sp,
                                    color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = reportFieldColors(cs)
                        )
                        if (state.photos.size > 1) {
                            IconButton(
                                onClick = { vm.removePhoto(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, "Remove",
                                    tint = StatusError, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                TextButton(
                    onClick = vm::addPhoto,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(Icons.Default.AddCircleOutline, null,
                        modifier = Modifier.size(14.dp), tint = cs.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Add photo URL", fontFamily = Mono, fontSize = 11.sp, color = cs.primary)
                }

                HorizontalDivider(color = cs.outlineVariant)

                // ── Action buttons ────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.save(eventId, submit = false) },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cs.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onBackground)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp),
                                color = cs.primary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, null,
                                modifier = Modifier.size(14.dp), tint = cs.onBackground)
                            Spacer(Modifier.width(6.dp))
                            Text("SAVE DRAFT", fontFamily = Mono, fontWeight = FontWeight.Bold,
                                fontSize = 10.sp, letterSpacing = 1.sp)
                        }
                    }
                    Button(
                        onClick = { vm.save(eventId, submit = true) },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp),
                                color = cs.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, null,
                                modifier = Modifier.size(14.dp), tint = cs.onPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text("SUBMIT", fontFamily = Mono, fontWeight = FontWeight.Bold,
                                fontSize = 10.sp, letterSpacing = 1.sp, color = cs.onPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontFamily = Mono, fontWeight = FontWeight.Bold,
        fontSize = 9.sp, letterSpacing = 2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun reportFieldColors(cs: ColorScheme) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = cs.primary,
    unfocusedBorderColor = cs.outline,
    focusedContainerColor = cs.surface,
    unfocusedContainerColor = cs.surface,
    focusedTextColor = cs.onSurface,
    unfocusedTextColor = cs.onSurface,
    cursorColor = cs.primary,
    focusedLabelColor = cs.primary,
    unfocusedLabelColor = cs.onSurfaceVariant
)
