package com.clubeve.cc.ui.cc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.theme.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CcFeedbackEditorScreen(
    eventId: String,
    onBack: () -> Unit,
    vm: CcFeedbackEditorViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(eventId) { vm.load(eventId) }
    LaunchedEffect(state.snackbar) {
        state.snackbar?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    // Reorderable list state
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        vm.moveQuestion(from.index - 1, to.index - 1) // -1 for the header item
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 80.dp)) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FEEDBACK QUESTIONS", fontFamily = Mono,
                            fontWeight = FontWeight.Black, fontSize = 13.sp,
                            letterSpacing = 1.sp, color = cs.onBackground)
                        if (state.eventTitle.isNotBlank()) {
                            Text(state.eventTitle.uppercase(), fontFamily = Mono,
                                fontSize = 9.sp, color = cs.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = cs.onBackground)
                    }
                },
                actions = {
                    // Save button in top bar
                    TextButton(
                        onClick = { vm.save(eventId) },
                        enabled = !state.isSaving && state.isDirty
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp),
                                color = cs.primary, strokeWidth = 2.dp)
                        } else {
                            Text("SAVE", fontFamily = Mono, fontWeight = FontWeight.Bold,
                                fontSize = 11.sp, letterSpacing = 1.sp,
                                color = if (state.isDirty) cs.primary else cs.onSurfaceVariant)
                        }
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

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: count + add button
            item {
                HorizontalDivider(color = cs.outline)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "${state.questions.size} QUESTION${if (state.questions.size != 1) "S" else ""}",
                            fontFamily = Mono, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, letterSpacing = 1.sp,
                            color = if (state.questions.size < 3) StatusError else cs.onBackground
                        )
                        Text(
                            "Minimum 3 required",
                            fontFamily = Mono, fontSize = 9.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = vm::addQuestion,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp),
                            tint = cs.onPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("ADD QUESTION", fontFamily = Mono, fontWeight = FontWeight.Bold,
                            fontSize = 10.sp, letterSpacing = 1.sp, color = cs.onPrimary)
                    }
                }
                Spacer(Modifier.height(4.dp))

                // Empty state
                if (state.questions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp)
                            .border(1.dp, cs.outline, RoundedCornerShape(12.dp))
                            .background(cs.surface, RoundedCornerShape(12.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Quiz, null, tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(32.dp))
                            Text("No questions yet.", fontFamily = Mono,
                                fontSize = 12.sp, color = cs.onSurfaceVariant)
                            Text("Tap ADD QUESTION to start building the survey.",
                                fontFamily = Mono, fontSize = 10.sp,
                                color = cs.onSurfaceVariant)
                        }
                    }
                }

                // Drag hint
                if (state.questions.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cs.outlineVariant, RoundedCornerShape(8.dp))
                            .background(cs.surface, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.DragHandle, null, tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(14.dp))
                        Text("Hold the drag handle to reorder questions",
                            fontFamily = Mono, fontSize = 9.sp,
                            letterSpacing = 0.5.sp, color = cs.onSurfaceVariant)
                    }
                }
            }

            // Question cards — reorderable
            items(state.questions, key = { it.id }) { question ->
                ReorderableItem(reorderState, key = question.id) { isDragging ->
                    val elevation by animateDpAsState(
                        if (isDragging) 12.dp else 0.dp, label = "elevation"
                    )
                    QuestionCard(
                        question = question,
                        isDragging = isDragging,
                        elevation = elevation,
                        dragHandle = {
                            Icon(
                                Icons.Default.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .draggableHandle(
                                        onDragStarted = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    )
                            )
                        },
                        onUpdate = { vm.updateQuestion(question.id, it) },
                        onRemove = { vm.removeQuestion(question.id) },
                        cs = cs
                    )
                }
            }
        }
    }
}

// ── Question card ─────────────────────────────────────────────────────────────

