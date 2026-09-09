package com.example.cityflow.data.model

/**
 * Μοντέλο χρήστη της εφαρμογής CityFlow.
 * Αποθηκεύεται στη συλλογή "users" του Firestore.
 *
 * Τα default values επιτρέπουν την αυτόματη μετατροπή από το Firestore.
 */
data class User(
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = UserRole.CITIZEN.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    val userRole: UserRole
        get() = UserRole.fromString(role)

    val isEmployeeOrAdmin: Boolean
        get() = userRole == UserRole.EMPLOYEE || userRole == UserRole.ADMIN

    val isAdmin: Boolean
        get() = userRole == UserRole.ADMIN
}
