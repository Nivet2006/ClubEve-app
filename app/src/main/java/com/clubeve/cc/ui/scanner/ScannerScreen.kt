package com.clubeve.cc.ui.scanner

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import com.clubeve.cc.ui.components.ConfirmCard
import com.clubeve.cc.ui.theme.*
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = White
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Tab row — minimal B&W
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = White,
                contentColor = Black,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                        color = Black,
                        height = 2.dp
                    )
                },
                divider = { HorizontalDivider(color = BorderDefault) }
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { vm.selectTab(0) },
                    text = {
                        Text("SCAN QR", fontFamily = Mono, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { vm.selectTab(1) },
                    text = {
                        Text("MANUAL USN", fontFamily = Mono, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                )
            }

            // Back row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Black)
                }
                Text("Back to Event", fontFamily = Mono, fontSize = 11.sp, color = MidGray)
            }

            HorizontalDivider(color = BorderDefault)

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
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermission.status.isGranted) {
            if (state.cameraActive) {
                CameraPreview(onQrScanned = { vm.onQrScanned(it, eventId) })
            }
            ScannerOverlay()

            if (state.scanResult == null && !state.isProcessing) {
                Text(
                    "Point at student's QR code",
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 200.dp)
                )
            }

            if (state.isProcessing) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 200.dp)) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().background(White),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("CAMERA PERMISSION REQUIRED", fontFamily = Mono, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, letterSpacing = 1.sp, color = Black)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { cameraPermission.launchPermissionRequest() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Black),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("GRANT PERMISSION", fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.sp, color = White)
                }
            }
        }

        AnimatedVisibility(
            visible = state.scanResult != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            state.scanResult?.let { result ->
                ConfirmCard(
                    result = result,
                    onConfirmCheckIn = { regId, name -> vm.confirmCheckIn(regId, name) },
                    onCancel = vm::resetScanner,
                    onScanNext = vm::resetScanner
                )
            }
        }
    }
}

@Composable
private fun ManualUsnTab(eventId: String, state: ScannerUiState, vm: ScannerViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        Modifier.fillMaxSize().background(White).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("ENTER STUDENT USN", fontFamily = Mono, fontWeight = FontWeight.Bold,
            fontSize = 11.sp, letterSpacing = 1.5.sp, color = MidGray)

        OutlinedTextField(
            value = state.manualUsn,
            onValueChange = vm::onManualUsnChange,
            label = { Text("USN", fontFamily = Mono, fontSize = 11.sp) },
            placeholder = { Text("1GD24CS001", fontFamily = Mono, fontSize = 13.sp, color = LightGray) },
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
                vm.findByManualUsn(eventId)
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Black,
                unfocusedBorderColor = BorderDefault,
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                cursorColor = Black,
                focusedLabelColor = Black,
                unfocusedLabelColor = MidGray
            )
        )

        Button(
            onClick = { keyboardController?.hide(); vm.findByManualUsn(eventId) },
            enabled = !state.isProcessing && state.manualUsn.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Black,
                disabledContainerColor = LightGray
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = White, strokeWidth = 2.dp)
            } else {
                Text("FIND STUDENT", fontFamily = Mono, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, letterSpacing = 1.sp, color = White)
            }
        }

        state.scanResult?.let { result ->
            ConfirmCard(
                result = result,
                onConfirmCheckIn = { regId, name -> vm.confirmCheckIn(regId, name) },
                onCancel = vm::resetScanner,
                onScanNext = vm::resetScanner
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
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, analysis)
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
        drawLine(accent.copy(alpha = 0.6f), Offset(left + 4.dp.toPx(), lineY), Offset(left + rw - 4.dp.toPx(), lineY), 1.5.dp.toPx())
    }
}
