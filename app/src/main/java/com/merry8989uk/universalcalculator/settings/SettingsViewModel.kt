package com.merry8989uk.universalcalculator.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The settings, as the screens see them.
 *
 * Every setter is a one-line hand-off to the repository, which does the writing.
 * Keeping them flat like this means a screen never has to know how a setting is
 * stored, and the repository is the only place that touches the data store.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val state: StateFlow<SettingsState> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    /** The expression that was on screen when the app was last closed. */
    val savedExpression: Flow<String> = repo.savedExpression

    /** Puts every setting back to its default. */
    fun reset() = viewModelScope.launch { repo.reset() }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setThemeName(name: String) = viewModelScope.launch { repo.setThemeName(name) }
    fun setTextScale(scale: Float) = viewModelScope.launch { repo.setTextScale(scale) }
    fun setDensity(value: Float) = viewModelScope.launch { repo.setDensity(value) }
    fun setLanguage(language: String) = viewModelScope.launch { repo.setLanguage(language) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { repo.setNotifications(enabled) }
    fun setDiagnosticsEnabled(enabled: Boolean) = viewModelScope.launch { repo.setDiagnosticsEnabled(enabled) }
    fun setLivePreview(enabled: Boolean) = viewModelScope.launch { repo.setLivePreview(enabled) }
    fun setDegreesMode(enabled: Boolean) = viewModelScope.launch { repo.setDegreesMode(enabled) }
    fun setNumberFormat(value: String) = viewModelScope.launch { repo.setNumberFormat(value) }
    fun setDecimals(value: Int) = viewModelScope.launch { repo.setDecimals(value) }
    fun setKeySound(value: String) = viewModelScope.launch { repo.setKeySound(value) }
    fun setHistoryEnabled(enabled: Boolean) = viewModelScope.launch { repo.setHistoryEnabled(enabled) }
    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { repo.setHapticsEnabled(enabled) }
    fun setAiEnabled(enabled: Boolean) = viewModelScope.launch { repo.setAiEnabled(enabled) }
    fun setFractionsEnabled(enabled: Boolean) = viewModelScope.launch { repo.setFractionsEnabled(enabled) }
    fun setLayoutStyle(style: String) = viewModelScope.launch { repo.setLayoutStyle(style) }
    fun setBitWidth(width: String) = viewModelScope.launch { repo.setBitWidth(width) }
    fun setComplexFormat(format: String) = viewModelScope.launch { repo.setComplexFormat(format) }
    fun setIntervalNotation(notation: String) = viewModelScope.launch { repo.setIntervalNotation(notation) }
    fun setFrequencyColumn(enabled: Boolean) = viewModelScope.launch { repo.setFrequencyColumn(enabled) }
    fun setCasMode(enabled: Boolean) = viewModelScope.launch { repo.setCasMode(enabled) }
    fun setDivisionSign(sign: String) = viewModelScope.launch { repo.setDivisionSign(sign) }
    fun setMultiplicationSign(sign: String) = viewModelScope.launch { repo.setMultiplicationSign(sign) }
    fun setBlinkCursor(enabled: Boolean) = viewModelScope.launch { repo.setBlinkCursor(enabled) }
    fun setClearOnExit(enabled: Boolean) = viewModelScope.launch { repo.setClearOnExit(enabled) }
    fun setShowCalcInfo(enabled: Boolean) = viewModelScope.launch { repo.setShowCalcInfo(enabled) }
    fun setImpliedMultiplication(mode: String) = viewModelScope.launch { repo.setImpliedMultiplication(mode) }
    fun setPercentageMode(mode: String) = viewModelScope.launch { repo.setPercentageMode(mode) }
    fun setDecimalFormat(format: String) = viewModelScope.launch { repo.setDecimalFormat(format) }
    fun setDecimalDigits(digits: Int) = viewModelScope.launch { repo.setDecimalDigits(digits) }
    fun setAutoCloseBrackets(enabled: Boolean) = viewModelScope.launch { repo.setAutoCloseBrackets(enabled) }
    fun setIgnoreTrailingOperator(enabled: Boolean) = viewModelScope.launch { repo.setIgnoreTrailingOperator(enabled) }
    fun setAngleUnit(unit: String) = viewModelScope.launch { repo.setAngleUnit(unit) }
    fun setMinimalMode(enabled: Boolean) = viewModelScope.launch { repo.setMinimalMode(enabled) }
    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { repo.setKeepScreenOn(enabled) }
    fun setSaveState(enabled: Boolean) = viewModelScope.launch { repo.setSaveState(enabled) }
    fun setSavedExpression(text: String) = viewModelScope.launch { repo.setSavedExpression(text) }
}
