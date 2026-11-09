package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.Contrast
import com.merry8989uk.universalcalculator.ui.KeyPalette
import com.merry8989uk.universalcalculator.ui.PAD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #40: whether the keypad can actually be read.
 *
 * The colours of the pad were literals inside a composable, where nothing could
 * check them and a white label on a neon face looked fine on a bright desk. They
 * now live in [KeyPalette], so the ratios can be measured here against exactly
 * what is drawn — including both ends of every gradient, because a label sits
 * over the whole of one.
 *
 * The arithmetic is WCAG 2.1: 4.5:1 for ordinary text, 3:1 for large text. The
 * key labels are 16sp, which is ordinary text, so 4.5:1 is the bar every one of
 * them has to clear.
 */
class KeyContrastTest {

    // ---------------- the measure itself ----------------

    @Test fun blackOnWhiteIsTheHighestRatioThereIs() {
        assertEquals(21.0, Contrast.ratio(0xFF000000, 0xFFFFFFFF), 0.001)
    }

    @Test fun aColourAgainstItselfIsOne() {
        assertEquals(1.0, Contrast.ratio(KeyPalette.EQUALS_FROM, KeyPalette.EQUALS_FROM), 0.0001)
    }

    @Test fun theRatioDoesNotDependOnWhichColourIsWhich() {
        val one = Contrast.ratio(KeyPalette.INK, KeyPalette.EQUALS_FROM)
        val other = Contrast.ratio(KeyPalette.EQUALS_FROM, KeyPalette.INK)
        assertEquals(one, other, 0.0001)
    }

    @Test fun aDarkerFaceScoresHigherAgainstAWhiteLabel() {
        // which is the whole reason the operator blue was darkened: white needs
        // something dark underneath it
        val dark = Contrast.ratio(KeyPalette.PAPER, KeyPalette.OPERATOR_TO)
        val pale = Contrast.ratio(KeyPalette.PAPER, 0xFF93C5FD)
        assertTrue("darkening the face should raise the ratio", dark > pale)
    }

    @Test fun theTwoThresholdsAreTheOnesTheStandardNames() {
        assertEquals(4.5, Contrast.AA_NORMAL, 0.0)
        assertEquals(3.0, Contrast.AA_LARGE, 0.0)
    }

    @Test fun aRatioOnTheLinePasses() {
        // a ratio is compared with >=, so exactly 4.5 is a pass and not a fail
        assertTrue(Contrast.passesNormal(0xFF000000, 0xFFFFFFFF))
        assertFalse(Contrast.passesNormal(0xFFFFFFFF, 0xFF06B6D4))
        assertTrue("large text only needs 3:1", Contrast.passesLarge(0xFFFFFFFF, 0xFFEF4444))
    }

    @Test fun theRatioIsWrittenTheWayTheStandardWritesIt() {
        assertEquals("21.00:1", Contrast.text(0xFF000000, 0xFFFFFFFF))
    }

    // ---------------- the real keys ----------------

    @Test fun everyKeyThatCarriesTextPassesAtOrdinarySize() {
        val failures = ArrayList<String>()
        for (key in PAD.flatten()) {
            val paint = KeyPalette.paintFor(key) ?: continue
            for (face in paint.face) {
                val ratio = Contrast.ratio(paint.label, face)
                if (ratio < Contrast.AA_NORMAL) {
                    failures.add("$key: ${Contrast.text(paint.label, face)} on #${face.toString(16)}")
                }
            }
        }
        assertTrue(
            "these keys are below 4.5:1 and cannot be read as normal text: $failures",
            failures.isEmpty(),
        )
    }

    @Test fun theEqualsKeyIsComfortablyPastTheBar() {
        // it was 2.43:1, the worst key on the pad, and it is the one everybody presses
        val paint = KeyPalette.paintFor("=")!!
        for (face in paint.face) {
            assertTrue(
                "= is only ${Contrast.text(paint.label, face)}",
                Contrast.ratio(paint.label, face) >= Contrast.AAA_NORMAL,
            )
        }
    }

    @Test fun theClearKeysArePastTheBar() {
        for (key in listOf("C", "del", "AC", "CE")) {
            val paint = KeyPalette.paintFor(key)!!
            for (face in paint.face) {
                assertTrue(
                    "$key is only ${Contrast.text(paint.label, face)}",
                    Contrast.ratio(paint.label, face) >= Contrast.AA_NORMAL,
                )
            }
        }
    }

