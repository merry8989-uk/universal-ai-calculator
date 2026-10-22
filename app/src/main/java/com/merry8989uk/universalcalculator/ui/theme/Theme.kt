package com.merry8989uk.universalcalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme()
private val LightColors = lightColorScheme()

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
