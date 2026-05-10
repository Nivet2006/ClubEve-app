package com.clubeve.cc.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.clubeve.cc.MainActivity
import com.clubeve.cc.R

object AssignmentNotifier {

    private const val CHANNEL_ID   = "assignment_notifications"
    private const val CHANNEL_NAME = "Event Assignments"
    private const val CHANNEL_DESC = "Notifies when you are assigned to a new event"

    /** Call once on app start to register the notification channel (Android 8+). */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }

    /** Posts a notification for a new event assignment. */
    fun notify(context: Context, eventTitle: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New Event Assignment")
            .setContentText("You've been assigned to: $eventTitle")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've been assigned as PR officer for \"$eventTitle\". Open the app to view details."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                System.currentTimeMillis().toInt(), notification
            )
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — silently skip
        }
    }
}
