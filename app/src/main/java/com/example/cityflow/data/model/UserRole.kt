package com.example.cityflow.data.model

/**
 * Ρόλοι χρηστών στην εφαρμογή CityFlow:
 * - CITIZEN: Απλός πολίτης που υποβάλλει αναφορές και λαμβάνει ειδοποιήσεις.
 * - EMPLOYEE: Υπάλληλος δήμου ή φορέα μεταφορών που ελέγχει/επικυρώνει αναφορές.
 * - ADMIN: Διαχειριστής συστήματος που μπορεί να ρυθμίζει παραμέτρους (ακτίνες, βάρη κ.λπ.).
 */
enum class UserRole {
    CITIZEN,
    EMPLOYEE,
    ADMIN;

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CITIZEN
        }
    }
}
