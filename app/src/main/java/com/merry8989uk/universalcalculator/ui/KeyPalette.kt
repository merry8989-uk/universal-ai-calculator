package com.merry8989uk.universalcalculator.ui

/**
 * The colours the number pad paints its keys with, in one place.
 *
 * The pad used to decide these inside the composable, in a `when` that nothing
 * could check. A contrast failure is not visible in a diff and not visible on a
 * developer's phone either — it is visible to somebody reading the pad in
 * sunlight — so the colours live here as plain numbers, which a test can measure
 * with [Contrast] and a reviewer can check against the ratios written beside
 * them.
 *
 * The design is unchanged: the same three gradients, the same neon faces, the
 * same white border. What changed is which colour is printed on them, and by how
 * much one stop of one gradient had to move. Each ratio below is measured
 * against the *worst* end of its gradient, because a label sits over all of it.
 *
 * Issue #40 — WCAG AA needs 4.5:1 for the text on these keys (the labels are
 * 16sp, which is normal text, not large). Before:
 *
 *   =        white on Cyan #06B6D4 / Emerald #10B981   2.43:1 / 2.54:1  FAIL
 *   clear    white on Orange #F97316 / Coral #EF4444   2.80:1 / 3.76:1  FAIL
 *   operator white on Violet #6C4CF1 / Indigo #3B82F6  5.33:1 / 3.68:1  FAIL
 *
 * After:
 *
 *   =        ink #0A0C14 on Cyan #06B6D4 / Emerald #10B981   8.04:1 / 7.70:1  PASS (AAA)
 *   clear    ink #0A0C14 on Orange #F97316 / Coral #EF4444   6.97:1 / 5.19:1  PASS
 *   operator white #FFFFFF on Violet #6C4CF1 / Blue #2563EB  5.33:1 / 5.17:1  PASS
 *
 * The plain keys — digits, `ans`, `±`, GT, MU, the memory row — are drawn in the
 * theme's own `surfaceVariant` and `onSurface`, and already passed comfortably
 * (13.3:1 dark, 15.4:1 light); they are listed here so the check covers the
 * whole pad rather than only the bright keys.
 */
object KeyPalette {

    /** Which family a key belongs to. */
    enum class Kind { EQUALS, CLEAR, OPERATOR, PLAIN }

    /** A face (one stop per end of its gradient) and the colour printed on it. */
    data class Paint(val face: List<Long>, val label: Long)

    // ---- the faces, unchanged except where noted ----

    /** `=`: the brightest pair on the pad, so it keeps its face and gains a dark label. */
    const val EQUALS_FROM = 0xFF06B6D4L // Cyan
    const val EQUALS_TO = 0xFF10B981L // Emerald

    /** The clearing keys. */
    const val CLEAR_FROM = 0xFFF97316L // Orange
    const val CLEAR_TO = 0xFFEF4444L // Coral

    /** The four operators. */
    const val OPERATOR_FROM = 0xFF6C4CF1L // Violet

    /**
     * The blue end of the operator gradient, one shade deeper than the theme's
     * Indigo (#3B82F6): white on the old blue was 3.68:1, and darkening the face
     * costs the look less than darkening the label would cost the neon.
     */
    const val OPERATOR_TO = 0xFF2563EBL

    // ---- the labels ----

    /**
     * The near-black the theme already puts on its bright colours (`onPrimary`),
     * used here for the same reason: white cannot sit on a neon face.
     */
    const val INK = 0xFF0A0C14L

    /** White, for the faces dark enough to carry it. */
    const val PAPER = 0xFFFFFFFFL

    // ---- the plain keys, as the two schemes in Theme.kt paint them ----

    const val PLAIN_DARK_FACE = 0xFF1D2030L // surfaceVariant, dark
    const val PLAIN_DARK_LABEL = 0xFFE7E9F2L // onSurface, dark
    const val PLAIN_LIGHT_FACE = 0xFFEDEBF8L // surfaceVariant, light
    const val PLAIN_LIGHT_LABEL = 0xFF14161FL // onSurface, light

    private val CLEAR_KEYS = setOf("C", "del", "AC", "CE")
    private val OPERATOR_KEYS = setOf("/", "*", "-", "+", "%")

    /** Which family [key] is painted as. */
    fun kindOf(key: String): Kind = when {
        key == "=" -> Kind.EQUALS
        key in CLEAR_KEYS -> Kind.CLEAR
        key in OPERATOR_KEYS -> Kind.OPERATOR
        else -> Kind.PLAIN
    }

    /**
     * What [key] is painted with, or null when it is a plain key and the theme
     * decides. The first stop of a face is where its gradient starts, the last
     * is what a flat, classic-style key is filled with.
     */
    fun paintFor(key: String): Paint? = when (kindOf(key)) {
        Kind.EQUALS -> Paint(listOf(EQUALS_FROM, EQUALS_TO), INK)
        Kind.CLEAR -> Paint(listOf(CLEAR_FROM, CLEAR_TO), INK)
        Kind.OPERATOR -> Paint(listOf(OPERATOR_FROM, OPERATOR_TO), PAPER)
        Kind.PLAIN -> null
    }
}
