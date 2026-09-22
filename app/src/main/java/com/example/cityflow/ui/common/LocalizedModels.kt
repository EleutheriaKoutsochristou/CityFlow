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
