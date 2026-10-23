package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechMathTest {

    private fun n(s: String) = SpeechMath.normalise(s)

    @Test fun simpleOperations() {
        assertEquals("2+3", n("two plus three"))
        assertEquals("5*4", n("five times four"))
        assertEquals("10/2", n("ten divided by two"))
        assertEquals("7-3", n("seven minus three"))
        assertEquals("9^2", n("nine squared"))
        assertEquals("2^10", n("two to the power of ten"))
        assertEquals("6!", n("six factorial"))
    }

    @Test fun numberWordsAreCombined() {
        assertEquals("100+50", n("one hundred plus fifty"))
        assertEquals("100+5", n("one hundred and five"))
        assertEquals("2500", n("two thousand five hundred"))
        assertEquals("25*4", n("twenty five times four"))
        assertEquals("12*12", n("twelve times twelve"))
        assertEquals("100000", n("one lakh"))
    }

    @Test fun decimalsAndBrackets() {
        assertEquals("3.5", n("three point five"))
        assertEquals("sqrt(16)", n("square root of sixteen"))
        assertEquals("cbrt(27)", n("cube root of twenty seven"))
        assertEquals("(2+3)*4", n("open bracket two plus three close bracket times four"))
    }

    @Test fun fillerWordsAreDropped() {
        assertEquals("20%*50", n("what is twenty percent of fifty"))
        assertEquals("8", n("calculate eight"))
        assertEquals("2+2", n("two plus two equals"))
    }

    @Test fun negativesAndSymbols() {
        assertEquals("-5+10", n("minus five plus ten"))
        assertEquals("pi", n("pi"))
    }

    @Test fun resultsAreEvaluable() {
        for (phrase in listOf(
            "two plus three", "ten divided by two", "twenty percent of fifty",
            "nine squared", "square root of sixteen", "two to the power of ten",
            "one hundred and five", "three point five",
        )) {
            val expr = n(phrase)
            try {
                Evaluator.evaluate(expr)
            } catch (e: Exception) {
                throw AssertionError("'$phrase' produced '$expr' which does not evaluate: ${e.message}")
            }
        }
    }

    @Test fun usableCheck() {
        assertTrue(SpeechMath.looksUsable("2+3"))
        assertTrue(SpeechMath.looksUsable("sqrt(16)"))
        assertTrue(SpeechMath.looksUsable("pi"))
        assertFalse(SpeechMath.looksUsable(""))
        assertFalse(SpeechMath.looksUsable("hello there"))
    }
}
