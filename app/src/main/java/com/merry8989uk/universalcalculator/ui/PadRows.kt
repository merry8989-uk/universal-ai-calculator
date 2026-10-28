package com.merry8989uk.universalcalculator.ui

/**
 * How the keypad reacts to the GT key being switched off.
 *
 * Two decisions live here, kept out of the composable so they can be tested:
 * which keys a row shows, and how wide the equals key is. Everything else about
 * the pad is untouched — this only ever removes one key and only ever widens one.
 */
object PadRows {

    /**
     * The keys a row actually shows.
     *
     * With the GT key switched off, the one key named `GT` is dropped and the
     * rest of the row closes up around it. No other key is affected, so `=` stays
     * exactly where it was.
     */
    fun keysFor(row: List<String>, showGt: Boolean): List<String> =
        if (showGt) row else row.filter { it != "GT" }

    /**
     * How much of the row a key takes.
     *
     * `=` is twice as wide when GT is gone, because the space it filled is now
     * free and a bigger equals key is what the user asked for. With GT on the
     * pad, every key is the same width as before.
     */
    fun weightFor(key: String, showGt: Boolean): Float =
        if (key == "=" && !showGt) 2f else 1f

    /** True when this row contained the GT key, whatever the switch says. */
    fun rowHasGt(row: List<String>): Boolean = row.contains("GT")
}
