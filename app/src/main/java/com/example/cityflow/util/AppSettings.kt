package com.example.cityflow.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Application-wide settings and preferences manager:
 * 1. Dark / Light theme toggle (reactive Compose [StateFlow] backed by SharedPreferences).
 * 2. In-app language switching (Greek "el" / English "en" using [AppCompatDelegate] and runtime locale overrides).
 */
object AppSettings {

    private const val PREFS_NAME = "cityflow_app_settings"
    private const val KEY_DARK_MODE = "dark_mode_setting" // -1: Follow system, 0: Force Light, 1: Force Dark
    private const val KEY_LANGUAGE = "selected_language"

    /** State flow representing dark mode: true = Dark, false = Light, null = Follow System. */
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    /** State flow representing active language code ("el" or "en"). */
    private val _currentLanguage = MutableStateFlow("el")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private var initialized = false

    /**
     * Initializes app settings from persistent [SharedPreferences].
     * Configures default language and theme upon application startup.
     *
     * @param context Application or Activity context.
     */
    fun init(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Read stored theme preference
        val darkVal = prefs.getInt(KEY_DARK_MODE, -1)
        _isDarkMode.value = when (darkVal) {
            1 -> true
            0 -> false
            else -> null
        }

        // Read stored locale preference, falling back to system or default Greek ("el")
        val savedLang = prefs.getString(KEY_LANGUAGE, null)
        val activeLang = if (savedLang != null) {
            val localeList = LocaleListCompat.forLanguageTags(savedLang)
            AppCompatDelegate.setApplicationLocales(localeList)
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
        Locale.setDefault(Locale(activeLang))
        _currentLanguage.value = activeLang
        initialized = true
    }

    /**
     * Toggles between Light and Dark mode.
     */
    fun toggleDarkMode(context: Context) {
        val current = _isDarkMode.value ?: false
        setDarkMode(context, !current)
    }

    /**
     * Persists and updates the dark mode state.
     *
     * @param context Application or Activity context.
     * @param isDark True for Dark, false for Light, null for system default.
     */
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
     * Persists and applies a new application language (e.g. "el" or "en").
     * Updates default Java Locale and triggers [AppCompatDelegate] locale update.
     *
     * @param languageCode ISO language tag ("el" or "en").
     * @param context Application or Activity context.
     */
    fun setLanguage(languageCode: String, context: Context) {
        _currentLanguage.value = languageCode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
