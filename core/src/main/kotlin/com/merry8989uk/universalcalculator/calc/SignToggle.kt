package com.merry8989uk.universalcalculator.calc

/**
 * What the ± key does to the expression.
 *
 * A calculator's sign key does not edit the whole sum, it flips the sign of the
 * entry the caret is in — the number just typed — and leaves everything around
 * it alone. So `12+5` with the caret at the end becomes `12+(-5)`, not `-(12+5)`.
 *
 * The one rule that matters beyond that is that pressing the key twice puts the
 * expression back exactly as it was, because a key that cannot be undone by
 * pressing it again is a key people are afraid of. That is why a negated entry
 * is written as `(-x)` and taken back out of its brackets rather than being
 * stacked up as `-(-x)`.
 *
 * Kept here, away from the screen, so the arithmetic of it can be tested without
 * a phone: [apply] is a pure function of the text and the caret.
 */
object SignToggle {

    /** The expression after the key, and where the caret should be left. */
    data class Result(val text: String, val caret: Int)

    /**
     * Flips the sign of the entry at [caret].
     *
     * With nothing typed yet the key opens the minus the next digit will hang
     * from, which is what a calculator does at the start of a sum. With the
     * caret after an operator there is no entry to flip, so the same minus is
     * opened there instead.
     */
    fun apply(text: String, caret: Int = text.length): Result {
        if (text.isBlank()) return Result("-", 1)
        val at = caret.coerceIn(0, text.length)

        // Already negated by this key: take the brackets back off. Checked first
        // so that a second press is the exact inverse of the first.
        unwrap(text, at)?.let { return it }

        val start = entryStart(text, at)
        val end = entryEnd(text, at)
        if (start >= end) {
            // Nothing to negate here, so open a minus for the entry about to be typed.
            return Result(text.substring(0, at) + "-" + text.substring(at), at + 1)
        }

        // A sign that is already written in front of the entry comes off, which
        // is the other half of "flip": -5 becomes 5 rather than -(-5).
        if (text[start] == '-') {
            val kept = text.substring(0, start) + text.substring(start + 1)
            return Result(kept, (at - 1).coerceAtLeast(start))
        }

        val entry = text.substring(start, end)
        return Result(
            text.substring(0, start) + "(-" + entry + ")" + text.substring(end),
            end + 3,
        )
    }

    /**
     * A `(-x)` group the caret is sitting at, opened back up.
     *
     * Only a group that holds a single entry is undone: `(-5+3)` was written by
     * the user, not by this key, and guessing which half of it they meant is
     * worse than leaving it alone.
     */
    private fun unwrap(text: String, at: Int): Result? {
        val close = when {
            at > 0 && text[at - 1] == ')' -> at - 1
            at < text.length && text[at] == ')' -> at
            else -> return null
        }
        val open = matchingOpen(text, close) ?: return null
        if (open + 1 >= close || text[open + 1] != '-') return null
        val inner = text.substring(open + 2, close)
        if (!isOneEntry(inner)) return null
        val kept = text.substring(0, open) + inner + text.substring(close + 1)
        return Result(kept, open + inner.length)
    }

    /** Where the entry the caret is in, or has just finished, begins. */
    private fun entryStart(text: String, at: Int): Int {
        var i = at
        // A factorial belongs to the number it follows, so it is stepped over
        // rather than treated as the start of a new entry.
        while (i > 0 && text[i - 1] == '!') i--
        if (i > 0 && text[i - 1] == ')') {
            val open = matchingOpen(text, i - 1)
            if (open != null) i = open
        } else {
            while (i > 0 && isEntryChar(text[i - 1])) i--
        }
        // A minus written straight before the entry is its sign, not a subtraction.
        if (i > 0 && text[i - 1] == '-' && (i == 1 || text[i - 2] in "+-*/^%(")) i--
        return i
    }

    /** Where it ends. */
    private fun entryEnd(text: String, at: Int): Int {
        var i = at
        while (i < text.length && isEntryChar(text[i])) i++
        while (i < text.length && text[i] == '!') i++
        return i
    }

    private fun isEntryChar(c: Char): Boolean = c.isLetterOrDigit() || c == '.' || c == '_'

    /** The `(` that closes at [close], or null when the brackets do not meet. */
    private fun matchingOpen(text: String, close: Int): Int? {
        var depth = 0
        for (i in close downTo 0) {
            when (text[i]) {
                ')' -> depth++
                '(' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    /** True when [s] is one entry and not a sum of several. */
    private fun isOneEntry(s: String): Boolean {
        if (s.isEmpty()) return false
        var depth = 0
        for (c in s) {
            when (c) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth < 0) return false
                }
                in "+-*/^%," -> if (depth == 0) return false
            }
        }
        return depth == 0
    }
}
