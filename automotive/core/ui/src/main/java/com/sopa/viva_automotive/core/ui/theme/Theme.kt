package com.sopa.viva_automotive.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val VivaDarkColorScheme = darkColorScheme(
    primary = DarkCyan,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceHigh,
    onPrimaryContainer = DarkCyan,
    secondary = DarkAmber,
    onSecondary = DarkBackground,
    tertiary = DarkGreen,
    onTertiary = DarkBackground,
    error = DarkRed,
    onError = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkOnSurfaceMuted,
    outline = DarkOutline,
)

private val VivaLightColorScheme = lightColorScheme(
    primary = LightCyan,
    onPrimary = LightSurface,
    primaryContainer = LightCyanContainer,
    onPrimaryContainer = LightOnCyanContainer,
    secondary = LightAmber,
    onSecondary = LightSurface,
    tertiary = LightGreen,
    onTertiary = LightSurface,
    error = LightRed,
    onError = LightSurface,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightOnSurfaceMuted,
    outline = LightOutline,
)

object VivaDimens {
        val TouchTargetMin: Dp = 44.dp

        val TouchTarget: Dp = 56.dp

        val ButtonHeight: Dp = 56.dp

    val SpacingXs: Dp = 4.dp
    val SpacingS: Dp = 8.dp
    val SpacingM: Dp = 16.dp
    val SpacingL: Dp = 24.dp
    val SpacingXl: Dp = 32.dp

        val ScreenPadding: Dp = 24.dp
}

@Composable
fun VivaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) VivaDarkColorScheme else VivaLightColorScheme,
        typography = VivaTypography,
        content = content,
    )
}
