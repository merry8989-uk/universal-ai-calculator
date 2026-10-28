package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shape of the app's animations.
 *
 * An animation that does not begin and end at rest leaves the screen slightly
 * wrong for the rest of the session, which is the kind of fault that is easy to
 * ship and hard to notice.
 */
class MotionTest {

    // ---- the display swell on = ----

    @Test fun theSwellStartsAtRest() {
        assertEquals(1f, Motion.swellAt(0f), 1e-6f)
    }

    @Test fun theSwellEndsAtRest() {
        assertEquals(1f, Motion.swellAt(1f), 1e-6f)
    }

    @Test fun theSwellIsBiggestInTheMiddle() {
        val middle = Motion.swellAt(0.5f)
        assertTrue("the swell should grow", middle > Motion.swellAt(0.25f))
        assertTrue("the swell should be small", middle < 1.05f)
    }

    @Test fun theSwellNeverShrinksTheDisplay() {
        var p = 0f
        while (p <= 1f) {
            assertTrue("at $p the panel shrank", Motion.swellAt(p) >= 1f)
            p += 0.01f
        }
    }

    @Test fun theSwellIsForgivingOfOutOfRangeProgress() {
        assertEquals(1f, Motion.swellAt(-0.5f), 1e-6f)
        assertEquals(1f, Motion.swellAt(1.5f), 1e-6f)
    }

    // ---- the answer easing in ----

    @Test fun theAnswerIsInvisibleBeforeItArrives() {
        assertEquals(0f, Motion.answerAlpha(0f), 1e-6f)
    }

    @Test fun theAnswerIsFullyVisibleWhenSettled() {
        assertEquals(1f, Motion.answerAlpha(1f), 1e-6f)
    }

    @Test fun theAnswerHasFinishedRisingWhenSettled() {
        assertEquals(0f, Motion.answerRise(1f), 1e-6f)
    }

    @Test fun theAnswerRisesFromItsOffset() {
        assertEquals(Motion.ANSWER_RISE_DP, Motion.answerRise(0f), 1e-6f)
        assertTrue("halfway should be halfway", Motion.answerRise(0.5f) < Motion.answerRise(0.2f))
    }

    @Test fun theAlphaIsClamped() {
        assertEquals(0f, Motion.answerAlpha(-1f), 1e-6f)
        assertEquals(1f, Motion.answerAlpha(2f), 1e-6f)
    }

    // ---- the timings are sensible ----

    @Test fun theAnimationsAreQuickEnoughToFeelImmediate() {
        assertTrue("the tab is sluggish", Motion.TAB_MS in 100..400)
        assertTrue("the answer is sluggish", Motion.ANSWER_FADE_MS in 80..300)
    }

    @Test fun theTabGlyphGrowsButNotEnoughToDisturbTheRow() {
        assertTrue(Motion.TAB_GLYPH_SCALE > 1f)
        assertTrue("a glyph this large would push the row about", Motion.TAB_GLYPH_SCALE < 1.2f)
    }
}
