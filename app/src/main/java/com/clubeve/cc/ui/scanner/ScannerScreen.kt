package com.clubeve.cc.ui.scanner

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.*
import com.clubeve.cc.ui.components.AppSnackbarHost
import com.clubeve.cc.ui.components.ConfirmCard
import com.clubeve.cc.ui.theme.*
import com.clubeve.cc.utils.ScanFeedback
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    eventId: String,
    onBack: () -> Unit,
    vm: ScannerViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Join presence channel for this event
    LaunchedEffect(eventId) {
        vm.joinPresence(eventId)
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = {
            AppSnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 80.dp))
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val cs = MaterialTheme.colorScheme
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Tab row
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = cs.background,
                contentColor = cs.onBackground,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                        color = cs.primary,
                        height = 2.dp
                    )
                },
                divider = { HorizontalDivider(color = cs.outline) }
            ) {
                Tab(selected = state.selectedTab == 0, onClick = { vm.selectTab(0) },
                    text = { Text("SCAN QR", fontFamily = Mono, fontWeight = FontWeight.Bold,
                        fontSize = 11.sp, letterSpacing = 1.sp) })
                Tab(selected = state.selectedTab == 1, onClick = { vm.selectTab(1) },
                    text = { Text("MANUAL USN", fontFamily = Mono, fontWeight = FontWeight.Bold,
                        fontSize = 11.sp, letterSpacing = 1.sp) })
            }

            // Back row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = cs.onBackground)
                }
                Text("Back to Event", fontFamily = Mono, fontSize = 11.sp, color = cs.onSurfaceVariant)
            }

            HorizontalDivider(color = cs.outline)

            when (state.selectedTab) {
                0 -> QRScanTab(eventId, state, vm, cameraPermission)
                1 -> ManualUsnTab(eventId, state, vm)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun QRScanTab(
    eventId: String,
    state: ScannerUiState,
    vm: ScannerViewModel,
    cameraPermission: PermissionState
) {
    val context = LocalContext.current

    // Trigger haptic + beep whenever a new flash arrives
    LaunchedEffect(state.scanFlash) {
        state.scanFlash?.let { flash ->
            when {
                flash.isError            -> ScanFeedback.error(context)
                flash.isAlreadyCheckedIn -> ScanFeedback.warning(context)
                flash.isOffline          -> ScanFeedback.successOffline(context)
                else                     -> ScanFeedback.success(context)
            }
            // Auto-dismiss the flash after 2 seconds
            delay(2_000)
            vm.clearFlash()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermission.status.isGranted) {
            // Camera is always active in continuous mode
            CameraPreview(onQrScanned = { vm.onQrScanned(it, eventId, context) })
            ScannerOverlay()

            // Hint text — only when idle
            if (state.scanFlash == null) {
                Text(
                    "Point at student's QR code",
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 200.dp)
                )
            }

            // Batch counter badge — top-right (my own scans)
            if (state.batchCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "${state.batchCount} checked in",
                            fontFamily = Mono,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Co-PR peer badges — top-left, one per connected peer
            if (state.peerScans.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.peerScans.entries.forEachIndexed { index, (_, count) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64B5F6),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "PR${index + 2}: $count",
                                    fontFamily = Mono,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Flash result card — slides up, auto-dismisses
            AnimatedVisibility(
                visible = state.scanFlash != null,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                state.scanFlash?.let { flash ->
                    ScanFlashCard(flash)
                }
            }

        } else {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val cs = MaterialTheme.colorScheme
                Text("CAMERA PERMISSION REQUIRED", fontFamily = Mono, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, letterSpacing = 1.sp, color = cs.onBackground)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { cameraPermission.launchPermissionRequest() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("GRANT PERMISSION", fontFamily = Mono, fontSize = 11.sp,
                        letterSpacing = 1.sp, color = cs.onPrimary)
                }
            }
        }
    }
}

/**
 * Non-blocking toast-style card shown briefly after each auto-confirmed scan.
 * Does not pause the camera or require any user interaction.
 */
@Composable
private fun ScanFlashCard(flash: ScanFlash) {
    val bgColor: Color
    val dotColor: Color
    val label: String

    when {
        flash.isError -> {
            bgColor = Color(0xFF1A0000)
            dotColor = Color(0xFFFF3B30)
            label = "ERROR"
        }
        flash.isAlreadyCheckedIn -> {
            bgColor = Color(0xFF1A1400)
            dotColor = Color(0xFFFF9500)
            label = "ALREADY CHECKED IN"
        }
        else -> {
            bgColor = Color(0xFF001A00)
            dotColor = Color(0xFF4CAF50)
            label = "CHECKED IN ✓"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Drag handle visual
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(dotColor, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                fontFamily = Mono,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                color = dotColor
            )
        }

        Spacer(Modifier.height(12.dp))

        if (flash.isError) {
            Text(flash.errorMessage, fontFamily = Mono, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
        } else {
            Text(flash.studentName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(flash.studentUsn, fontFamily = Mono, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))

            if (flash.isOffline && !flash.isAlreadyCheckedIn) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "Saved offline — syncs when online",
                        fontFamily = Mono,
                        fontSize = 10.sp,
                        color = Color(0xFFFF9500)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualUsnTab(eventId: String, state: ScannerUiState, vm: ScannerViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxSize().background(cs.background).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("ENTER STUDENT USN", fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.5.sp, color = cs.onSurfaceVariant)

        OutlinedTextField(
            value = state.manualUsn,
            onValueChange = vm::onManualUsnChange,
            label = { Text("USN", fontFamily = Mono, fontSize = 11.sp) },
            placeholder = { Text("1GD24CS001", fontFamily = Mono, fontSize = 13.sp,
                color = cs.onSurfaceVariant.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                vm.findByManualUsn(eventId, context)
            }),
            colors = OutlinedTextFieldDefaults.colors(
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
        )

        Button(
            onClick = { keyboardController?.hide(); vm.findByManualUsn(eventId, context) },
            enabled = !state.isProcessing && state.manualUsn.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.primary,
                disabledContainerColor = cs.surfaceVariant
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp),
                    color = cs.onPrimary, strokeWidth = 2.dp)
            } else {
                Text("FIND STUDENT", fontFamily = Mono, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, letterSpacing = 1.sp, color = cs.onPrimary)
            }
        }

        state.scanResult?.let { result ->
            ConfirmCard(
                result = result,
                onConfirmCheckIn = { regId, name -> vm.confirmCheckIn(regId, name, context) },
                onCancel = vm::resetScanner,
                onScanNext = vm::resetScanner,
                isOffline = state.isOfflineCheckIn
            )
        }
    }
}

@Composable
fun CameraPreview(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            val preview = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val previewUseCase = Preview.Builder().build()
                    .also { it.setSurfaceProvider(preview.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, QRAnalyzer(onQrScanned)) }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        previewUseCase,
                        analysis
                    )
                } catch (e: Exception) { e.printStackTrace() }
            }, ContextCompat.getMainExecutor(ctx))
            preview
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ScannerOverlay() {
    val reticleSize = 240.dp
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "line"
    )

    Canvas(Modifier.fillMaxSize()) {
        val rw = reticleSize.toPx()
        val rh = reticleSize.toPx()
        val left = (size.width - rw) / 2
        val top = (size.height - rh) / 2
        val corner = 28.dp.toPx()
        val sw = 2.5.dp.toPx()
        val overlay = Color(0xCC000000)
        val accent = Color.White

        drawRect(overlay, size = Size(size.width, top))
        drawRect(overlay, topLeft = Offset(0f, top + rh), size = Size(size.width, size.height - top - rh))
        drawRect(overlay, topLeft = Offset(0f, top), size = Size(left, rh))
        drawRect(overlay, topLeft = Offset(left + rw, top), size = Size(size.width - left - rw, rh))

        // White corner brackets
        drawLine(accent, Offset(left, top), Offset(left, top + corner), sw)
        drawLine(accent, Offset(left, top), Offset(left + corner, top), sw)
        drawLine(accent, Offset(left + rw, top), Offset(left + rw, top + corner), sw)
        drawLine(accent, Offset(left + rw - corner, top), Offset(left + rw, top), sw)
        drawLine(accent, Offset(left, top + rh - corner), Offset(left, top + rh), sw)
        drawLine(accent, Offset(left, top + rh), Offset(left + corner, top + rh), sw)
        drawLine(accent, Offset(left + rw, top + rh - corner), Offset(left + rw, top + rh), sw)
        drawLine(accent, Offset(left + rw - corner, top + rh), Offset(left + rw, top + rh), sw)

        // Scan line
        val lineY = top + rh * scanLineY
        drawLine(
            accent.copy(alpha = 0.6f),
            Offset(left + 4.dp.toPx(), lineY),
            Offset(left + rw - 4.dp.toPx(), lineY),
            1.5.dp.toPx()
        )
    }
}
