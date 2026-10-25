package com.merry8989uk.universalcalculator.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val state: StateFlow<SettingsState> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setThemeName(name: String) = viewModelScope.launch { repo.setThemeName(name) }
    fun setTextScale(scale: Float) = viewModelScope.launch { repo.setTextScale(scale) }
    fun setDensity(value: Float) = viewModelScope.launch { repo.setDensity(value) }
    fun setLanguage(language: String) = viewModelScope.launch { repo.setLanguage(language) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { repo.setNotifications(enabled) }
    fun setLivePreview(enabled: Boolean) = viewModelScope.launch { repo.setLivePreview(enabled) }
    fun setDegreesMode(enabled: Boolean) = viewModelScope.launch { repo.setDegreesMode(enabled) }
    fun setNumberFormat(value: String) = viewModelScope.launch { repo.setNumberFormat(value) }
    fun setDecimals(value: Int) = viewModelScope.launch { repo.setDecimals(value) }
    fun setUnitSystem(value: String) = viewModelScope.launch { repo.setUnitSystem(value) }
    fun setKeySound(value: String) = viewModelScope.launch { repo.setKeySound(value) }
    fun setHistoryEnabled(enabled: Boolean) = viewModelScope.launch { repo.setHistoryEnabled(enabled) }
    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { repo.setHapticsEnabled(enabled) }
    fun setAiEnabled(enabled: Boolean) = viewModelScope.launch { repo.setAiEnabled(enabled) }
    fun setFractionsEnabled(enabled: Boolean) = viewModelScope.launch { repo.setFractionsEnabled(enabled) }
    fun setLayoutStyle(style: String) = viewModelScope.launch { repo.setLayoutStyle(style) }
    fun setMinimalMode(enabled: Boolean) = viewModelScope.launch { repo.setMinimalMode(enabled) }
    fun setAngleUnit(unit: String) = viewModelScope.launch { repo.setAngleUnit(unit) }
    fun setDecimalFormat(format: String) = viewModelScope.launch { repo.setDecimalFormat(format) }
    fun setPercentageMode(mode: String) = viewModelScope.launch { repo.setPercentageMode(mode) }
    fun setDivisionSign(sign: String) = viewModelScope.launch { repo.setDivisionSign(sign) }
    fun setMultiplicationSign(sign: String) = viewModelScope.launch { repo.setMultiplicationSign(sign) }
    fun setKeyTextScale(scale: Float) = viewModelScope.launch { repo.setKeyTextScale(scale) }
    fun setBlinkCursor(enabled: Boolean) = viewModelScope.launch { repo.setBlinkCursor(enabled) }
    fun setClearOnExit(enabled: Boolean) = viewModelScope.launch { repo.setClearOnExit(enabled) }
    fun setShowCalcInfo(enabled: Boolean) = viewModelScope.launch { repo.setShowCalcInfo(enabled) }
    fun setImpliedMultiplication(mode: String) =
        viewModelScope.launch { repo.setImpliedMultiplication(mode) }
    fun setDecimalDigits(digits: Int) = viewModelScope.launch { repo.setDecimalDigits(digits) }
    fun setAutoCloseBrackets(enabled: Boolean) = viewModelScope.launch { repo.setAutoCloseBrackets(enabled) }
    fun setIgnoreTrailingOperator(enabled: Boolean) =
        viewModelScope.launch { repo.setIgnoreTrailingOperator(enabled) }
    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { repo.setKeepScreenOn(enabled) }
    fun setSaveState(enabled: Boolean) = viewModelScope.launch { repo.setSaveState(enabled) }

    /** The remembered expression, so the board survives a restart. */
    val savedExpression: StateFlow<String> = repo.savedExpression
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun setSavedExpression(text: String) = viewModelScope.launch { repo.setSavedExpression(text) }

    fun setDiagnosticsEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.setDiagnosticsEnabled(enabled)
        DiagnosticsLogger.setEnabled(enabled)
    }

    fun reset() = viewModelScope.launch { repo.reset() }
}
