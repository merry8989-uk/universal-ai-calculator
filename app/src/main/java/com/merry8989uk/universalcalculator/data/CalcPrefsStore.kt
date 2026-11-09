package com.merry8989uk.universalcalculator.data

import android.content.Context
import java.io.File

/** What the hub remembers about a person's own use of it. */
data class CalcPrefs(
    /** Starred calculators, in the order they were starred. */
    val favourites: List<String> = emptyList(),
    /** Recently opened calculators, most recent first. */
    val recents: List<String> = emptyList(),
)

/**
 * Favourites and recents for the calculators hub.
 *
 * Three hundred and fifty calculators is a library, not a list: the six somebody
 * uses every day are the six they have to hunt for. This keeps two short lists -
 * the ones they starred, and the ones they opened last - so the hub can put them
 * first.
 *
 * The format is a plain line-per-entry text file, and the reading, writing and
 * ordering are ordinary pure functions, so a test exercises them without
 * Android. Anything unreadable is skipped rather than thrown: one bad line must
 * not lose the list.
 */
object CalcPrefsStore {

    private const val HEADER = "NEXUSPREFS1"
    private const val FILE = "calculator_prefs.txt"
    private const val FAVOURITE = "favourite"
    private const val RECENT = "recent"

    /** How many recents are worth keeping. More than this is a history, not a shortcut. */
    const val MAX_RECENTS = 12

    private fun file(context: Context) = File(context.filesDir, FILE)

    fun load(context: Context): CalcPrefs {
        val f = file(context)
        if (!f.exists()) return CalcPrefs()
        return runCatching { parse(f.readText()) }.getOrDefault(CalcPrefs())
    }

    fun save(context: Context, prefs: CalcPrefs) {
        runCatching { file(context).writeText(serialize(prefs)) }
    }

    // ---- pure, testable ----

    fun serialize(prefs: CalcPrefs): String = buildString {
        appendLine(HEADER)
        prefs.favourites.forEach { appendLine("$FAVOURITE|${clean(it)}") }
        prefs.recents.forEach { appendLine("$RECENT|${clean(it)}") }
    }

    fun parse(text: String): CalcPrefs {
        val lines = text.split("\n")
        if (lines.isEmpty() || lines[0].trim() != HEADER) return CalcPrefs()
        val favourites = ArrayList<String>()
        val recents = ArrayList<String>()
        for (line in lines.drop(1)) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val bar = trimmed.indexOf('|')
            if (bar <= 0) continue
            val kind = trimmed.substring(0, bar)
            val id = trimmed.substring(bar + 1).trim()
            if (id.isEmpty()) continue
            when (kind) {
                FAVOURITE -> if (id !in favourites) favourites.add(id)
                RECENT -> if (id !in recents && recents.size < MAX_RECENTS) recents.add(id)
                else -> Unit
            }
        }
        return CalcPrefs(favourites, recents)
    }

    /** Starring twice takes the star off again. */
    fun toggleFavourite(prefs: CalcPrefs, id: String): CalcPrefs =
        if (id.isBlank()) prefs
        else if (id in prefs.favourites) prefs.copy(favourites = prefs.favourites - id)
        else prefs.copy(favourites = prefs.favourites + id)

    /** Opening a calculator puts it at the front, once, and keeps the list short. */
    fun recordRecent(prefs: CalcPrefs, id: String): CalcPrefs {
        if (id.isBlank()) return prefs
        val next = ArrayList<String>(prefs.recents.size + 1)
        next.add(id)
        prefs.recents.forEach { if (it != id && next.size < MAX_RECENTS) next.add(it) }
        return prefs.copy(recents = next)
    }

    /** A newline in an id would break the file it is written to. */
    private fun clean(id: String): String = id.replace("\n", " ").replace("\r", " ").trim()
}
