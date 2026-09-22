package com.example.cityflow.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cityflow.R
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.User
import com.example.cityflow.domain.PriorityCalculator
import com.example.cityflow.ui.common.SettingsDialog
import com.example.cityflow.ui.common.localizedTitle
import com.example.cityflow.ui.common.localizedAdvice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentUser: User?,
    onNavigateToEmployeeDashboard: () -> Unit,
    onNavigateToStats: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    var wCount by remember { mutableFloatStateOf(PriorityCalculator.weightCount.toFloat()) }
    var wProximity by remember { mutableFloatStateOf(PriorityCalculator.weightProximity.toFloat()) }
    var wSeverity by remember { mutableFloatStateOf(PriorityCalculator.weightSeverity.toFloat()) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.admin_dashboard_title), fontWeight = FontWeight.Bold)
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
                    // Quick jump to Stats
                    if (onNavigateToStats != null) {
                        IconButton(onClick = onNavigateToStats) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = stringResource(R.string.stats_title),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    // Quick jump to Employee Dashboard
                    IconButton(onClick = onNavigateToEmployeeDashboard) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = stringResource(R.string.admin_view_employee_dashboard),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    // Logout
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Admin Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Συνδεδεμένος: ${currentUser?.fullName ?: "Διαχειριστής"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentUser?.email.orEmpty()} • ${stringResource(R.string.role_admin)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Button to View Employee Dashboard
            item {
                ElevatedCard(
                    onClick = onNavigateToEmployeeDashboard,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.admin_view_employee_dashboard),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Έλεγχος και επικύρωση όλων των υποβληθέντων αναφορών",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Button to View Stats & History
            if (onNavigateToStats != null) {
                item {
                    ElevatedCard(
                        onClick = onNavigateToStats,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.stats_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Προβολή δεικτών, κατανομής κατηγοριών και ποσοστού επίλυσης",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Priority Formula Weights Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.admin_weights_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "PriorityScore = w1 * Count + w2 * Proximity + w3 * Severity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // w1 - Count
                        Text(stringResource(R.string.admin_weight_count, wCount))
                        Slider(
                            value = wCount,
                            onValueChange = { wCount = Math.round(it * 10f) / 10f },
                            valueRange = 0.5f..5.0f,
                            steps = 9
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // w2 - Proximity
                        Text(stringResource(R.string.admin_weight_proximity, wProximity))
                        Slider(
                            value = wProximity,
                            onValueChange = { wProximity = Math.round(it * 10f) / 10f },
                            valueRange = 0.5f..5.0f,
                            steps = 9
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // w3 - Severity
                        Text(stringResource(R.string.admin_weight_severity, wSeverity))
                        Slider(
                            value = wSeverity,
                            onValueChange = { wSeverity = Math.round(it * 10f) / 10f },
                            valueRange = 0.5f..5.0f,
                            steps = 9
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                PriorityCalculator.weightCount = wCount.toDouble()
                                PriorityCalculator.weightProximity = wProximity.toDouble()
                                PriorityCalculator.weightSeverity = wSeverity.toDouble()
                                Toast.makeText(context, context.getString(R.string.admin_weights_saved), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.admin_save_weights))
                        }
                    }
                }
            }

            // Categories & Radii Oversight
            item {
                Text(
                    text = stringResource(R.string.admin_category_radii_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(ReportCategory.entries) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.localizedTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("Ακτίνα: ${category.defaultRadiusMeters.toInt()}m")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Σοβαρότητα: ${category.defaultSeverity}/5 • Οδηγία: ${category.localizedAdvice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }
}
