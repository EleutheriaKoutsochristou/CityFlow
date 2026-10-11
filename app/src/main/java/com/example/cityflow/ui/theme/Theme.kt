package com.example.cityflow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CityBlue80,
    secondary = CityAmber80,
    tertiary = CityBlueGrey80,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = CityBlue40,
    secondary = CityAmber40,
    tertiary = CityBlueGrey40,
    background = LightBackground,
    surface = LightSurface
)

/**
 * Application Theme wrapper for CityFlow.
 * Applies Material 3 Color Schemes (Dark or Light) and Typography.
 * Also configures the Android System Window Status Bar styling and contrast.
 *
 * @param darkTheme Whether to render in Dark Theme mode (defaults to system setting).
 * @param content Child Compose tree.
 */
@Composable
fun CityFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    // Synchronize window status bar color with theme primary color
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
