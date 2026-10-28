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
    /**
     * The master switch. Off means nothing is stored anywhere: no archive, no
     * history, no logs. The individual switches below it stop mattering.
     */
    val dataSavingEnabled: Boolean = true,
    /**
     * Whether the GT key is on the keypad.
     *
     * The grand total itself is not affected: the figure still appears in the
     * display and still accumulates on every `=`. Only the key that shows it
     * comes and goes, and with it gone the `=` key takes the freed space.
     */
    val showGtKey: Boolean = true,
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
    /** 32 or 64 bits, for the programmer's keys. */
    val bitWidth: String = "BITS_64",
    /** Rectangular a+bi or polar r-angle. */
    val complexFormat: String = "RECTANGULAR",
    /** How an interval is written: [0, 1) or [0; 1) or [0; 1]. */
    val intervalNotation: String = "PARENTHESES",
    /** Show the frequency (n) column in the statistical editor. */
    val frequencyColumn: Boolean = false,
    /** Computer-algebra mode: when off, the symbolic tools are put away. */
    val casMode: Boolean = true,
    /** What the divide key shows: the division sign, or a plain slash. */
    val divisionSign: String = "÷",
    /** What the multiply key shows: the multiplication sign, or an asterisk. */
    val multiplicationSign: String = "×",
    /** How large the key labels are drawn, as a multiple of the default. */
    /** Show the caret in the expression. */
    val blinkCursor: Boolean = true,
    /** Clear the expression when the user leaves the calculator. */
    val clearOnExit: Boolean = false,
    /** Show a note under the result saying how it was read. */
    val showCalcInfo: Boolean = true,
    /** Same precedence as x, or binding tighter: 1/2pi = (1/2)pi or 1/(2pi). */
    val impliedMultiplication: String = "SAME_PRECEDENCE",
    /** Multiplicative (100+20% = 120) or Additive (100+20% = 100.2). */
    val percentageMode: String = "MULTIPLICATIVE",
    /** Normal, Fixed, Scientific, Engineering or Engineering SI. */
    val decimalFormat: String = "NORMAL",
    /** How many digits after the point, for the formats that take a count. */
    val decimalDigits: Int = 10,
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
