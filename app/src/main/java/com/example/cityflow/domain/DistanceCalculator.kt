package com.example.cityflow.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Υπολογιστής γεωγραφικών αποστάσεων με χρήση του μαθηματικού τύπου Haversine.
 * Είναι υλοποιημένο σε καθαρή Kotlin ώστε να μπορεί να ελεγχθεί άμεσα με Unit Tests
 * χωρίς εξάρτηση από το Android Framework.
 */
object DistanceCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Υπολογίζει την απόσταση σε μέτρα ανάμεσα σε δύο σημεία συντεταγμένων.
     */
    fun distanceBetweenMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        val dLat = Math.toRadians(endLatitude - startLatitude)
        val dLon = Math.toRadians(endLongitude - startLongitude)

        val originLatRad = Math.toRadians(startLatitude)
        val targetLatRad = Math.toRadians(endLatitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(originLatRad) * cos(targetLatRad) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Ελέγχει αν το σημείο B βρίσκεται εντός της ακτίνας (radiusMeters) από το σημείο A.
     */
    fun isWithinRadius(
        userLat: Double,
        userLon: Double,
        eventLat: Double,
        eventLon: Double,
        radiusMeters: Double
    ): Boolean {
        val distance = distanceBetweenMeters(userLat, userLon, eventLat, eventLon)
        return distance <= radiusMeters
    }

    /**
     * Μορφοποίηση απόστασης για εμφάνιση στο UI (π.χ. "120 m" ή "1.4 km").
     */
    fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters < 1000) {
            "${distanceMeters.toInt()} m"
        } else {
            String.format("%.1f km", distanceMeters / 1000.0)
        }
    }
}
