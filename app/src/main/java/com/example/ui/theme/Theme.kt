package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RacingColorScheme = darkColorScheme(
    primary = RacingOrange,
    onPrimary = Color.White,
    primaryContainer = RacingRed,
    onPrimaryContainer = Color.White,
    secondary = NitroCyan,
    onSecondary = Color.Black,
    secondaryContainer = CarbonCard,
    onSecondaryContainer = NitroCyan,
    tertiary = GoldTrophy,
    background = CarbonDark,
    onBackground = TextWhite,
    surface = CarbonSurface,
    onSurface = TextWhite,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = TextMuted,
    outline = CarbonBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RacingColorScheme,
        typography = Typography,
        content = content
    )
}