    @Test fun theOperatorKeysArePastTheBar() {
        for (key in listOf("/", "*", "-", "+", "%")) {
            val paint = KeyPalette.paintFor(key)!!
            for (face in paint.face) {
                assertTrue(
                    "$key is only ${Contrast.text(paint.label, face)}",
                    Contrast.ratio(paint.label, face) >= Contrast.AA_NORMAL,
                )
            }
        }
    }

    @Test fun theRatiosQuotedInThePaletteAreTheRatiosItHas() {
        // the comment in KeyPalette is the thing a reviewer checks by hand, so it
        // is checked here too: if a colour moves, this fails rather than the
        // comment quietly going stale
        val equals = KeyPalette.paintFor("=")!!
        assertEquals("8.04:1", Contrast.text(equals.label, KeyPalette.EQUALS_FROM))
        assertEquals("7.70:1", Contrast.text(equals.label, KeyPalette.EQUALS_TO))

        val clear = KeyPalette.paintFor("C")!!
        assertEquals("6.97:1", Contrast.text(clear.label, KeyPalette.CLEAR_FROM))
        assertEquals("5.19:1", Contrast.text(clear.label, KeyPalette.CLEAR_TO))

        val operator = KeyPalette.paintFor("+")!!
        assertEquals("5.33:1", Contrast.text(operator.label, KeyPalette.OPERATOR_FROM))
        assertEquals("5.17:1", Contrast.text(operator.label, KeyPalette.OPERATOR_TO))
    }

    @Test fun theBrightFacesAreNotPrintedInWhite() {
        // white on the cyan and orange faces is what failed; nothing bright may
        // be labelled with it again
        for (key in listOf("=", "C", "CE", "del")) {
            val paint = KeyPalette.paintFor(key)!!
            assertEquals("$key went back to a white label", KeyPalette.INK, paint.label)
        }
    }

    @Test fun theDarkOperatorFaceKeepsItsWhiteLabel() {
        // white on the violet end was always fine; the blue end is what had to move
        val paint = KeyPalette.paintFor("+")!!
        assertEquals(KeyPalette.PAPER, paint.label)
        assertFalse(
            "the operator blue is still the shade that failed",
            paint.face.contains(0xFF3B82F6),
        )
    }

    // ---------------- the plain keys ----------------

    @Test fun theDigitsAreReadableInBothSchemes() {
        for ((name, face, label) in listOf(
            Triple("dark", KeyPalette.PLAIN_DARK_FACE, KeyPalette.PLAIN_DARK_LABEL),
            Triple("light", KeyPalette.PLAIN_LIGHT_FACE, KeyPalette.PLAIN_LIGHT_LABEL),
        )) {
            assertTrue(
                "the plain keys are unreadable in the $name scheme",
                Contrast.ratio(label, face) >= Contrast.AA_NORMAL,
            )
        }
    }

    // ---------------- the whole pad, accounted for ----------------

    @Test fun everyKeyOnThePadIsAccountedFor() {
        // a key is either a neon one with its own paint, or a plain one the theme
        // colours; there is no third answer and no key that falls between them
        val wrong = PAD.flatten().filter { key ->
            val painted = KeyPalette.paintFor(key) != null
            val neon = KeyPalette.kindOf(key) != KeyPalette.Kind.PLAIN
            painted != neon
        }
        assertTrue("keys whose family and paint disagree: $wrong", wrong.isEmpty())
    }

    @Test fun theNewKeysAreDrawnAsPlainKeys() {
        // ± and ans are ordinary ink on the theme's surface, like the digits
        assertNull(KeyPalette.paintFor("neg"))
        assertNull(KeyPalette.paintFor("ans"))
        assertNotNull(KeyPalette.paintFor("="))
    }

    @Test fun theKeyFamiliesAreTheOnesTheDesignUses() {
        assertEquals(KeyPalette.Kind.EQUALS, KeyPalette.kindOf("="))
        assertEquals(KeyPalette.Kind.CLEAR, KeyPalette.kindOf("del"))
        assertEquals(KeyPalette.Kind.OPERATOR, KeyPalette.kindOf("%"))
        assertEquals(KeyPalette.Kind.PLAIN, KeyPalette.kindOf("7"))
        assertEquals(KeyPalette.Kind.PLAIN, KeyPalette.kindOf("GT"))
    }

    @Test fun everyFaceHasTwoStopsForItsGradient() {
        for (key in listOf("=", "C", "+")) {
            assertEquals("$key is not a gradient any more", 2, KeyPalette.paintFor(key)!!.face.size)
        }
    }
}
