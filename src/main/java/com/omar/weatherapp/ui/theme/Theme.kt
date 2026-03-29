package com.omar.weatherapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val WeatherBackground  = Color(0xFFF0EDE8)
val WeatherSurface     = Color(0xFFE8E5E0)
val WeatherOnSurface   = Color(0xFF1A1A1A)
val WeatherAccentBlue  = Color(0xFF2196F3)
val WeatherAccentRed   = Color(0xFFE8372A)
val WeatherText        = Color(0xFF0F0F0F)
val WeatherSubText     = Color(0xFF6B6B6B)
val WeatherDivider     = Color(0xFFD0CCC8)
val WeatherCardBg      = Color(0xFFEAE7E2)
val WeatherHourBg      = Color(0xFFE0DDD8)

private val LightColorScheme = lightColorScheme(
    primary         = WeatherAccentBlue,
    onPrimary       = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    background      = WeatherBackground,
    onBackground    = WeatherText,
    surface         = WeatherSurface,
    onSurface       = WeatherText,
    surfaceVariant  = WeatherCardBg,
    onSurfaceVariant = WeatherSubText,
    error           = WeatherAccentRed,
    outline         = WeatherDivider
)

@Composable
fun WeatherAppTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = WeatherBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
