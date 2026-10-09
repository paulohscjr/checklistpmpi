package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PmpiColorScheme = lightColorScheme(
    primary = PmpiBlue,
    onPrimary = Color.White,
    primaryContainer = PmpiBlueContainer,
    onPrimaryContainer = PmpiOnBlueContainer,
    secondary = PmpiBlueLight,
    onSecondary = Color.White,
    secondaryContainer = PmpiBlueContainer,
    onSecondaryContainer = PmpiOnBlueContainer,
    tertiary = PmpiGold,
    onTertiary = Color.White,
    tertiaryContainer = PmpiGoldContainer,
    background = PmpiBackground,
    onBackground = PmpiTextPrimary,
    surface = PmpiSurface,
    onSurface = PmpiTextPrimary,
    surfaceVariant = PmpiSurfaceVariant,
    onSurfaceVariant = PmpiTextSecondary,
    outline = PmpiOutline,
    outlineVariant = Color(0xFFE2E7F0)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PmpiColorScheme,
        typography = Typography,
        content = content
    )
}

