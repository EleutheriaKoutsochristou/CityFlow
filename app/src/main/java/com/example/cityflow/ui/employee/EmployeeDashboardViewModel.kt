package com.example.cityflow.ui.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.model.Resource
import com.example.cityflow.data.repository.ReportRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class EmployeeReportFilter {
    ALL,
    PENDING,    // SUBMITTED
    ACTIVE,     // VERIFIED, IN_PROGRESS
    RESOLVED    // RESOLVED, REJECTED
}

enum class EmployeeReportSort {
    PRIORITY_DESC,
    DATE_DESC
}

/**
 * ViewModel για τον Πίνακα Ελέγχου του Υπαλλήλου (Employee Flow).
 * Διαχειρίζεται τη real-time ροή των αναφορών, τα φίλτρα, την ταξινόμηση
 * και τις ενέργειες επικύρωσης, αλλαγής κατάστασης και απόρριψης.
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

    val displayedReports: StateFlow<List<Report>> = combine(
        _rawReports,
        _selectedFilter,
        _selectedSort
    ) { reports, filter, sort ->
        val filtered = when (filter) {
            EmployeeReportFilter.ALL -> reports
            EmployeeReportFilter.PENDING -> reports.filter { it.status == ReportStatus.SUBMITTED.name }
            EmployeeReportFilter.ACTIVE -> reports.filter {
                it.status == ReportStatus.VERIFIED.name || it.status == ReportStatus.IN_PROGRESS.name
            }
            EmployeeReportFilter.RESOLVED -> reports.filter {
                it.status == ReportStatus.RESOLVED.name || it.status == ReportStatus.REJECTED.name
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

    fun setFilter(filter: EmployeeReportFilter) {
        _selectedFilter.value = filter
    }

    fun setSort(sort: EmployeeReportSort) {
        _selectedSort.value = sort
    }

    /**
     * Ενημέρωση κατάστασης αναφοράς (Επικύρωση, Σε Εξέλιξη, Επίλυση, Απόρριψη).
     */
    fun updateStatus(
        reportId: String,
        newStatus: ReportStatus,
        verifiedBy: String,
        practicalAdvice: String? = null
    ) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = reportRepository.updateReportStatus(
                reportId = reportId,
                newStatus = newStatus,
                verifiedBy = verifiedBy,
                practicalAdvice = practicalAdvice
            )

            _actionState.value = when (result) {
                is Resource.Success -> {
                    val statusText = when (newStatus) {
                        ReportStatus.VERIFIED -> "επικυρώθηκε"
                        ReportStatus.IN_PROGRESS -> "τέθηκε σε εξέλιξη"
                        ReportStatus.RESOLVED -> "επιλύθηκε"
                        ReportStatus.REJECTED -> "απορρίφθηκε"
                        else -> "ενημερώθηκε"
                    }
                    Resource.Success("Η αναφορά $statusText επιτυχώς.")
                }
                is Resource.Error -> Resource.Error(result.message)
                else -> Resource.Idle
            }
        }
    }

    fun clearActionState() {
        _actionState.value = Resource.Idle
    }
}
