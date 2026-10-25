package com.merry8989uk.universalcalculator.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Screen-reader labels. The important test is the first one: it walks the real
 * keypad, so a key added later cannot ship without a spoken label.
 */
class SpokenTest {

    @Test fun everyKeypadKeyHasASpokenLabel() {
        val keys = PAD.flatten() + SCIENTIFIC_KEYS
        assertTrue("the keypad lists are empty, the test is not looking at anything", keys.size > 30)

        val missing = keys.filterNot { Spoken.isSpoken(it) }
        assertTrue(
            "these keypad keys have no spoken label, so a screen reader would read " +
                "the glyph or nothing at all: $missing",
            missing.isEmpty(),
        )
    }

    @Test fun everyScientificDrawerKeyHasASpokenLabel() {
        // The drawer renders SCIENTIFIC_KEYS, so those keys matter just as much.
        val missing = SCIENTIFIC_KEYS.filterNot { Spoken.isSpoken(it) }
        assertTrue("these scientific keys have no spoken label: $missing", missing.isEmpty())
        assertTrue("SCIENTIFIC_KEYS is empty", SCIENTIFIC_KEYS.size > 15)
    }

    @Test fun theMemoryKeysHaveLabels() {
        for (key in listOf("MC", "M+", "M\u2212", "MR")) {
            assertTrue("$key has no spoken label", Spoken.isSpoken(key))
        }
    }

    @Test fun operatorsAreSpelledOut() {
        assertEquals("multiply", Spoken.key("*"))
        assertEquals("divide", Spoken.key("/"))
        assertEquals("minus", Spoken.key("-"))
        assertEquals("plus", Spoken.key("+"))
        assertEquals("percent", Spoken.key("%"))
        assertEquals("equals", Spoken.key("="))
        assertEquals("to the power of", Spoken.key("^"))
    }

    @Test fun glyphKeysAreSpelledOut() {
        assertEquals("delete", Spoken.key("del"))
        assertEquals("plus or minus", Spoken.key("neg"))
        assertEquals("square root", Spoken.key("sqrt"))
        assertEquals("cube root", Spoken.key("cbrt"))
        assertEquals("x squared", Spoken.key("x^2"))
        assertEquals("one over x", Spoken.key("1/x"))
        assertEquals("factorial", Spoken.key("!"))
        assertEquals("open bracket", Spoken.key("("))
        assertEquals("close bracket", Spoken.key(")"))
    }

    @Test fun wordsNotLettersForFunctions() {
        assertEquals("sine", Spoken.key("sin"))
        assertEquals("cosine", Spoken.key("cos"))
        assertEquals("tangent", Spoken.key("tan"))
        assertEquals("natural logarithm", Spoken.key("ln"))
        assertEquals("logarithm base ten", Spoken.key("log"))
        assertEquals("combinations", Spoken.key("nCr"))
        assertEquals("permutations", Spoken.key("nPr"))
        assertEquals("imaginary unit", Spoken.key("i"))
        assertEquals("previous answer", Spoken.key("ans"))
        assertEquals("double zero", Spoken.key("00"))
    }

    @Test fun digitsSayThemselves() {
        for (d in '0'..'9') {
            assertEquals(d.toString(), Spoken.key(d.toString()))
            assertTrue(Spoken.isSpoken(d.toString()))
        }
    }

    // ---- the localised labels ----

    @Test fun everySpokenWordHasATranslatableResource() {
        val missing = Spoken.WORDS.keys.filterNot { it in Spoken.SPOKEN_RES }
        assertTrue(
            "these keys have an English label but no string resource, so TalkBack " +
                "would fall back to English in Hindi: $missing",
            missing.isEmpty(),
        )
    }

    @Test fun theResourceMapHasNoStaleEntries() {
        val extra = Spoken.SPOKEN_RES.keys.filterNot { it in Spoken.WORDS }
        assertTrue(
            "these resource ids are not keys any more: $extra",
            extra.isEmpty(),
        )
    }

    @Test fun theResourceMapCoversTheWholeKeypad() {
        val keys = (PAD.flatten() + SCIENTIFIC_KEYS).distinct()
        val missing = keys.filterNot { key ->
            key in Spoken.SPOKEN_RES || key.all { it.isDigit() }
        }
        assertTrue("these keypad keys have no localised label: $missing", missing.isEmpty())
    }

    @Test fun theTwoMapsHaveTheSameSize() {
        assertEquals(
            "the English map and the resource map have drifted apart",
            Spoken.WORDS.size,
            Spoken.SPOKEN_RES.size,
        )
        assertTrue("the label maps look empty", Spoken.WORDS.size > 40)
    }

    @Test fun anUnknownKeyFallsBackToItself() {
        assertEquals("somethingNew", Spoken.key("somethingNew"))
    }

    @Test fun noLabelIsBlankOrAGlyph() {
        val glyphs = listOf("\u232B", "\u00B1", "\u221A", "\u221B", "\u00D7", "\u00F7", "\u2212", "\u03C0")
        for (key in PAD.flatten() + SCIENTIFIC_KEYS) {
            val spoken = Spoken.key(key)
            assertTrue("'$key' produced a blank label", spoken.isNotBlank())
            assertTrue(
                "'$key' was read as the glyph '$spoken' instead of a word",
                glyphs.none { spoken == it },
            )
        }
    }
}
