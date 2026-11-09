package com.merry8989uk.universalcalculator.updater

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.merry8989uk.universalcalculator.settings.SettingsRepository
import kotlinx.coroutines.launch

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val progress: DownloadProgress) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

/**
 * Whether an available update should be put in front of the user at all.
 *
 * Nothing asks this at launch any more: the prompt lives in Settings, where the
 * user went to look, so opening the app is never interrupted by a version they
 * did not ask about. What is left is the one question worth answering — has
 * this version already been put aside? A version the user has said "later" to
 * stays quiet until the process dies and comes back, which is only true because
 * the answer is stored rather than remembered; a version they have never seen
 * is news, and may ask once.
 *
 * A plain function on purpose: no Android, no view model, so the rule can be
 * read and tested on its own.
 */
internal fun updatePromptVisible(
    dismissedVersion: String,
    latestVersion: String?,
    checking: Boolean,
): Boolean {
    // mid-check there is nothing to show yet, and half an answer is worse than
    // the last one
    if (checking) return false
    if (latestVersion.isNullOrBlank()) return false
    return latestVersion != dismissedVersion
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val context = app.applicationContext

    /**
     * The settings store, for the one thing here that has to outlive the
     * process: which version the user put aside. The same store Settings reads
     * and writes, so there is a single answer to "what has been dismissed".
     */
    private val settings = SettingsRepository(context)

    var state: UpdateUiState by mutableStateOf(UpdateUiState.Idle)
        private set

    /** The check the user asked for, from Settings or the app-info dialog. */
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

    /**
     * Puts this version aside for good.
     *
     * The version goes to the settings store, not into a field: a field dies
     * with the process, and that is exactly why the prompt came back on every
     * single launch. Storing the version rather than a plain "dismissed" flag
     * is what keeps the next release able to ask once while this one stays
     * quiet.
     */
    fun later(info: UpdateInfo) {
        viewModelScope.launch { settings.setDismissedUpdateVersion(info.latestVersion) }
        if (state is UpdateUiState.Available) state = UpdateUiState.Idle
    }

    fun dismissFailure() {
        if (state is UpdateUiState.Failed) state = UpdateUiState.Idle
    }
}