@Composable
private fun QuestionCard(
    question: EditableQuestion,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    dragHandle: @Composable () -> Unit,
    onUpdate: (EditableQuestion) -> Unit,
    onRemove: () -> Unit,
    cs: ColorScheme
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = if (isDragging) cs.surfaceVariant else cs.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDragging) cs.primary.copy(alpha = 0.5f) else cs.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row: drag handle + type selector + delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                dragHandle()

                // Type dropdown
                TypeDropdown(
                    selected = question.type,
                    onSelect = { onUpdate(question.copy(type = it, options = emptyList())) },
                    modifier = Modifier.weight(1f),
                    cs = cs
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, "Remove question",
                        tint = StatusError, modifier = Modifier.size(18.dp))
                }
            }

            // Label field
            OutlinedTextField(
                value = question.label,
                onValueChange = { onUpdate(question.copy(label = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Question label", fontFamily = Mono, fontSize = 10.sp) },
                placeholder = {
                    Text("e.g. How was the event?", fontFamily = Mono, fontSize = 12.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                colors = questionFieldColors(cs)
            )

            // Options field — only for choice-based types
            AnimatedVisibility(
                visible = question.hasOptions,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("OPTIONS (comma-separated)", fontFamily = Mono,
                        fontSize = 8.sp, letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                    OutlinedTextField(
                        value = question.options.joinToString(", "),
                        onValueChange = { raw ->
                            val opts = raw.split(",").map { it.trim() }
                            onUpdate(question.copy(options = opts))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Option A, Option B, Option C", fontFamily = Mono,
                                fontSize = 11.sp, color = cs.onSurfaceVariant.copy(alpha = 0.5f))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = questionFieldColors(cs)
                    )
                    Text("Separate each option with a comma",
                        fontFamily = Mono, fontSize = 8.sp, color = cs.onSurfaceVariant)
                }
            }

            // Required toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("REQUIRED", fontFamily = Mono, fontWeight = FontWeight.Bold,
                    fontSize = 9.sp, letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                Switch(
                    checked = question.required,
                    onCheckedChange = { onUpdate(question.copy(required = it)) },
                    modifier = Modifier.height(24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = cs.onPrimary,
                        checkedTrackColor = cs.primary,
                        uncheckedThumbColor = cs.onSurfaceVariant,
                        uncheckedTrackColor = cs.surfaceVariant
                    )
                )
            }
        }
    }
}

// ── Type dropdown ─────────────────────────────────────────────────────────────

@Composable
private fun TypeDropdown(
    selected: QuestionType,
    onSelect: (QuestionType) -> Unit,
    modifier: Modifier = Modifier,
    cs: ColorScheme
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cs.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onBackground),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(questionTypeIcon(selected), null,
                modifier = Modifier.size(14.dp), tint = cs.primary)
            Spacer(Modifier.width(6.dp))
            Text(selected.displayName, fontFamily = Mono, fontSize = 10.sp,
                modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, null,
                modifier = Modifier.size(16.dp), tint = cs.onSurfaceVariant)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(cs.surface)
        ) {
            QuestionType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(questionTypeIcon(type), null,
                                modifier = Modifier.size(14.dp),
                                tint = if (type == selected) cs.primary else cs.onSurfaceVariant)
                            Text(type.displayName, fontFamily = Mono, fontSize = 11.sp,
                                color = if (type == selected) cs.primary else cs.onBackground,
                                fontWeight = if (type == selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    onClick = { onSelect(type); expanded = false }
                )
            }
        }
    }
}

private fun questionTypeIcon(type: QuestionType): ImageVector = when (type) {
    QuestionType.SHORT_TEXT      -> Icons.Default.ShortText
    QuestionType.LONG_TEXT       -> Icons.Default.Notes
    QuestionType.RATING          -> Icons.Default.Star
    QuestionType.MULTIPLE_CHOICE -> Icons.Default.RadioButtonChecked
    QuestionType.CHECKBOXES      -> Icons.Default.CheckBox
    QuestionType.BOOLEAN         -> Icons.Default.ToggleOn
    QuestionType.DROPDOWN        -> Icons.Default.ArrowDropDownCircle
}

@Composable
private fun questionFieldColors(cs: ColorScheme) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = cs.primary,
    unfocusedBorderColor = cs.outline,
    focusedContainerColor = cs.background,
    unfocusedContainerColor = cs.background,
    focusedTextColor = cs.onSurface,
    unfocusedTextColor = cs.onSurface,
    cursorColor = cs.primary,
    focusedLabelColor = cs.primary,
    unfocusedLabelColor = cs.onSurfaceVariant
)
