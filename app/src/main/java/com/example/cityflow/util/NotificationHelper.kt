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
 * Βοηθητική κλάση διαχείρισης τοπικών ειδοποιήσεων Android (Nearby Incident Alerts).
 *
 * Υλοποιεί τις προδιαγραφές 4 & 5:
 * - Ειδοποιεί τον πολίτη όταν επικυρωμένο πρόβλημα βρίσκεται εντός της ακτίνας του.
 * - Περιλαμβάνει κατηγορία, απόσταση, πρακτική οδηγία («Αποφύγετε το σημείο» κ.λπ.)
 * - Αποτρέπει επαναλαμβανόμενες ενοχλήσεις για το ίδιο συμβάν μέσω tracking των IDs.
 */
object NotificationHelper {

    const val CHANNEL_ID = "cityflow_alerts_channel"
    private const val CHANNEL_NAME = "CityFlow Mobility Alerts"
    private const val CHANNEL_DESC = "Ειδοποιήσεις για προβλήματα προσβασιμότητας και κινητικότητας κοντά σας"

    private const val PREFS_NAME = "cityflow_prefs"
    private const val KEY_ALERT_COUNT = "received_alerts_count"

    // Καταγραφή των αναφορών για τις οποίες έχει ήδη σταλεί ειδοποίηση σε αυτή τη συνεδρία
    private val notifiedReportIds = ConcurrentHashMap.newKeySet<String>()

    /**
     * Ανάκτηση του συνολικού πλήθους ειδοποιήσεων που έχουν ληφθεί στη συσκευή.
     */
    fun getReceivedAlertsCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ALERT_COUNT, 0)
    }

    /**
     * Αύξηση του μετρητή ληφθεισών ειδοποιήσεων.
     */
    fun incrementAlertCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_ALERT_COUNT, 0)
        prefs.edit().putInt(KEY_ALERT_COUNT, current + 1).apply()
    }

    /**
     * Δημιουργία NotificationChannel (απαραίτητο για Android 8.0+ / API 26+).
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
     * Εμφάνιση ειδοποίησης για επικυρωμένο συμβάν εντός ακτίνας.
     */
    fun showNearbyAlertNotification(
        context: Context,
        report: Report,
        distanceMeters: Double
    ) {
        // Έλεγχος αν έχει ήδη σταλεί ειδοποίηση για το συγκεκριμένο συμβάν
        if (report.id.isNotBlank() && !notifiedReportIds.add(report.id)) {
            return
        }

        // Καταγραφή της ληφθείσας ειδοποίησης για τα στατιστικά
        incrementAlertCount(context)

        // Έλεγχος άδειας POST_NOTIFICATIONS για Android 13+ (API 33+)
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

        // Intent για άνοιγμα της εφαρμογής κατά το πάτημα της ειδοποίησης
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
     * Καθαρισμός του ιστορικού ειδοποιημένων αναφορών (π.χ. κατά το logout).
     */
    fun clearNotifiedReports() {
        notifiedReportIds.clear()
    }
}
