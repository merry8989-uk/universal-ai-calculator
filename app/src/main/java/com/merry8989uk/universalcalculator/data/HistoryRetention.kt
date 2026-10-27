package com.merry8989uk.universalcalculator.data

/**
 * How long a line of history lives, and how an old one is recognised.
 *
 * A stored line is `epochMillis|text`. A line written before this format existed
 * has no stamp; it is given one the first time it is read, so an upgrade costs a
 * user nothing and the three-day rule starts from that moment.
 *
 * Kept as plain functions so the rule can be tested without a device.
 */
object HistoryRetention {

    /** How long an entry may stay. */
    const val KEEP_DAYS = 3L

    /** The longest the list may get, whatever its age. */
    const val MAX_ENTRIES = 500

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    /** An entry's moment and its text. An unstamped line is given [now]. */
    fun stamp(line: String, now: Long): Pair<Long, String> {
        val bar = line.indexOf('|')
        if (bar > 0) {
            val at = line.substring(0, bar).toLongOrNull()
            if (at != null) return at to line.substring(bar + 1)
        }
        return now to line
    }

    /** The text of a stored line, without its stamp. */
    fun text(line: String, now: Long): String = stamp(line, now).second

    /** What a text becomes when it is stored. */
    fun stored(text: String, now: Long): String = now.toString() + "|" + text.replace("\n", " ")

    /**
     * The lines worth keeping: nothing older than [days], nothing beyond [max],
     * and everything stamped.
     */
    fun keep(
        lines: List<String>,
        now: Long,
        days: Long = KEEP_DAYS,
        max: Int = MAX_ENTRIES,
    ): List<String> {
        val cutoff = now - days * DAY_MS
        return lines
            .filter { it.isNotBlank() }
            .map { stamp(it, now) }
            .filter { it.first >= cutoff }
            .takeLast(max)
            .map { it.first.toString() + "|" + it.second }
    }
}
