package com.clubeve.cc.update

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Dialog shown when a newer version is available on GitHub Releases.
 *
 * States:
 *  - Idle      → "Update Now" / "Later" buttons
 *  - Downloading → indeterminate or percentage progress bar, no buttons
 *  - Error     → error message + "Retry" / "Cancel"
 *
 * On download completion the system installer is launched automatically.
 */
@Composable
fun UpdateDialog(
    release: UpdateChecker.ReleaseInfo,
    onDismiss: () -> Unit
) {
    val context: Context = LocalContext.current
    val scope = rememberCoroutineScope()

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
        title = {
            Text("Update available — v${release.latestVersion}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    errorMessage != null -> {
                        Text(
                            "Download failed: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    isDownloading -> {
                        Text(
                            if (downloadPercent >= 0) "Downloading… $downloadPercent%"
                            else "Downloading…",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        if (downloadPercent >= 0) {
                            LinearProgressIndicator(
                                progress = { downloadPercent / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    else -> {
                        if (release.releaseNotes.isNotBlank()) {
                            Text(
                                release.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                isDownloading -> { /* no buttons while downloading */ }
                errorMessage != null -> {
                    TextButton(onClick = { startDownload() }) { Text("Retry") }
                }
                else -> {
                    TextButton(onClick = { startDownload() }) { Text("Update now") }
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(onClick = onDismiss) {
                    Text(if (errorMessage != null) "Cancel" else "Later")
                }
            }
        }
    )
}
