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
 * Represents a nearby incident alert paired with its calculated distance (in meters) from the user.
 *
 * @property report The incident report.
 * @property distanceMeters Real-time great-circle distance in meters.
 */
data class NearbyAlertItem(
    val report: Report,
    val distanceMeters: Double
)

/**
 * ViewModel for the Citizen landing feed and map overview (Citizen Flow).
 *
 * Responsibilities:
 * - Tracks user GPS location and geocoded street address.
 * - Subscribes to real-time incident reports stream from Firestore.
 * - Computes dynamic spatial proximity and filters active incidents within each category's radius.
 * - Dispatches local Android notifications (Heads-up Alerts) when approaching nearby hazards.
 * - Surfaces recent public resolution announcements.
 */
class CitizenHomeViewModel(
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _userLatitude = MutableStateFlow<Double?>(null)
    /** User's current GPS latitude. */
    val userLatitude: StateFlow<Double?> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow<Double?>(null)
    /** User's current GPS longitude. */
    val userLongitude: StateFlow<Double?> = _userLongitude.asStateFlow()

    private val _userAddress = MutableStateFlow("")
    /** Geocoded human-readable location address string. */
    val userAddress: StateFlow<String> = _userAddress.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    /** Loading indicator state for GPS acquisition. */
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    /** Error message if GPS location lookup fails. */
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    // Internal cache of all reports received from Firestore
    private val _allReports = MutableStateFlow<List<Report>>(emptyList())
    val isLoadingReports = MutableStateFlow(true)

    /**
     * Dynamically computed list of nearby alerts within notification radius, sorted closest first.
     */
    val nearbyAlerts: StateFlow<List<NearbyAlertItem>> = combine(
        _allReports,
        _userLatitude,
        _userLongitude
    ) { reports, lat, lon ->
        if (lat == null || lon == null) {
            emptyList()
        } else {
            reports.mapNotNull { report ->
                // Filter only acknowledged/active reports
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
            }.sortedBy { it.distanceMeters } // Ascending distance (nearest first)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Feed of the 5 most recent officially resolved incidents featuring resolution announcements.
     */
    val recentResolvedAnnouncements: StateFlow<List<Report>> = _allReports.map { reports ->
        reports.filter {
            it.status == ReportStatus.RESOLVED.name && it.resolutionAnnouncement.isNotBlank()
        }.sortedByDescending { it.resolvedAt ?: it.timestamp }.take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeReports()
    }

    /**
     * Subscribes to the real-time reports Flow from [ReportRepository].
     */
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
     * Updates the citizen's GPS coordinates and triggers proximity alert checks.
     *
     * @param lat Latitude in degrees.
     * @param lon Longitude in degrees.
     * @param address Optional reverse-geocoded address.
     * @param context Application context for dispatching notifications.
     */
    fun setUserLocation(lat: Double, lon: Double, address: String = "", context: Context? = null) {
        _userLatitude.value = lat
        _userLongitude.value = lon
        _userAddress.value = address
        _isLocating.value = false
        _locationError.value = null

        // Trigger nearby proximity notifications if context is available
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
     * Iterates over active incidents, evaluating proximity against each incident's geofence radius,
     * and shows a system notification if within range.
     *
     * @param context Application context.
     * @param lat Citizen current latitude.
     * @param lon Citizen current longitude.
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
