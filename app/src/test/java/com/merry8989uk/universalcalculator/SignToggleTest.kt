package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.SignToggle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #41: the ± key.
 *
 * The standard pad had no sign key at all, and the one the scientific drawer
 * offered flipped the sign of the whole sum. A calculator's ± flips the entry the
 * caret is in and nothing else, and pressing it twice has to put the expression
 * back the way it was, or nobody dares press it.
 *
 * [SignToggle.apply] is a pure function of the text and the caret, so all of that
 * is checked here rather than by tapping a phone.
 */
class SignToggleTest {

    // ---------------- the ordinary case ----------------

    @Test fun theNumberJustTypedIsPutInBrackets() {
        assertEquals("(-5)", SignToggle.apply("5").text)
    }

    @Test fun onlyTheLastEntryChanges() {
        assertEquals("12+(-5)", SignToggle.apply("12+5").text)
    }

    @Test fun aMultipliedEntryIsNegatedOnItsOwn() {
        // the sign belongs to the 3, not to the product
        assertEquals("12+5*(-3)", SignToggle.apply("12+5*3").text)
    }

    @Test fun theEntryUnderTheCaretIsTheOneThatChanges() {
        // "1+23+4" with the caret after the 23
        assertEquals("1+(-23)+4", SignToggle.apply("1+23+4", 4).text)
    }

    @Test fun aNumberIsNeverSplitInHalf() {
        // the caret in the middle of 12345 still negates the whole number
        assertEquals("(-12345)", SignToggle.apply("12345", 2).text)
    }

    @Test fun theAnswerCanBeNegated() {
        assertEquals("(-ans)", SignToggle.apply("ans").text)
    }

    @Test fun aFactorialStaysWithItsNumber() {
        assertEquals("(-5!)", SignToggle.apply("5!").text)
    }

    @Test fun aBracketedEntryIsWrappedWhole() {
        assertEquals("(-(2+3))", SignToggle.apply("(2+3)").text)
    }

    // ---------------- pressing it twice ----------------

    @Test fun pressingItTwicePutsTheExpressionBack() {
        for (text in listOf("5", "12+5", "ans", "5!", "(2+3)", "12+5*3", "2*ans+7")) {
            val once = SignToggle.apply(text)
            val twice = SignToggle.apply(once.text, once.caret)
            assertEquals("± twice changed '$text'", text, twice.text)
        }
    }

    @Test fun pressingItTwicePutsTheCaretBack() {
        for (text in listOf("5", "12+5", "ans")) {
            val once = SignToggle.apply(text)
            val twice = SignToggle.apply(once.text, once.caret)
            assertEquals("the caret moved away from the end of '$text'", text.length, twice.caret)
        }
    }

    @Test fun aMinusAlreadyWrittenInFrontOfTheEntryComesOff() {
        // -5 is already negative, so the key makes it 5 rather than -(-5)
        assertEquals("5", SignToggle.apply("-5").text)
    }

    @Test fun aSumWrittenByHandIsNotTakenApart() {
        // (-5+3) was typed, not made by this key: which half did they mean?
        assertEquals("(-(-5+3))", SignToggle.apply("(-5+3)").text)
    }

    // ---------------- the awkward places ----------------

    @Test fun anEmptyBoardOpensAMinus() {
        val result = SignToggle.apply("")
        assertEquals("-", result.text)
        assertEquals(1, result.caret)
    }

    @Test fun afterAnOperatorItOpensTheNextEntry() {
        assertEquals("5+-", SignToggle.apply("5+").text)
        assertEquals(3, SignToggle.apply("5+").caret)
    }

    @Test fun theCaretIsLeftAfterTheEntryItChanged() {
        val result = SignToggle.apply("12+5")
        assertEquals(result.text.length, result.caret)
    }

    @Test fun aCaretOutsideTheTextIsPulledBackInside() {
        assertEquals("(-5)", SignToggle.apply("5", 99).text)
        assertEquals("(-5)", SignToggle.apply("5", -3).text)
    }

    // ---------------- it is arithmetic, not decoration ----------------

    @Test fun theSumChangesByExactlyTheSignOfThatEntry() {
        assertEquals(17.0, Evaluator.evaluate("12+5"), 1e-9)
        assertEquals(7.0, Evaluator.evaluate(SignToggle.apply("12+5").text), 1e-9)
    }

    @Test fun whatItProducesIsAlwaysWorkable() {
        for (text in listOf("5", "12+5", "ans", "5!", "(2+3)", "-5")) {
            val toggled = SignToggle.apply(text).text
            val value = runCatching {
                Evaluator.evaluate(toggled, vars = mapOf("ans" to 4.0))
            }
            assertTrue("'$text' became '$toggled', which will not work out: ${value.exceptionOrNull()}", value.isSuccess)
        }
    }
}
