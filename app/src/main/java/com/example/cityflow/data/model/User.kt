package com.example.cityflow.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Μοντέλο χρήστη της εφαρμογής CityFlow.
 * Αποθηκεύεται στη συλλογή "users" του Firestore.
 *
 * Τα default values επιτρέπουν την αυτόματη μετατροπή από το Firestore.
 */
@IgnoreExtraProperties
data class User(
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = UserRole.CITIZEN.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    @get:Exclude
    val userRole: UserRole
        get() = UserRole.fromString(role)

    @get:Exclude
    val isEmployeeOrAdmin: Boolean
        get() = userRole == UserRole.EMPLOYEE || userRole == UserRole.ADMIN

    @get:Exclude
    val isAdmin: Boolean
        get() = userRole == UserRole.ADMIN
}
