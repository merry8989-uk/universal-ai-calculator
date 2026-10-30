package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
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

    // ---- the sparks on the AI button ----

    private fun magic(): String? = listOf(
        File("../app/src/main/java/com/merry8989uk/universalcalculator/ui/MagicButton.kt"),
        File("app/src/main/java/com/merry8989uk/universalcalculator/ui/MagicButton.kt"),
    ).firstOrNull { it.exists() }?.readText()

    @Test fun theSparksAreNotClippedAway() {
        // The bug: clip(CircleShape) sat on the box that held the sparks, so
        // every spark was cut off at the rim and the button looked still.
        val text = magic() ?: return
        // from the function body only: the comment above it describes the old
        // bug in words and would otherwise trip this check
        val outer = text.substringAfter("fun MagicButton(")
            .substringBefore("Canvas(modifier = Modifier.matchParentSize())")
        assertTrue(
            "the box that holds the sparks is clipped, so none of them can leave it",
            !outer.contains("clip("),
        )
        // and the circle itself must still be clipped, or it is a square
        val circle = text.substringAfter("// The button itself")
        assertTrue("the button is no longer a circle", circle.contains("clip(CircleShape)"))
    }

    @Test fun theSparksTravelOutwardPastTheRim() {
        val text = magic() ?: return
        // a spark that starts and ends inside the circle would never be seen leaving
        val distance = Regex("edge \\* \\(([0-9.]+)f \\+ ([0-9.]+)f \\* phase\\)")
            .find(text)?.groupValues
        assertTrue("the spark distance is not in the form this test expects", distance != null)
        val start = distance!![1].toFloat()
        val travel = distance[2].toFloat()
        assertTrue("the spark starts outside the button", start <= 0.9f)
        assertTrue("the spark never leaves the button", start + travel > 1.3f)
    }

    @Test fun thereAreEnoughSparksToReadAsSparkling() {
        val text = magic() ?: return
        val n = Regex("SPARKS = (\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull()
        assertTrue("only $n sparks: it will look like a slow blink", n != null && n >= 8)
    }

    @Test fun theSparksFadeBeforeTheyAreCutOff() {
        // a spark that vanishes at full brightness pops out of existence
        val text = magic() ?: return
        assertTrue("no fade", text.contains("val fade = (1f - phase) * (1f - phase)"))
        assertTrue("the fade is not applied to the colour", text.contains("Color.White.copy(alpha = fade"))
    }
}
