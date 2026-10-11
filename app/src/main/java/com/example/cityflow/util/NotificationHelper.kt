package com.example.cityflow.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.cityflow.MainActivity
import com.example.cityflow.R
import com.example.cityflow.data.model.Report
import com.example.cityflow.domain.DistanceCalculator
import com.example.cityflow.ui.common.getLocalizedAdvice
import com.example.cityflow.ui.common.getLocalizedDescription
import java.util.concurrent.ConcurrentHashMap

/**
 * Helper utility managing local Android system notifications (Nearby Incident Alerts).
 *
 * Implements Proximity Alert specifications:
 * - Alerts citizen when an active verified incident is within their category radius.
 * - Displays category name, real-time distance, and safety/routing advice.
 * - Deduplicates alerts to avoid multiple notifications for the same incident during a session.
 */
object NotificationHelper {

    const val CHANNEL_ID = "cityflow_alerts_channel"
    private const val CHANNEL_NAME = "CityFlow Mobility Alerts"
    private const val CHANNEL_DESC = "Notifications for accessibility and urban mobility incidents near your location"

    private const val PREFS_NAME = "cityflow_prefs"
    private const val KEY_ALERT_COUNT = "received_alerts_count"

    // Concurrent thread-safe set tracking incident IDs already notified during the active session
    private val notifiedReportIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * Retrieves the total count of proximity notifications delivered to this device.
     */
    fun getReceivedAlertsCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ALERT_COUNT, 0)
    }

    /**
     * Increments the persistent counter of received proximity alerts.
     */
    fun incrementAlertCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_ALERT_COUNT, 0)
        prefs.edit().putInt(KEY_ALERT_COUNT, current + 1).apply()
    }

    /**
     * Creates the Android notification channel required for Android 8.0+ (API level 26+).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Dispatches a high-priority heads-up notification for an incident occurring within user proximity.
     * Checks permissions, deduplicates previously notified IDs, formats localized text,
     * and attaches an intent opening the main activity.
     *
     * @param context Application or Activity context.
     * @param report Incident report details.
     * @param distanceMeters Calculated physical distance to the incident in meters.
     */
    fun showNearbyAlertNotification(
        context: Context,
        report: Report,
        distanceMeters: Double
    ) {
        // Prevent duplicate alerts for the same report in the current session
        if (report.id.isNotBlank() && !notifiedReportIds.add(report.id)) {
            return
        }

        // Record alert count for local device statistics
        incrementAlertCount(context)

        // Verify POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val formattedDistance = DistanceCalculator.formatDistance(distanceMeters)
        val currentLang = AppSettings.currentLanguage.value
        val isEn = currentLang == "en"
        val categoryTitle = report.reportCategory.getTitle(currentLang)
        val advice = report.getLocalizedAdvice(currentLang)
        val description = report.getLocalizedDescription(currentLang)

        val title = "⚠️ $categoryTitle ($formattedDistance)"
        val shortText = if (advice.isNotBlank()) "${if (isEn) "Advice" else "Οδηγία"}: $advice" else description

        val bigText = buildString {
            append(if (isEn) "Distance: $formattedDistance" else "Απόσταση: $formattedDistance")
            if (report.address.isNotBlank()) {
                append(if (isEn) "\nLocation: ${report.address}" else "\nΤοποθεσία: ${report.address}")
            }
            if (advice.isNotBlank()) {
                append(if (isEn) "\n💡 Advice: $advice" else "\n💡 Οδηγία: $advice")
            }
            if (description.isNotBlank()) {
                append(if (isEn) "\nDescription: $description" else "\nΠεριγραφή: $description")
            }
        }

        // PendingIntent to launch the application when the user taps the notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            report.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (report.id.isNotBlank()) report.id.hashCode() else System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Clears tracked notification history (e.g., when the user logs out).
     */
    fun clearNotifiedReports() {
        notifiedReportIds.clear()
    }
}
