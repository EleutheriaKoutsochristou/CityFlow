package com.example.cityflow.domain

import com.example.cityflow.data.model.Report

/**
 * Incident priority calculator (Backend Ranking Module).
 *
 * Implements the ranking model specified in requirements:
 * `PriorityScore = w1 * count + w2 * proximity + w3 * severity`
 *
 * - w1: Weight for the volume/count of similar reports in the same time window (e.g. 30 minutes).
 * - w2: Weight for spatial proximity (reports closer to the incident increase urgency).
 * - w3: Weight for category severity (inherent base severity from 1 to 5).
 */
object PriorityCalculator {

    // Default algorithm weights (can be tuned dynamically via the Admin Dashboard)
    var weightCount: Double = 2.5
    var weightProximity: Double = 1.5
    var weightSeverity: Double = 1.0

    // Time window for identifying correlated/duplicate reports (30 minutes in milliseconds)
    const val DEFAULT_TIME_WINDOW_MS: Long = 30 * 60 * 1000L

    // Maximum distance radius for reports to be considered part of the same incident cluster (50 meters)
    const val CLUSTER_RADIUS_METERS: Double = 50.0

    /**
     * Enum representing classified priority levels for UI badges and sorting.
     */
    enum class PriorityLevel(val titleEl: String, val titleEn: String) {
        LOW("Χαμηλή", "Low"),
        MEDIUM("Μέτρια", "Medium"),
        HIGH("Υψηλή", "High"),
        CRITICAL("Κρίσιμη", "Critical");

        fun getTitle(lang: String): String = if (lang == "en") titleEn else titleEl
    }

    /**
     * Identifies similar reports based on matching category, geographic proximity (within cluster radius),
     * and temporal proximity (within specified time window).
     *
     * @param target The report being evaluated.
     * @param allReports Collection of all candidate reports.
     * @param timeWindowMs Maximum time difference threshold in milliseconds.
     * @param maxDistanceMeters Maximum geographical distance threshold in meters.
     * @return List of matching correlated reports excluding the target itself.
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
     * Computes the compound Priority Score for a given report relative to existing reports.
     *
     * The formula accounts for:
     * 1. Number of unique reporting users (preventing single-user spam distortion).
     * 2. Average spatial proximity score (0 to 10 scale, higher when closer to center).
     * 3. Base category severity (1 to 5 scale).
     *
     * @param target The report to evaluate.
     * @param allReports All available reports for correlation.
     * @return Calculated priority score rounded to 1 decimal place.
     */
    fun calculateScore(
        target: Report,
        allReports: List<Report>
    ): Double {
        val similarReports = findSimilarReports(target, allReports)
        // Count distinct user IDs contributing to this incident cluster to prevent inflation by a single user
        val uniqueUserIds = (similarReports.map { it.userId } + target.userId)
            .filter { it.isNotBlank() }
            .distinct()
        val count = if (uniqueUserIds.isNotEmpty()) uniqueUserIds.size else (similarReports.size + 1)

        // Calculate average geographic distance of clustered reports from target
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

        // Spatial proximity score (0..10): closer distance to 0m yields higher proximity score
        val proximityScore = if (similarReports.isEmpty()) {
            5.0 // Neutral base proximity when standalone
        } else {
            val ratio = (1.0 - (avgDistance / CLUSTER_RADIUS_METERS)).coerceIn(0.0, 1.0)
            ratio * 10.0
        }

        // Inherent category severity (1..5)
        val severity = target.reportCategory.defaultSeverity.toDouble()

        // Weighted linear combination
        val rawScore = (weightCount * count) + (weightProximity * proximityScore) + (weightSeverity * severity)
        return Math.round(rawScore * 10.0) / 10.0
    }

    /**
     * Maps numeric priority scores to qualitative priority tiers for UI presentation.
     *
     * @param score Calculated numeric priority score.
     * @return [PriorityLevel] tier (LOW, MEDIUM, HIGH, CRITICAL).
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
