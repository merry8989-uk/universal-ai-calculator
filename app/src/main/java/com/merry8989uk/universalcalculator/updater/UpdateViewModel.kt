package com.merry8989uk.universalcalculator.updater

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val progress: DownloadProgress) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext

    var state: UpdateUiState by mutableStateOf(UpdateUiState.Idle)
        private set

    /** Set once the user says "later", so the same version is not raised again. */
    var dismissedVersion: String? by mutableStateOf(null)
        private set

    /** True while an update should be in front of the user. */
    val promptVisible: Boolean
        get() = when (val s = state) {
            is UpdateUiState.Available -> s.info.latestVersion != dismissedVersion
            is UpdateUiState.Downloading -> true
            is UpdateUiState.Failed -> true
            else -> false
        }

    /** Whether the app has asked the release page yet this run. */
    var checkedThisRun: Boolean by mutableStateOf(false)
        private set

    /**
     * The check the app makes by itself when it opens. Quiet: it never shows a
     * failure, because the user did not ask for it.
     */
    fun checkQuietly(currentVersion: String) {
        if (checkedThisRun) return
        checkedThisRun = true
        viewModelScope.launch {
            val info = UpdateChecker.check(currentVersion)
            if (info != null && state !is UpdateUiState.Downloading) {
                state = UpdateUiState.Available(info)
            }
        }
    }

    /** The check the user asked for, from the menu. */
    fun check(currentVersion: String) {
        if (state is UpdateUiState.Checking || state is UpdateUiState.Downloading) return
        state = UpdateUiState.Checking
        viewModelScope.launch {
            val info = UpdateChecker.check(currentVersion)
            state = if (info == null) UpdateUiState.UpToDate else UpdateUiState.Available(info)
        }
    }

    fun install(info: UpdateInfo) {
        if (state is UpdateUiState.Downloading) return
        state = UpdateUiState.Downloading(DownloadProgress(0L, 0L))
        viewModelScope.launch {
            val result = AppUpdater.downloadAndInstall(context, info) { progress ->
                state = UpdateUiState.Downloading(progress)
            }
            state = result.fold(
                onSuccess = { UpdateUiState.Idle },
                onFailure = { UpdateUiState.Failed(it.message ?: "Update failed") },
            )
        }
    }

    fun later(info: UpdateInfo) {
        dismissedVersion = info.latestVersion
        if (state is UpdateUiState.Available) state = UpdateUiState.Idle
    }

    fun dismissFailure() {
        if (state is UpdateUiState.Failed) state = UpdateUiState.Idle
    }
}
