package com.example.safetyvestinator.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.safetyvestinator.R

object NotificationHelper {
    private const val CHANNEL_ID = "impact_alerts"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Impact alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Fires when the vest detects a hard impact."
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun showImpact(context: Context) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_home)
            .setContentTitle("Impact detected")
            .setContentText("A significant jerk event was reported by the vest.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            try {
                manager.notify(NOTIFICATION_ID, notification)
            } catch (e: SecurityException) {
                // POST_NOTIFICATIONS not granted; silently skip
            }
        }
    }
}