package com.merry8989uk.universalcalculator.calc

/**
 * Where multiplication that was not written down sits in the order of things.
 *
 * `2π` and `3(4+5)` mean multiply, but the sign is missing. That leaves a real
 * question when a division is nearby:
 *
 *   Same precedence     1/2π = (1/2)·π ≈ 1.5708   the missing sign is a `*`
 *   Higher precedence   1/2π = 1/(2·π) ≈ 0.1592   the missing sign binds first
 *
 * Neither is wrong. Textbooks write it the first way; calculators that show a
 * fraction bar write it the second, because the bar reaches under the whole of
 * `2π`. This is the user's call.
 */
enum class ImpliedMultiplication {
    SAME_PRECEDENCE,
    HIGHER_PRECEDENCE;

    val label: String
        get() = when (this) {
            SAME_PRECEDENCE -> "Same as ×"
            HIGHER_PRECEDENCE -> "Binds tighter than ×"
        }

    /** The example shown next to the choice, so the difference is visible. */
    val example: String
        get() = when (this) {
            SAME_PRECEDENCE -> "1/2\u03C0 = 1/2\u00D7\u03C0 = 1.5708"
            HIGHER_PRECEDENCE -> "1/2\u03C0 = 1/(2\u00D7\u03C0) = 0.1592"
        }

    val blurb: String
        get() = when (this) {
            SAME_PRECEDENCE -> "A missing multiplication sign counts as a ×, and waits its turn like one."
            HIGHER_PRECEDENCE -> "A missing multiplication sign is worked out before × and ÷, as if a fraction bar reached under it."
        }

    companion object {
        fun of(name: String): ImpliedMultiplication = when (name.trim().uppercase()) {
            "HIGHER_PRECEDENCE", "HIGHER", "TIGHT", "IMPLICIT" -> HIGHER_PRECEDENCE
            else -> SAME_PRECEDENCE
        }

        val all: List<ImpliedMultiplication> = entries.toList()
    }
}
