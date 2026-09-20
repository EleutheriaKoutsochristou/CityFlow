package com.example.cityflow.ui.citizen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.repository.ReportRepository
import com.example.cityflow.domain.DistanceCalculator
import com.example.cityflow.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Αναπαριστά ένα κοντινό συμβάν μαζί με την υπολογισμένη απόσταση από τον πολίτη.
 */
data class NearbyAlertItem(
    val report: Report,
    val distanceMeters: Double
)

/**
 * ViewModel για την αρχική οθόνη του Πολίτη (Citizen Flow).
 *
 * Διαχειρίζεται:
 * - Την τρέχουσα τοποθεσία GPS του χρήστη
 * - Real-time ροή επικυρωμένων αναφορών από το Firestore
 * - Αυτόματο υπολογισμό χωρικής εγγύτητας και φιλτράρισμα βάσει της ακτίνας κάθε κατηγορίας
 * - Αποστολή τοπικών ειδοποιήσεων Android (Heads-up Notifications) όταν εντοπίζονται νέα κοντινά προβλήματα
 */
class CitizenHomeViewModel(
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _userLatitude = MutableStateFlow<Double?>(null)
    val userLatitude: StateFlow<Double?> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow<Double?>(null)
    val userLongitude: StateFlow<Double?> = _userLongitude.asStateFlow()

    private val _userAddress = MutableStateFlow("")
    val userAddress: StateFlow<String> = _userAddress.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    // Real-time ροή όλων των αναφορών από το Firestore
    private val _allReports = MutableStateFlow<List<Report>>(emptyList())
    val isLoadingReports = MutableStateFlow(true)

    // Λίστα κοντινών ειδοποιήσεων υπολογισμένη σε πραγματικό χρόνο
    val nearbyAlerts: StateFlow<List<NearbyAlertItem>> = combine(
        _allReports,
        _userLatitude,
        _userLongitude
    ) { reports, lat, lon ->
        if (lat == null || lon == null) {
            emptyList()
        } else {
            reports.mapNotNull { report ->
                // Μόνο επικυρωμένες ή σε εξέλιξη αναφορές
                val isActive = report.status == ReportStatus.VERIFIED.name ||
                        report.status == ReportStatus.IN_PROGRESS.name
                if (!isActive) return@mapNotNull null

                val distance = DistanceCalculator.distanceBetweenMeters(
                    startLatitude = lat,
                    startLongitude = lon,
                    endLatitude = report.latitude,
                    endLongitude = report.longitude
                )

                val radius = if (report.notificationRadiusMeters > 0) {
                    report.notificationRadiusMeters
                } else {
                    report.reportCategory.defaultRadiusMeters
                }

                if (distance <= radius) {
                    NearbyAlertItem(report, distance)
                } else {
                    null
                }
            }.sortedBy { it.distanceMeters } // Πιο κοντινά πρώτα
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeReports()
    }

    private fun observeReports() {
        viewModelScope.launch {
            reportRepository.getAllReportsFlow()
                .catch {
                    isLoadingReports.value = false
                }
                .collect { reports ->
                    _allReports.value = reports
                    isLoadingReports.value = false
                }
        }
    }

    /**
     * Ενημέρωση των συντεταγμένων του χρήστη και αποστολή ειδοποιήσεων για νέα συμβάντα.
     */
    fun setUserLocation(lat: Double, lon: Double, address: String = "", context: Context? = null) {
        _userLatitude.value = lat
        _userLongitude.value = lon
        _userAddress.value = address
        _isLocating.value = false
        _locationError.value = null

        // Έλεγχος και αποστολή Push Notification αν δόθηκε context
        if (context != null) {
            checkAndTriggerNotifications(context, lat, lon)
        }
    }

    fun setLocationLoading(loading: Boolean) {
        _isLocating.value = loading
        if (loading) {
            _locationError.value = null
        }
    }

    fun setLocationError(error: String?) {
        _locationError.value = error
        _isLocating.value = false
    }

    /**
     * Ελέγχει αν υπάρχουν νέα επικυρωμένα συμβάντα εντός ακτίνας και στέλνει ειδοποίηση Android.
     */
    fun checkAndTriggerNotifications(context: Context, lat: Double, lon: Double) {
        val currentReports = _allReports.value
        for (report in currentReports) {
            val isActive = report.status == ReportStatus.VERIFIED.name ||
                    report.status == ReportStatus.IN_PROGRESS.name
            if (!isActive) continue

            val distance = DistanceCalculator.distanceBetweenMeters(
                lat, lon, report.latitude, report.longitude
            )
            val radius = if (report.notificationRadiusMeters > 0) {
                report.notificationRadiusMeters
            } else {
                report.reportCategory.defaultRadiusMeters
            }

            if (distance <= radius) {
                NotificationHelper.showNearbyAlertNotification(context, report, distance)
            }
        }
    }
}
