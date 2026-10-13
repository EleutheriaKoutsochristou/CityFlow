package com.example.cityflow.data.repository

import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.model.Resource
import com.example.cityflow.domain.DistanceCalculator
import com.example.cityflow.domain.PriorityCalculator
import com.example.cityflow.util.AppSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository handling Incident Reports lifecycle, persistence, and synchronization in Cloud Firestore.
 * Supports real-time updates via Kotlin Coroutines [Flow] (`callbackFlow`) to keep Citizen,
 * Employee, and Statistics screens synchronized instantly across all connected clients.
 */
class ReportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val reportsCollection = firestore.collection("reports")
    private val configDocRef = firestore.collection("system_config").document("priority_weights")

    init {
        // Observe algorithm weights from Cloud Firestore in real time to sync across all devices
        configDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.w("ReportRepository", "Failed to listen to priority weights", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val wCount = snapshot.getDouble("weightCount")
                val wProx = snapshot.getDouble("weightProximity")
                val wSev = snapshot.getDouble("weightSeverity")
                if (wCount != null) PriorityCalculator.weightCount = wCount
                if (wProx != null) PriorityCalculator.weightProximity = wProx
                if (wSev != null) PriorityCalculator.weightSeverity = wSev
            }
        }
    }

    /**
     * Submits a new incident report:
     * 1. Fetches recent reports in the same category to compute spatial/temporal correlation.
     * 2. Checks duplicate submission rules (prevents user from reporting same issue twice within 30 min / 50m).
     * 3. Computes algorithmic priority score using [PriorityCalculator].
     * 4. Atomically commits the new report and updates correlated cluster members in a Firestore batch write.
     *
     * @param report The new report data.
     * @return [Resource.Success] with the generated report document ID, or [Resource.Error].
     */
    suspend fun submitReport(report: Report): Resource<String> {
        return try {
            val docRef = if (report.id.isBlank()) {
                reportsCollection.document()
            } else {
                reportsCollection.document(report.id)
            }

            val docId = docRef.id

            // Retrieve recent reports in the same category to detect clustering and compute priority score
            val recentSnapshots = reportsCollection
                .whereEqualTo("category", report.category)
                .get()
                .await()

            val existingReports = recentSnapshots.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Report::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }

            // Guard against duplicate reports from the same user within 30 minutes and 50 meters
            val hasDuplicateFromSameUser = existingReports.any { existing ->
                existing.userId.isNotBlank() &&
                existing.userId == report.userId &&
                Math.abs(report.timestamp - existing.timestamp) <= PriorityCalculator.DEFAULT_TIME_WINDOW_MS &&
                DistanceCalculator.distanceBetweenMeters(
                    report.latitude,
                    report.longitude,
                    existing.latitude,
                    existing.longitude
                ) <= PriorityCalculator.CLUSTER_RADIUS_METERS
            }

            if (hasDuplicateFromSameUser) {
                val isEn = AppSettings.currentLanguage.value == "en"
                return Resource.Error(
                    if (isEn) "You have already submitted a report for this location in the last 30 minutes. Please wait for it to be processed."
                    else "Έχετε ήδη υποβάλει αναφορά για αυτό το σημείο τα τελευταία 30 λεπτά. Παρακαλώ περιμένετε την επεξεργασία της."
                )
            }

            // Identify similar reports in the same spatio-temporal cluster and calculate priority score
            val similarReports = PriorityCalculator.findSimilarReports(report, existingReports)
            val calculatedScore = PriorityCalculator.calculateScore(report, existingReports)

            val finalReport = report.copy(
                id = docId,
                priorityScore = calculatedScore,
                similarReportsCount = similarReports.size + 1
            )

            // Atomic batch write: write new report and update score/count of all clustered neighbors
            val batch = firestore.batch()
            batch.set(docRef, finalReport)

            val allReportsWithNew = existingReports.filter { it.id != docId } + finalReport
            val clusterCount = similarReports.size + 1

            for (simReport in similarReports) {
                if (simReport.id.isNotBlank()) {
                    val simDocRef = reportsCollection.document(simReport.id)
                    val updatedScore = PriorityCalculator.calculateScore(simReport, allReportsWithNew)
                    batch.update(
                        simDocRef,
                        mapOf(
                            "similarReportsCount" to clusterCount,
                            "priorityScore" to updatedScore
                        )
                    )
                }
            }

            batch.commit().await()
            Resource.Success(docId)
        } catch (e: Exception) {
            val isEn = AppSettings.currentLanguage.value == "en"
            Resource.Error(e.localizedMessage ?: (if (isEn) "Failed to submit report." else "Αποτυχία υποβολής αναφοράς."), e)
        }
    }

    /**
     * Real-time [Flow] stream of all reports ordered by descending timestamp (newest first).
     * Used primarily by the Employee Dashboard and Admin screen.
     */
    fun getAllReportsFlow(): Flow<List<Report>> = callbackFlow {
        val listener = reportsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ReportRepository", "Error listening to all reports", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reports = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Report::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("ReportRepository", "Failed to deserialize report: ${doc.id}", e)
                            null
                        }
                    }
                    trySend(reports)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Real-time [Flow] stream of reports created by a specific user (for My Reports / history).
     *
     * @param userId The author's user ID.
     */
    fun getUserReportsFlow(userId: String): Flow<List<Report>> = callbackFlow {
        val listener = reportsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ReportRepository", "Error listening to user reports for $userId", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reports = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Report::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.timestamp }
                    trySend(reports)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Updates the status of an incident report (e.g. Verified, In Progress, Resolved, Rejected).
     * Records associated metadata such as reviewer, resolution announcement, or rejection reason.
     *
     * @param reportId Unique Firestore document ID.
     * @param newStatus New target [ReportStatus].
     * @param verifiedBy UID/name of verifying staff member.
     * @param practicalAdvice Optional customized safety advice for citizens.
     * @param rejectionReason Justification reason if status is REJECTED.
     * @param resolutionAnnouncement Explanation message if status is RESOLVED.
     */
    suspend fun updateReportStatus(
        reportId: String,
        newStatus: ReportStatus,
        verifiedBy: String? = null,
        practicalAdvice: String? = null,
        rejectionReason: String? = null,
        resolutionAnnouncement: String? = null
    ): Resource<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name
            )

            if (verifiedBy != null) {
                updates["verifiedBy"] = verifiedBy
            }

            when (newStatus) {
                ReportStatus.VERIFIED -> {
                    updates["verifiedAt"] = System.currentTimeMillis()
                    if (!practicalAdvice.isNullOrBlank()) updates["practicalAdvice"] = practicalAdvice
                }
                ReportStatus.REJECTED -> {
                    if (!rejectionReason.isNullOrBlank()) {
                        updates["rejectionReason"] = rejectionReason.trim()
                    }
                }
                ReportStatus.RESOLVED -> {
                    updates["resolvedAt"] = System.currentTimeMillis()
                    if (!resolutionAnnouncement.isNullOrBlank()) {
                        updates["resolutionAnnouncement"] = resolutionAnnouncement.trim()
                    }
                }
                else -> Unit
            }

            reportsCollection.document(reportId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            val isEn = AppSettings.currentLanguage.value == "en"
            Resource.Error(e.localizedMessage ?: (if (isEn) "Failed to update status." else "Αποτυχία ενημέρωσης κατάστασης."), e)
        }
    }

    /**
     * Filters active verified reports that fall within their designated alert radius from the user's current GPS coordinates.
     *
     * @param reports Candidate reports list.
     * @param userLat Current latitude of the citizen.
     * @param userLon Current longitude of the citizen.
     * @return List of nearby active reports.
     */
    fun filterNearbyVerifiedReports(
        reports: List<Report>,
        userLat: Double,
        userLon: Double
    ): List<Report> {
        return reports.filter { report ->
            // Include only active reports (VERIFIED or IN_PROGRESS)
            val isActive = report.status == ReportStatus.VERIFIED.name ||
                    report.status == ReportStatus.IN_PROGRESS.name

            if (!isActive) return@filter false

            val radius = if (report.notificationRadiusMeters > 0) {
                report.notificationRadiusMeters
            } else {
                report.reportCategory.defaultRadiusMeters
            }

            DistanceCalculator.isWithinRadius(
                userLat = userLat,
                userLon = userLon,
                eventLat = report.latitude,
                eventLon = report.longitude,
                radiusMeters = radius
            )
        }
    }

    /**
     * Deletes an incident report permanently from Firestore.
     *
     * @param reportId Unique document ID.
     */
    suspend fun deleteReport(reportId: String): Resource<Unit> {
        return try {
            reportsCollection.document(reportId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            val isEn = AppSettings.currentLanguage.value == "en"
            Resource.Error(e.localizedMessage ?: (if (isEn) "Failed to delete report." else "Αποτυχία διαγραφής αναφοράς."), e)
        }
    }

    /**
     * Updates priority algorithm weights in Cloud Firestore and retroactively recalculates
     * the priority scores of all existing reports in the database using batch writes.
     *
     * @param weightCount Weight for report count (w1).
     * @param weightProximity Weight for spatial proximity (w2).
     * @param weightSeverity Weight for category severity (w3).
     * @return [Resource.Success] with the count of recalculated reports, or [Resource.Error].
     */
    suspend fun updateWeightsAndRecalculate(
        weightCount: Double,
        weightProximity: Double,
        weightSeverity: Double
    ): Resource<Int> {
        return try {
            // 1. Immediately update in-memory calculator
            PriorityCalculator.weightCount = weightCount
            PriorityCalculator.weightProximity = weightProximity
            PriorityCalculator.weightSeverity = weightSeverity

            // 2. Persist updated weights to Cloud Firestore for cross-device synchronization
            val weightsData = mapOf(
                "weightCount" to weightCount,
                "weightProximity" to weightProximity,
                "weightSeverity" to weightSeverity,
                "updatedAt" to System.currentTimeMillis()
            )
            configDocRef.set(weightsData).await()

            // 3. Fetch all existing reports to recompute priority scores under the new weights
            val snapshots = reportsCollection.get().await()
            val allReports = snapshots.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Report::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }

            if (allReports.isEmpty()) {
                return Resource.Success(0)
            }

            // 4. Recalculate priorityScore for all reports and commit in batches (Firestore limit: 500 writes/batch)
            var updatedCount = 0
            val chunkedReports = allReports.chunked(450)
            for (chunk in chunkedReports) {
                val batch = firestore.batch()
                for (report in chunk) {
                    if (report.id.isNotBlank()) {
                        val newScore = PriorityCalculator.calculateScore(report, allReports)
                        val docRef = reportsCollection.document(report.id)
                        batch.update(docRef, "priorityScore", newScore)
                        updatedCount++
                    }
                }
                batch.commit().await()
            }

            Resource.Success(updatedCount)
        } catch (e: Exception) {
            val isEn = AppSettings.currentLanguage.value == "en"
            Resource.Error(
                e.localizedMessage ?: (if (isEn) "Failed to update weights and recalculate reports." else "Αποτυχία ενημέρωσης παραμέτρων και επανυπολογισμού αναφορών."),
                e
            )
        }
    }
}
