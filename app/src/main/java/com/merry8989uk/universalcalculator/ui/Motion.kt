package com.merry8989uk.universalcalculator.ui

import kotlin.math.PI
import kotlin.math.sin

/**
 * The numbers behind the app's movement.
 *
 * They live here rather than inline so that the shape of each animation can be
 * checked without a device: a swell that does not start and end at rest, or a
 * tab that snaps instead of easing, is a thing a test can catch and an eye on a
 * fast phone cannot.
 */
object Motion {

    /** How long a tab takes to change colour and grow. */
    const val TAB_MS = 220

    /** How much bigger the selected tab's glyph is. */
    const val TAB_GLYPH_SCALE = 1.12f

    /** How long the answer takes to ease in. */
    const val ANSWER_FADE_MS = 180

    /** How far the answer rises as it appears, in dp. */
    const val ANSWER_RISE_DP = 12f

    /** The most the display swells while the light sweeps across it. */
    const val SWELL = 0.015f

    /**
     * The display's scale at a point in the `=` sweep, from 0 to 1.
     *
     * A half sine: exactly 1 at both ends, so the panel is never left slightly
     * enlarged, and largest in the middle, where the light is brightest.
     */
    fun swellAt(progress: Float): Float {
        if (progress <= 0f || progress >= 1f) return 1f
        return 1f + SWELL * sin(progress * PI).toFloat()
    }

    /** The answer's opacity at a point in its fade, from 0 to 1. */
    fun answerAlpha(progress: Float): Float = progress.coerceIn(0f, 1f)

    /** How far the answer still has to rise, in dp. */
    fun answerRise(progress: Float): Float =
        (1f - progress.coerceIn(0f, 1f)) * ANSWER_RISE_DP
}
