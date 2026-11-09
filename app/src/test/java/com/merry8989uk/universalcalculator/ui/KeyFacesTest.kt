package com.merry8989uk.universalcalculator.ui

import com.merry8989uk.universalcalculator.calc.AngleUnit
import com.merry8989uk.universalcalculator.calc.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a key shows on its face.
 *
 * The pad used to print the evaluator's own name for a function, so the drawer
 * asked a person to recognise `harmmean` before they could find the harmonic
 * mean — while the screen reader already knew the word, because [Spoken] has
 * always carried it. A face is that same word, so these tests pin one
 * vocabulary rather than two: every key that has a word shows the word, and the
 * printed second labels say what the key actually runs.
 *
 * The angle state is here too. It was the one state the pad changed without
 * showing, so the tests ask the two questions a person would: what does the key
 * print, and what does it say out loud.
 */
class KeyFacesTest {

    private fun everyClassicKey(): List<ClassicKey> =
        CLASSIC_PAGE_ONE.flatten() + CLASSIC_PAGE_TWO.flatten()

    // ---- the drawer's faces ----

    @Test fun everyDrawerKeyShowsSomething() {
        assertTrue("the drawer lists nothing, the test is looking at nothing", SCIENTIFIC_KEYS.size > 30)
        val blank = SCIENTIFIC_KEYS.filter { KeyFaces.face(it).isBlank() }
        assertTrue("keys with no face at all: $blank", blank.isEmpty())
    }

    @Test fun noKeyShowsTheEvaluatorsNameWhenAWordExists() {
        // the reported bug: the face fell through to the key string
        val stillIdentifiers = SCIENTIFIC_KEYS.filter { key ->
            val word = Spoken.key(key)
            word != key && KeyFaces.face(key) == key
        }
        assertTrue(
            "these keys still print the evaluator's name instead of the word: $stillIdentifiers",
            stillIdentifiers.isEmpty(),
        )
    }

    @Test fun theFourKeysTheReportNamedReadAsWords() {
        assertEquals("Lambert W", KeyFaces.face("lambertw"))
        assertEquals("harmonic mean", KeyFaces.face("harmmean"))
        assertEquals("natural log of one plus x", KeyFaces.face("log1p"))
        assertEquals("integer square root", KeyFaces.face("isqrt"))
    }

    @Test fun aFaceIsTheWordTheScreenReaderAlreadySays() {
        // one vocabulary, read from one place: a printed face and a spoken label
        // cannot drift apart if they are the same words
        val drifted = SCIENTIFIC_KEYS
            .filter { KeyFaces.isWord(it) }
            .filter { KeyFaces.face(it) != Spoken.key(it) }
        assertTrue("these faces have drifted from the spoken words: $drifted", drifted.isEmpty())
    }

    @Test fun aKeyThatIsASignKeepsItsSign() {
        // a sign is what a calculator prints, and what a person looks for at a glance
        assertEquals("\u221A", KeyFaces.face("sqrt"))
        assertEquals("\u03C0", KeyFaces.face("pi"))
        assertEquals("x\u00B2", KeyFaces.face("x^2"))
        assertEquals("\u00F7", KeyFaces.face("/"))
        assertEquals("\u00D7", KeyFaces.face("*"))
    }

    @Test fun theTwoOperatorSignsStillFollowTheSetting() {
        assertEquals("/", KeyFaces.face("/", divisionSign = "/"))
        assertEquals("*", KeyFaces.face("*", multiplicationSign = "*"))
    }

    @Test fun thePadsOwnSpellingsFollowTheWord() {
        // the classic pad spells two keys its own way; the face follows the word
        assertEquals("harmonic mean", KeyFaces.face("harmean"))
        assertEquals("golden ratio", KeyFaces.face("golden"))
        assertEquals("variance", KeyFaces.face("variance"))
    }

    // ---- the three labels that lied ----

    @Test fun theLogShiftPrintsTheBaseItRaises() {
        val log = CLASSIC_PAGE_ONE.flatten().first { it.action == "log" }
        assertEquals("the shift runs 2^x", "exp2", log.shiftAction)
        assertEquals("2\u02E3", log.shift)
        assertFalse(
            "the shift still claims ten to the power of x: ${log.shift}",
            log.shift.orEmpty().contains("10"),
        )
    }

    @Test fun theAnsShiftSaysItStores() {
        val ans = CLASSIC_PAGE_ONE.flatten().first { it.action == "ans" }
        assertEquals("the shift stores", "MS", ans.shiftAction)
        assertEquals("STO", ans.shift)
        assertFalse(
            "the shift still claims a memory subtraction: ${ans.shift}",
            ans.shift.orEmpty().contains("M\u2212"),
        )
    }

    @Test fun theLambertWAlphaPrintsGammaBecauseThatIsWhatItRuns() {
        val w = CLASSIC_PAGE_TWO.flatten().first { it.action == "lambertw" }
        assertEquals("the alpha runs gamma", "gamma", w.alphaAction)
        assertEquals("\u0393", w.alpha)
        assertFalse(
            "the alpha still claims a gamma-prime the calculator cannot work out: ${w.alpha}",
            w.alpha.orEmpty().contains("'"),
        )
    }

    @Test fun everySecondLabelThatNamesAFunctionStillNamesOneItRuns() {
        // the three above were the visible ones; this is the shape of the mistake
        // rather than the three instances of it
        val known = Evaluator.FUNCTION_NAMES.toSet()
        val broken = everyClassicKey().flatMap { key ->
            listOfNotNull(key.shift to key.shiftAction, key.alpha to key.alphaAction)
        }.filter { (label, action) ->
            // a label that is itself a function name must run that same function;
            // the pad prints some of them in capitals
            label in known && !label.equals(action, ignoreCase = true)
        }
        assertTrue("these labels name a function the key does not run: $broken", broken.isEmpty())
    }

    // ---- the angle state ----

    @Test fun theAngleKeyPrintsTheUnitItIsIn() {
        assertEquals("DEG", angleFace(CLASSIC_DEG, "\u2220", AngleUnit.DEGREE))
        assertEquals("RAD", angleFace(CLASSIC_DEG, "\u2220", AngleUnit.RADIAN))
        assertEquals("GRAD", angleFace(CLASSIC_DEG, "\u2220", AngleUnit.GRADIAN))
    }

    @Test fun aKeyThatIsNotTheAngleKeyKeepsItsOwnFace() {
        assertEquals("Sin", angleFace("sin", "Sin", AngleUnit.RADIAN))
        assertEquals("\u03C0", angleFace("pi", "\u03C0", AngleUnit.GRADIAN))
    }

    @Test fun theAngleKeySaysWhichUnitItIsIn() {
        assertEquals("angle unit, degrees", angleSpoken(AngleUnit.DEGREE))
        assertEquals("angle unit, radians", angleSpoken(AngleUnit.RADIAN))
        assertEquals("angle unit, gradians", angleSpoken(AngleUnit.GRADIAN))
    }

    @Test fun theAngleKeyIsOneKeyThatCyclesThreeUnits() {
        // one key, three states: the printed face and the light are what make
        // the states visible, so there must be exactly three of them
        val angle = CLASSIC_PAGE_ONE.flatten().first { it.action == CLASSIC_DEG }
        assertEquals("\u2220", angle.label)
        assertEquals(3, AngleUnit.entries.size)
        assertEquals(
            "every unit needs its own word",
            3, AngleUnit.entries.map { angleWord(it) }.distinct().size,
        )
    }
}
