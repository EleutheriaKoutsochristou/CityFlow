package com.example.cityflow.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.ReportStatus
import com.example.cityflow.domain.PriorityCalculator
import com.example.cityflow.util.AppSettings

/**
 * Extension properties for Compose UI to dynamically display localized model titles
 * based on the active language in AppSettings.
 */
val ReportCategory.localizedTitle: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getTitle(lang)
    }

val ReportCategory.localizedAdvice: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getDefaultAdvice(lang)
    }

val ReportStatus.localizedTitle: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getTitle(lang)
    }

val PriorityCalculator.PriorityLevel.localizedTitle: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        return getTitle(lang)
    }

/**
 * Επιστρέφει την πρακτική οδηγία της αναφοράς μεταφρασμένη στη γλώσσα της εφαρμογής
 * εφόσον αντιστοιχεί σε προεπιλεγμένη οδηγία κατηγορίας.
 */
val com.example.cityflow.data.model.Report.localizedAdvice: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()
        val cat = reportCategory

        if (practicalAdvice.isBlank() ||
            practicalAdvice.equals(cat.defaultAdviceEl, ignoreCase = true) ||
            practicalAdvice.equals(cat.defaultAdviceEn, ignoreCase = true)
        ) {
            return cat.getDefaultAdvice(lang)
        }

        for (c in ReportCategory.entries) {
            if (practicalAdvice.equals(c.defaultAdviceEl, ignoreCase = true) ||
                practicalAdvice.equals(c.defaultAdviceEn, ignoreCase = true)
            ) {
                return c.getDefaultAdvice(lang)
            }
        }

        return practicalAdvice
    }

/**
 * Επιστρέφει την περιγραφή της αναφοράς μεταφρασμένη αν αντιστοιχεί σε τίτλο ή οδηγία κατηγορίας.
 */
val com.example.cityflow.data.model.Report.localizedDescription: String
    @Composable
    get() {
        val lang by AppSettings.currentLanguage.collectAsState()

        for (c in ReportCategory.entries) {
            if (description.equals(c.titleEl, ignoreCase = true) ||
                description.equals(c.titleEn, ignoreCase = true)
            ) {
                return c.getTitle(lang)
            }
            if (description.equals(c.defaultAdviceEl, ignoreCase = true) ||
                description.equals(c.defaultAdviceEn, ignoreCase = true)
            ) {
                return c.getDefaultAdvice(lang)
            }
        }

        return description
    }

