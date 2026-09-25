package com.example.cityflow.ui.employee

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityflow.R
import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.model.Resource
import com.example.cityflow.data.model.User
import com.example.cityflow.domain.PriorityCalculator
import com.example.cityflow.ui.common.SettingsDialog
import com.example.cityflow.ui.common.localizedTitle
import com.example.cityflow.ui.common.localizedAdvice
import com.example.cityflow.ui.common.localizedDescription
import com.example.cityflow.util.AppSettings
import com.example.cityflow.util.ImageUtils
import com.example.cityflow.util.MapUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Πίνακας Ελέγχου Υπαλλήλου (Employee Flow).
 * Περιλαμβάνει:
 * - Real-time λίστα όλων των αναφορών
 * - Φίλτρα ανά κατάσταση (Όλες, Προς Έλεγχο, Ενεργές, Επιλυμένες) & Ταξινόμηση
 * - Badges προτεραιότητας («Κρίσιμη», «Υψηλή», «Μέτρια», «Χαμηλή») με σκορ
 * - Κουμπιά επικύρωσης (Verify), αλλαγής κατάστασης (In Progress, Resolved) και απόρριψης (Reject)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(
    currentUser: User?,
    onNavigateToAdmin: (() -> Unit)? = null,
    onNavigateToStats: (() -> Unit)? = null,
    onLogout: () -> Unit,
    viewModel: EmployeeDashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val reports by viewModel.displayedReports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedSort by viewModel.selectedSort.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    // Dialog state για προαιρετική επεξεργασία οδηγίας κατά την επικύρωση
    var reportToVerify by remember { mutableStateOf<Report?>(null) }
    var verifyAdviceText by remember { mutableStateOf("") }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val defaultEmployeeName = stringResource(R.string.role_employee)
    val employeeIdentifier = currentUser?.fullName?.ifBlank { currentUser.email }
        ?: currentUser?.email.orEmpty().ifBlank { defaultEmployeeName }

    // Εμφάνιση μηνυμάτων ενεργειών σε Snackbar
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(
                    message = state.data,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearActionState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearActionState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.employee_dashboard_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    if (onNavigateToStats != null) {
                        IconButton(onClick = onNavigateToStats) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = stringResource(R.string.stats_title),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    if (currentUser?.isAdmin == true && onNavigateToAdmin != null) {
                        IconButton(onClick = onNavigateToAdmin) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = stringResource(R.string.admin_switch_to_admin),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.logout),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Header με καλωσόρισμα & ρόλο
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.welcome_user, employeeIdentifier),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.role_label, stringResource(R.string.role_employee)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(
                            text = stringResource(R.string.reports_count, reports.size),
                            modifier = Modifier.padding(4.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // 2. Φίλτρα κατάστασης & Ταξινόμηση
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == EmployeeReportFilter.ALL,
                    onClick = { viewModel.setFilter(EmployeeReportFilter.ALL) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
                FilterChip(
                    selected = selectedFilter == EmployeeReportFilter.SUBMITTED,
                    onClick = { viewModel.setFilter(EmployeeReportFilter.SUBMITTED) },
                    label = { Text(ReportStatus.SUBMITTED.localizedTitle) }
                )
                FilterChip(
                    selected = selectedFilter == EmployeeReportFilter.UNDER_REVIEW,
                    onClick = { viewModel.setFilter(EmployeeReportFilter.UNDER_REVIEW) },
                    label = { Text(ReportStatus.UNDER_REVIEW.localizedTitle) }
                )
                FilterChip(
                    selected = selectedFilter == EmployeeReportFilter.VERIFIED,
                    onClick = { viewModel.setFilter(EmployeeReportFilter.VERIFIED) },
                    label = { Text(ReportStatus.VERIFIED.localizedTitle) }
                )
                FilterChip(
                    selected = selectedFilter == EmployeeReportFilter.IN_PROGRESS,
                    onClick = { viewModel.setFilter(EmployeeReportFilter.IN_PROGRESS) },
                    label = { Text(ReportStatus.IN_PROGRESS.localizedTitle) }
                )
                FilterChip(
                    selected = selectedFilter == EmployeeReportFilter.RESOLVED,
                    onClick = { viewModel.setFilter(EmployeeReportFilter.RESOLVED) },
                    label = { Text(ReportStatus.RESOLVED.localizedTitle) }
                )
                FilterChip(
                    selected = selectedFilter == EmployeeReportFilter.REJECTED,
                    onClick = { viewModel.setFilter(EmployeeReportFilter.REJECTED) },
                    label = { Text(ReportStatus.REJECTED.localizedTitle) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Sort toggle
                AssistChip(
                    onClick = {
                        val nextSort = if (selectedSort == EmployeeReportSort.PRIORITY_DESC) {
                            EmployeeReportSort.DATE_DESC
                        } else {
                            EmployeeReportSort.PRIORITY_DESC
                        }
                        viewModel.setSort(nextSort)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (selectedSort == EmployeeReportSort.PRIORITY_DESC) Icons.Default.Sort else Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (selectedSort == EmployeeReportSort.PRIORITY_DESC)
                                stringResource(R.string.sort_priority)
                            else
                                stringResource(R.string.sort_date)
                        )
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 3. Λίστα αναφορών σε πραγματικό χρόνο
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (reports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_reports_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reports, key = { it.id }) { report ->
                        ReportEmployeeCard(
                            report = report,
                            onVerify = {
                                reportToVerify = report
                                verifyAdviceText = report.practicalAdvice.ifBlank {
                                    report.reportCategory.getDefaultAdvice(AppSettings.currentLanguage.value)
                                }
                            },
                            onSetUnderReview = {
                                viewModel.updateStatus(
                                    reportId = report.id,
                                    newStatus = ReportStatus.UNDER_REVIEW,
                                    verifiedBy = employeeIdentifier
                                )
                            },
                            onSetInProgress = {
                                viewModel.updateStatus(
                                    reportId = report.id,
                                    newStatus = ReportStatus.IN_PROGRESS,
                                    verifiedBy = employeeIdentifier
                                )
                            },
                            onResolve = {
                                viewModel.updateStatus(
                                    reportId = report.id,
                                    newStatus = ReportStatus.RESOLVED,
                                    verifiedBy = employeeIdentifier
                                )
                            },
                            onReject = {
                                viewModel.updateStatus(
                                    reportId = report.id,
                                    newStatus = ReportStatus.REJECTED,
                                    verifiedBy = employeeIdentifier
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Διάλογος επιβεβαίωσης & οδηγίας επικύρωσης
    if (reportToVerify != null) {
        val target = reportToVerify!!
        AlertDialog(
            onDismissRequest = { reportToVerify = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.verify_report_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.verify_incident_prompt, target.reportCategory.localizedTitle),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.verify_advice_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = verifyAdviceText,
                        onValueChange = { verifyAdviceText = it },
                        label = { Text(stringResource(R.string.advice_label)) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStatus(
                             reportId = target.id,
                             newStatus = ReportStatus.VERIFIED,
                             verifiedBy = employeeIdentifier,
                             practicalAdvice = verifyAdviceText.trim()
                        )
                        reportToVerify = null
                    }
                ) {
                    Text(stringResource(R.string.btn_verify))
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToVerify = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }
}

/**
 * Κάρτα Αναφοράς για τον Υπάλληλο με Badges, Πληροφορίες & Κουμπιά Ενεργειών.
 */
@Composable
fun ReportEmployeeCard(
    report: Report,
    onVerify: () -> Unit,
    onSetUnderReview: () -> Unit,
    onSetInProgress: () -> Unit,
    onResolve: () -> Unit,
    onReject: () -> Unit
) {
    val priorityLevel = PriorityCalculator.getPriorityLevel(report.priorityScore)
    val status = report.reportStatus
    val categoryTitle = report.reportCategory.localizedTitle

    // Μορφοποίηση ημερομηνίας
    val dateFormatted = remember(report.timestamp) {
        val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(report.timestamp))
    }

    // Αποκωδικοποίηση φωτογραφίας εάν υπάρχει
    val photoBitmap = remember(report.imageBase64) {
        if (!report.imageBase64.isNullOrBlank()) {
            ImageUtils.base64ToBitmap(report.imageBase64)
        } else null
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Τίτλος Κατηγορίας & Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Priority Badge («Κρίσιμη», «Υψηλή» κ.λπ.)
                PriorityBadge(priorityLevel = priorityLevel, score = report.priorityScore)
            }

            // Υπότιτλος: Status Badge & Ένδειξη Cluster
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = status)

                if (report.similarReportsCount > 1) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                stringResource(R.string.similar_reports_count, report.similarReportsCount),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }

            // Τοποθεσία & Κουμπί Χάρτη
            val cardContext = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.address.ifBlank {
                            String.format(Locale.US, "GPS: %.5f, %.5f", report.latitude, report.longitude)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = {
                        MapUtils.openMap(
                            context = cardContext,
                            latitude = report.latitude,
                            longitude = report.longitude,
                            label = categoryTitle
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

            // Περιγραφή χρήστη
            val descriptionText = report.localizedDescription
            if (descriptionText.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.description_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Φωτογραφία αναφοράς
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = "Report Attachment",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Πρακτική Οδηγία
            val adviceText = report.localizedAdvice
            if (adviceText.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.advice_prefix, adviceText),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Divider()

            // Κουμπιά Ενεργειών (Employee Actions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    ReportStatus.SUBMITTED -> {
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_reject))
                        }

                        Button(
                            onClick = onSetUnderReview,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0)
                            )
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_under_review))
                        }
                    }

                    ReportStatus.UNDER_REVIEW -> {
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_reject))
                        }

                        Button(
                            onClick = onVerify,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_verify))
                        }
                    }

                    ReportStatus.VERIFIED -> {
                        Button(
                            onClick = onSetInProgress,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF57C00) // Orange
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_in_progress))
                        }
                    }

                    ReportStatus.IN_PROGRESS -> {
                        Button(
                            onClick = onResolve,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32) // Dark Green
                            )
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_resolve))
                        }
                    }

                    ReportStatus.RESOLVED -> {
                        AssistChip(
                            onClick = {},
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(ReportStatus.RESOLVED.localizedTitle) }
                        )
                    }

                    ReportStatus.REJECTED -> {
                        AssistChip(
                            onClick = {},
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(ReportStatus.REJECTED.localizedTitle) }
                        )
                    }

                    else -> Unit
                }
            }
        }
    }
}

/**
 * Priority Badge με χρωματική κωδικοποίηση.
 */
@Composable
fun PriorityBadge(
    priorityLevel: PriorityCalculator.PriorityLevel,
    score: Double
) {
    val (backgroundColor, textColor) = when (priorityLevel) {
        PriorityCalculator.PriorityLevel.CRITICAL -> Color(0xFFFFCDD2) to Color(0xFFB71C1C) // Red
        PriorityCalculator.PriorityLevel.HIGH -> Color(0xFFFFE0B2) to Color(0xFFE65100)     // Orange
        PriorityCalculator.PriorityLevel.MEDIUM -> Color(0xFFFFF9C4) to Color(0xFFF57F17)   // Amber/Yellow
        PriorityCalculator.PriorityLevel.LOW -> Color(0xFFE1F5FE) to Color(0xFF0277BD)      // Light Blue
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${priorityLevel.localizedTitle} (${String.format(Locale.US, "%.1f", score)})",
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Status Badge με χρωματική κωδικοποίηση.
 */
@Composable
fun StatusBadge(status: ReportStatus) {
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
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
