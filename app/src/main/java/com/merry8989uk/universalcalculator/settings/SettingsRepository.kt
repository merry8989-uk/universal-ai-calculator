package com.merry8989uk.universalcalculator.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persists every setting to DataStore. Reads are a Flow so the UI reacts the
 * moment a value changes (Rule 4: no dead toggles, applies immediately).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = longPreferencesKey("accent_color")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val DENSITY = floatPreferencesKey("density")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val DIAGNOSTICS = booleanPreferencesKey("diagnostics_enabled")
        val LIVE_PREVIEW = booleanPreferencesKey("live_preview")
        val DEGREES = booleanPreferencesKey("degrees_mode")
        val GROUP_DIGITS = booleanPreferencesKey("group_digits")
        val HISTORY = booleanPreferencesKey("history_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
    }

    val settings: Flow<SettingsState> = context.dataStore.data.map { p ->
        SettingsState(
            themeMode = p[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            accentColor = p[Keys.ACCENT_COLOR] ?: 0xFF6C4CF1,
            textScale = p[Keys.TEXT_SCALE] ?: 1.0f,
            density = p[Keys.DENSITY] ?: 1.0f,
            language = p[Keys.LANGUAGE] ?: "system",
            notificationsEnabled = p[Keys.NOTIFICATIONS] ?: true,
            diagnosticsEnabled = p[Keys.DIAGNOSTICS] ?: true,
            livePreview = p[Keys.LIVE_PREVIEW] ?: true,
            degreesMode = p[Keys.DEGREES] ?: true,
            groupDigits = p[Keys.GROUP_DIGITS] ?: true,
            historyEnabled = p[Keys.HISTORY] ?: true,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            aiEnabled = p[Keys.AI_ENABLED] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setAccentColor(color: Long) =
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = color }

    suspend fun setTextScale(scale: Float) =
        context.dataStore.edit { it[Keys.TEXT_SCALE] = scale }

    suspend fun setDensity(value: Float) =
        context.dataStore.edit { it[Keys.DENSITY] = value }

    suspend fun setLanguage(language: String) =
        context.dataStore.edit { it[Keys.LANGUAGE] = language }

    suspend fun setNotifications(enabled: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }

    suspend fun setDiagnosticsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DIAGNOSTICS] = enabled }

    suspend fun setLivePreview(enabled: Boolean) =
        context.dataStore.edit { it[Keys.LIVE_PREVIEW] = enabled }

    suspend fun setDegreesMode(enabled: Boolean) =
        context.dataStore.edit { it[Keys.DEGREES] = enabled }

    suspend fun setGroupDigits(enabled: Boolean) =
        context.dataStore.edit { it[Keys.GROUP_DIGITS] = enabled }

    suspend fun setHistoryEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.HISTORY] = enabled }

    suspend fun setHapticsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }

    suspend fun setAiEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.AI_ENABLED] = enabled }

    suspend fun reset() = context.dataStore.edit { it.clear() }
}
