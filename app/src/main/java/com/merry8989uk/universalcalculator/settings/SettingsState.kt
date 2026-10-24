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
    /**
     * How trigonometry reads its arguments: "DEGREE", "RADIAN" or "GRADIAN".
     * Gradians are the third unit the classic scientific calculators offer.
     */
    val angleUnit: String = "DEGREE",
    /** Close any bracket the user has not closed before working it out. */
    val autoCloseBrackets: Boolean = true,
    /** Ignore a plus or minus left dangling at the end of the input. */
    val ignoreTrailingOperator: Boolean = true,
    /** Thousands separators in results. */
    /** Digit grouping: "Indian", "International", "European" or "None". */
    val numberFormat: String = "Indian",
    /** Fixed decimal places for results, or -1 for automatic. */
    val decimals: Int = -1,
    /** Key tone: Off, Click, Bubble or Pop. */
    val keySound: String = "Off",
    /** Preferred measurement system for the converters. */
    val unitSystem: String = "Metric",
    /** Keep a history of calculations. */
    val historyEnabled: Boolean = true,
    /** Haptic feedback on key press. */
    val hapticsEnabled: Boolean = true,
    /** The built-in AI assistant. The key is fixed on the server. */
    val aiEnabled: Boolean = true,
    /** Also show results as simple fractions, e.g. 0.75 as 3/4. */
    val fractionsEnabled: Boolean = false,
    /**
     * Keypad style: "Modern" (rounded, gradient), "Classic" (flat, square) or
     * "Classic Scientific" (the full multi-row scientific pad).
     */
    val layoutStyle: String = "Modern",
    /**
     * Minimal mode. Only the three calculators are left: everything else —
     * the assistant, the formula reference, the converters, the tools — is
     * hidden until this is switched off again.
     */
    val minimalMode: Boolean = false,
    /** Keep the screen awake while the calculator is open. */
    val keepScreenOn: Boolean = false,
    /** Remember the expression and bring it back on the next launch. */
    val saveState: Boolean = true,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
