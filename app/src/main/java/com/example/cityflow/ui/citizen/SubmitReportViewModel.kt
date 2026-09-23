package com.example.cityflow.ui.citizen

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.data.model.Resource
import com.example.cityflow.data.repository.ReportRepository
import com.example.cityflow.util.AppSettings
import com.example.cityflow.util.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel για την οθόνη υποβολής νέας αναφοράς προβλήματος (Citizen Flow).
 */
class SubmitReportViewModel(
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(ReportCategory.RAMP_BLOCKED)
    val selectedCategory: StateFlow<ReportCategory> = _selectedCategory.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude: StateFlow<Double?> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude: StateFlow<Double?> = _longitude.asStateFlow()

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _photoBitmap = MutableStateFlow<Bitmap?>(null)
    val photoBitmap: StateFlow<Bitmap?> = _photoBitmap.asStateFlow()

    private val _photoBase64 = MutableStateFlow<String?>(null)
    val photoBase64: StateFlow<String?> = _photoBase64.asStateFlow()

    private val _submitState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val submitState: StateFlow<Resource<String>> = _submitState.asStateFlow()

    fun selectCategory(category: ReportCategory) {
        _selectedCategory.value = category
    }

    fun updateDescription(newDesc: String) {
        _description.value = newDesc
    }

    fun setLocation(lat: Double, lon: Double, addr: String = "") {
        _latitude.value = lat
        _longitude.value = lon
        _address.value = addr
        _isLocating.value = false
        _locationError.value = null
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

    fun setPhoto(bitmap: Bitmap?) {
        _photoBitmap.value = bitmap
        if (bitmap != null) {
            _photoBase64.value = ImageUtils.bitmapToBase64(bitmap)
        } else {
            _photoBase64.value = null
        }
    }

    fun clearPhoto() {
        _photoBitmap.value = null
        _photoBase64.value = null
    }

    /**
     * Υποβολή της αναφοράς στο Firestore με προληπτικούς ελέγχους εγκυρότητας.
     */
    fun submitReport(userId: String, userEmail: String, isNetworkAvailable: Boolean = true) {
        val isEn = AppSettings.currentLanguage.value == "en"

        // 1. Προληπτικός έλεγχος σύνδεσης στο διαδίκτυο
        if (!isNetworkAvailable) {
            _submitState.value = Resource.Error(
                if (isEn) "No active internet connection. Please check your network."
                else "Δεν υπάρχει ενεργή σύνδεση στο διαδίκτυο. Παρακαλώ ελέγξτε τη σύνδεσή σας."
            )
            return
        }

        // 2. Έλεγχος τοποθεσίας GPS
        val lat = _latitude.value
        val lon = _longitude.value
        if (lat == null || lon == null) {
            _submitState.value = Resource.Error(
                if (isEn) "GPS location required before submission."
                else "Απαιτείται λήψη τοποθεσίας GPS πριν την υποβολή."
            )
            return
        }

        // 3. Έλεγχος υποχρεωτικής περιγραφής
        val trimmedDesc = _description.value.trim()
        if (trimmedDesc.isBlank()) {
            _submitState.value = Resource.Error(
                if (isEn) "Please enter a brief description of the issue."
                else "Παρακαλώ πληκτρολογήστε μια σύντομη περιγραφή του προβλήματος."
            )
            return
        }

        val category = _selectedCategory.value
        val report = Report(
            userId = userId,
            userEmail = userEmail,
            category = category.name,
            description = _description.value.trim(),
            latitude = lat,
            longitude = lon,
            address = _address.value.trim(),
            imageBase64 = _photoBase64.value,
            timestamp = System.currentTimeMillis(),
            status = ReportStatus.SUBMITTED.name,
            notificationRadiusMeters = category.defaultRadiusMeters,
            practicalAdvice = category.getDefaultAdvice(AppSettings.currentLanguage.value)
        )

        viewModelScope.launch {
            _submitState.value = Resource.Loading
            val result = reportRepository.submitReport(report)
            _submitState.value = result
        }
    }

    fun resetSubmitState() {
        _submitState.value = Resource.Idle
    }

    fun resetForm() {
        _description.value = ""
        clearPhoto()
        _submitState.value = Resource.Idle
    }
}
