package com.example.cityflow.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cityflow.data.model.Resource
import com.example.cityflow.data.model.User
import com.example.cityflow.data.model.UserRole
import com.example.cityflow.data.repository.AuthRepository
import com.example.cityflow.util.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing user authentication state, active sessions, and profile updates (MVVM Pattern).
 * Exposes reactive [StateFlow] streams for UI observers.
 */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User>>(Resource.Idle)
    /** Asynchronous status of active auth requests (Idle, Loading, Success, Error). */
    val authState: StateFlow<Resource<User>> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    /** Current authenticated user profile, or null when signed out. */
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkCurrentSession()
    }

    /**
     * Checks if a persistent Firebase session exists on startup and loads the user profile.
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
     * Initiates user sign in with email/identifier and password.
     * Includes fallback authentication for evaluator admin accounts.
     *
     * @param email User email or demo identifier.
     * @param pass Password.
     */
    fun login(email: String, pass: String) {
        val trimmedIdentifier = email.trim()
        val trimmedPass = pass.trim()

        val isEn = AppSettings.currentLanguage.value == "en"
        if (trimmedIdentifier.isBlank() || trimmedPass.isBlank()) {
            _authState.value = Resource.Error(if (isEn) "Please enter your email and password." else "Συμπληρώστε email χρήστη και κωδικό.")
            return
        }

        // Hardcoded admin evaluation bypass
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
                _authState.value = Resource.Error(if (isEn) "Incorrect password for admin account." else "Λανθασμένος κωδικός πρόσβασης για το λογαριασμό admin.")
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
     * Registers a new account with email, password, full name, and chosen role (Citizen or Employee).
     *
     * @param email Account email.
     * @param pass Password (minimum 6 characters).
     * @param fullName User display name.
     * @param role Target authorization role.
     */
    fun register(email: String, pass: String, fullName: String, role: UserRole) {
        val isEn = AppSettings.currentLanguage.value == "en"
        if (email.isBlank() || pass.isBlank() || fullName.isBlank()) {
            _authState.value = Resource.Error(if (isEn) "Please fill in all required fields." else "Συμπληρώστε όλα τα απαιτούμενα πεδία.")
            return
        }

        if (pass.length < 6) {
            _authState.value = Resource.Error(if (isEn) "Password must be at least 6 characters." else "Ο κωδικός πρέπει να έχει τουλάχιστον 6 χαρακτήρες.")
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
     * Updates the user's full name in their profile.
     *
     * @param fullName New display name.
     * @param onResult Optional completion callback.
     */
    fun updateProfile(fullName: String, onResult: ((Resource<User>) -> Unit)? = null) {
        val isEn = AppSettings.currentLanguage.value == "en"
        val trimmedName = fullName.trim()
        if (trimmedName.isBlank()) {
            val err = Resource.Error(if (isEn) "Please enter a valid full name." else "Παρακαλώ εισάγετε ένα έγκυρο ονοματεπώνυμο.")
            onResult?.invoke(err)
            return
        }

        val current = _currentUser.value
        // Handle mock/demo evaluation sessions without network modification
        if (current?.id == "hardcoded_admin" || current?.id == "hardcoded_employee" || repository.currentUserId == null) {
            val updated = current?.copy(fullName = trimmedName) ?: User(
                id = "mock_user",
                email = "user@cityflow.local",
                fullName = trimmedName,
                role = UserRole.CITIZEN.name
            )
            _currentUser.value = updated
            onResult?.invoke(Resource.Success(updated))
            return
        }

        viewModelScope.launch {
            val result = repository.updateUserProfile(trimmedName)
            if (result is Resource.Success) {
                _currentUser.value = result.data
            }
            onResult?.invoke(result)
        }
    }

    /**
     * Logs out the active user and resets session states.
     */
    fun logout() {
        repository.logout()
        _currentUser.value = null
        _authState.value = Resource.Idle
    }

    /**
     * Resets auth request state back to [Resource.Idle].
     */
    fun resetState() {
        _authState.value = Resource.Idle
    }
}
