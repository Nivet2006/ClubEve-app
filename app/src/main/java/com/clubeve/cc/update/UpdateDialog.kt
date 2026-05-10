package com.clubeve.cc.update

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clubeve.cc.BuildConfig
import com.clubeve.cc.ui.theme.GlassState
import com.clubeve.cc.ui.theme.Mono
import com.clubeve.cc.ui.theme.StatusSuccess
import kotlinx.coroutines.launch

@Composable
fun UpdateDialog(
    release: UpdateChecker.ReleaseInfo,
    onDismiss: () -> Unit
) {
    val context: Context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val isGlass = GlassState.isGlass
    val dialogBg = if (isGlass) Color(0xCC0D0D2B) else cs.surface

    var isDownloading by remember { mutableStateOf(false) }
    var downloadPercent by remember { mutableIntStateOf(-1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startDownload() {
        isDownloading = true
        errorMessage = null
        scope.launch {
            UpdateDownloader.download(context, release.apkDownloadUrl).collect { state ->
                when (state) {
                    is DownloadState.Progress -> downloadPercent = state.percent
                    is DownloadState.Done -> {
                        isDownloading = false
                        AppInstaller.install(context, state.file)
                        onDismiss()
                    }
                    is DownloadState.Error -> {
                        isDownloading = false
                        errorMessage = state.message
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        icon = {
            Icon(Icons.Default.SystemUpdate, null,
                tint = cs.primary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("Update Available", fontFamily = Mono,
                fontWeight = FontWeight.Black, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Version comparison row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CURRENT", fontFamily = Mono, fontSize = 9.sp,
                            letterSpacing = 1.sp, color = cs.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("v${BuildConfig.VERSION_NAME}", fontFamily = Mono,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = cs.onSurface)
                    }
                    Text("→", fontSize = 18.sp, color = cs.onSurfaceVariant)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NEW", fontFamily = Mono, fontSize = 9.sp,
                            letterSpacing = 1.sp, color = StatusSuccess)
                        Spacer(Modifier.height(4.dp))
                        Text("v${release.latestVersion}", fontFamily = Mono,
                            fontWeight = FontWeight.Black, fontSize = 14.sp,
                            color = StatusSuccess)
                    }
                }

                when {
                    errorMessage != null -> {
                        Text("Download failed: $errorMessage",
                            color = cs.error, fontFamily = Mono, fontSize = 12.sp)
                    }
                    isDownloading -> {
                        Text(
                            if (downloadPercent >= 0) "Downloading… $downloadPercent%"
                            else "Downloading…",
                            fontFamily = Mono, fontSize = 12.sp, color = cs.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        if (downloadPercent >= 0) {
                            LinearProgressIndicator(
                                progress = { downloadPercent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = cs.primary
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(),
                                color = cs.primary)
                        }
                    }
                    else -> {
                        if (release.releaseNotes.isNotBlank() &&
                            release.releaseNotes != "Bug fixes and improvements.") {
                            Text(release.releaseNotes, fontFamily = Mono,
                                fontSize = 11.sp, color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                isDownloading -> { /* no buttons while downloading */ }
                errorMessage != null -> {
                    Button(onClick = { startDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary)) {
                        Text("Retry", fontFamily = Mono, fontWeight = FontWeight.Bold,
                            color = cs.onPrimary)
                    }
                }
                else -> {
                    Button(onClick = { startDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary)) {
                        Text("Update Now", fontFamily = Mono, fontWeight = FontWeight.Bold,
                            color = cs.onPrimary)
                    }
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(if (errorMessage != null) "Cancel" else "Later",
                        fontFamily = Mono, color = cs.onSurfaceVariant)
                }
            }
        },
        containerColor = dialogBg,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(16.dp)
    )
}
