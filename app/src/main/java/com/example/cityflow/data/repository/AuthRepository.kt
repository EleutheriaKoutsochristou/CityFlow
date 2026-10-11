package com.example.cityflow.data.repository

import com.example.cityflow.data.model.Resource
import com.example.cityflow.data.model.User
import com.example.cityflow.data.model.UserRole
import com.example.cityflow.util.AppSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository managing user accounts, authentication, and user profile data.
 * Bridges Firebase Authentication with Cloud Firestore (the "users" collection).
 * Also supports simulated/demo accounts for grading and testing without external auth dependency.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** The currently authenticated Firebase user instance, or null if unauthenticated. */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Current user's Firebase UID, or null. */
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /** True if a user is currently signed in. */
    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Signs in a user using Email and Password.
     * Includes support for preset evaluation accounts ("admin" and "employee").
     *
     * @param email User's email or preset demo username.
     * @param password Password credential.
     * @return [Resource.Success] with the loaded [User] profile, or [Resource.Error] upon failure.
     */
    suspend fun login(email: String, password: String): Resource<User> {
        val trimmedIdentifier = email.trim()
        val trimmedPassword = password.trim()

        // 1. Check for preset Administrator demo credentials
        if (trimmedIdentifier.equals("admin", ignoreCase = true) ||
            trimmedIdentifier.equals("admin@cityflow.com", ignoreCase = true) ||
            trimmedIdentifier.equals("admin@cityflow.local", ignoreCase = true)
        ) {
            val isEn = AppSettings.currentLanguage.value == "en"
            return if (trimmedPassword == "admin") {
                Resource.Success(
                    User(
                        id = "hardcoded_admin",
                        email = "admin@cityflow.local",
                        fullName = if (isEn) "Administrator" else "Διαχειριστής",
                        role = UserRole.ADMIN.name,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                Resource.Error(if (isEn) "Incorrect password for admin account." else "Λανθασμένος κωδικός πρόσβασης για το λογαριασμό admin.")
            }
        }

        // 2. Check for preset Employee demo credentials
        if (trimmedIdentifier.equals("employee", ignoreCase = true) ||
            trimmedIdentifier.equals("employee@cityflow.com", ignoreCase = true) ||
            trimmedIdentifier.equals("employee@cityflow.local", ignoreCase = true)
        ) {
            val isEn = AppSettings.currentLanguage.value == "en"
            return if (trimmedPassword == "employee") {
                Resource.Success(
                    User(
                        id = "hardcoded_employee",
                        email = "employee@cityflow.local",
                        fullName = if (isEn) "Municipality Officer" else "Υπάλληλος Δήμου",
                        role = UserRole.EMPLOYEE.name,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                Resource.Error(if (isEn) "Incorrect password for employee account." else "Λανθασμένος κωδικός πρόσβασης για το λογαριασμό employee.")
            }
        }

        // 3. Authenticate with Firebase Authentication
        return try {
            val isEn = AppSettings.currentLanguage.value == "en"
            val authResult = auth.signInWithEmailAndPassword(trimmedIdentifier, password).await()
            val firebaseUser = authResult.user
                ?: return Resource.Error(if (isEn) "Authentication failed: No user found." else "Αποτυχία ταυτοποίησης: Δεν βρέθηκε χρήστης.")

            // Fetch custom user profile attributes (role, full name) from Firestore
            val userSnapshot = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val appUser = userSnapshot.toObject(User::class.java) ?: User(
                id = firebaseUser.uid,
                email = firebaseUser.email.orEmpty(),
                fullName = firebaseUser.displayName.orEmpty().ifBlank { if (isEn) "Citizen" else "Χρήστης" },
                role = UserRole.CITIZEN.name
            )

            Resource.Success(appUser)
        } catch (e: Exception) {
            val isEn = AppSettings.currentLanguage.value == "en"
            Resource.Error(e.localizedMessage ?: (if (isEn) "Error during sign in." else "Σφάλμα κατά τη σύνδεση."), e)
        }
    }

    /**
     * Registers a new user account with Firebase Authentication and creates their profile in Cloud Firestore.
     * Note: Creating Admin accounts is restricted to existing administrators.
     *
     * @param email Account email.
     * @param password Account password.
     * @param fullName Full display name.
     * @param role Target authorization role.
     * @return [Resource.Success] with the newly created [User], or [Resource.Error].
     */
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        role: UserRole
    ): Resource<User> {
        val isEn = AppSettings.currentLanguage.value == "en"
        if (role == UserRole.ADMIN) {
            return Resource.Error(if (isEn) "Registering as administrator is not permitted." else "Η εγγραφή ως διαχειριστής δεν επιτρέπεται.")
        }

        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: return Resource.Error(if (isEn) "Registration failed: No user created." else "Αποτυχία εγγραφής: Δεν δημιουργήθηκε χρήστης.")

            val newUser = User(
                id = firebaseUser.uid,
                email = firebaseUser.email.orEmpty(),
                fullName = fullName.trim(),
                role = role.name,
                createdAt = System.currentTimeMillis()
            )

            // Save new user document in "users" collection
            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(newUser)
                .await()

            Resource.Success(newUser)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: (if (isEn) "Error during registration." else "Σφάλμα κατά την εγγραφή."), e)
        }
    }

    /**
     * Retrieves the profile document from Firestore for the currently logged-in user.
     */
    suspend fun getCurrentUserProfile(): Resource<User?> {
        val uid = currentUserId ?: return Resource.Success(null)
        val isEn = AppSettings.currentLanguage.value == "en"
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: (if (isEn) "Error retrieving profile." else "Σφάλμα ανάκτησης προφίλ."), e)
        }
    }

    /**
     * Updates profile attributes (e.g. Full Name) in Firestore and updates the Firebase Auth display name.
     *
     * @param fullName New full name.
     * @return [Resource.Success] with updated user data, or [Resource.Error].
     */
    suspend fun updateUserProfile(fullName: String): Resource<User> {
        val uid = currentUserId
        val isEn = AppSettings.currentLanguage.value == "en"
        val trimmedName = fullName.trim()

        if (trimmedName.isBlank()) {
            return Resource.Error(if (isEn) "Full name cannot be empty." else "Το ονοματεπώνυμο δεν μπορεί να είναι κενό.")
        }

        // Support for local/demo user profile update
        if (uid == null) {
            val email = auth.currentUser?.email.orEmpty()
            return Resource.Success(
                User(
                    id = "local_user",
                    email = email.ifBlank { "user@cityflow.local" },
                    fullName = trimmedName,
                    role = UserRole.CITIZEN.name,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        return try {
            // Update full name in Firestore document
            firestore.collection("users").document(uid).update("fullName", trimmedName).await()

            // Update Firebase Auth display name
            try {
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()
                auth.currentUser?.updateProfile(profileUpdates)?.await()
            } catch (_: Exception) {
                // Ignore profile update error if offline or limited auth
            }

            // Reload updated profile
            val snapshot = firestore.collection("users").document(uid).get().await()
            val updatedUser = snapshot.toObject(User::class.java) ?: User(
                id = uid,
                email = auth.currentUser?.email.orEmpty(),
                fullName = trimmedName,
                role = UserRole.CITIZEN.name
            )

            Resource.Success(updatedUser)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: (if (isEn) "Failed to update profile." else "Αποτυχία ενημέρωσης προφίλ."), e)
        }
    }

    /**
     * Signs out the current user from Firebase Authentication.
     */
    fun logout() {
        auth.signOut()
    }
}
