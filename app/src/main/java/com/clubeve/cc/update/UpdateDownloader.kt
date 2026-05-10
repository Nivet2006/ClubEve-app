package com.clubeve.cc.update

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Emitted by [UpdateDownloader.download] to report progress. */
sealed class DownloadState {
    data class Progress(val percent: Int) : DownloadState()
    data class Done(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * Downloads an APK from [apkUrl] into the app's external Downloads directory.
 * Emits [DownloadState] updates as a cold [Flow].
 *
 * Uses a plain [HttpURLConnection] so we can stream with progress without
 * pulling in an extra library — Ktor's streaming API doesn't expose
 * Content-Length reliably enough for a progress bar.
 */
object UpdateDownloader {

    fun download(context: Context, apkUrl: String): Flow<DownloadState> = flow {
        try {
            val destFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "clubeve-update.apk"
            )
            // Remove stale partial download if present
            if (destFile.exists()) destFile.delete()

            val connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 60_000
                connect()
            }

            val totalBytes = connection.contentLength.toLong()
            var downloaded = 0L

            connection.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(8_192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        val percent = if (totalBytes > 0) {
                            ((downloaded * 100) / totalBytes).toInt()
                        } else -1
                        emit(DownloadState.Progress(percent))
                    }
                    output.flush()
                }
            }

            emit(DownloadState.Done(destFile))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)
}
