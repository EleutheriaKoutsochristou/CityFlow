package com.example.cityflow.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geographic distance calculator utilizing the Haversine formula.
 *
 * Implemented in pure Kotlin without dependencies on Android Framework classes (like android.location.Location),
 * allowing it to be directly tested via JVM Unit Tests and used across background modules.
 */
object DistanceCalculator {

    /** Approximate mean radius of the Earth in meters. */
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle distance in meters between two geographic coordinates
     * using the Haversine formula.
     *
     * @param startLatitude Latitude of the starting point in degrees.
     * @param startLongitude Longitude of the starting point in degrees.
     * @param endLatitude Latitude of the destination point in degrees.
     * @param endLongitude Longitude of the destination point in degrees.
     * @return Distance in meters.
     */
    fun distanceBetweenMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        // Convert latitude and longitude differences from degrees to radians
        val dLat = Math.toRadians(endLatitude - startLatitude)
        val dLon = Math.toRadians(endLongitude - startLongitude)

        val originLatRad = Math.toRadians(startLatitude)
        val targetLatRad = Math.toRadians(endLatitude)

        // Apply Haversine trigonometric formula
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(originLatRad) * cos(targetLatRad) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Checks if a target coordinate (e.g., report event) lies within a given circular radius (in meters)
     * from a base coordinate (e.g., user current location).
     *
     * @param userLat Latitude of the user.
     * @param userLon Longitude of the user.
     * @param eventLat Latitude of the event.
     * @param eventLon Longitude of the event.
     * @param radiusMeters Threshold radius in meters.
     * @return True if within the radius, false otherwise.
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
     * Formats a raw distance in meters into a user-friendly string for UI display.
     * Examples: 120 -> "120 m", 1450 -> "1.5 km".
     *
     * @param distanceMeters Distance in meters.
     * @return Formatted distance string.
     */
    fun formatDistance(distanceMeters: Double): String {
        return if (distanceMeters < 1000) {
            "${distanceMeters.toInt()} m"
        } else {
            String.format("%.1f km", distanceMeters / 1000.0)
        }
    }
}
