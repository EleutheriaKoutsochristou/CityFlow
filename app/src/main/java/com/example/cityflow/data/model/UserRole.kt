package com.example.cityflow.data.model

/**
 * System authorization roles for CityFlow users:
 * - [CITIZEN]: Regular citizen submitting reports and receiving nearby push/in-app notifications.
 * - [EMPLOYEE]: Municipal / public transit employee reviewing, verifying, updating, or resolving reports.
 * - [ADMIN]: Municipality supervisor configuring prioritization weights, cluster radii, and system settings.
 */
enum class UserRole {
    CITIZEN,
    EMPLOYEE,
    ADMIN;

    companion object {
        /**
         * Parses a case-insensitive string into a [UserRole], defaulting to [CITIZEN] if unmatched or null.
         */
        fun fromString(value: String?): UserRole {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CITIZEN
        }
    }
}
