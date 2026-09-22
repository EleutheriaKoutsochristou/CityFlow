package com.example.cityflow

import android.content.res.Configuration
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)

        setContent {
            val isDarkMode by AppSettings.isDarkMode.collectAsState()
            val currentLang by AppSettings.currentLanguage.collectAsState()
            val useDarkTheme = isDarkMode ?: isSystemInDarkTheme()

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

            CompositionLocalProvider(
                LocalConfiguration provides updatedConfig,
                LocalContext provides updatedContext
            ) {
                CityFlowTheme(darkTheme = useDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavHost()
                    }
                }
            }
        }
    }
}
