package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HighDensityPrimaryDark,
    secondary = HighDensitySecondaryDark,
    tertiary = HighDensityTertiaryDark,
    background = HighDensityBackgroundDark,
    surface = HighDensitySurfaceDark,
    surfaceVariant = HighDensityPillBgDark,
    onPrimary = Color(0xFF21005D),
    onSecondary = Color(0xFF21005D),
    onTertiary = Color(0xFF21005D),
    onBackground = HighDensityTextLight,
    onSurface = HighDensityTextLight,
    onSurfaceVariant = HighDensitySecondaryDark,
    outline = HighDensityBorderDark,
    outlineVariant = HighDensityBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = HighDensityPrimary,
    secondary = HighDensitySecondary,
    tertiary = HighDensityTertiary,
    background = HighDensityBackground,
    surface = HighDensitySurface,
    surfaceVariant = HighDensityCardBg,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = HighDensityTextDark,
    onSurface = HighDensityTextDark,
    onSurfaceVariant = HighDensitySecondary,
    outline = HighDensityBorder,
    outlineVariant = HighDensityBorderLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We bypass global OS dynamic coloring to enforce our curated brand-focused Emerald palette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
