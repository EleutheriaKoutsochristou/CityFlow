package com.example.cityflow.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityflow.data.model.Resource
import com.example.cityflow.data.model.User
import com.example.cityflow.data.model.UserRole
import com.example.cityflow.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel διαχείρισης κατάστασης ταυτοποίησης (Login / Registration / Session).
 */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User>>(Resource.Idle)
    val authState: StateFlow<Resource<User>> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkCurrentSession()
    }

    /**
     * Έλεγχος αν ο χρήστης είναι ήδη συνδεδεμένος.
     */
    fun checkCurrentSession() {
        if (repository.isUserLoggedIn) {
            viewModelScope.launch {
                _authState.value = Resource.Loading
                when (val result = repository.getCurrentUserProfile()) {
                    is Resource.Success -> {
                        val user = result.data
                        if (user != null) {
                            _currentUser.value = user
                            _authState.value = Resource.Success(user)
                        } else {
                            _authState.value = Resource.Idle
                        }
                    }
                    is Resource.Error -> {
                        _authState.value = Resource.Idle
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * Σύνδεση με email/username και κωδικό.
     */
    fun login(email: String, pass: String) {
        val trimmedIdentifier = email.trim()
        val trimmedPass = pass.trim()

        if (trimmedIdentifier.isBlank() || trimmedPass.isBlank()) {
            _authState.value = Resource.Error("Συμπληρώστε email/όνομα χρήστη και κωδικό.")
            return
        }

        // Hardcoded admin login
        if (trimmedIdentifier.equals("admin", ignoreCase = true)) {
            if (trimmedPass == "admin") {
                val adminUser = User(
                    id = "hardcoded_admin",
                    email = "admin@cityflow.local",
                    fullName = "Administrator",
                    role = UserRole.ADMIN.name,
                    createdAt = System.currentTimeMillis()
                )
                _currentUser.value = adminUser
                _authState.value = Resource.Success(adminUser)
            } else {
                _authState.value = Resource.Error("Λανθασμένος κωδικός πρόσβασης για το λογαριασμό admin.")
            }
            return
        }

        viewModelScope.launch {
            _authState.value = Resource.Loading
            val result = repository.login(trimmedIdentifier, pass)
            _authState.value = result
            if (result is Resource.Success) {
                _currentUser.value = result.data
            }
        }
    }

    /**
     * Εγγραφή νέου χρήστη με ρόλο (CITIZEN ή EMPLOYEE).
     */
    fun register(email: String, pass: String, fullName: String, role: UserRole) {
        if (email.isBlank() || pass.isBlank() || fullName.isBlank()) {
            _authState.value = Resource.Error("Συμπληρώστε όλα τα απαιτούμενα πεδία.")
            return
        }

        if (role == UserRole.ADMIN) {
            _authState.value = Resource.Error("Η εγγραφή ως διαχειριστής δεν επιτρέπεται.")
            return
        }

        if (pass.length < 6) {
            _authState.value = Resource.Error("Ο κωδικός πρέπει να έχει τουλάχιστον 6 χαρακτήρες.")
            return
        }

        viewModelScope.launch {
            _authState.value = Resource.Loading
            val result = repository.register(email, pass, fullName, role)
            _authState.value = result
            if (result is Resource.Success) {
                _currentUser.value = result.data
            }
        }
    }

    /**
     * Αποσύνδεση χρήστη.
     */
    fun logout() {
        repository.logout()
        _currentUser.value = null
        _authState.value = Resource.Idle
    }

    fun resetState() {
        _authState.value = Resource.Idle
    }
}
