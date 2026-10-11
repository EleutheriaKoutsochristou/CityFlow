package com.example.cityflow.ui.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cityflow.R
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.ui.common.localizedTitle
import com.example.cityflow.util.AppSettings
import java.util.Locale
import kotlin.math.max

/**
 * Resolves color palette mappings for incident categories in analytics charts.
 */
fun getCategoryColor(category: ReportCategory): Color {
    return when (category) {
        ReportCategory.RAMP_BLOCKED -> Color(0xFFE53935)          // Red (PRM ramp / sidewalk)
        ReportCategory.MICROMOBILITY_OBSTACLE -> Color(0xFFFB8C00) // Orange (Bike / scooter lane)
        ReportCategory.POTHOLE -> Color(0xFFFFB300)                // Amber (Pothole / pavement)
        ReportCategory.ROAD_WORK -> Color(0xFF8E24AA)              // Purple (Road work)
        ReportCategory.TRAFFIC_LIGHT -> Color(0xFF1E88E5)           // Blue (Traffic signal)
        ReportCategory.TRANSIT_OVERCROWDING -> Color(0xFF00ACC1)    // Cyan/Teal (Transit)
        ReportCategory.OTHER -> Color(0xFF78909C)                   // Blue Grey (Other)
    }
}

/**
 * Resolves color mappings for report lifecycle statuses in analytics charts.
 */
fun getStatusColor(status: ReportStatus): Color {
    return when (status) {
        ReportStatus.SUBMITTED -> Color(0xFFFFA726)   // Orange
        ReportStatus.VERIFIED -> Color(0xFF42A5F5)    // Blue
        ReportStatus.IN_PROGRESS -> Color(0xFFAB47BC) // Purple
        ReportStatus.RESOLVED -> Color(0xFF66BB6A)    // Green
        ReportStatus.REJECTED -> Color(0xFFEF5350)    // Red
        ReportStatus.UNDER_REVIEW -> Color(0xFF29B6F6) // Light Blue
    }
}

/**
 * Returns abbreviated status strings suitable for compact horizontal chart axis labels.
 */
fun getStatusShortLabel(status: ReportStatus, isEnglish: Boolean = false): String {
    return if (isEnglish) {
        when (status) {
            ReportStatus.SUBMITTED -> "Submit."
            ReportStatus.VERIFIED -> "Verif."
            ReportStatus.IN_PROGRESS -> "Prog."
            ReportStatus.RESOLVED -> "Resolv."
            ReportStatus.REJECTED -> "Reject."
            ReportStatus.UNDER_REVIEW -> "Review"
        }
    } else {
        when (status) {
            ReportStatus.SUBMITTED -> "Υποβλ."
            ReportStatus.VERIFIED -> "Επικυρ."
            ReportStatus.IN_PROGRESS -> "Εξέλιξη"
            ReportStatus.RESOLVED -> "Επιλύθ."
            ReportStatus.REJECTED -> "Απορρ."
            ReportStatus.UNDER_REVIEW -> "Έλεγχος"
        }
    }
}

/**
 * Interactive Donut Chart card displaying the category distribution breakdown of city incidents.
 * Features animated sweep angle arcs drawn on a Compose [Canvas] with centered incident count.
 *
 * @param breakdown List of calculated category metrics.
 * @param totalReports Total number of reports recorded.
 * @param modifier Composable modifier.
 */
@Composable
fun CategoryDonutChartCard(
    breakdown: List<CategoryStatItem>,
    totalReports: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_category_distribution_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Animated Donut Graphic with centered Total
            var animationTriggered by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                animationTriggered = true
            }

            val animProgress by animateFloatAsState(
                targetValue = if (animationTriggered) 1f else 0f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "donutAnim"
            )

            val activeItems = remember(breakdown) { breakdown.filter { it.count > 0 } }
            val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(170.dp)) {
                        val strokeWidth = 24.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        if (totalReports == 0 || activeItems.isEmpty()) {
                            drawArc(
                                color = emptyRingColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                topLeft = topLeft,
                                size = arcSize
                            )
                        } else {
                            var currentStartAngle = -90f
                            for (item in activeItems) {
                                val itemSweep = (item.percentage * 360f) * animProgress
                                val color = getCategoryColor(item.category)
                                drawArc(
                                    color = color,
                                    startAngle = currentStartAngle,
                                    sweepAngle = max(itemSweep - 2f, 1f), // μικρό κενό μεταξύ των τομέων
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                    topLeft = topLeft,
                                    size = arcSize
                                )
                                currentStartAngle += itemSweep
                            }
                        }
                    }

                    val isEnglish = AppSettings.currentLanguage.collectAsState().value == "en"
                    // Total Count Label in Center
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalReports",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEnglish) (if (totalReports == 1) "Incident" else "Incidents") else (if (totalReports == 1) "Συμβάν" else "Συμβάντα"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // Legend / Υπόμνημα Κατηγοριών
            if (activeItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.stats_no_reports_city),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeItems.forEach { item ->
                        val color = getCategoryColor(item.category)
                        val percentInt = (item.percentage * 100).toInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.category.localizedTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${item.count} ($percentInt%)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Vertical Bar Chart card representing the lifecycle status distribution funnel of incidents.
 *
 * @param statusBreakdown Computed status counts and percentage metrics.
 * @param modifier Composable modifier.
 */
@Composable
fun StatusBarChartCard(
    statusBreakdown: List<StatusStatItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.stats_status_funnel_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val maxCount = remember(statusBreakdown) {
                statusBreakdown.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            }

            var animationTriggered by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                animationTriggered = true
            }

            val animProgress by animateFloatAsState(
                targetValue = if (animationTriggered) 1f else 0f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                label = "barAnim"
            )
            val isEnglish = AppSettings.currentLanguage.collectAsState().value == "en"

            // Bar Chart Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                statusBreakdown.forEach { item ->
                    val barRatio = (item.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
                    val barHeightFraction = (barRatio * animProgress).coerceAtLeast(0.04f)
                    val color = getStatusColor(item.status)
                    val shortLabel = getStatusShortLabel(item.status, isEnglish)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Count text above bar
                        Text(
                            text = "${item.count}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Bar
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .weight(1f, fill = false)
                                .fillMaxHeight(fraction = barHeightFraction)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (item.count > 0) color
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Status label
                        Text(
                            text = shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider()

            // Status Summary Chips (Legend)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                statusBreakdown.take(3).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getStatusColor(item.status))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.status.localizedTitle}: ${item.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                statusBreakdown.drop(3).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getStatusColor(item.status))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.status.localizedTitle}: ${item.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
