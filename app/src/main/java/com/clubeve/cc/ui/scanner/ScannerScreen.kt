package com.clubeve.cc.ui.scanner

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.*
import com.clubeve.cc.ui.components.SyncStatusBar
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    eventId: String,
    onScanned: (String) -> Unit,
    onViewList: () -> Unit,
    onBack: () -> Unit,
    vm: ScannerViewModel = viewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val isOnline by vm.isOnlineFlow.collectAsState()
    var scannedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraPermission.status.isGranted) {
            CameraPreview(
                onQrScanned = { usn ->
                    if (!scannedOnce) {
                        scannedOnce = true
                        onScanned(usn)
                    }
                }
            )
            ScannerOverlay()
        } else {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Camera permission required", color = Color.White)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }

        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onViewList) {
                Icon(Icons.AutoMirrored.Filled.List, null, tint = Color.White)
            }
        }

        // Bottom info
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            SyncStatusBar(isOnline = isOnline, pendingCount = 0)
            Text(
                "Point camera at student QR code",
                color = Color.White.copy(0.7f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun CameraPreview(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
            preview
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ScannerOverlay() {
    val reticleSize = 280.dp
    Canvas(Modifier.fillMaxSize()) {
        val rw = reticleSize.toPx()
        val rh = reticleSize.toPx()
        val left = (size.width - rw) / 2
        val top = (size.height - rh) / 2
        val cornerLen = 40.dp.toPx()
        val strokeW = 3.dp.toPx()
        val overlayColor = Color(0xBB000000)

        // Dark surroundings
        drawRect(overlayColor, size = Size(size.width, top))
        drawRect(overlayColor, topLeft = Offset(0f, top + rh), size = Size(size.width, size.height - top - rh))
        drawRect(overlayColor, topLeft = Offset(0f, top), size = Size(left, rh))
        drawRect(overlayColor, topLeft = Offset(left + rw, top), size = Size(size.width - left - rw, rh))

        // Purple corner brackets
        val accent = Color(0xFF7C3AED)
        // Top-left
        drawLine(accent, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = strokeW)
        drawLine(accent, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = strokeW)
        // Top-right
        drawLine(accent, Offset(left + rw, top), Offset(left + rw, top + cornerLen), strokeWidth = strokeW)
        drawLine(accent, Offset(left + rw - cornerLen, top), Offset(left + rw, top), strokeWidth = strokeW)
        // Bottom-left
        drawLine(accent, Offset(left, top + rh - cornerLen), Offset(left, top + rh), strokeWidth = strokeW)
        drawLine(accent, Offset(left, top + rh), Offset(left + cornerLen, top + rh), strokeWidth = strokeW)
        // Bottom-right
        drawLine(accent, Offset(left + rw, top + rh - cornerLen), Offset(left + rw, top + rh), strokeWidth = strokeW)
        drawLine(accent, Offset(left + rw - cornerLen, top + rh), Offset(left + rw, top + rh), strokeWidth = strokeW)
    }
}
