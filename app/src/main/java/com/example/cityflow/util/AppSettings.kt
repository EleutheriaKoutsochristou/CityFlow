package com.example.cityflow.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Διαχειριστής ρυθμίσεων εφαρμογής:
 * 1. Εναλλαγή Dark / Light Theme (με άμεση ενημέρωση Compose State)
 * 2. Εναλλαγή Γλώσσας (Ελληνικά / English μέσω AppCompatDelegate)
 */
object AppSettings {

    private const val PREFS_NAME = "cityflow_app_settings"
    private const val KEY_DARK_MODE = "dark_mode_setting" // -1: System, 0: Light, 1: Dark
    private const val KEY_LANGUAGE = "selected_language"

    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    private val _currentLanguage = MutableStateFlow("el")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val darkVal = prefs.getInt(KEY_DARK_MODE, -1)
        _isDarkMode.value = when (darkVal) {
            1 -> true
            0 -> false
            else -> null
        }

        val savedLang = prefs.getString(KEY_LANGUAGE, null)
        val activeLang = if (savedLang != null) {
            savedLang
        } else {
            val appLocales = AppCompatDelegate.getApplicationLocales()
            if (!appLocales.isEmpty) {
                appLocales.get(0)?.language ?: "el"
            } else {
                val sysLang = Locale.getDefault().language
                if (sysLang == "el" || sysLang == "en") sysLang else "el"
            }
        }
        _currentLanguage.value = activeLang
        initialized = true
    }

    /**
     * Εναλλαγή Dark Mode (Light -> Dark -> Light).
     */
    fun toggleDarkMode(context: Context) {
        val current = _isDarkMode.value ?: false
        setDarkMode(context, !current)
    }

    fun setDarkMode(context: Context, isDark: Boolean?) {
        _isDarkMode.value = isDark
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = when (isDark) {
            true -> 1
            false -> 0
            null -> -1
        }
        prefs.edit().putInt(KEY_DARK_MODE, value).apply()
    }

    /**
     * Αλλαγή γλώσσας εφαρμογής (π.χ. "el" ή "en").
     */
    fun setLanguage(languageCode: String, context: Context) {
        _currentLanguage.value = languageCode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
