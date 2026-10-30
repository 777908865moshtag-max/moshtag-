package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val LuxuryDarkColorScheme = darkColorScheme(
    primary = StudioGold,
    onPrimary = Color(0xFF14110A),
    primaryContainer = Color(0xFF3A2A10),
    onPrimaryContainer = StudioGoldLight,
    secondary = StudioCopper,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3E221B),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = StudioCyan,
    onTertiary = Color(0xFF06202B),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = StudioError,
    onError = Color.White
)

private val StudioLightColorScheme = lightColorScheme(
    primary = StudioGoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8C2),
    onPrimaryContainer = Color(0xFF382200),
    secondary = StudioCopper,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD1),
    onSecondaryContainer = Color(0xFF3A0B01),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = StudioError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) LuxuryDarkColorScheme else StudioLightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
