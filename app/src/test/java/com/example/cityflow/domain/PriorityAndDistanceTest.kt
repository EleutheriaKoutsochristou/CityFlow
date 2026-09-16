package com.example.cityflow.domain

import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportCategory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests για τον έλεγχο της επιχειρησιακής λογικής (Domain Logic):
 * 1. Υπολογισμοί αποστάσεων (DistanceCalculator)
 * 2. Αλγόριθμος προτεραιότητας & ομαδοποίησης αναφορών (PriorityCalculator)
 */
class PriorityAndDistanceTest {

    @Before
    fun setUp() {
        // Επαναφορά προεπιλεγμένων βαρών
        PriorityCalculator.weightCount = 2.5
        PriorityCalculator.weightProximity = 1.5
        PriorityCalculator.weightSeverity = 1.0
    }

    @Test
    fun testHaversineDistance_SameLocation_ReturnsZero() {
        val distance = DistanceCalculator.distanceBetweenMeters(
            startLatitude = 37.9838,
            startLongitude = 23.7275,
            endLatitude = 37.9838,
            endLongitude = 23.7275
        )
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun testIsWithinRadius_InsideRadius_ReturnsTrue() {
        // Σημεία με απόσταση περίπου 50 μέτρα
        val userLat = 37.983810
        val userLon = 23.727538
        val eventLat = 37.984000
        val eventLon = 23.727800

        val distance = DistanceCalculator.distanceBetweenMeters(userLat, userLon, eventLat, eventLon)
        val isWithin = DistanceCalculator.isWithinRadius(userLat, userLon, eventLat, eventLon, radiusMeters = 150.0)

        assertTrue(distance < 150.0)
        assertTrue(isWithin)
    }

    @Test
    fun testIsWithinRadius_OutsideRadius_ReturnsFalse() {
        // Σημεία στην Αθήνα και στη Θεσσαλονίκη
        val athensLat = 37.9838
        val athensLon = 23.7275
        val thessalonikiLat = 40.6401
        val thessalonikiLon = 22.9444

        val isWithin = DistanceCalculator.isWithinRadius(athensLat, athensLon, thessalonikiLat, thessalonikiLon, radiusMeters = 500.0)
        assertFalse(isWithin)
    }

    @Test
    fun testPriorityCalculator_SingleReport_CalculatesBasicScore() {
        val report = Report(
            id = "rep_1",
            category = ReportCategory.ROAD_WORK.name, // Severity = 5
            latitude = 37.9838,
            longitude = 23.7275,
            timestamp = System.currentTimeMillis()
        )

        val score = PriorityCalculator.calculateScore(report, emptyList())
        // Formula: w1 * 1 + w2 * 5.0 (default proximity) + w3 * 5.0 = 2.5*1 + 1.5*5.0 + 1.0*5.0 = 2.5 + 7.5 + 5.0 = 15.0
        assertEquals(15.0, score, 0.5)
    }

    @Test
    fun testPriorityCalculator_ClusteredReports_IncreasesScore() {
        val now = System.currentTimeMillis()

        val mainReport = Report(
            id = "rep_main",
            category = ReportCategory.ROAD_WORK.name,
            latitude = 37.983800,
            longitude = 23.727500,
            timestamp = now
        )

        val nearbyReport1 = Report(
            id = "rep_2",
            category = ReportCategory.ROAD_WORK.name,
            latitude = 37.983810,
            longitude = 23.727510,
            timestamp = now - (10 * 60 * 1000L) // 10 λεπτά πριν
        )

        val nearbyReport2 = Report(
            id = "rep_3",
            category = ReportCategory.ROAD_WORK.name,
            latitude = 37.983820,
            longitude = 23.727520,
            timestamp = now - (15 * 60 * 1000L) // 15 λεπτά πριν
        )

        val existingList = listOf(nearbyReport1, nearbyReport2)

        val scoreSingle = PriorityCalculator.calculateScore(mainReport, emptyList())
        val scoreClustered = PriorityCalculator.calculateScore(mainReport, existingList)

        // Με 3 όμοιες αναφορές στον ίδιο τόπο και χρόνο, το σκορ πρέπει να είναι σαφώς υψηλότερο
        assertTrue(scoreClustered > scoreSingle)
    }

    @Test
    fun testAdminWeightModification_AltersCalculatedScore() {
        val report = Report(
            id = "rep_1",
            category = ReportCategory.RAMP_BLOCKED.name,
            latitude = 37.9838,
            longitude = 23.7275,
            timestamp = System.currentTimeMillis()
        )

        val initialScore = PriorityCalculator.calculateScore(report, emptyList())

        // Ο διαχειριστής αυξάνει το βάρος σοβαρότητας w3
        PriorityCalculator.weightSeverity = 5.0
        val higherSeverityScore = PriorityCalculator.calculateScore(report, emptyList())

        assertTrue(higherSeverityScore > initialScore)
    }
}
