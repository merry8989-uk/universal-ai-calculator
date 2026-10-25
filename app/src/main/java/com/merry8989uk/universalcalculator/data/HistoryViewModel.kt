package com.merry8989uk.universalcalculator.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shared across the whole app, so Standard and Scientific share one history. */
class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HistoryRepository(app)

    val entries: StateFlow<String> = repo.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun append(entry: String) = viewModelScope.launch { repo.append(entry) }

    fun clear() = viewModelScope.launch { repo.clear() }

    /** Drops one entry, so a single wrong line can go without losing the rest. */
    fun remove(entry: String) = viewModelScope.launch { repo.remove(entry) }
}
