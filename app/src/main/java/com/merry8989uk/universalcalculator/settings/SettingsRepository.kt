package com.merry8989uk.universalcalculator.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.merry8989uk.universalcalculator.locale.LocaleHelper
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persists every setting to DataStore. Reads are a Flow so the UI reacts the
 * moment a value changes (Rule 4: no dead toggles, applies immediately).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_NAME = stringPreferencesKey("theme_name")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val DENSITY = floatPreferencesKey("density")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val DATA_SAVING = booleanPreferencesKey("data_saving")
        val SHOW_GT = booleanPreferencesKey("show_gt")
        val CUSTOM_THEME = stringPreferencesKey("custom_theme")
        val KEY_VOLUME = floatPreferencesKey("key_volume")
        val CUSTOM_TONE = stringPreferencesKey("custom_tone")
        val DIAGNOSTICS = booleanPreferencesKey("diagnostics_enabled")
        val LIVE_PREVIEW = booleanPreferencesKey("live_preview")
        val DEGREES = booleanPreferencesKey("degrees_mode")
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val BIT_WIDTH = stringPreferencesKey("bit_width")
        val COMPLEX_FORMAT = stringPreferencesKey("complex_format")
        val INTERVAL_NOTATION = stringPreferencesKey("interval_notation")
        val FREQUENCY_COLUMN = booleanPreferencesKey("frequency_column")
        val CAS_MODE = booleanPreferencesKey("cas_mode")
        val DIVISION_SIGN = stringPreferencesKey("division_sign")
        val MULTIPLICATION_SIGN = stringPreferencesKey("multiplication_sign")
        val BLINK_CURSOR = booleanPreferencesKey("blink_cursor")
        val CLEAR_ON_EXIT = booleanPreferencesKey("clear_on_exit")
        val SHOW_CALC_INFO = booleanPreferencesKey("show_calc_info")
        val IMPLIED_MULTIPLICATION = stringPreferencesKey("implied_multiplication")
        val PERCENTAGE_MODE = stringPreferencesKey("percentage_mode")
        val DECIMAL_FORMAT = stringPreferencesKey("decimal_format")
        val DECIMAL_DIGITS = intPreferencesKey("decimal_digits")
        val AUTO_CLOSE = booleanPreferencesKey("auto_close_brackets")
        val IGNORE_TRAILING = booleanPreferencesKey("ignore_trailing_operator")
        val NUMBER_FORMAT = stringPreferencesKey("number_format")
        val DECIMALS = intPreferencesKey("decimals")
        val KEY_SOUND = stringPreferencesKey("key_sound")
        val HISTORY = booleanPreferencesKey("history_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val FRACTIONS = booleanPreferencesKey("fractions_enabled")
        val LAYOUT_STYLE = stringPreferencesKey("layout_style")
        val MINIMAL_MODE = booleanPreferencesKey("minimal_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SAVE_STATE = booleanPreferencesKey("save_state")
        val SAVED_EXPRESSION = stringPreferencesKey("saved_expression")
    }

    val settings: Flow<SettingsState> = context.dataStore.data.map { p ->
        SettingsState(
            themeMode = p[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            themeName = p[Keys.THEME_NAME] ?: "Cyberpunk Neon",
            textScale = p[Keys.TEXT_SCALE] ?: 1.0f,
            density = p[Keys.DENSITY] ?: 1.0f,
            language = p[Keys.LANGUAGE] ?: "system",
            notificationsEnabled = p[Keys.NOTIFICATIONS] ?: true,
            dataSavingEnabled = p[Keys.DATA_SAVING] ?: true,
            showGtKey = p[Keys.SHOW_GT] ?: true,
            customThemeId = p[Keys.CUSTOM_THEME] ?: "",
            keySoundVolume = p[Keys.KEY_VOLUME] ?: 0.7f,
            customTonePath = p[Keys.CUSTOM_TONE] ?: "",
            diagnosticsEnabled = p[Keys.DIAGNOSTICS] ?: true,
            livePreview = p[Keys.LIVE_PREVIEW] ?: true,
            degreesMode = p[Keys.DEGREES] ?: true,
            angleUnit = p[Keys.ANGLE_UNIT] ?: "DEGREE",
            bitWidth = p[Keys.BIT_WIDTH] ?: "BITS_64",
            complexFormat = p[Keys.COMPLEX_FORMAT] ?: "RECTANGULAR",
            intervalNotation = p[Keys.INTERVAL_NOTATION] ?: "PARENTHESES",
            frequencyColumn = p[Keys.FREQUENCY_COLUMN] ?: false,
            casMode = p[Keys.CAS_MODE] ?: true,
            divisionSign = p[Keys.DIVISION_SIGN] ?: "\u00F7",
            multiplicationSign = p[Keys.MULTIPLICATION_SIGN] ?: "\u00D7",
            blinkCursor = p[Keys.BLINK_CURSOR] ?: true,
            clearOnExit = p[Keys.CLEAR_ON_EXIT] ?: false,
            showCalcInfo = p[Keys.SHOW_CALC_INFO] ?: true,
            impliedMultiplication = p[Keys.IMPLIED_MULTIPLICATION] ?: "SAME_PRECEDENCE",
            percentageMode = p[Keys.PERCENTAGE_MODE] ?: "MULTIPLICATIVE",
            decimalFormat = p[Keys.DECIMAL_FORMAT] ?: "NORMAL",
            decimalDigits = p[Keys.DECIMAL_DIGITS] ?: 10,
            autoCloseBrackets = p[Keys.AUTO_CLOSE] ?: true,
            ignoreTrailingOperator = p[Keys.IGNORE_TRAILING] ?: true,
            numberFormat = p[Keys.NUMBER_FORMAT] ?: "Indian",
            decimals = p[Keys.DECIMALS] ?: -1,
            keySound = p[Keys.KEY_SOUND] ?: "Off",
            historyEnabled = p[Keys.HISTORY] ?: true,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            aiEnabled = p[Keys.AI_ENABLED] ?: true,
            fractionsEnabled = p[Keys.FRACTIONS] ?: false,
            // a device that had the classic pad chosen falls back to the modern
            // one while that feature is switched off, rather than showing a pad
            // the app no longer offers
            layoutStyle = (p[Keys.LAYOUT_STYLE] ?: "Modern").let {
                if (it == "Classic Scientific" && !com.merry8989uk.universalcalculator.ui.Features.SCI_CLASSIC) "Modern" else it
            },
            minimalMode = p[Keys.MINIMAL_MODE] ?: false,
            keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: false,
            saveState = p[Keys.SAVE_STATE] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setThemeName(name: String) =
        context.dataStore.edit { it[Keys.THEME_NAME] = name }

    suspend fun setTextScale(scale: Float) =
        context.dataStore.edit { it[Keys.TEXT_SCALE] = scale }

    suspend fun setDensity(value: Float) =
        context.dataStore.edit { it[Keys.DENSITY] = value }

    /**
     * The settings worth carrying to another device, as plain strings.
     *
     * Diagnostics, notifications and the saved expression are deliberately left
     * out: they are about this device, not the user's choices.
     */
    suspend fun exportSyncable(): Map<String, String> {
        val p = context.dataStore.data.first()
        val out = LinkedHashMap<String, String>()
        fun put(name: String, value: Any?) { if (value != null) out[name] = value.toString() }
        put("themeMode", p[Keys.THEME_MODE])
        put("themeName", p[Keys.THEME_NAME])
        put("textScale", p[Keys.TEXT_SCALE])
        put("density", p[Keys.DENSITY])
        put("language", p[Keys.LANGUAGE])
        put("livePreview", p[Keys.LIVE_PREVIEW])
        put("degrees", p[Keys.DEGREES])
        put("numberFormat", p[Keys.NUMBER_FORMAT])
        put("decimals", p[Keys.DECIMALS])
        put("keySound", p[Keys.KEY_SOUND])
        put("history", p[Keys.HISTORY])
        put("haptics", p[Keys.HAPTICS])
        put("aiEnabled", p[Keys.AI_ENABLED])
        put("fractions", p[Keys.FRACTIONS])
        put("layoutStyle", p[Keys.LAYOUT_STYLE])
        put("minimalMode", p[Keys.MINIMAL_MODE])
        put("angleUnit", p[Keys.ANGLE_UNIT])
        put("autoCloseBrackets", p[Keys.AUTO_CLOSE])
        put("ignoreTrailingOperator", p[Keys.IGNORE_TRAILING])
        put("decimalFormat", p[Keys.DECIMAL_FORMAT])
        put("decimalDigits", p[Keys.DECIMAL_DIGITS])
        put("percentageMode", p[Keys.PERCENTAGE_MODE])
        put("impliedMultiplication", p[Keys.IMPLIED_MULTIPLICATION])
        put("bitWidth", p[Keys.BIT_WIDTH])
        put("complexFormat", p[Keys.COMPLEX_FORMAT])
        put("intervalNotation", p[Keys.INTERVAL_NOTATION])
        put("frequencyColumn", p[Keys.FREQUENCY_COLUMN])
        put("casMode", p[Keys.CAS_MODE])
        put("divisionSign", p[Keys.DIVISION_SIGN])
        put("multiplicationSign", p[Keys.MULTIPLICATION_SIGN])
        put("blinkCursor", p[Keys.BLINK_CURSOR])
        put("clearOnExit", p[Keys.CLEAR_ON_EXIT])
        put("showCalcInfo", p[Keys.SHOW_CALC_INFO])
        put("keepScreenOn", p[Keys.KEEP_SCREEN_ON])
        put("saveState", p[Keys.SAVE_STATE])
        return out
    }

    /** Applies a set of settings from a sync payload. Unknown keys are ignored. */
    suspend fun importSyncable(values: Map<String, String>) {
        context.dataStore.edit { p ->
            fun str(name: String, key: androidx.datastore.preferences.core.Preferences.Key<String>) {
                values[name]?.let { p[key] = it }
            }
            fun bool(name: String, key: androidx.datastore.preferences.core.Preferences.Key<Boolean>) {
                values[name]?.toBooleanStrictOrNull()?.let { p[key] = it }
            }
            fun num(name: String, key: androidx.datastore.preferences.core.Preferences.Key<Int>) {
                values[name]?.toIntOrNull()?.let { p[key] = it }
            }
            fun flt(name: String, key: androidx.datastore.preferences.core.Preferences.Key<Float>) {
                values[name]?.toFloatOrNull()?.let { p[key] = it }
            }
            str("themeMode", Keys.THEME_MODE)
            str("themeName", Keys.THEME_NAME)
            flt("textScale", Keys.TEXT_SCALE)
            flt("density", Keys.DENSITY)
            str("language", Keys.LANGUAGE)
            bool("livePreview", Keys.LIVE_PREVIEW)
            bool("degrees", Keys.DEGREES)
            str("numberFormat", Keys.NUMBER_FORMAT)
            num("decimals", Keys.DECIMALS)
            str("keySound", Keys.KEY_SOUND)
            bool("history", Keys.HISTORY)
            bool("haptics", Keys.HAPTICS)
            bool("aiEnabled", Keys.AI_ENABLED)
            bool("fractions", Keys.FRACTIONS)
            str("layoutStyle", Keys.LAYOUT_STYLE)
            bool("minimalMode", Keys.MINIMAL_MODE)
            str("angleUnit", Keys.ANGLE_UNIT)
            bool("autoCloseBrackets", Keys.AUTO_CLOSE)
            bool("ignoreTrailingOperator", Keys.IGNORE_TRAILING)
            str("decimalFormat", Keys.DECIMAL_FORMAT)
            num("decimalDigits", Keys.DECIMAL_DIGITS)
            str("percentageMode", Keys.PERCENTAGE_MODE)
            str("impliedMultiplication", Keys.IMPLIED_MULTIPLICATION)
            str("bitWidth", Keys.BIT_WIDTH)
            str("complexFormat", Keys.COMPLEX_FORMAT)
            str("intervalNotation", Keys.INTERVAL_NOTATION)
            bool("frequencyColumn", Keys.FREQUENCY_COLUMN)
            bool("casMode", Keys.CAS_MODE)
            str("divisionSign", Keys.DIVISION_SIGN)
            str("multiplicationSign", Keys.MULTIPLICATION_SIGN)
            bool("blinkCursor", Keys.BLINK_CURSOR)
            bool("clearOnExit", Keys.CLEAR_ON_EXIT)
            bool("showCalcInfo", Keys.SHOW_CALC_INFO)
            bool("keepScreenOn", Keys.KEEP_SCREEN_ON)
            bool("saveState", Keys.SAVE_STATE)
        }
        // the language is mirrored for the activity, so keep that in step
        values["language"]?.let { LocaleHelper.store(context, it) }
    }

    suspend fun setLanguage(language: String) {
        // Kept in SharedPreferences too: the activity reads it synchronously.
        LocaleHelper.store(context, language)
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setNotifications(enabled: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }

    /**
     * The master switch as it stands right now.
     *
     * Blocking on purpose: the callers are already on a background thread and
     * need the answer before they decide whether to write a file. Waiting for a
     * flow to emit would mean the decision came after the write.
     */
    fun currentSavingEnabled(): Boolean = kotlinx.coroutines.runBlocking {
        settings.first().dataSavingEnabled
    }

    /** The theme the user built that should be in use, or blank for a built-in one. */
    suspend fun setCustomThemeId(id: String) =
        context.dataStore.edit { it[Keys.CUSTOM_THEME] = id }

    /** How loud a key is, from 0 to 1. */
    suspend fun setKeySoundVolume(value: Float) =
        context.dataStore.edit { it[Keys.KEY_VOLUME] = value.coerceIn(0f, 1f) }

    /** Where the user's own key tone lives. Blank means they have not chosen one. */
    suspend fun setCustomTonePath(path: String) =
        context.dataStore.edit { it[Keys.CUSTOM_TONE] = path }

    /** Whether the GT key appears on the keypad. The total itself is unaffected. */
    suspend fun setShowGtKey(shown: Boolean) =
        context.dataStore.edit { it[Keys.SHOW_GT] = shown }

    /** The master switch: off means nothing at all is stored. */
    suspend fun setDataSavingEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DATA_SAVING] = enabled }

    suspend fun setDiagnosticsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DIAGNOSTICS] = enabled }

    suspend fun setLivePreview(enabled: Boolean) =
        context.dataStore.edit { it[Keys.LIVE_PREVIEW] = enabled }

    suspend fun setDegreesMode(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DEGREES] = enabled }

    suspend fun setNumberFormat(value: String) =
        context.dataStore.edit { it[Keys.NUMBER_FORMAT] = value }

    suspend fun setDecimals(value: Int) =
        context.dataStore.edit { it[Keys.DECIMALS] = value }

    suspend fun setKeySound(value: String) =
        context.dataStore.edit { it[Keys.KEY_SOUND] = value }

    suspend fun setHistoryEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.HISTORY] = enabled }

    suspend fun setHapticsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }

    suspend fun setAiEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.AI_ENABLED] = enabled }

    suspend fun setFractionsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.FRACTIONS] = enabled }

    suspend fun setLayoutStyle(style: String) =
        context.dataStore.edit { it[Keys.LAYOUT_STYLE] = style }

    suspend fun setBitWidth(width: String) =
        context.dataStore.edit { it[Keys.BIT_WIDTH] = width }

    suspend fun setComplexFormat(format: String) =
        context.dataStore.edit { it[Keys.COMPLEX_FORMAT] = format }

    suspend fun setIntervalNotation(notation: String) =
        context.dataStore.edit { it[Keys.INTERVAL_NOTATION] = notation }

    suspend fun setFrequencyColumn(enabled: Boolean) =
        context.dataStore.edit { it[Keys.FREQUENCY_COLUMN] = enabled }

    suspend fun setCasMode(enabled: Boolean) =
        context.dataStore.edit { it[Keys.CAS_MODE] = enabled }

    suspend fun setDivisionSign(sign: String) =
        context.dataStore.edit { it[Keys.DIVISION_SIGN] = sign }

    suspend fun setMultiplicationSign(sign: String) =
        context.dataStore.edit { it[Keys.MULTIPLICATION_SIGN] = sign }

    suspend fun setBlinkCursor(enabled: Boolean) =
        context.dataStore.edit { it[Keys.BLINK_CURSOR] = enabled }

    suspend fun setClearOnExit(enabled: Boolean) =
        context.dataStore.edit { it[Keys.CLEAR_ON_EXIT] = enabled }

    suspend fun setShowCalcInfo(enabled: Boolean) =
        context.dataStore.edit { it[Keys.SHOW_CALC_INFO] = enabled }







    suspend fun setImpliedMultiplication(mode: String) =
        context.dataStore.edit { it[Keys.IMPLIED_MULTIPLICATION] = mode }

    suspend fun setPercentageMode(mode: String) =
        context.dataStore.edit { it[Keys.PERCENTAGE_MODE] = mode }

    suspend fun setDecimalFormat(format: String) =
        context.dataStore.edit { it[Keys.DECIMAL_FORMAT] = format }

    suspend fun setDecimalDigits(digits: Int) =
        context.dataStore.edit { it[Keys.DECIMAL_DIGITS] = digits.coerceIn(0, 16) }

    suspend fun setAutoCloseBrackets(enabled: Boolean) =
        context.dataStore.edit { it[Keys.AUTO_CLOSE] = enabled }

    suspend fun setIgnoreTrailingOperator(enabled: Boolean) =
        context.dataStore.edit { it[Keys.IGNORE_TRAILING] = enabled }

    suspend fun setAngleUnit(unit: String) =
        context.dataStore.edit { it[Keys.ANGLE_UNIT] = unit }

    suspend fun setMinimalMode(enabled: Boolean) =
        context.dataStore.edit { it[Keys.MINIMAL_MODE] = enabled }

    suspend fun setKeepScreenOn(enabled: Boolean) =
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }

    suspend fun setSaveState(enabled: Boolean) =
        context.dataStore.edit { it[Keys.SAVE_STATE] = enabled }

    /** The expression to restore on the next launch. */
    val savedExpression: Flow<String> = context.dataStore.data.map { it[Keys.SAVED_EXPRESSION] ?: "" }

    suspend fun setSavedExpression(text: String) =
        context.dataStore.edit { it[Keys.SAVED_EXPRESSION] = text }

    suspend fun reset() = context.dataStore.edit { it.clear() }
}
