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
    fun setGroupDigits(enabled: Boolean) = viewModelScope.launch { repo.setGroupDigits(enabled) }
    fun setHistoryEnabled(enabled: Boolean) = viewModelScope.launch { repo.setHistoryEnabled(enabled) }
    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { repo.setHapticsEnabled(enabled) }
    fun setAiEnabled(enabled: Boolean) = viewModelScope.launch { repo.setAiEnabled(enabled) }

    fun setDiagnosticsEnabled(enabled: Boolean) = viewModelScope.launch {
        repo.setDiagnosticsEnabled(enabled)
        DiagnosticsLogger.setEnabled(enabled)
    }

    fun reset() = viewModelScope.launch { repo.reset() }
}
