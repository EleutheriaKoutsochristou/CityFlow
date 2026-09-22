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
 * Repository για τη διαχείριση χρηστών και ταυτοποίησης (Authentication).
 * Συνδυάζει το Firebase Authentication με το Firestore (συλλογή "users").
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Σύνδεση χρήστη με Email & Password (ή hardcoded admin).
     */
    suspend fun login(email: String, password: String): Resource<User> {
        val trimmedIdentifier = email.trim()
        val trimmedPassword = password.trim()

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

        return try {
            val isEn = AppSettings.currentLanguage.value == "en"
            val authResult = auth.signInWithEmailAndPassword(trimmedIdentifier, password).await()
            val firebaseUser = authResult.user
                ?: return Resource.Error(if (isEn) "Authentication failed: No user found." else "Αποτυχία ταυτοποίησης: Δεν βρέθηκε χρήστης.")

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
     * Εγγραφή νέου χρήστη και αποθήκευση του ρόλου του στο Firestore.
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
     * Ανάκτηση του προφίλ του τρέχοντος συνδεδεμένου χρήστη.
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
     * Αποσύνδεση χρήστη.
     */
    fun logout() {
        auth.signOut()
    }
}
