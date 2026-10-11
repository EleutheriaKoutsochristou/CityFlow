package com.example.cityflow.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utility helper for dispatching navigation/location intents to external map applications (e.g. Google Maps).
 */
object MapUtils {

    /**
     * Launches the location in the device's default navigation or map application.
     * If no native map app is installed or intent resolution fails, gracefully falls back to opening Google Maps in a web browser.
     *
     * @param context Application or Activity context.
     * @param latitude Target latitude coordinate.
     * @param longitude Target longitude coordinate.
     * @param label Optional text pin label for the marker on the map.
     */
    fun openMap(context: Context, latitude: Double, longitude: Double, label: String = "") {
        val isEn = AppSettings.currentLanguage.value == "en"
        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(
                context,
                if (isEn) "Coordinates not available" else "Μη διαθέσιμες συντεταγμένες",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Construct standard geo URI with optional label query
        val uriString = if (label.isNotBlank()) {
            "geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})"
        } else {
            "geo:$latitude,$longitude?q=$latitude,$longitude"
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            // Attempt to start native map application
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            try {
                // Fallback to web browser Google Maps search
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (isEn) "Could not open map" else "Δεν ήταν δυνατό το άνοιγμα του χάρτη",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
