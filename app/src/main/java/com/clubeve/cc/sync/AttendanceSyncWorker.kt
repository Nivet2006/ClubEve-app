package com.clubeve.cc.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class AttendanceSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val report = SyncManager.syncPendingCheckIns(applicationContext)
        return if (report.failed == 0) Result.success() else Result.retry()
    }

    companion object {
        const val TAG = "attendance_sync"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
