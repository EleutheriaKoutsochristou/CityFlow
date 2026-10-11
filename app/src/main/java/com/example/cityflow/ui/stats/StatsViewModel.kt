package com.example.cityflow.ui.stats

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityflow.data.model.User
import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.repository.ReportRepository
import com.example.cityflow.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Time duration filter for analytics aggregation and history filtering.
 */
enum class StatsTimeFilter {
    ALL_TIME,
    LAST_7_DAYS,
    LAST_30_DAYS
}

/**
 * Status filter criteria for analytics history views.
 */
enum class StatsStatusFilter {
    ALL,
    SUBMITTED,
    VERIFIED,
    IN_PROGRESS,
    RESOLVED,
    REJECTED
}

/**
 * Category breakdown analytics item.
 *
 * @property category The incident category.
 * @property count Total number of incidents in this category.
 * @property percentage Proportion of total incidents (0.0 to 1.0).
 */
data class CategoryStatItem(
    val category: ReportCategory,
    val count: Int,
    val percentage: Float
)

/**
 * Status funnel analytics item.
 *
 * @property status The lifecycle status.
 * @property count Total incidents currently in this status.
 * @property percentage Proportion of total incidents (0.0 to 1.0).
 */
data class StatusStatItem(
    val status: ReportStatus,
    val count: Int,
    val percentage: Float
)

/**
 * ViewModel for Statistics & Analytics visualization and incident history (MVVM pattern).
 *
 * Computes role-aware metrics:
 * - Citizen view: Personal contribution summary (submitted, verified, resolved counts) & local alerts counter.
 * - Employee / Admin view: Operational triage metrics (pending review, active in progress, critical priority count).
 * - City-wide metrics: Incident resolution rate, category distribution percentages, and lifecycle funnel.
 */
