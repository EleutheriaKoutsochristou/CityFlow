package com.example.cityflow.domain

import com.example.cityflow.data.model.Report

/**
 * Υπολογιστής προτεραιότητας αναφορών (Backend Ranking Module).
 *
 * Σύμφωνα με την εκφώνηση:
 * PriorityScore = w1 * πλήθος_αναφορών + w2 * χωρική_εγγύτητα + w3 * σοβαρότητα_κατηγορίας
 *
 * - w1: Βάρος για το πλήθος παρόμοιων αναφορών στο ίδιο χρονικό παράθυρο (π.χ. 60 λεπτά).
 * - w2: Βάρος για τη χωρική εγγύτητα (όσο πιο κοντά βρίσκονται οι αναφορές, τόσο υψηλότερο το σκορ).
 * - w3: Βάρος για τη σοβαρότητα της κατηγορίας προβλήματος (1-5).
 */
object PriorityCalculator {

    // Προεπιλεγμένα βάρη (μπορούν να τροποποιηθούν και από τον Διαχειριστή)
    var weightCount: Double = 2.5
    var weightProximity: Double = 1.5
    var weightSeverity: Double = 1.0

    // Χρονικό παράθυρο παρόμοιων αναφορών (60 λεπτά σε milliseconds)
    const val DEFAULT_TIME_WINDOW_MS: Long = 60 * 60 * 1000L

    // Μέγιστη απόσταση για να θεωρηθούν δύο αναφορές ως το ίδιο συμβάν (100 μέτρα)
    const val CLUSTER_RADIUS_METERS: Double = 100.0

    enum class PriorityLevel(val titleEl: String, val titleEn: String) {
        LOW("Χαμηλή", "Low"),
        MEDIUM("Μέτρια", "Medium"),
        HIGH("Υψηλή", "High"),
        CRITICAL("Κρίσιμη", "Critical")
    }

    /**
     * Εντοπίζει παρόμοιες αναφορές (ίδια κατηγορία, κοντινή απόσταση, πρόσφατο χρονικό διάστημα).
     */
    fun findSimilarReports(
        target: Report,
        allReports: List<Report>,
        timeWindowMs: Long = DEFAULT_TIME_WINDOW_MS,
        maxDistanceMeters: Double = CLUSTER_RADIUS_METERS
    ): List<Report> {
        return allReports.filter { candidate ->
            candidate.id != target.id &&
                    candidate.category == target.category &&
                    Math.abs(target.timestamp - candidate.timestamp) <= timeWindowMs &&
                    DistanceCalculator.distanceBetweenMeters(
                        target.latitude,
                        target.longitude,
                        candidate.latitude,
                        candidate.longitude
                    ) <= maxDistanceMeters
        }
    }

    /**
     * Υπολογίζει το τελικό PriorityScore για μια αναφορά.
     */
    fun calculateScore(
        target: Report,
        allReports: List<Report>
    ): Double {
        val similarReports = findSimilarReports(target, allReports)
        val count = similarReports.size + 1 // Συμπεριλαμβάνουμε και την ίδια την αναφορά

        // Υπολογισμός μέσης απόστασης των παρόμοιων αναφορών
        val avgDistance = if (similarReports.isEmpty()) {
            0.0
        } else {
            similarReports.map {
                DistanceCalculator.distanceBetweenMeters(
                    target.latitude,
                    target.longitude,
                    it.latitude,
                    it.longitude
                )
            }.average()
        }

        // Βαθμός χωρικής εγγύτητας (κλίμακα 0..10: όσο πιο κοντά στο 0m, τόσο πλησιάζει το 10)
        val proximityScore = if (similarReports.isEmpty()) {
            5.0
        } else {
            val ratio = (1.0 - (avgDistance / CLUSTER_RADIUS_METERS)).coerceIn(0.0, 1.0)
            ratio * 10.0
        }

        // Σοβαρότητα κατηγορίας (1..5)
        val severity = target.reportCategory.defaultSeverity.toDouble()

        val rawScore = (weightCount * count) + (weightProximity * proximityScore) + (weightSeverity * severity)
        return Math.round(rawScore * 10.0) / 10.0
    }

    /**
     * Κατηγοριοποίηση σε επίπεδο προτεραιότητας για ευανάγνωστα badges στο UI.
     */
    fun getPriorityLevel(score: Double): PriorityLevel {
        return when {
            score >= 25.0 -> PriorityLevel.CRITICAL
            score >= 18.0 -> PriorityLevel.HIGH
            score >= 12.0 -> PriorityLevel.MEDIUM
            else -> PriorityLevel.LOW
        }
    }
}
