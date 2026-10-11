package com.example.cityflow

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.cityflow.ui.navigation.AppNavHost
import com.example.cityflow.ui.theme.CityFlowTheme
import com.example.cityflow.util.AppSettings
import java.util.Locale

/**
 * Main entry point Activity for the CityFlow application.
 *
 * Extends [AppCompatActivity] to provide backwards-compatible Activity capabilities.
 * Manages runtime theme switching (Light/Dark mode) and runtime locale/language changes
 * by providing updated Configuration and Context instances to the Jetpack Compose tree.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize persistent application settings (preferences, saved language, and theme)
        AppSettings.init(this)

        setContent {
            // Observe reactive theme and language states
            val isDarkMode by AppSettings.isDarkMode.collectAsState()
            val currentLang by AppSettings.currentLanguage.collectAsState()
            // Fall back to system dark theme if no user preference is stored
            val useDarkTheme = isDarkMode ?: isSystemInDarkTheme()

            // Update app-wide locale dynamically without requiring an Activity restart
            val locale = remember(currentLang) { Locale(currentLang) }
            val configuration = LocalConfiguration.current
            val updatedConfig = remember(currentLang, configuration) {
                Configuration(configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            }
            val context = LocalContext.current
            val updatedContext = remember(currentLang, context) {
                context.createConfigurationContext(updatedConfig)
            }

            // Supply overridden configuration and context down the Compose hierarchy
            CompositionLocalProvider(
                LocalConfiguration provides updatedConfig,
                LocalContext provides updatedContext,
                LocalActivityResultRegistryOwner provides this
            ) {
                CityFlowTheme(darkTheme = useDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Root navigation host for the application
                        AppNavHost()
                    }
                }
            }
        }
    }
}
