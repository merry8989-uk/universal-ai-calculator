package com.merry8989uk.universalcalculator.calc

/**
 * How a number is written out.
 *
 * These are the five the classic scientific calculators offer:
 *
 *   Normal         the shortest exact form, trailing zeros trimmed
 *   Fixed          exactly the chosen number of decimal places
 *   Scientific     one digit before the point, times a power of ten
 *   Engineering    the exponent forced to a multiple of three
 *   Engineering SI Engineering, with the power of ten replaced by its SI prefix
 */
enum class DecimalFormat {
    NORMAL,
    FIXED,
    SCIENTIFIC,
    ENGINEERING,
    ENGINEERING_SI;

    /** A short name for the settings list. */
    val label: String
        get() = when (this) {
            NORMAL -> "Normal"
            FIXED -> "Fixed"
            SCIENTIFIC -> "Scientific"
            ENGINEERING -> "Engineering"
            ENGINEERING_SI -> "Engineering SI"
        }

    companion object {
        /** Tolerant of how the value was stored. */
        fun of(name: String): DecimalFormat = when (name.trim().uppercase().replace(" ", "_")) {
            "FIXED" -> FIXED
            "SCIENTIFIC", "SCI" -> SCIENTIFIC
            "ENGINEERING", "ENG" -> ENGINEERING
            "ENGINEERING_SI", "ENG_SI", "ENGINEERINGSI", "SI" -> ENGINEERING_SI
            else -> NORMAL
        }

        /** The order the settings list shows them in. */
        val all: List<DecimalFormat> = entries.toList()
    }
}

/** The SI prefixes, largest first, with the power of ten each stands for. */
internal val SI_PREFIXES: List<Pair<Int, String>> = listOf(
    24 to "Y", 21 to "Z", 18 to "E", 15 to "P", 12 to "T", 9 to "G", 6 to "M", 3 to "k",
    0 to "",
    -3 to "m", -6 to "\u00B5", -9 to "n", -12 to "p", -15 to "f", -18 to "a", -21 to "z", -24 to "y",
)
