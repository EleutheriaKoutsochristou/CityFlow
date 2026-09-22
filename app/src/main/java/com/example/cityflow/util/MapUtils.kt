package com.example.cityflow.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Βοηθητική κλάση για άνοιγμα τοποθεσίας σε εξωτερική εφαρμογή χάρτη (π.χ. Google Maps).
 */
object MapUtils {

    /**
     * Ανοίγει την τοποθεσία στον προεπιλεγμένο χάρτη της συσκευής (Google Maps κ.ά.).
     * Σε περίπτωση που δεν υπάρχει εγκατεστημένη εφαρμογή χαρτών, ανοίγει το Google Maps στον browser.
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

        val uriString = if (label.isNotBlank()) {
            "geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})"
        } else {
            "geo:$latitude,$longitude?q=$latitude,$longitude"
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            try {
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
