package com.merry8989uk.universalcalculator.calc

/**
 * An expression that carries fill-in slots.
 *
 * Pressing a key like x^y does not just type a caret: it puts down a shape with
 * two holes in it, `□^□`, and the cursor sits in the first. Type 2 and 8 and you
 * have `2^8`. Press backspace and the 8 goes back to being a hole, so the shape
 * survives while you change your mind about what goes in it.
 *
 * The slots are tracked rather than guessed. A digit that filled a slot looks
 * exactly like a digit that was typed freely, so the only way to know which is
 * which is to remember.
 */
data class BoxExpression(
    val text: String = "",
    /** Where the slots are. Each holds exactly one character: a box, or what was typed into it. */
    val slots: List<Int> = emptyList(),
) {

    val isEmpty: Boolean get() = text.isEmpty()

    /** Are there holes still to fill? */
    fun hasEmptySlot(): Boolean = slots.any { it in text.indices && text[it] == BOX }

    /** The first hole at or after [from], or -1. */
    fun nextEmptySlot(from: Int): Int =
        slots.filter { it >= from && it in text.indices && text[it] == BOX }.minOrNull() ?: -1

    /**
     * Puts [template] down at [at] and leaves the cursor in its first hole.
     *
     * A template with no hole in it is simply typed, and the cursor lands after it.
     */
    fun template(at: Int, template: String): Pair<BoxExpression, Int> {
        val cut = at.coerceIn(0, text.length)
        val before = text.substring(0, cut)
        val after = text.substring(cut)
        val newSlots = ArrayList<Int>()
        template.forEachIndexed { i, c -> if (c == BOX) newSlots.add(cut + i) }
        val shifted = slots.map { if (it >= cut) it + template.length else it }
        val next = BoxExpression(before + template + after, (shifted + newSlots).sorted())
        val landing = newSlots.firstOrNull() ?: (cut + template.length)
        return next to landing
    }

    /**
     * Types [typed] at [cursor].
     *
     * When the cursor sits on a hole the first character of [typed] goes into it
     * and the cursor moves on to the next hole, so a shape can be filled without
     * pressing anything to move between its holes.
     */
    fun type(cursor: Int, typed: String): Pair<BoxExpression, Int> {
        if (typed.isEmpty()) return this to cursor
        val cut = cursor.coerceIn(0, text.length)
        if (cut in slots) {
            // one character per hole
            val fill = typed.first().toString()
            val replaced = text.substring(0, cut) + fill + text.substring(cut + 1)
            val next = BoxExpression(replaced, slots)
            val landing = next.nextEmptySlot(cut + 1).let { if (it >= 0) it else cut + 1 }
            return next to landing
        }
        val inserted = text.substring(0, cut) + typed + text.substring(cut)
        val shifted = slots.map { if (it >= cut) it + typed.length else it }
        return BoxExpression(inserted, shifted) to (cut + typed.length)
    }

    /**
     * Backspace at [cursor].
     *
     * A filled hole goes back to being a hole before anything is deleted — that
     * is the whole point of the shape. Anything else is deleted as usual.
     */
    fun backspace(cursor: Int): Pair<BoxExpression, Int> {
        val cut = cursor.coerceIn(0, text.length)
        if (cut == 0) return this to 0
        // standing on an empty hole: empty the one before it, so a shape can be
        // taken apart hole by hole without deleting its structure
        if (cut in slots && text[cut] == BOX) {
            val earlier = slots.lastOrNull { it < cut && text[it] != BOX }
            if (earlier != null) {
                val restored = text.substring(0, earlier) + BOX + text.substring(earlier + 1)
                return BoxExpression(restored, slots) to earlier
            }
        }
        val previous = cut - 1
        if (previous in slots) {
            val restored = text.substring(0, previous) + BOX + text.substring(previous + 1)
            return BoxExpression(restored, slots) to previous
        }
        val deleted = text.substring(0, previous) + text.substring(cut)
        val shifted = slots.filter { it != previous }.map { if (it > previous) it - 1 else it }
        return BoxExpression(deleted, shifted) to previous
    }

    /** The text with every hole read as a zero, so a half-filled shape still gives an answer. */
    fun filled(): String = text.replace(BOX.toString(), "0")

    companion object {
        /** The hole. A box that a number can go into. */
        const val BOX = '\u25A1'

        /** What each key puts down. A key that is not here simply types itself. */
        val TEMPLATES: Map<String, String> = mapOf(
            "x^y" to "$BOX^$BOX",
            "x/y" to "$BOX/$BOX",
            "1/x" to "1/$BOX",
        )

        fun templateFor(key: String): String? = TEMPLATES[key]
    }
}
