package com.example.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NewsHubBlue,
    onPrimary = Color.White,
    secondary = NewsHubNavy,
    onSecondary = Color.White,
    background = NewsHubBackgroundLight,
    onBackground = NewsHubTextLight,
    surface = NewsHubSurfaceLight,
    onSurface = NewsHubTextLight,
    surfaceVariant = NewsHubSkeletonLight,
    onSurfaceVariant = NewsHubMutedLight,
    outline = NewsHubBorderLight,
    outlineVariant = NewsHubCardBorderLight,
    tertiary = NewsHubSuccess,
    tertiaryContainer = NewsHubSuccessLight,
    error = NewsHubError,
    errorContainer = NewsHubErrorLight
)

private val DarkColorScheme = darkColorScheme(
    primary = NewsHubBlueLight,
    onPrimary = NewsHubBackgroundDark,
    secondary = NewsHubNavy,
    onSecondary = Color.White,
    background = NewsHubBackgroundDark,
    onBackground = NewsHubTextDark,
    surface = NewsHubSurfaceDark,
    onSurface = NewsHubTextDark,
    surfaceVariant = NewsHubSkeletonDark,
    onSurfaceVariant = NewsHubMutedDark,
    outline = NewsHubBorderDark,
    outlineVariant = NewsHubCardBorderDark,
    tertiary = Color(0xFF4ADE80),
    tertiaryContainer = NewsHubSuccessDark,
    error = NewsHubErrorDark,
    errorContainer = NewsHubErrorContainerDark
)

@Composable
fun MobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}