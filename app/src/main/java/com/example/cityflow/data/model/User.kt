package com.example.cityflow.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * User data entity in CityFlow.
 * Persisted in the "users" collection in Cloud Firestore.
 *
 * Default parameter values enable automatic Firestore deserialization via reflection.
 *
 * @property id Unique user identifier matching Firebase Auth UID.
 * @property email User's account email address.
 * @property fullName Full display name of the user.
 * @property role Role identifier string corresponding to [UserRole] enum name.
 * @property createdAt Epoch timestamp (in ms) when the account was registered.
 */
@IgnoreExtraProperties
data class User(
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: String = UserRole.CITIZEN.name,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Resolved [UserRole] enum instance. */
    @get:Exclude
    val userRole: UserRole
        get() = UserRole.fromString(role)

    /** Returns true if the user possesses municipal staff or administrator privileges. */
    @get:Exclude
    val isEmployeeOrAdmin: Boolean
        get() = userRole == UserRole.EMPLOYEE || userRole == UserRole.ADMIN

    /** Returns true if the user has full administrator privileges. */
    @get:Exclude
    val isAdmin: Boolean
        get() = userRole == UserRole.ADMIN
}
