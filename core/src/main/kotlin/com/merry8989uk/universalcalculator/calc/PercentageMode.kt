package com.merry8989uk.universalcalculator.calc

/**
 * What `+` and `-` do when the term they are given carries a percent sign.
 *
 *   Multiplicative  100 + 20% = 120   the percent is taken of the running total,
 *                                     which is how a shop discount is worked out
 *   Additive        100 + 20% = 100.2 the percent is just a number, 20% = 0.2,
 *                                     which is how a maths paper writes it
 *
 * Neither setting touches `*` or `/`: there a percent is always the number
 * divided by a hundred, so 200 * 10% is 20 either way.
 */
enum class PercentageMode {
    MULTIPLICATIVE,
    ADDITIVE;

    val label: String
        get() = when (this) {
            MULTIPLICATIVE -> "Of the total"
            ADDITIVE -> "As a plain number"
        }

    /** The example shown next to the choice, so the difference is visible. */
    val example: String
        get() = when (this) {
            MULTIPLICATIVE -> "100 + 20% = 120"
            ADDITIVE -> "100 + 20% = 100.2"
        }

    val blurb: String
        get() = when (this) {
            MULTIPLICATIVE -> "A percent after + or - is taken of the number before it. Good for discounts and markups."
            ADDITIVE -> "A percent after + or - is just a number: 20% means 0.2."
        }

    companion object {
        fun of(name: String): PercentageMode = when (name.trim().uppercase()) {
            "ADDITIVE", "PLAIN", "NUMBER" -> ADDITIVE
            else -> MULTIPLICATIVE
        }

        val all: List<PercentageMode> = entries.toList()
    }
}
