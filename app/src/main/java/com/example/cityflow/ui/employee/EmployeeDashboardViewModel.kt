package com.example.cityflow.ui.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.model.Resource
import com.example.cityflow.data.repository.ReportRepository
import com.example.cityflow.util.AppSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Filter options for the municipal staff incident queue.
 */
enum class EmployeeReportFilter {
    ALL,
    SUBMITTED,
    UNDER_REVIEW,
    VERIFIED,
    IN_PROGRESS,
    RESOLVED,
    REJECTED
}

/**
 * Sorting criteria for municipal staff incident queue.
 */
enum class EmployeeReportSort {
    /** Sort by descending algorithmic PriorityScore (most urgent incidents first) */
    PRIORITY_DESC,
    /** Sort by descending chronological timestamp (newest submissions first) */
    DATE_DESC
}

/**
 * ViewModel for the Municipal Employee Dashboard (Employee Flow).
 *
 * Responsibilities:
 * - Subscribes to real-time incident reports stream from Firestore.
 * - Provides dynamic status filtering (All, Submitted, Under Review, Verified, In Progress, Resolved, Rejected).
 * - Applies sorting by Priority Score (default) or chronological order.
 * - Handles status transitions (Verify, Put in Progress, Resolve, Reject) with associated dialog inputs.
 */
class EmployeeDashboardViewModel(
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _rawReports = MutableStateFlow<List<Report>>(emptyList())
    val isLoading = MutableStateFlow(true)

    private val _selectedFilter = MutableStateFlow(EmployeeReportFilter.ALL)
    val selectedFilter: StateFlow<EmployeeReportFilter> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(EmployeeReportSort.PRIORITY_DESC)
    val selectedSort: StateFlow<EmployeeReportSort> = _selectedSort.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val actionState: StateFlow<Resource<String>> = _actionState.asStateFlow()

    /**
     * Combined reactive list of incident reports matching selected filter and sort order.
     */
    val displayedReports: StateFlow<List<Report>> = combine(
        _rawReports,
        _selectedFilter,
        _selectedSort
    ) { reports, filter, sort ->
        val filtered = when (filter) {
            EmployeeReportFilter.ALL -> reports
            EmployeeReportFilter.SUBMITTED -> reports.filter {
                it.status == ReportStatus.SUBMITTED.name
            }
            EmployeeReportFilter.UNDER_REVIEW -> reports.filter {
                it.status == ReportStatus.UNDER_REVIEW.name
            }
            EmployeeReportFilter.VERIFIED -> reports.filter {
                it.status == ReportStatus.VERIFIED.name
            }
            EmployeeReportFilter.IN_PROGRESS -> reports.filter {
                it.status == ReportStatus.IN_PROGRESS.name
            }
            EmployeeReportFilter.RESOLVED -> reports.filter {
                it.status == ReportStatus.RESOLVED.name
            }
            EmployeeReportFilter.REJECTED -> reports.filter {
                it.status == ReportStatus.REJECTED.name
            }
        }

        when (sort) {
            EmployeeReportSort.PRIORITY_DESC -> filtered.sortedByDescending { it.priorityScore }
            EmployeeReportSort.DATE_DESC -> filtered.sortedByDescending { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeReports()
    }

    /**
     * Observes real-time reports from [ReportRepository].
     */
    private fun observeReports() {
        viewModelScope.launch {
            reportRepository.getAllReportsFlow()
                .catch {
                    isLoading.value = false
                }
                .collect { reports ->
                    _rawReports.value = reports
                    isLoading.value = false
                }
        }
    }

    /**
     * Updates active status filter.
     */
    fun setFilter(filter: EmployeeReportFilter) {
        _selectedFilter.value = filter
    }

    /**
     * Updates sort ordering.
     */
    fun setSort(sort: EmployeeReportSort) {
        _selectedSort.value = sort
    }

    /**
     * Updates the status of an incident report in Firestore.
     *
     * @param reportId Unique report document ID.
     * @param newStatus New target [ReportStatus].
     * @param verifiedBy UID or name of municipal staff performing the action.
     * @param practicalAdvice Optional custom advice provided during verification.
     * @param rejectionReason Optional justification entered if rejecting.
     * @param resolutionAnnouncement Optional public announcement entered upon resolving.
     */
    fun updateStatus(
        reportId: String,
        newStatus: ReportStatus,
        verifiedBy: String,
        practicalAdvice: String? = null,
        rejectionReason: String? = null,
        resolutionAnnouncement: String? = null
    ) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = reportRepository.updateReportStatus(
                reportId = reportId,
                newStatus = newStatus,
                verifiedBy = verifiedBy,
                practicalAdvice = practicalAdvice,
                rejectionReason = rejectionReason,
                resolutionAnnouncement = resolutionAnnouncement
            )

            _actionState.value = when (result) {
                is Resource.Success -> {
                    val isEn = AppSettings.currentLanguage.value == "en"
                    val msg = if (isEn) {
                        val statusText = when (newStatus) {
                            ReportStatus.VERIFIED -> "verified"
                            ReportStatus.IN_PROGRESS -> "marked in progress"
                            ReportStatus.RESOLVED -> "resolved"
                            ReportStatus.REJECTED -> "rejected"
                            ReportStatus.UNDER_REVIEW -> "marked under review"
                            ReportStatus.SUBMITTED -> "reset to submitted"
                        }
                        "Report $statusText successfully."
                    } else {
                        val statusText = when (newStatus) {
                            ReportStatus.VERIFIED -> "επικυρώθηκε"
                            ReportStatus.IN_PROGRESS -> "τέθηκε σε εξέλιξη"
                            ReportStatus.RESOLVED -> "επιλύθηκε"
                            ReportStatus.REJECTED -> "απορρίφθηκε"
                            ReportStatus.UNDER_REVIEW -> "τέθηκε υπό διερεύνηση"
                            ReportStatus.SUBMITTED -> "επαναφέρθηκε σε κατάσταση υποβολής"
                        }
                        "Η αναφορά $statusText επιτυχώς."
                    }
                    Resource.Success(msg)
                }
                is Resource.Error -> Resource.Error(result.message)
                else -> Resource.Idle
            }
        }
    }

    /**
     * Clears action notification message state.
     */
    fun clearActionState() {
        _actionState.value = Resource.Idle
    }
}
