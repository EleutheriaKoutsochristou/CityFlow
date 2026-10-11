package com.example.cityflow.ui.citizen

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityflow.R
import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.model.User
import com.example.cityflow.domain.DistanceCalculator
import com.example.cityflow.ui.common.SettingsDialog
import com.example.cityflow.ui.common.localizedTitle
import com.example.cityflow.ui.common.localizedAdvice
import com.example.cityflow.ui.common.localizedDescription
import com.example.cityflow.util.ImageUtils
import com.example.cityflow.util.MapUtils
import com.example.cityflow.util.NotificationHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Citizen Main Landing Screen (Citizen Flow).
 *
 * Implements:
 * - Current GPS location display with manual refresh and reverse geocoding.
 * - Quick action banner buttons (Submit New Report, Incident Analytics & History).
 * - Real-time stream of nearby verified hazards and accessibility obstacles within category geofence radius.
 * - Localized practical advice alerts ("Avoid area", "Exercise caution").
 * - Photo dialog preview for captured evidence images.
 * - Push notification trigger upon entering geofenced areas.
 * - Top app bar with language/theme settings and profile navigation.
 *
 * @param currentUser Currently signed-in citizen profile.
 * @param onNavigateToNewReport Navigation callback to open report submission.
 * @param onNavigateToStats Navigation callback to open stats and history.
 * @param onNavigateToProfile Navigation callback to open profile.
 * @param onLogout Callback for signing out.
 * @param viewModel ViewModel tracking location, nearby incidents, and announcements.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenHomeScreen(
    currentUser: User?,
    onNavigateToNewReport: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToProfile: (() -> Unit)? = null,
    onLogout: () -> Unit,
    viewModel: CitizenHomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val userLat by viewModel.userLatitude.collectAsState()
    val userLon by viewModel.userLongitude.collectAsState()
    val userAddress by viewModel.userAddress.collectAsState()
    val isLocating by viewModel.isLocating.collectAsState()
    val locationError by viewModel.locationError.collectAsState()
    val nearbyAlerts by viewModel.nearbyAlerts.collectAsState()
    val resolvedAnnouncements by viewModel.recentResolvedAnnouncements.collectAsState()
    val isLoadingReports by viewModel.isLoadingReports.collectAsState()

    var selectedImageForDialog by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Geocoder Helper
    fun resolveAddress(lat: Double, lon: Double) {
        coroutineScope.launch(Dispatchers.IO) {
            var resolved = ""
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val thoroughfare = addr.thoroughfare ?: ""
                            val subThoroughfare = addr.subThoroughfare ?: ""
                            val locality = addr.locality ?: ""
                            resolved = if (thoroughfare.isNotBlank()) {
                                "$thoroughfare $subThoroughfare, $locality".trim().removePrefix(",").trim()
                            } else {
                                addr.getAddressLine(0) ?: ""
                            }
                        }
                        viewModel.setUserLocation(lat, lon, resolved, context)
                    }
                    return@launch
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val thoroughfare = addr.thoroughfare ?: ""
                        val subThoroughfare = addr.subThoroughfare ?: ""
                        val locality = addr.locality ?: ""
                        resolved = if (thoroughfare.isNotBlank()) {
                            "$thoroughfare $subThoroughfare, $locality".trim().removePrefix(",").trim()
                        } else {
                            addr.getAddressLine(0) ?: ""
                        }
                    }
                }
            } catch (_: Exception) {
                // Αν αποτύχει το geocoding, διατηρούμε τις συντεταγμένες
            }
            viewModel.setUserLocation(lat, lon, resolved, context)
        }
    }

    // Λήψη τρέχουσας τοποθεσίας GPS
    fun fetchLocation() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            viewModel.setLocationError(context.getString(R.string.location_permission_needed))
            return
        }

        viewModel.setLocationLoading(true)

        try {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        resolveAddress(location.latitude, location.longitude)
                    } else {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                resolveAddress(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                viewModel.setLocationError(context.getString(R.string.location_gps_failed))
                            }
                        }.addOnFailureListener {
                            viewModel.setLocationError(context.getString(R.string.location_not_available))
                        }
                    }
                }
                .addOnFailureListener {
                    viewModel.setLocationError(context.getString(R.string.location_not_available))
                }
        } catch (_: SecurityException) {
            viewModel.setLocationError(context.getString(R.string.location_permission_denied))
        }
    }

    // Διαχειριστής αιτημάτων αδειών (GPS & Push Notifications)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val fineGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            viewModel.setLocationError(context.getString(R.string.location_permission_denied))
        }
    }

    // Αρχικοποίηση καναλιού ειδοποιήσεων & αίτημα αδειών κατά την εκκίνηση
    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)

        val neededPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(neededPermissions.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    if (onNavigateToProfile != null) {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = stringResource(R.string.profile_title),
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // User Greeting & GPS Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val defaultRoleName = stringResource(R.string.role_citizen)
                        val citizenName = currentUser?.fullName?.ifBlank { defaultRoleName } ?: defaultRoleName
                        Text(
                            text = stringResource(R.string.welcome_user, citizenName),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.role_label, defaultRoleName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // GPS Status Chip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (userLat != null) Icons.Default.LocationOn else Icons.Default.LocationOff,
                                    contentDescription = null,
                                    tint = if (userLat != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))

                                when {
                                    isLocating -> {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.location_fetching),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    userLat != null -> {
                                        Text(
                                            text = if (userAddress.isNotBlank()) userAddress else "Lat: ${String.format("%.4f", userLat)}, Lon: ${String.format("%.4f", userLon)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                    locationError != null -> {
                                        Text(
                                            text = locationError ?: stringResource(R.string.location_unknown),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 1
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = stringResource(R.string.location_tap_refresh),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { fetchLocation() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.location_retry),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions: Submit Report & Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Action 1: New Report
                    ElevatedCard(
                        onClick = onNavigateToNewReport,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.nav_new_report),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.btn_submit_gps),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Action 2: Stats & History
                    ElevatedCard(
                        onClick = onNavigateToStats,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.nav_stats),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.stats_history_quick),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section Header: Nearby Mobility Alerts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.nearby_alerts_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (userLat != null && nearbyAlerts.isNotEmpty()) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = "${nearbyAlerts.size}",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            // Nearby Alerts Content / Feed
            when {
                userLat == null && !isLocating -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.gps_enable_prompt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = {
                                    val needed = mutableListOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                    permissionsLauncher.launch(needed.toTypedArray())
                                }) {
                                    Text(stringResource(R.string.btn_enable_location))
                                }
                            }
                        }
                    }
                }

                isLoadingReports -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                nearbyAlerts.isEmpty() -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32), // Πράσινο
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = stringResource(R.string.no_nearby_alerts),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = stringResource(R.string.no_nearby_obstacles),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    items(nearbyAlerts, key = { it.report.id }) { alertItem ->
                        NearbyAlertCard(
                            alertItem = alertItem,
                            onImageClick = { base64 -> selectedImageForDialog = base64 }
                        )
                    }
                }
            }

            // Section: Πρόσφατες Ανακοινώσεις Επίλυσης
            if (resolvedAnnouncements.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.nearby_resolved_announcements_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(resolvedAnnouncements, key = { "resolved_${it.id}" }) { resolvedReport ->
                    ResolvedAnnouncementCard(report = resolvedReport)
                }
            }
        }
    }

    // Modal προβολής φωτογραφίας σε πλήρες μέγεθος
    selectedImageForDialog?.let { base64 ->
        Dialog(onDismissRequest = { selectedImageForDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val bitmap = remember(base64) { ImageUtils.base64ToBitmap(base64) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.incident_photo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { selectedImageForDialog = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.close))
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
 * Κάρτα προβολής κοντινού συμβάντος (Nearby Alert Card).
 *
 * Εμφανίζει:
 * - Κατηγορία προβλήματος και αντίστοιχο εικονίδιο
 * - Υπολογισμένη απόσταση από τον χρήστη
 * - Ένδειξη κατάστασης (Επικυρωμένη / Σε εξέλιξη)
 * - Highlighted Πρακτική Οδηγία («Αποφύγετε το σημείο» κ.λπ.)
 * - Περιγραφή, διεύθυνση, χρονοσφραγίδα
 * - Προαιρετική φωτογραφία
 */
@Composable
fun NearbyAlertCard(
    alertItem: NearbyAlertItem,
    onImageClick: (String) -> Unit
) {
    val report = alertItem.report
    val distanceFormatted = DistanceCalculator.formatDistance(alertItem.distanceMeters)
    val category = report.reportCategory
    val categoryTitle = category.localizedTitle

    val practicalAdvice = report.localizedAdvice
    val descriptionText = report.localizedDescription

    val dateFormatter = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }
    val formattedDate = remember(report.timestamp) {
        dateFormatter.format(Date(report.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category Header, Distance & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Distance Chip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = stringResource(R.string.distance_away, distanceFormatted),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Highlighted Practical Advice Box (Προδιαγραφή 5)
            if (practicalAdvice.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.advice_prefix, practicalAdvice),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Description
            if (descriptionText.isNotBlank()) {
                Text(
                    text = "${stringResource(R.string.description_label)}: $descriptionText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Location Address & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (report.address.isNotBlank()) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = report.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Optional Thumbnail Photo
            report.imageBase64?.let { base64 ->
                val bitmap = remember(base64) { ImageUtils.base64ToBitmap(base64) }
                if (bitmap != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.incident_photo),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClick(base64) },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Κουμπί Προβολής στον Χάρτη
            val context = LocalContext.current
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    MapUtils.openMap(
                        context = context,
                        latitude = report.latitude,
                        longitude = report.longitude,
                        label = categoryTitle
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.view_on_map),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Επιστρέφει εικονίδιο Material Icons ανά κατηγορία προβλήματος.
 */
fun getCategoryIcon(category: ReportCategory): ImageVector {
    return when (category) {
        ReportCategory.RAMP_BLOCKED -> Icons.AutoMirrored.Filled.Accessible
        ReportCategory.MICROMOBILITY_OBSTACLE -> Icons.AutoMirrored.Filled.DirectionsBike
        ReportCategory.POTHOLE -> Icons.Default.Warning
        ReportCategory.ROAD_WORK -> Icons.Default.Construction
        ReportCategory.TRAFFIC_LIGHT -> Icons.Default.Traffic
        ReportCategory.TRANSIT_OVERCROWDING -> Icons.Default.DirectionsBus
        ReportCategory.OTHER -> Icons.Default.ReportProblem
    }
}

/**
 * Κάρτα προβολής ανακοίνωσης επίλυσης προβλήματος (Resolved Announcement Card).
 */
@Composable
fun ResolvedAnnouncementCard(report: Report) {
    val categoryTitle = report.reportCategory.localizedTitle
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val formattedDate = remember(report.resolvedAt, report.timestamp) {
        dateFormatter.format(Date(report.resolvedAt ?: report.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = stringResource(R.string.filter_resolved),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2E7D32).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.resolutionAnnouncement,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            if (report.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = report.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.resolved_at_label, formattedDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

