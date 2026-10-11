package com.example.cityflow.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Κεντρικό μοντέλο αναφοράς συμβάντος/προβλήματος.
 * Αποθηκεύεται στη συλλογή "reports" του Firestore.
 *
 * Όλα τα πεδία έχουν default values για απρόσκοπτο serialization/deserialization από το Firestore.
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
    @get:Exclude
    val reportCategory: ReportCategory
        get() = ReportCategory.fromString(category)

    @get:Exclude
    val reportStatus: ReportStatus
        get() = ReportStatus.fromString(status)

    @get:Exclude
    val isVerified: Boolean
        get() = reportStatus == ReportStatus.VERIFIED ||
                reportStatus == ReportStatus.IN_PROGRESS ||
                reportStatus == ReportStatus.RESOLVED
}
