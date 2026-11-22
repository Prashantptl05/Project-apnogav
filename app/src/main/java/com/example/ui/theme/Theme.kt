package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DesiLuxuryColorScheme = darkColorScheme(
    primary = MarigoldSaffron,
    onPrimary = Color(0xFF1A1200),
    primaryContainer = Color(0xFF452800),
    onPrimaryContainer = WarmAmberGlow,
    secondary = ChanaEmerald,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF0A3A1E),
    onSecondaryContainer = ChanaEmeraldLight,
    tertiary = SkyAzureAccent,
    onTertiary = Color(0xFF002133),
    error = LaserCrimson,
    onError = Color(0xFFFFFFFF),
    background = ObsidianSurface,
    onBackground = WarmIvoryText,
    surface = ObsidianCard,
    onSurface = WarmIvoryText,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = MutedSandText,
    outline = SubtleGoldBorder,
)

@Composable
fun JoharBarhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DesiLuxuryColorScheme,
        typography = Typography,
        content = content,
    )
}
