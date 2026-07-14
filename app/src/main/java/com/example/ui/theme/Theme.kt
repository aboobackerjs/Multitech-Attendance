package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LogoTeal,
    secondary = LogoRed,
    background = SlateDarkBackground,
    surface = SlateDarkSurface,
    error = CrimsonError,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = SlateDarkText,
    onSurface = SlateDarkText,
    surfaceVariant = SlateDarkSurfaceCard,
    onSurfaceVariant = SlateDarkText
)

private val LightColorScheme = lightColorScheme(
    primary = LogoTeal,
    secondary = LogoRed,
    background = SlateLightBackground,
    surface = SlateLightSurface,
    error = CrimsonError,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = SlateLightText,
    onSurface = SlateLightText,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = SlateLightText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure our premium customized corporate colors are used
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
