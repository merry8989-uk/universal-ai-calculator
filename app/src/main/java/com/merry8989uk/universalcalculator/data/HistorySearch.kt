package com.merry8989uk.universalcalculator.data

import com.merry8989uk.universalcalculator.calc.ArchiveEntry

/**
 * Finding one line again in the history or in the archive.
 *
 * Both screens used to be a list you could only scroll. The tape keeps three
 * days of work and the archive keeps years of it, so by the time a line is worth
 * going back to it is somewhere below the fold, and the only way to reach it was
 * to read the whole list by eye. The formula library already answers this with a
 * search box; this is the same answer, applied to lines of arithmetic.
 *
 * It is kept here, away from the screens, so the rule can be tested without a
 * device — and so both screens can only ever agree about what "matches" means.
 * Case and surrounding spaces are ignored, because a search box is not a place
 * to be exact; everything else is literal, because a half-typed sum and the same
 * sum typed out are not the same line.
 */
object HistorySearch {

    /** Whether [line] contains what was typed. An empty query matches everything. */
    fun matches(line: String, query: String): Boolean {
        val wanted = query.trim()
        return wanted.isEmpty() || line.contains(wanted, ignoreCase = true)
    }

    /** The lines that match, in the order they were given. */
    fun lines(lines: List<String>, query: String): List<String> =
        lines.filter { matches(it, query) }

    /**
     * The archive entries that match, in the order they were given.
     *
     * An archive row reads "expression = result", so both halves are searched:
     * the person looking for `9500*2` and the person looking for `19,000` are
     * looking for the same line, and neither should have to know which half it
     * is in.
     */
    fun entries(entries: List<ArchiveEntry>, query: String): List<ArchiveEntry> =
        entries.filter { matches(it.expression + " = " + it.result, query) }
}
