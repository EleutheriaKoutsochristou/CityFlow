package com.example.cityflow.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Core entity model representing an urban incident or accessibility issue.
 * Persisted in the "reports" collection in Cloud Firestore.
 *
 * Default parameter values ensure seamless reflection serialization / deserialization with Firestore.
 *
 * @property id Unique Firestore document identifier.
 * @property userId Firebase Auth UID of the reporting citizen.
 * @property userEmail Account email of the reporting citizen.
 * @property category Category key corresponding to [ReportCategory] enum name.
 * @property description Detailed issue description provided by the citizen.
 * @property latitude GPS latitude coordinate of the reported problem.
 * @property longitude GPS longitude coordinate of the reported problem.
 * @property address Geocoded human-readable street address or location description.
 * @property imageBase64 Base64-encoded compressed image captured at the scene.
 * @property timestamp Epoch timestamp (ms) when the report was submitted.
 * @property status Current lifecycle status key corresponding to [ReportStatus] enum name.
 * @property priorityScore Algorithmic priority ranking score computed by PriorityCalculator.
 * @property notificationRadiusMeters Geofence proximity alert radius around this location in meters.
 * @property verifiedAt Epoch timestamp (ms) when verified by a municipal employee, if applicable.
 * @property verifiedBy UID or name of municipal staff who verified the report.
 * @property similarReportsCount Count of clustered/correlated reports in this incident area.
 * @property practicalAdvice Practical safety or routing advice attached to the incident.
 * @property rejectionReason Rejection explanation if marked invalid by municipal employee.
 * @property resolutionAnnouncement Official public announcement explaining how the issue was fixed.
 * @property resolvedAt Epoch timestamp (ms) when marked as resolved.
 */
@IgnoreExtraProperties
data class Report(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val category: String = ReportCategory.OTHER.name,
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = ReportStatus.SUBMITTED.name,
    val priorityScore: Double = 0.0,
    val notificationRadiusMeters: Double = 200.0,
    val verifiedAt: Long? = null,
    val verifiedBy: String? = null,
    val similarReportsCount: Int = 1,
    val practicalAdvice: String = "",
    val rejectionReason: String = "",
    val resolutionAnnouncement: String = "",
    val resolvedAt: Long? = null
) {
    /** Resolved [ReportCategory] enum object. */
    @get:Exclude
    val reportCategory: ReportCategory
        get() = ReportCategory.fromString(category)

    /** Resolved [ReportStatus] enum object. */
    @get:Exclude
    val reportStatus: ReportStatus
        get() = ReportStatus.fromString(status)

    /** Indicates whether the report has passed the verification stage and is acknowledged. */
    @get:Exclude
    val isVerified: Boolean
        get() = reportStatus == ReportStatus.VERIFIED ||
                reportStatus == ReportStatus.IN_PROGRESS ||
                reportStatus == ReportStatus.RESOLVED
}
