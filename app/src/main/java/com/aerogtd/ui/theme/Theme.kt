package com.aerogtd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary          = DarkPrimary,
    onPrimary        = DarkOnPrimary,
    primaryContainer = DarkPrimary.copy(alpha = 0.15f),
    background       = DarkBackground,
    surface          = DarkSurface,
    surfaceVariant   = DarkSurface,
    onBackground     = DarkTextPrimary,
    onSurface        = DarkTextPrimary,
    onSurfaceVariant = DarkTextPrimary.copy(alpha = 0.7f),
    outline          = DarkOutline,
    error            = DarkError
)

private val LightColorScheme = lightColorScheme(
    primary          = LightPrimary,
    onPrimary        = LightOnPrimary,
    primaryContainer = LightPrimary.copy(alpha = 0.1f),
    background       = LightBackground,
    surface          = LightSurface,
    surfaceVariant   = LightSurface,
    onBackground     = LightTextPrimary,
    onSurface        = LightTextPrimary,
    onSurfaceVariant = LightTextPrimary.copy(alpha = 0.7f),
    outline          = LightOutline,
    error            = LightError
)

@Composable
fun ToDoPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
