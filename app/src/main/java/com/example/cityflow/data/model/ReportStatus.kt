package com.example.cityflow.data.model

/**
 * Lifecycle statuses of an urban mobility incident report in CityFlow:
 * - [SUBMITTED]: Initial status upon citizen submission.
 * - [VERIFIED]: Confirmed and acknowledged by a municipal employee.
 * - [UNDER_REVIEW]: Under detailed investigation or review by transit teams.
 * - [IN_PROGRESS]: Remediation / field crew action currently underway.
 * - [RESOLVED]: Issue successfully resolved with an optional public resolution announcement.
 * - [REJECTED]: Report marked as invalid or rejected with an optional justification.
 *
 * @property titleEl Greek display title.
 * @property titleEn English display title.
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

    /** Returns the localized title according to language code ("en" or "el"). */
    fun getTitle(lang: String): String = if (lang == "en") titleEn else titleEl

    companion object {
        /**
         * Resolves a case-insensitive status string to [ReportStatus], defaulting to [SUBMITTED].
         */
        fun fromString(value: String?): ReportStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SUBMITTED
        }
    }
}
