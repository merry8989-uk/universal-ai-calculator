package com.merry8989uk.universalcalculator.data

/**
 * How long a line of history lives, how an old one is recognised, which ones
 * the user has asked to keep, and the number that came out of it.
 *
 * A stored line is `epochMillis|text`. A line the user has pinned is
 * `epochMillis*|text`, and a line whose answer was a number carries it as
 * `epochMillis#19000|text`. Both marks are written into the stamp rather than in
 * front of the text so that the text stays exactly what was worked out, and so
 * that a file written before either mark existed still reads: no `*` means not
 * pinned, no `#` means no number carried, which is what every line meant when it
 * was written. Nothing has to be migrated.
 *
 * The number is carried rather than read back out of the text, because the text
 * is the answer as the user asked to see it — "19,000", "19.000", "1.9×10⁴" —
 * and turning that back into a number means guessing at separators and at a
 * number format that may have been changed since the line was written. A carried
 * number is the same number whatever the settings were on the day. A line stored
 * before this existed simply has none, which the reader treats as "no number"
 * rather than as a reason to guess.
 *
 * A line written before stamps existed has no stamp; it is given one the first
 * time it is read, so an upgrade costs a user nothing and the three-day rule
 * starts from that moment.
 *
 * Kept as plain functions so the rule can be tested without a device.
 */
object HistoryRetention {

    /** How long an entry may stay. */
    const val KEEP_DAYS = 3L

    /** The longest the list may get, whatever its age. */
    const val MAX_ENTRIES = 500

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    /** The mark on a line the user asked to keep. */
    private const val PIN = '*'

    /** What comes before the number carried beside the text. */
    private const val NUMBER = '#'

    /** A stored line, taken apart. */
    data class Line(
        val at: Long,
        val pinned: Boolean,
        val text: String,
        /** The number behind the text, when the answer was one. */
        val value: Double? = null,
    )

    /**
     * What one pass of the rule did.
     *
     * [removed] is the part the tape has to own up to. A line that leaves
     * without being named is a line the user finds missing and cannot explain,
     * so the count and the lines both come back out of here rather than being
     * thrown away inside a filter.
     */
    data class Pass(val kept: List<String>, val removed: List<String>)

    /** A stored line's moment, whether it is pinned, its text, and its number. */
    fun parse(line: String, now: Long): Line {
        val bar = line.indexOf('|')
        if (bar > 0) {
            var head = line.substring(0, bar)
            var value: Double? = null
            val hash = head.indexOf(NUMBER)
            if (hash > 0) {
                // A stamp whose number will not read back is taken as no number
                // at all: the text is still the line the user worked out, and
                // losing it over a damaged mark would be the worse trade.
                value = head.substring(hash + 1).toDoubleOrNull()
                head = head.substring(0, hash)
            }
            val pinned = head.endsWith(PIN)
            if (pinned) head = head.dropLast(1)
            val at = head.toLongOrNull()
            if (at != null) return Line(at, pinned, line.substring(bar + 1), value)
        }
        return Line(now, false, line)
    }

    /** The text of a stored line, without its stamp. */
    fun text(line: String, now: Long): String = parse(line, now).text

    /**
     * The number behind a stored line, when its answer was one.
     *
     * This is what "Use this again" needs: the exact answer, not the display
     * string the user was shown. Null means the answer was not a plain number —
     * a list, a complex value, an error — and nothing is invented in its place.
     */
    fun valueOf(line: String, now: Long): Double? = parse(line, now).value

    /** Whether the user asked for this line to be kept whatever its age. */
    fun isPinned(line: String, now: Long): Boolean = parse(line, now).pinned

    /** What a text becomes when it is stored, with the number behind it if there was one. */
    fun stored(text: String, now: Long, value: Double? = null): String =
        stampOf(Line(now, false, text.replace("\n", " "), value))

    /** The same line, marked as kept for good — or released to age again. */
    fun withPin(line: String, now: Long, pinned: Boolean = true): String {
        val l = parse(line, now)
        // The number travels with the pin: keeping a line for good and then
        // finding it can no longer be used again would be a strange reward.
        return stampOf(Line(l.at, pinned, l.text, l.value))
    }

    /**
     * One pass of the rule: the lines worth keeping, and the ones it took away.
     *
     * A pinned line is never removed for its age and never removed to make room.
     * The cap exists to stop a runaway file, not to punish an old line, so when
     * it has to drop something it drops the oldest line that is free to go.
     */
    fun pass(
        lines: List<String>,
        now: Long,
        days: Long = KEEP_DAYS,
        max: Int = MAX_ENTRIES,
    ): Pass {
        val cutoff = now - days * DAY_MS
        val parsed = lines.filter { it.isNotBlank() }.map { parse(it, now) }
        val keep = parsed.map { it.pinned || it.at >= cutoff }.toBooleanArray()

        var over = keep.count { it } - max
        if (over > 0) {
            // oldest first: the list is stored oldest first
            for (i in parsed.indices) {
                if (over == 0) break
                if (keep[i] && !parsed[i].pinned) {
                    keep[i] = false
                    over--
                }
            }
        }

        val kept = ArrayList<String>(parsed.size)
        val removed = ArrayList<String>()
        parsed.forEachIndexed { i, line ->
            if (keep[i]) kept.add(stampOf(line)) else removed.add(stampOf(line))
        }
        return Pass(kept, removed)
    }

    /**
     * The lines worth keeping: nothing older than [days] unless it is pinned,
     * nothing beyond [max], and everything stamped.
     */
    fun keep(
        lines: List<String>,
        now: Long,
        days: Long = KEEP_DAYS,
        max: Int = MAX_ENTRIES,
    ): List<String> = pass(lines, now, days, max).kept

    private fun stampOf(line: Line): String = buildString {
        append(line.at)
        if (line.pinned) append(PIN)
        // Written as the shortest text that reads back as the same Double,
        // because this is the number a second calculation carries on from
        // rather than one to look at. An infinity or a NaN has no exact value to
        // carry, so it is left out.
        line.value?.takeIf { it.isFinite() }?.let { append(NUMBER).append(it) }
        append('|').append(line.text)
    }
}
