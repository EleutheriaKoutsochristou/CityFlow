package com.example.cityflow.ui.citizen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cityflow.R
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.Resource
import com.example.cityflow.data.model.User
import com.example.cityflow.ui.common.localizedTitle
import com.example.cityflow.util.ImageUtils
import com.example.cityflow.util.NetworkUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Screen for submitting an incident report by a citizen (Citizen Flow).
 *
 * Includes:
 * - Category picker dropdown with visual indicators.
 * - Mandatory detailed description field.
 * - Automatic GPS location detection with reverse geocoding to human-readable address.
 * - Photo capture via Camera and Gallery image picker with image preview and delete option.
 * - Connectivity check and form validation prior to submission.
 * - Submission feedback via Snackbar and automatic navigation pop on success.
 *
 * @param currentUser The citizen currently submitting the report.
 * @param viewModel ViewModel controlling form state and submission.
 * @param onNavigateBack Callback when back button is pressed.
 * @param onSubmitSuccess Callback invoked upon successful report creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitReportScreen(
    currentUser: User?,
    viewModel: SubmitReportViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val description by viewModel.description.collectAsState()
    val latitude by viewModel.latitude.collectAsState()
    val longitude by viewModel.longitude.collectAsState()
    val address by viewModel.address.collectAsState()
    val isLocating by viewModel.isLocating.collectAsState()
    val locationError by viewModel.locationError.collectAsState()
    val photoBitmap by viewModel.photoBitmap.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    var dropdownExpanded by remember { mutableStateOf(false) }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Helper για επίλυση διεύθυνσης μέσω Geocoder
    fun resolveAddress(lat: Double, lon: Double) {
        coroutineScope.launch(Dispatchers.IO) {
            var resolvedAddr = ""
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val thoroughfare = addr.thoroughfare ?: ""
                            val subThoroughfare = addr.subThoroughfare ?: ""
                            val locality = addr.locality ?: ""
                            resolvedAddr = if (thoroughfare.isNotBlank()) {
                                "$thoroughfare $subThoroughfare, $locality".trim().removePrefix(",").trim()
                            } else {
                                addr.getAddressLine(0) ?: ""
                            }
                        }
                        viewModel.setLocation(lat, lon, resolvedAddr)
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
                        resolvedAddr = if (thoroughfare.isNotBlank()) {
                            "$thoroughfare $subThoroughfare, $locality".trim().removePrefix(",").trim()
                        } else {
                            addr.getAddressLine(0) ?: ""
                        }
                    }
                }
            } catch (_: Exception) {
                // Αν αποτύχει το geocoding, οι συντεταγμένες παραμένουν έγκυρες
            }
            viewModel.setLocation(lat, lon, resolvedAddr)
        }
    }

    // Λήψη τρέχουσας τοποθεσίας
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
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { loc ->
                if (loc != null) {
                    resolveAddress(loc.latitude, loc.longitude)
                } else {
                    // Fallback στο lastLocation
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            resolveAddress(lastLoc.latitude, lastLoc.longitude)
                        } else {
                            viewModel.setLocationError(context.getString(R.string.location_not_available))
                        }
                    }.addOnFailureListener {
                        viewModel.setLocationError(context.getString(R.string.location_not_available))
                    }
                }
            }.addOnFailureListener {
                // Fallback στο lastLocation σε περίπτωση σφάλματος
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null) {
                        resolveAddress(lastLoc.latitude, lastLoc.longitude)
                    } else {
                        viewModel.setLocationError(context.getString(R.string.location_not_available))
                    }
                }.addOnFailureListener {
                    viewModel.setLocationError(context.getString(R.string.location_not_available))
                }
            }
        } catch (_: SecurityException) {
            viewModel.setLocationError(context.getString(R.string.location_permission_needed))
        }
    }

    // Launcher για αίτημα αδειών τοποθεσίας
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            viewModel.setLocationError(context.getString(R.string.location_permission_needed))
        }
    }

    // Launcher για λήψη φωτογραφίας από κάμερα
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.setPhoto(bitmap)
        }
    }

    // Launcher για αίτημα άδειας κάμερας
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        }
    }

    // Launcher για επιλογή φωτογραφίας από συλλογή
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = ImageUtils.uriToBitmap(context, uri)
            viewModel.setPhoto(bitmap)
        }
    }

    // Αυτόματη έναρξη λήψης τοποθεσίας κατά το άνοιγμα της οθόνης
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            fetchLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Χειρισμός αποτελέσματος υποβολής (Snackbar επιβεβαίωσης)
    LaunchedEffect(submitState) {
        when (val state = submitState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.report_submit_success),
                    duration = SnackbarDuration.Short
                )
                delay(1200)
                viewModel.resetForm()
                onSubmitSuccess()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long
                )
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.submit_report_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Dropdown Επιλογής Κατηγορίας
            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory.localizedTitle,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    ReportCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = cat.localizedTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = stringResource(R.string.severity_level, cat.defaultSeverity) +
                                                " • " + stringResource(R.string.radius_meters, cat.defaultRadiusMeters),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectCategory(cat)
                                dropdownExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // 2. Περιγραφή / Σχόλια
            Text(
                text = "${stringResource(R.string.description_label)} *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.updateDescription(it) },
                placeholder = { Text(stringResource(R.string.description_hint)) },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Αυτόματη Λήψη GPS Τοποθεσίας
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.location_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { fetchLocation() },
                            enabled = !isLocating
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.location_retry),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        isLocating -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = stringResource(R.string.location_fetching),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        latitude != null && longitude != null -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.location_obtained),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "GPS: %.5f, %.5f", latitude, longitude),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (address.isNotBlank()) {
                                    Text(
                                        text = address,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        locationError != null -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = locationError ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                ) {
                                    Text(stringResource(R.string.location_retry))
                                }
                            }
                        }
                    }
                }
            }

            // 4. Κουμπί για Φωτογραφία & Προεπισκόπηση
            Text(
                text = stringResource(R.string.photo_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (photoBitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Image(
                            bitmap = photoBitmap!!.asImageBitmap(),
                            contentDescription = "Report Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.clearPhoto() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.remove_photo))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasCamera = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCamera) {
                                cameraLauncher.launch()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.take_photo))
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.choose_gallery))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. Κουμπί Υποβολής & Εμφάνιση Snackbar Επιβεβαίωσης
            val canSubmit = latitude != null && longitude != null && description.trim().isNotBlank() && submitState !is Resource.Loading

            Button(
                onClick = {
                    val userId = currentUser?.id.orEmpty().ifBlank { "citizen_local" }
                    val userEmail = currentUser?.email.orEmpty().ifBlank { "citizen@cityflow.local" }
                    val isOnline = NetworkUtils.isNetworkAvailable(context)
                    viewModel.submitReport(userId, userEmail, isOnline)
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (submitState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.report_submitting))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.btn_submit_report),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
