package com.example.cityflow.data.repository

import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.model.Resource
import com.example.cityflow.domain.DistanceCalculator
import com.example.cityflow.domain.PriorityCalculator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository για τη διαχείριση των αναφορών (Reports) στο Firestore.
 * Υποστηρίζει Real-time συγχρονισμό μέσω Flow για άμεση ενημέρωση όλων των χρηστών.
 */
class ReportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val reportsCollection = firestore.collection("reports")

    /**
     * Υποβολή νέας αναφοράς:
     * 1. Ανακτά τις υπάρχουσες πρόσφατες αναφορές.
     * 2. Υπολογίζει το αρχικό PriorityScore μέσω του PriorityCalculator.
     * 3. Αποθηκεύει την αναφορά στο Firestore.
     */
    suspend fun submitReport(report: Report): Resource<String> {
        return try {
            val docRef = if (report.id.isBlank()) {
                reportsCollection.document()
            } else {
                reportsCollection.document(report.id)
            }

            val docId = docRef.id

            // Ανάκτηση πρόσφατων αναφορών για τον υπολογισμό του PriorityScore
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

            // Έλεγχος αποφυγής διπλότυπης αναφοράς από τον ίδιο χρήστη (Option B)
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
                return Resource.Error("Έχετε ήδη υποβάλει αναφορά για αυτό το σημείο τα τελευταία 30 λεπτά. Παρακαλώ περιμένετε την επεξεργασία της.")
            }

            // Εύρεση παρόμοιων αναφορών και υπολογισμός σκορ
            val similarReports = PriorityCalculator.findSimilarReports(report, existingReports)
            val calculatedScore = PriorityCalculator.calculateScore(report, existingReports)

            val finalReport = report.copy(
                id = docId,
                priorityScore = calculatedScore,
                similarReportsCount = similarReports.size + 1
            )

            // Ατομική αποθήκευση της νέας αναφοράς και ενημέρωση των παλαιότερων
            // παρόμοιων αναφορών του ίδιου cluster (πλήθος και PriorityScore)
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
            Resource.Error(e.localizedMessage ?: "Αποτυχία υποβολής αναφοράς.", e)
        }
    }

    /**
     * Real-time ροή (Flow) όλων των αναφορών ταξινομημένων κατά ημερομηνία (νεότερες πρώτα).
     * Ιδανικό για την οθόνη του υπαλλήλου.
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
     * Real-time ροή (Flow) των αναφορών ενός συγκεκριμένου χρήστη (για την οθόνη ιστορικού).
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
     * Ενημέρωση κατάστασης αναφοράς από τον υπάλληλο (Επικύρωση, Απόρριψη, Επίλυση κ.λπ.).
     */
    suspend fun updateReportStatus(
        reportId: String,
        newStatus: ReportStatus,
        verifiedBy: String? = null,
        practicalAdvice: String? = null
    ): Resource<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name
            )

            if (newStatus == ReportStatus.VERIFIED) {
                updates["verifiedAt"] = System.currentTimeMillis()
                if (verifiedBy != null) updates["verifiedBy"] = verifiedBy
                if (!practicalAdvice.isNullOrBlank()) updates["practicalAdvice"] = practicalAdvice
            }

            reportsCollection.document(reportId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Αποτυχία ενημέρωσης κατάστασης.", e)
        }
    }

    /**
     * Φιλτράρισμα επικυρωμένων αναφορών που βρίσκονται εντός της ακτίνας ειδοποίησης
     * από την τρέχουσα τοποθεσία του χρήστη.
     */
    fun filterNearbyVerifiedReports(
        reports: List<Report>,
        userLat: Double,
        userLon: Double
    ): List<Report> {
        return reports.filter { report ->
            // Μόνο επικυρωμένες ή σε εξέλιξη αναφορές
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
     * Διαγραφή αναφοράς (π.χ. από διαχειριστή).
     */
    suspend fun deleteReport(reportId: String): Resource<Unit> {
        return try {
            reportsCollection.document(reportId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Αποτυχία διαγραφής αναφοράς.", e)
        }
    }
}
