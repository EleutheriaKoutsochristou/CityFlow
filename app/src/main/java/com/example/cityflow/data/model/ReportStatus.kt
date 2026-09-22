package com.example.cityflow.data.model

/**
 * Καταστάσεις κύκλου ζωής μιας αναφοράς (Report Status).
 */
enum class ReportStatus(
    val titleEl: String,
    val titleEn: String
) {
    SUBMITTED("Υποβλήθηκε", "Submitted"),
    VERIFIED("Επικυρώθηκε", "Verified"),
    UNDER_REVIEW("Υπό διερεύνηση", "Under Review"),
    IN_PROGRESS("Σε εξέλιξη", "In Progress"),
    RESOLVED("Επιλύθηκε", "Resolved"),
    REJECTED("Απορρίφθηκε", "Rejected");

    fun getTitle(lang: String): String = if (lang == "en") titleEn else titleEl

    companion object {
        fun fromString(value: String?): ReportStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SUBMITTED
        }
    }
}