class StatsViewModel(
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _allReports = MutableStateFlow<List<Report>>(emptyList())
    val isLoading = MutableStateFlow(true)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /** True if active user has Municipal Employee or Admin role. */
    val isEmployeeOrAdmin: StateFlow<Boolean> = _currentUser.map { it?.isEmployeeOrAdmin == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _userId = MutableStateFlow("")
    private val _timeFilter = MutableStateFlow(StatsTimeFilter.ALL_TIME)
    val timeFilter: StateFlow<StatsTimeFilter> = _timeFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow(StatsStatusFilter.ALL)
    val statusFilter: StateFlow<StatsStatusFilter> = _statusFilter.asStateFlow()

    private val _receivedAlertsCount = MutableStateFlow(0)
    val receivedAlertsCount: StateFlow<Int> = _receivedAlertsCount.asStateFlow()

    init {
        observeAllReports()
    }

    /**
     * Initializes user context and retrieves persistent local device proximity alert counts.
     */
    fun initUser(user: User?, context: Context) {
        _currentUser.value = user
        _userId.value = user?.id.orEmpty()
        _receivedAlertsCount.value = NotificationHelper.getReceivedAlertsCount(context)
    }

    /**
     * Observes real-time reports flow from repository.
     */
    private fun observeAllReports() {
        viewModelScope.launch {
            reportRepository.getAllReportsFlow()
                .catch {
                    isLoading.value = false
                }
                .collect { reports ->
                    _allReports.value = reports
                    isLoading.value = false
                }
        }
    }

    /**
     * Updates active time range filter.
     */
    fun setTimeFilter(filter: StatsTimeFilter) {
        _timeFilter.value = filter
    }

    /**
     * Updates active status filter.
     */
    fun setStatusFilter(filter: StatsStatusFilter) {
        _statusFilter.value = filter
    }

    /**
     * Reports submitted by the currently authenticated user.
     */
    val userReports: StateFlow<List<Report>> = combine(_allReports, _userId) { reports, uid ->
        if (uid.isBlank()) emptyList()
        else reports.filter { it.userId == uid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Personal citizen metric: total reports submitted by user.
     */
    val userTotalCount: StateFlow<Int> = userReports.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Personal citizen metric: count of user's reports that have been verified or progressed.
     */
    val userVerifiedCount: StateFlow<Int> = userReports.map { reports ->
        reports.count { it.status == ReportStatus.VERIFIED.name || it.status == ReportStatus.IN_PROGRESS.name || it.status == ReportStatus.RESOLVED.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Personal citizen metric: count of user's reports successfully resolved.
     */
    val userResolvedCount: StateFlow<Int> = userReports.map { reports ->
        reports.count { it.status == ReportStatus.RESOLVED.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Operational metric: count of reports awaiting municipal review.
     */
    val pendingReviewCount: StateFlow<Int> = _allReports.map { reports ->
        reports.count { it.status == ReportStatus.SUBMITTED.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Operational metric: count of active reports currently in progress.
     */
    val activeInProgressCount: StateFlow<Int> = _allReports.map { reports ->
        reports.count { it.status == ReportStatus.VERIFIED.name || it.status == ReportStatus.IN_PROGRESS.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Operational metric: count of critical priority incidents (PriorityScore >= 18.0).
     */
    val criticalPriorityCount: StateFlow<Int> = _allReports.map { reports ->
        reports.count { it.priorityScore >= 18.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * City-wide metric: total number of reports recorded in the system.
     */
    val cityTotalCount: StateFlow<Int> = _allReports.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * City-wide metric: total number of resolved incidents.
     */
    val cityResolvedCount: StateFlow<Int> = _allReports.map { reports ->
        reports.count { it.status == ReportStatus.RESOLVED.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * City-wide metric: overall percentage of incidents resolved (0 to 100%).
     */
    val cityResolutionRate: StateFlow<Int> = _allReports.map { reports ->
        if (reports.isEmpty()) 0
        else {
            val resolved = reports.count { it.status == ReportStatus.RESOLVED.name }
            (resolved * 100) / reports.size
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Category breakdown sorted in descending order of frequency.
     */
    val categoryBreakdown: StateFlow<List<CategoryStatItem>> = _allReports.map { reports ->
        val total = reports.size
        if (total == 0) {
            ReportCategory.entries.map { CategoryStatItem(it, 0, 0f) }
        } else {
            val countsByCategory = reports.groupBy { it.reportCategory }
            ReportCategory.entries.map { category ->
                val count = countsByCategory[category]?.size ?: 0
                val percentage = (count.toFloat() / total.toFloat())
                CategoryStatItem(category, count, percentage)
            }.sortedByDescending { it.count }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Lifecycle distribution of reports across statuses.
     */
    val statusBreakdown: StateFlow<List<StatusStatItem>> = _allReports.map { reports ->
        val total = reports.size
        val countsByStatus = reports.groupBy { it.reportStatus }
        listOf(
            ReportStatus.SUBMITTED,
            ReportStatus.VERIFIED,
            ReportStatus.IN_PROGRESS,
            ReportStatus.RESOLVED,
            ReportStatus.REJECTED
        ).map { status ->
            val count = countsByStatus[status]?.size ?: 0
            val percentage = if (total > 0) (count.toFloat() / total.toFloat()) else 0f
            StatusStatItem(status, count, percentage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val baseHistoryReports: StateFlow<List<Report>> = combine(_allReports, _currentUser) { reports, user ->
        if (user?.isEmployeeOrAdmin == true) {
            reports
        } else {
            val uid = user?.id.orEmpty()
            if (uid.isBlank()) emptyList() else reports.filter { it.userId == uid }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Filtered list of history reports (user's personal reports for citizens, city-wide for employees).
     */
    val filteredUserReports: StateFlow<List<Report>> = combine(
        baseHistoryReports,
        _timeFilter,
        _statusFilter
    ) { reports, time, status ->
        val now = System.currentTimeMillis()
        val timeFiltered = when (time) {
            StatsTimeFilter.ALL_TIME -> reports
            StatsTimeFilter.LAST_7_DAYS -> {
                val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
                reports.filter { now - it.timestamp <= sevenDaysMs }
            }
            StatsTimeFilter.LAST_30_DAYS -> {
                val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
                reports.filter { now - it.timestamp <= thirtyDaysMs }
            }
        }

        when (status) {
            StatsStatusFilter.ALL -> timeFiltered
            StatsStatusFilter.SUBMITTED -> timeFiltered.filter { it.status == ReportStatus.SUBMITTED.name }
            StatsStatusFilter.VERIFIED -> timeFiltered.filter { it.status == ReportStatus.VERIFIED.name }
            StatsStatusFilter.IN_PROGRESS -> timeFiltered.filter { it.status == ReportStatus.IN_PROGRESS.name }
            StatsStatusFilter.RESOLVED -> timeFiltered.filter { it.status == ReportStatus.RESOLVED.name }
            StatsStatusFilter.REJECTED -> timeFiltered.filter { it.status == ReportStatus.REJECTED.name }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
