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
 * out, and anything older than [HistoryRetention.KEEP_DAYS] goes on a pass of
 * the rule — but a pass is no longer something that happens quietly inside a
 * read. Reading shows what is stored; only [prune] and [append] remove
 * anything, and both hand back what they took so the tape can say so and offer
 * it back. A line the user pinned is never taken at all.
 *
 * A calculator's history is a convenience — what you did a minute ago, not a
 * record of your life — and the archive is the thing that keeps a record, if the
 * user asked for one.
 *
 * Nothing else in the app keeps its own history. This is the only one.
 */
class HistoryRepository(private val context: Context) {

    private val KEY = stringPreferencesKey("entries")

    private val MAX_ENTRIES = HistoryRetention.MAX_ENTRIES

    /** The stored lines as they are, blanks dropped. */
    private fun storedLines(prefs: Preferences): List<String> =
        (prefs[KEY] ?: "").split("\n").filter { it.isNotBlank() }

    /**
     * What the home screen shows: the text, without the stamps.
     *
     * This used to apply the three-day rule as well, which meant a line could
     * vanish on any read with nothing said and nothing to press. The rule now
     * lives in [prune], where it can be announced.
     */
    val entries: Flow<String> = context.historyStore.data.map { prefs ->
        val now = System.currentTimeMillis()
        storedLines(prefs).joinToString("\n") { HistoryRetention.text(it, now) }
    }

    /**
     * The number behind each line, by the text of the line.
     *
     * "Use this again" puts a past answer back on the board, and the board needs
     * the number, not the text the answer was shown as: "19,000" is a number to
     * read, not one to calculate with. The number travels beside the text, so it
     * is handed back here keyed by the line the user is holding. Where the same
     * line was worked out more than once the newest wins, which is the one at the
     * top of the tape.
     */
    val values: Flow<Map<String, Double>> = context.historyStore.data.map { prefs ->
        val now = System.currentTimeMillis()
        storedLines(prefs)
            .map { HistoryRetention.parse(it, now) }
            .mapNotNull { line -> line.value?.let { line.text to it } }
            .toMap()
    }

    /** The texts the user asked to keep for good. */
    val pinnedTexts: Flow<Set<String>> = context.historyStore.data.map { prefs ->
        val now = System.currentTimeMillis()
        storedLines(prefs)
            .map { HistoryRetention.parse(it, now) }
            .filter { it.pinned }
            .map { it.text }
            .toSet()
    }

    /**
     * Adds one entry, stamped with now, and applies the rule.
     *
     * [value] is the exact answer when the answer was a number, kept beside the
     * text so the line can be used again without reading the number back out of
     * a display string.
     *
     * Returns what the rule took away on the way, so an append cannot quietly
     * be the moment a month-old line disappeared.
     */
    suspend fun append(entry: String, value: Double? = null): List<String> {
        val line = HistoryRetention.stored(entry, System.currentTimeMillis(), value)
        var removed = emptyList<String>()
        context.historyStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val before = HistoryRetention.pass(storedLines(prefs), now, max = MAX_ENTRIES)
            // A second pass, because the new line is what can push the list over
            // the cap; it cannot take an old line for its age, the first pass
            // has already been through those.
            val after = HistoryRetention.pass(before.kept + line, now, max = MAX_ENTRIES)
            prefs[KEY] = after.kept.joinToString("\n")
            removed = before.removed + after.removed
        }
        return removed
    }

    /**
     * Drops one entry, leaving the rest alone.
     *
     * The clear button wipes everything, which is right for a fresh start and
     * useless for one wrong line. This is the other half — and it works on any
     * line, old or pinned, because deleting the one you are looking at is not
     * the same decision as deleting everything.
     */
    suspend fun remove(entry: String) {
        context.historyStore.edit { prefs ->
            val lines = storedLines(prefs).toMutableList()
            val now = System.currentTimeMillis()
            val at = lines.indexOfLast { HistoryRetention.text(it, now) == entry }
            if (at >= 0) {
                lines.removeAt(at)
                prefs[KEY] = lines.joinToString("\n")
            }
        }
    }

    /** Marks one line to be kept whatever its age, or lets it age again. */
    suspend fun setPinned(entry: String, pinned: Boolean) {
        context.historyStore.edit { prefs ->
            val lines = storedLines(prefs).toMutableList()
            val now = System.currentTimeMillis()
            val at = lines.indexOfLast { HistoryRetention.text(it, now) == entry }
            if (at >= 0) {
                lines[at] = HistoryRetention.withPin(lines[at], now, pinned)
                prefs[KEY] = lines.joinToString("\n")
            }
        }
    }

    /** Wipes the history — only the user does this, and only after being asked. */
    suspend fun clear() {
        context.historyStore.edit { it[KEY] = "" }
    }

    /**
     * Puts back what a purge took away.
     *
     * The lines come back pinned. They are already older than the rule allows,
     * so restoring them as they were would let the very next pass delete them
     * again and make the Undo a lie.
     */
    suspend fun restore(lines: List<String>) {
        if (lines.isEmpty()) return
        val now = System.currentTimeMillis()
        context.historyStore.edit { prefs ->
            val back = lines.filter { it.isNotBlank() }
                .map { HistoryRetention.withPin(it, now, pinned = true) }
            prefs[KEY] = HistoryRetention
                .pass(storedLines(prefs) + back, now, max = MAX_ENTRIES)
                .kept
                .joinToString("\n")
        }
    }

    /**
     * The whole history, stamps included, for a backup.
     *
     * It reads the store rather than the display flow, because the stamps
     * are what let the other device apply the same three-day rule.
     */
    suspend fun readAll(): String =
        HistoryRetention.keep(storedLines(context.historyStore.data.first()), System.currentTimeMillis())
            .joinToString("\n")

    /** Replaces the history with what came back from a backup, keeping the rules. */
    suspend fun replaceAll(text: String) {
        val now = System.currentTimeMillis()
        context.historyStore.edit {
            it[KEY] = HistoryRetention.keep(text.split("\n"), now).joinToString("\n")
        }
    }

    /**
     * One pass of the three-day rule, and a report of what it removed.
     *
     * This is the only place age deletes anything. It runs when the screen opens
     * and after each calculation, and the caller is expected to say out loud
     * what came back: a line that leaves without being named is a line the user
     * finds missing and cannot explain.
     */
    suspend fun prune(): List<String> {
        var removed = emptyList<String>()
        context.historyStore.edit { prefs ->
            val pass = HistoryRetention.pass(
                storedLines(prefs),
                System.currentTimeMillis(),
                max = MAX_ENTRIES,
            )
            val kept = pass.kept.joinToString("\n")
            if (kept != (prefs[KEY] ?: "")) prefs[KEY] = kept
            removed = pass.removed
        }
        return removed
    }
}
