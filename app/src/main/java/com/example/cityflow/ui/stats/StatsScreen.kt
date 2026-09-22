package com.example.cityflow.ui.stats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityflow.R
import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.model.User
import com.example.cityflow.ui.common.SettingsDialog
import com.example.cityflow.ui.common.localizedTitle
import com.example.cityflow.util.ImageUtils
import com.example.cityflow.util.MapUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Οθόνη Ιστορικού & Στατιστικών (Specification 10).
 *
 * Περιλαμβάνει:
 * 1. Προσωπικά στατιστικά χρήστη (Υποβληθείσες, Επικυρωμένες, Επιλυμένες, Ειδοποιήσεις).
 * 2. Συνολική εικόνα πόλης και ποσοστό επίλυσης.
 * 3. Συχνότερες κατηγορίες προβλημάτων με μπάρες κατανομής (Category Breakdown).
 * 4. Προσωπικό ιστορικό αναφορών με φίλτρα χρόνου και κατάστασης.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    currentUser: User?,
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val context = LocalContext.current

    val isEmployeeOrAdmin by viewModel.isEmployeeOrAdmin.collectAsState()
    val pendingReview by viewModel.pendingReviewCount.collectAsState()
    val activeInProgress by viewModel.activeInProgressCount.collectAsState()
    val criticalPriority by viewModel.criticalPriorityCount.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.stats_tab_overview),
        if (isEmployeeOrAdmin) stringResource(R.string.stats_tab_all_history)
        else stringResource(R.string.stats_tab_my_history)
    )

    val isLoading by viewModel.isLoading.collectAsState()
    val userTotal by viewModel.userTotalCount.collectAsState()
    val userVerified by viewModel.userVerifiedCount.collectAsState()
    val userResolved by viewModel.userResolvedCount.collectAsState()
    val receivedAlerts by viewModel.receivedAlertsCount.collectAsState()

    val cityTotal by viewModel.cityTotalCount.collectAsState()
    val cityResolved by viewModel.cityResolvedCount.collectAsState()
    val resolutionRate by viewModel.cityResolutionRate.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val statusBreakdown by viewModel.statusBreakdown.collectAsState()

    val filteredReports by viewModel.filteredUserReports.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    var selectedPhotoForDialog by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        viewModel.initUser(currentUser, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.stats_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTabIndex) {
                    0 -> {
                        // TAB 0: Επισκόπηση & Στατιστικά
                        StatsOverviewContent(
                            isEmployee = isEmployeeOrAdmin,
                            userTotal = userTotal,
                            userVerified = userVerified,
                            userResolved = userResolved,
                            receivedAlerts = receivedAlerts,
                            pendingReview = pendingReview,
                            activeInProgress = activeInProgress,
                            criticalPriority = criticalPriority,
                            cityTotal = cityTotal,
                            cityResolved = cityResolved,
                            resolutionRate = resolutionRate,
                            categoryBreakdown = categoryBreakdown,
                            statusBreakdown = statusBreakdown
                        )
                    }
                    1 -> {
                        // TAB 1: Το Ιστορικό μου & Φίλτρα
                        MyReportsHistoryContent(
                            isEmployee = isEmployeeOrAdmin,
                            reports = filteredReports,
                            timeFilter = timeFilter,
                            statusFilter = statusFilter,
                            onTimeFilterChange = { viewModel.setTimeFilter(it) },
                            onStatusFilterChange = { viewModel.setStatusFilter(it) },
                            onPhotoClick = { selectedPhotoForDialog = it }
                        )
                    }
                }
            }
        }
    }

    // Full photo dialog
    val photoToShow = selectedPhotoForDialog
    if (photoToShow != null) {
        val bitmap = remember(photoToShow) {
            ImageUtils.base64ToBitmap(photoToShow)
        }
        Dialog(onDismissRequest = { selectedPhotoForDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Full Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { selectedPhotoForDialog = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }
}

/**
 * Περιεχόμενο Tab 0: Κάρτες Δεικτών, Επισκόπηση Πόλης & Κατανομή Κατηγοριών.
 */
@Composable
private fun StatsOverviewContent(
    isEmployee: Boolean,
    userTotal: Int,
    userVerified: Int,
    userResolved: Int,
    receivedAlerts: Int,
    pendingReview: Int,
    activeInProgress: Int,
    criticalPriority: Int,
    cityTotal: Int,
    cityResolved: Int,
    resolutionRate: Int,
    categoryBreakdown: List<CategoryStatItem>,
    statusBreakdown: List<StatusStatItem>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Προσωπικοί ή Επιχειρησιακοί Δείκτες (KPI Cards 2x2)
        item {
            Text(
                text = if (isEmployee) stringResource(R.string.stats_employee_summary)
                       else stringResource(R.string.stats_my_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEmployee) {
                    // Employee KPI Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard(
                            title = stringResource(R.string.stats_pending_review),
                            value = pendingReview.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            iconColor = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = stringResource(R.string.stats_in_progress),
                            value = activeInProgress.toString(),
                            icon = Icons.Default.PlayArrow,
                            iconColor = Color(0xFF1565C0),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard(
                            title = stringResource(R.string.resolved_reports_count),
                            value = cityResolved.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = stringResource(R.string.stats_critical_incidents),
                            value = criticalPriority.toString(),
                            icon = Icons.Default.Warning,
                            iconColor = Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Citizen KPI Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard(
                            title = stringResource(R.string.total_reports_submitted),
                            value = userTotal.toString(),
                            icon = Icons.Default.Campaign,
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = stringResource(R.string.verified_reports_count),
                            value = userVerified.toString(),
                            icon = Icons.Default.Verified,
                            iconColor = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard(
                            title = stringResource(R.string.resolved_reports_count),
                            value = userResolved.toString(),
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF00695C),
                            modifier = Modifier.weight(1f)
                        )
                        KpiStatCard(
                            title = stringResource(R.string.nearby_alerts_received),
                            value = receivedAlerts.toString(),
                            icon = Icons.Default.NotificationsActive,
                            iconColor = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Συνολική Εικόνα Πόλης & Ποσοστό Επίλυσης
        item {
            Text(
                text = stringResource(R.string.stats_city_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Σύνολο Αναφορών Πόλης",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$cityTotal συμβάντα",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Επιλύθηκαν",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$cityResolved",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.stats_resolution_rate, resolutionRate),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (resolutionRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF2E7D32),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Ραβδόγραμμα Ροής & Καταστάσεων Αναφορών (Status Funnel Bar Chart)
        item {
            StatusBarChartCard(statusBreakdown = statusBreakdown)
        }

        // 4. Κυκλικό Διάγραμμα Κατανομής Προβλημάτων (Category Donut Chart)
        item {
            CategoryDonutChartCard(
                breakdown = categoryBreakdown,
                totalReports = cityTotal
            )
        }

        // 5. Αναλυτική Λίστα Συχνότερων Κατηγοριών
        item {
            Text(
                text = stringResource(R.string.category_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(categoryBreakdown) { item ->
            CategoryBreakdownRow(item = item)
        }
    }
}

/**
 * Κάρτα Δείκτη (KPI Stat Card).
 */
@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

/**
 * Γραμμή κατανομής κατηγορίας με μπάρα ποσοστού.
 */
@Composable
private fun CategoryBreakdownRow(item: CategoryStatItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.category.localizedTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${item.count} (${(item.percentage * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { item.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * Περιεχόμενο Tab 1: Ιστορικό Αναφορών Χρήστη με Φίλτρα.
 */
@Composable
private fun MyReportsHistoryContent(
    isEmployee: Boolean,
    reports: List<Report>,
    timeFilter: StatsTimeFilter,
    statusFilter: StatsStatusFilter,
    onTimeFilterChange: (StatsTimeFilter) -> Unit,
    onStatusFilterChange: (StatsStatusFilter) -> Unit,
    onPhotoClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Time Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = timeFilter == StatsTimeFilter.ALL_TIME,
                onClick = { onTimeFilterChange(StatsTimeFilter.ALL_TIME) },
                label = { Text(stringResource(R.string.stats_filter_all_time)) }
            )
            FilterChip(
                selected = timeFilter == StatsTimeFilter.LAST_7_DAYS,
                onClick = { onTimeFilterChange(StatsTimeFilter.LAST_7_DAYS) },
                label = { Text(stringResource(R.string.stats_filter_7_days)) }
            )
            FilterChip(
                selected = timeFilter == StatsTimeFilter.LAST_30_DAYS,
                onClick = { onTimeFilterChange(StatsTimeFilter.LAST_30_DAYS) },
                label = { Text(stringResource(R.string.stats_filter_30_days)) }
            )
        }

        // Status Filters Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = statusFilter == StatsStatusFilter.ALL,
                onClick = { onStatusFilterChange(StatsStatusFilter.ALL) },
                label = { Text(stringResource(R.string.filter_all)) }
            )
            FilterChip(
                selected = statusFilter == StatsStatusFilter.SUBMITTED,
                onClick = { onStatusFilterChange(StatsStatusFilter.SUBMITTED) },
                label = { Text(ReportStatus.SUBMITTED.localizedTitle) }
            )
            FilterChip(
                selected = statusFilter == StatsStatusFilter.VERIFIED,
                onClick = { onStatusFilterChange(StatsStatusFilter.VERIFIED) },
                label = { Text(ReportStatus.VERIFIED.localizedTitle) }
            )
            FilterChip(
                selected = statusFilter == StatsStatusFilter.IN_PROGRESS,
                onClick = { onStatusFilterChange(StatsStatusFilter.IN_PROGRESS) },
                label = { Text(ReportStatus.IN_PROGRESS.localizedTitle) }
            )
            FilterChip(
                selected = statusFilter == StatsStatusFilter.RESOLVED,
                onClick = { onStatusFilterChange(StatsStatusFilter.RESOLVED) },
                label = { Text(ReportStatus.RESOLVED.localizedTitle) }
            )
            FilterChip(
                selected = statusFilter == StatsStatusFilter.REJECTED,
                onClick = { onStatusFilterChange(StatsStatusFilter.REJECTED) },
                label = { Text(ReportStatus.REJECTED.localizedTitle) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Reports List
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HistoryEdu,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEmployee)
                            stringResource(R.string.stats_no_reports_city)
                        else
                            stringResource(R.string.stats_no_filtered_reports),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    UserReportHistoryCard(report = report, onPhotoClick = onPhotoClick)
                }
            }
        }
    }
}

/**
 * Κάρτα προβολής αναφοράς στο προσωπικό ιστορικό του χρήστη.
 */
@Composable
private fun UserReportHistoryCard(
    report: Report,
    onPhotoClick: (String) -> Unit
) {
    val dateFormatted = remember(report.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(report.timestamp))
    }

    val photoBitmap = remember(report.imageBase64) {
        if (!report.imageBase64.isNullOrBlank()) {
            ImageUtils.base64ToBitmap(report.imageBase64)
        } else null
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = report.reportCategory.localizedTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                HistoryStatusBadge(status = report.reportStatus)
            }

            Text(
                text = dateFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (report.address.isNotBlank() || (report.latitude != 0.0 && report.longitude != 0.0)) {
                val cardContext = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (report.address.isNotBlank()) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = report.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = {
                            MapUtils.openMap(
                                context = cardContext,
                                latitude = report.latitude,
                                longitude = report.longitude,
                                label = report.reportCategory.localizedTitle
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.view_on_map),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            if (report.description.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Σχόλια / Περιγραφή:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = report.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (photoBitmap != null && !report.imageBase64.isNullOrBlank()) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = "Attachment",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPhotoClick(report.imageBase64) },
                    contentScale = ContentScale.Crop
                )
            }

            if (report.practicalAdvice.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.practicalAdvice,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Status badge για το ιστορικό.
 */
@Composable
private fun HistoryStatusBadge(status: ReportStatus) {
    val (bgColor, txtColor) = when (status) {
        ReportStatus.SUBMITTED -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        ReportStatus.VERIFIED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        ReportStatus.IN_PROGRESS -> Color(0xFFEDE7F6) to Color(0xFF512DA8)
        ReportStatus.RESOLVED -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        ReportStatus.REJECTED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        ReportStatus.UNDER_REVIEW -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.localizedTitle,
            color = txtColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
