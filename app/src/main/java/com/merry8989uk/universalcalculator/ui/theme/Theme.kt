package com.merry8989uk.universalcalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Vivid palette used for the keypad gradients and category accents.
val Violet = Color(0xFF6C4CF1)
val Indigo = Color(0xFF3B82F6)
val Teal = Color(0xFF14B8A6)
val Amber = Color(0xFFF59E0B)
val Coral = Color(0xFFEF4444)
val Pink = Color(0xFFEC4899)
val Emerald = Color(0xFF10B981)
val Cyan = Color(0xFF06B6D4)
val Orange = Color(0xFFF97316)

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DFFF),
    onPrimaryContainer = Color(0xFF1D0A66),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF5EF),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Amber,
    onTertiary = Color(0xFF3A2600),
    tertiaryContainer = Color(0xFFFFE7B8),
    onTertiaryContainer = Color(0xFF3A2600),
    error = Coral,
    background = Color(0xFFF6F5FC),
    onBackground = Color(0xFF1A1B22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B22),
    surfaceVariant = Color(0xFFEDEBF8),
    onSurfaceVariant = Color(0xFF4A4B57),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9A7FF),
    onPrimary = Color(0xFF24126E),
    primaryContainer = Color(0xFF3A2A8F),
    onPrimaryContainer = Color(0xFFE6DFFF),
    secondary = Color(0xFF5EEAD4),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0F5F55),
    onSecondaryContainer = Color(0xFFCFF5EF),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF3A2600),
    tertiaryContainer = Color(0xFF6B4E12),
    onTertiaryContainer = Color(0xFFFFE7B8),
    error = Color(0xFFFF8A80),
    background = Color(0xFF0D0E15),
    onBackground = Color(0xFFE7E8F0),
    surface = Color(0xFF161824),
    onSurface = Color(0xFFE7E8F0),
    surfaceVariant = Color(0xFF232636),
    onSurfaceVariant = Color(0xFFB6B8C8),
)

/** One accent colour per calculator category, so the hub reads at a glance. */
object CategoryColors {
    fun of(category: String): Color = when (category) {
        "Everyday" -> Cyan
        "Finance" -> Emerald
        "Economics" -> Violet
        "Health" -> Coral
        "Geometry" -> Amber
        "Physics" -> Indigo
        "Engineering" -> Teal
        "Construction" -> Orange
        else -> Pink
    }
}

/**
 * Theme wrapper driven by the stored theme mode and accent colour, so the
 * Settings screen actually changes the appearance.
 */
@Composable
fun UniversalCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = if (accent != null) scheme.copy(primary = accent) else scheme,
        content = content,
    )
}
