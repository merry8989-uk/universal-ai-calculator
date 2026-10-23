package com.merry8989uk.universalcalculator.settings

/** Everything the Settings screen can control. */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeName: String = "Cyberpunk Neon",
    val textScale: Float = 1.0f,
    val density: Float = 1.0f,
    val language: String = "system",
    val notificationsEnabled: Boolean = true,
    /** Log collection. On by default (Rule 7). */
    val diagnosticsEnabled: Boolean = true,
    /** Show the live result preview while typing. */
    val livePreview: Boolean = true,
    /** Use degrees for trigonometry by default. */
    val degreesMode: Boolean = true,
    /** Thousands separators in results. */
    val groupDigits: Boolean = true,
    /** Keep a history of calculations. */
    val historyEnabled: Boolean = true,
    /** Haptic feedback on key press. */
    val hapticsEnabled: Boolean = true,
    /** The built-in AI assistant. The key is fixed on the server. */
    val aiEnabled: Boolean = true,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
