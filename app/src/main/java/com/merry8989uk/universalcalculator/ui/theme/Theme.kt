package com.merry8989uk.universalcalculator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
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

/** The four dynamic themes from the design blueprint. */
enum class AppTheme(val label: String, val blurb: String) {
    CYBERPUNK("Cyberpunk Neon", "Cyan + magenta on a deep midnight base"),
    EMERALD("Emerald Mint", "Mint greens with a soft aqua glow"),
    SUNSET("Sunset Crimson", "Crimson, orange and warm gold"),
    MIDNIGHT("Midnight Gold", "Gold accents on a near-black canvas"),
    DYNAMIC("Material You", "Colours taken from your wallpaper (Android 12 and newer)");

    companion object {
        val labels: List<String> = entries.map { it.label }
        fun byLabel(label: String?): AppTheme =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: CYBERPUNK
    }
}

private fun darkScheme(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = Color(0xFF0A0C14),
    primaryContainer = primary.copy(alpha = 0.22f),
    onPrimaryContainer = primary,
    secondary = secondary,
    onSecondary = Color(0xFF0A0C14),
    secondaryContainer = secondary.copy(alpha = 0.22f),
    onSecondaryContainer = secondary,
    tertiary = tertiary,
    onTertiary = Color(0xFF0A0C14),
    tertiaryContainer = tertiary.copy(alpha = 0.22f),
    onTertiaryContainer = tertiary,
    error = Coral,
    background = background,
    onBackground = Color(0xFFE7E9F2),
    surface = surface,
    onSurface = Color(0xFFE7E9F2),
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = Color(0xFFB6BACA),
)

private fun lightScheme(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.16f),
    onPrimaryContainer = Color(0xFF14161F),
    secondary = secondary,
    onSecondary = Color.White,
    secondaryContainer = secondary.copy(alpha = 0.16f),
    onSecondaryContainer = Color(0xFF14161F),
    tertiary = tertiary,
    onTertiary = Color(0xFF14161F),
    tertiaryContainer = tertiary.copy(alpha = 0.20f),
    onTertiaryContainer = Color(0xFF14161F),
    error = Coral,
    background = background,
    onBackground = Color(0xFF14161F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14161F),
    surfaceVariant = Color(0xFFEDEBF8),
    onSurfaceVariant = Color(0xFF4A4B57),
)

/** Returns the colour scheme for a theme in dark or light mode. */
fun schemeFor(theme: AppTheme, dark: Boolean): ColorScheme = when (theme) {
    AppTheme.CYBERPUNK -> if (dark) darkScheme(
        primary = Color(0xFF00E5FF), secondary = Color(0xFFFF2E97), tertiary = Color(0xFFB14EFF),
        background = Color(0xFF0A0C14), surface = Color(0xFF12141F), surfaceVariant = Color(0xFF1D2030),
    ) else lightScheme(
        primary = Color(0xFF0088A3), secondary = Color(0xFFC2007A), tertiary = Color(0xFF7B2CBF),
        background = Color(0xFFF3F7FA),
    )
    AppTheme.EMERALD -> if (dark) darkScheme(
        primary = Color(0xFF34D399), secondary = Color(0xFF2DD4BF), tertiary = Color(0xFFA7F3D0),
        background = Color(0xFF06110D), surface = Color(0xFF0C1A15), surfaceVariant = Color(0xFF15271F),
    ) else lightScheme(
        primary = Color(0xFF0F8A5F), secondary = Color(0xFF0D9488), tertiary = Color(0xFF7CB342),
        background = Color(0xFFF1FAF6),
    )
    AppTheme.SUNSET -> if (dark) darkScheme(
        primary = Color(0xFFFF4D6D), secondary = Color(0xFFFF8A3D), tertiary = Color(0xFFFFD166),
        background = Color(0xFF140A0C), surface = Color(0xFF1F1014), surfaceVariant = Color(0xFF2C1820),
    ) else lightScheme(
        primary = Color(0xFFD63355), secondary = Color(0xFFE06A15), tertiary = Color(0xFFB8860B),
        background = Color(0xFFFDF4F2),
    )
    AppTheme.MIDNIGHT -> if (dark) darkScheme(
        primary = Color(0xFFF5C542), secondary = Color(0xFFC9A227), tertiary = Color(0xFFE8D9A0),
        background = Color(0xFF0A0A0C), surface = Color(0xFF15140F), surfaceVariant = Color(0xFF23211A),
    ) else lightScheme(
        primary = Color(0xFFB8860B), secondary = Color(0xFF8A6D1F), tertiary = Color(0xFF6B5A12),
        background = Color(0xFFFAF8F2),
    )
}

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
 * Theme wrapper driven by the stored colour theme and light/dark mode, so the
 * Settings screen changes the whole appearance immediately.
 */
@Composable
fun UniversalCalculatorTheme(
    theme: AppTheme = AppTheme.CYBERPUNK,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (theme == AppTheme.DYNAMIC && dynamicColoursAvailable()) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Material You needs Android 12; older devices keep the Cyberpunk look.
        schemeFor(if (theme == AppTheme.DYNAMIC) AppTheme.CYBERPUNK else theme, darkTheme)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Wallpaper colours exist from Android 12 (API 31). */
fun dynamicColoursAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
