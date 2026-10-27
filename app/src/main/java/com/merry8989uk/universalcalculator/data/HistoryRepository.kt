package com.merry8989uk.universalcalculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.historyStore: DataStore<Preferences> by preferencesDataStore(name = "history")

/**
 * The calculation history on the home screen.
 *
 * It is deliberately short-lived. Each line carries the moment it was worked
 * out, and anything older than [KEEP_DAYS] is dropped the next time the history
 * is touched. A calculator's history is a convenience — what you did a minute
 * ago, not a record of your life — and the archive is the thing that keeps a
 * record, if the user asked for one.
 *
 * Nothing else in the app keeps its own history. This is the only one.
 */
class HistoryRepository(private val context: Context) {

    private val KEY = stringPreferencesKey("entries")

    private val MAX_ENTRIES = HistoryRetention.MAX_ENTRIES

    /** The lines that survive the three-day rule, stamped. */
    private fun fresh(lines: List<String>): List<String> =
        HistoryRetention.keep(lines, System.currentTimeMillis(), HistoryRetention.KEEP_DAYS, MAX_ENTRIES)

    /** What the home screen shows: the text, without the stamps. */
    val entries: Flow<String> = context.historyStore.data.map { prefs ->
        val now = System.currentTimeMillis()
        fresh((prefs[KEY] ?: "").split("\n")).joinToString("\n") {
            HistoryRetention.text(it, now)
        }
    }

    /** Adds one entry, stamped with now, and drops anything past its third day. */
    suspend fun append(entry: String) {
        val line = HistoryRetention.stored(entry, System.currentTimeMillis())
        context.historyStore.edit { prefs ->
            val current = fresh((prefs[KEY] ?: "").split("\n"))
            prefs[KEY] = (current + line).takeLast(MAX_ENTRIES).joinToString("\n")
        }
    }

    /**
     * Drops one entry, leaving the rest alone.
     *
     * The clear button wipes everything, which is right for a fresh start and
     * useless for one wrong line. This is the other half.
     */
    suspend fun remove(entry: String) {
        context.historyStore.edit { prefs ->
            val lines = fresh((prefs[KEY] ?: "").split("\n")).toMutableList()
            val now = System.currentTimeMillis()
            val at = lines.indexOfLast { HistoryRetention.text(it, now) == entry }
            if (at >= 0) {
                lines.removeAt(at)
                prefs[KEY] = lines.joinToString("\n")
            }
        }
    }

    /** Wipes the history — only the user does this. */
    suspend fun clear() {
        context.historyStore.edit { it[KEY] = "" }
    }

    /**
     * The whole history, stamps included, for a backup.
     *
     * It reads the store rather than the display flow, because the stamps
     * are what let the other device apply the same three-day rule.
     */
    suspend fun readAll(): String =
        fresh((context.historyStore.data.first()[KEY] ?: "").split("\n")).joinToString("\n")

    /** Replaces the history with what came back from a backup, keeping the rules. */
    suspend fun replaceAll(text: String) {
        context.historyStore.edit { it[KEY] = fresh(text.split("\n")).joinToString("\n") }
    }

    /** Drops anything past its third day, and stamps anything that arrived without one. */
    suspend fun prune() {
        context.historyStore.edit { prefs ->
            val kept = fresh((prefs[KEY] ?: "").split("\n")).joinToString("\n")
            if (kept != (prefs[KEY] ?: "")) prefs[KEY] = kept
        }
    }
}
