package com.merry8989uk.universalcalculator.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Shared across the whole app, so Standard and Scientific share one history. */
class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = HistoryRepository(app)

    /**
     * A purge waiting to be acknowledged.
     *
     * [lines] is kept in the form it was stored in, so Undo puts back the moment
     * each line was worked out rather than pretending it was all done now.
     */
    data class Purge(val lines: List<String>) {
        val count: Int get() = lines.size
    }

    val entries: StateFlow<String> = repo.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** The lines the user asked to keep for good. */
    val pinned: StateFlow<Set<String>> = repo.pinnedTexts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _purged = MutableStateFlow<Purge?>(null)

    /** What the last pass of the three-day rule took away, until it is undone. */
    val purged: StateFlow<Purge?> = _purged.asStateFlow()

    init {
        // Anything past its third day goes now rather than at the next
        // calculation, so a phone left alone for a week opens with an empty list
        // instead of a week of arithmetic — and says so, because an empty list
        // that appeared on its own is the thing this used to do wrong.
        viewModelScope.launch { announce(repo.prune()) }
    }

    fun append(entry: String) = viewModelScope.launch { announce(repo.append(entry)) }

    /** Wipes the tape. The caller asks first; this is what happens if they say yes. */
    fun clear() = viewModelScope.launch {
        repo.clear()
        _purged.value = null
    }

    /** Drops one entry, so a single wrong line can go without losing the rest. */
    fun remove(entry: String) = viewModelScope.launch { repo.remove(entry) }

    /** Keeps one line for good, or lets it age again. */
    fun setPinned(entry: String, pinned: Boolean) =
        viewModelScope.launch { repo.setPinned(entry, pinned) }

    /** Puts back what the last pass removed, and stops saying it removed them. */
    fun undoPurge() = viewModelScope.launch {
        val notice = _purged.value ?: return@launch
        repo.restore(notice.lines)
        _purged.value = null
    }

    /** Accepts the loss. The lines are already gone; this only drops the notice. */
    fun dismissPurge() {
        _purged.value = null
    }

    /** Turns a pass into a notice, if it actually took anything. */
    private fun announce(removed: List<String>) {
        if (removed.isNotEmpty()) _purged.value = Purge(removed)
    }
}
