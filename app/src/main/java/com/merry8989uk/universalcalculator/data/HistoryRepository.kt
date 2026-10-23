package com.merry8989uk.universalcalculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyStore: DataStore<Preferences> by preferencesDataStore(name = "history")

/**
 * Calculation history, stored on the device. It is kept until the user clears
 * it — it survives tab switches, app restarts and updates.
 */
class HistoryRepository(private val context: Context) {

    private val KEY = stringPreferencesKey("entries")
    private val MAX_ENTRIES = 500

    val entries: Flow<String> = context.historyStore.data.map { it[KEY] ?: "" }

    /** Appends one entry, newest last. */
    suspend fun append(entry: String) {
        context.historyStore.edit { prefs ->
            val current = prefs[KEY] ?: ""
            val lines = current.split("\n").filter { it.isNotBlank() }
            val updated = (lines + entry).takeLast(MAX_ENTRIES)
            prefs[KEY] = updated.joinToString("\n")
        }
    }

    /** Wipes the history — only the user does this. */
    suspend fun clear() {
        context.historyStore.edit { it[KEY] = "" }
    }
}
