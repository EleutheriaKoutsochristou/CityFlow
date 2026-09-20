package com.example.cityflow.data.repository

import com.example.cityflow.data.model.Resource
import com.example.cityflow.data.model.User
import com.example.cityflow.data.model.UserRole
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
            return if (trimmedPassword == "admin") {
                Resource.Success(
                    User(
                        id = "hardcoded_admin",
                        email = "admin@cityflow.local",
                        fullName = "Administrator",
                        role = UserRole.ADMIN.name,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                Resource.Error("Λανθασμένος κωδικός πρόσβασης για το λογαριασμό admin.")
            }
        }

        if (trimmedIdentifier.equals("employee", ignoreCase = true) ||
            trimmedIdentifier.equals("employee@cityflow.com", ignoreCase = true) ||
            trimmedIdentifier.equals("employee@cityflow.local", ignoreCase = true)
        ) {
            return if (trimmedPassword == "employee") {
                Resource.Success(
                    User(
                        id = "hardcoded_employee",
                        email = "employee@cityflow.local",
                        fullName = "Υπάλληλος Δήμου",
                        role = UserRole.EMPLOYEE.name,
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                Resource.Error("Λανθασμένος κωδικός πρόσβασης για το λογαριασμό employee.")
            }
        }

        return try {
            val authResult = auth.signInWithEmailAndPassword(trimmedIdentifier, password).await()
            val firebaseUser = authResult.user
                ?: return Resource.Error("Αποτυχία ταυτοποίησης: Δεν βρέθηκε χρήστης.")

            val userSnapshot = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val appUser = userSnapshot.toObject(User::class.java) ?: User(
                id = firebaseUser.uid,
                email = firebaseUser.email.orEmpty(),
                fullName = firebaseUser.displayName.orEmpty().ifBlank { "Χρήστης" },
                role = UserRole.CITIZEN.name
            )

            Resource.Success(appUser)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Σφάλμα κατά τη σύνδεση.", e)
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
        if (role == UserRole.ADMIN) {
            return Resource.Error("Η εγγραφή ως διαχειριστής δεν επιτρέπεται.")
        }

        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: return Resource.Error("Αποτυχία εγγραφής: Δεν δημιουργήθηκε χρήστης.")

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
            Resource.Error(e.localizedMessage ?: "Σφάλμα κατά την εγγραφή.", e)
        }
    }

    /**
     * Ανάκτηση του προφίλ του τρέχοντος συνδεδεμένου χρήστη.
     */
    suspend fun getCurrentUserProfile(): Resource<User?> {
        val uid = currentUserId ?: return Resource.Success(null)
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Σφάλμα ανάκτησης προφίλ.", e)
        }
    }

    /**
     * Αποσύνδεση χρήστη.
     */
    fun logout() {
        auth.signOut()
    }
}
