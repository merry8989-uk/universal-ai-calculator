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
        val DIAGNOSTICS = booleanPreferencesKey("diagnostics_enabled")
        val LIVE_PREVIEW = booleanPreferencesKey("live_preview")
        val DEGREES = booleanPreferencesKey("degrees_mode")
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val AUTO_CLOSE = booleanPreferencesKey("auto_close_brackets")
        val IGNORE_TRAILING = booleanPreferencesKey("ignore_trailing_operator")
        val NUMBER_FORMAT = stringPreferencesKey("number_format")
        val DECIMALS = intPreferencesKey("decimals")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
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
            diagnosticsEnabled = p[Keys.DIAGNOSTICS] ?: true,
            livePreview = p[Keys.LIVE_PREVIEW] ?: true,
            degreesMode = p[Keys.DEGREES] ?: true,
            angleUnit = p[Keys.ANGLE_UNIT] ?: "DEGREE",
            autoCloseBrackets = p[Keys.AUTO_CLOSE] ?: true,
            ignoreTrailingOperator = p[Keys.IGNORE_TRAILING] ?: true,
            numberFormat = p[Keys.NUMBER_FORMAT] ?: "Indian",
            decimals = p[Keys.DECIMALS] ?: -1,
            unitSystem = p[Keys.UNIT_SYSTEM] ?: "Metric",
            keySound = p[Keys.KEY_SOUND] ?: "Off",
            historyEnabled = p[Keys.HISTORY] ?: true,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            aiEnabled = p[Keys.AI_ENABLED] ?: true,
            fractionsEnabled = p[Keys.FRACTIONS] ?: false,
            layoutStyle = p[Keys.LAYOUT_STYLE] ?: "Modern",
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
        put("unitSystem", p[Keys.UNIT_SYSTEM])
        put("keySound", p[Keys.KEY_SOUND])
        put("history", p[Keys.HISTORY])
        put("haptics", p[Keys.HAPTICS])
        put("aiEnabled", p[Keys.AI_ENABLED])
        put("fractions", p[Keys.FRACTIONS])
        put("layoutStyle", p[Keys.LAYOUT_STYLE])
        put("minimalMode", p[Keys.MINIMAL_MODE])
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
            str("unitSystem", Keys.UNIT_SYSTEM)
            str("keySound", Keys.KEY_SOUND)
            bool("history", Keys.HISTORY)
            bool("haptics", Keys.HAPTICS)
            bool("aiEnabled", Keys.AI_ENABLED)
            bool("fractions", Keys.FRACTIONS)
            str("layoutStyle", Keys.LAYOUT_STYLE)
            bool("minimalMode", Keys.MINIMAL_MODE)
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

    suspend fun setUnitSystem(value: String) =
        context.dataStore.edit { it[Keys.UNIT_SYSTEM] = value }

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
