package com.merry8989uk.universalcalculator.ui

/**
 * The standard number pad, as rows of keys.
 *
 * It lives here rather than beside the composable that draws it because the pad
 * is a fact about the calculator, not about a screen: the layout tests read it,
 * the contrast test walks it, and neither should need a phone to do it.
 *
 * The key ids are what [insertFor] and `pressKey` understand — the printed label
 * is a separate thing, so `*` shows a × and `neg` shows a ±.
 */
internal val PAD = listOf(
    // The clearing row, at the top where it belongs. `±` takes the place AC
    // held, because AC and C are the same key in code — both empty the
    // expression — so the duplicated label is what pays for the sign toggle,
    // and C still clears. Issue #41.
    listOf("C", "CE", "del", "neg", "%"),
    // The memory keys, directly above the digits.
    listOf("MC", "M+", "M-", "MR", "/"),
    listOf("7", "8", "9", "MU", "*"),
    listOf("4", "5", "6", ")", "-"),
    listOf("1", "2", "3", "(", "+"),
    // GT sits beside the digits so the right-hand column can be the four
    // operators, the way a desk calculator arranges them. `ans` takes `00`'s
    // place: `00` is the one key whose whole job is a double tap on its
    // neighbour, and `ans` inserts a number, so it belongs among the numbers.
    listOf("0", "ans", ".", "GT", "="),
)

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
