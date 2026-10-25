package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.AnswerStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two lines a big answer is shown as.
 *
 * The case that matters is the one in the reference: 256 to the sixth is
 * 281474976710656, which is unreadable as a wall of digits. It is shown as
 * `281 474 976 710 656` with `= 2.814 749 767 11x10^14` beneath it.
 */
class AnswerStyleTest {

    @Test fun thePowerOfTenIsWrittenAsASuperscript() {
        assertEquals("\u00B9\u2074", AnswerStyle.superscript(14))
        assertEquals("\u2070", AnswerStyle.superscript(0))
        assertEquals("\u207B\u2075", AnswerStyle.superscript(-5))
        assertEquals("\u00B9\u00B2\u00B3", AnswerStyle.superscript(123))
    }

    @Test fun digitsAreGroupedInThrees() {
        assertEquals("281 474 976 710 656", AnswerStyle.groupThrees("281474976710656"))
        assertEquals("1 234 567", AnswerStyle.groupThrees("1234567"))
        assertEquals("123", AnswerStyle.groupThrees("123"))
        assertEquals("2.814 749 767 11", AnswerStyle.groupThrees("2.81474976711"))
    }

    @Test fun aShortNumberIsNotBrokenUp() {
        assertEquals("42", AnswerStyle.groupThrees("42"))
        assertEquals("0.5", AnswerStyle.groupThrees("0.5"))
    }

    @Test fun theSignStaysAtTheFront() {
        assertEquals("-1 234 567", AnswerStyle.groupThrees("-1234567"))
    }

    @Test fun anExponentIsLeftAlone() {
        // the bug this test exists for: 1e20 being chopped into 1.000 000 e20
        assertEquals("1.000000e20", AnswerStyle.groupThrees("1.000000e20"))
    }

    @Test fun aSmallAnswerIsOneLine() {
        val lines = AnswerStyle.lines(1024.0, "1 024")
        assertFalse(lines.isTwoLines)
        assertEquals("1 024", lines.full)
        assertEquals("", lines.scientific)
    }

    @Test fun anOrdinaryAnswerIsOneLine() {
        assertFalse(AnswerStyle.lines(0.5, "0.5").isTwoLines)
        assertFalse(AnswerStyle.lines(42.0, "42").isTwoLines)
        assertFalse(AnswerStyle.lines(-7.25, "-7.25").isTwoLines)
    }

    @Test fun theCaseFromTheReference() {
        // 256^6 = 281474976710656
        val lines = AnswerStyle.lines(281474976710656.0, "281 474 976 710 656")
        assertTrue(lines.isTwoLines)
        assertEquals("281 474 976 710 656", lines.full)
        assertTrue("the scientific line reads '${lines.scientific}'", lines.scientific.startsWith("= "))
        assertTrue("the power is missing: '${lines.scientific}'", lines.scientific.contains("\u00D710\u00B9\u2074"))
        assertTrue("the mantissa is not grouped: '${lines.scientific}'", lines.scientific.contains(" "))
    }

    @Test fun aSmallAnswerGetsASecondLineToo() {
        // 0.000012345 is as hard to read as a big one
        val lines = AnswerStyle.lines(0.000012345, "0.000012345")
        assertTrue(lines.isTwoLines)
        assertTrue(lines.scientific.contains("\u00D710\u207B\u2075"))
    }

    @Test fun zeroAndInfinityStayOnOneLine() {
        assertFalse(AnswerStyle.lines(0.0, "0").isTwoLines)
        assertFalse(AnswerStyle.lines(Double.POSITIVE_INFINITY, "Infinity").isTwoLines)
        assertFalse(AnswerStyle.lines(Double.NaN, "NaN").isTwoLines)
    }

    @Test fun theMantissaStaysUnderTen() {
        // a value that rounds up to ten belongs one power higher
        for (v in listOf(9.999999999999999e9, 1.0e15, 9.5e6)) {
            val lines = AnswerStyle.lines(v, "x")
            if (!lines.isTwoLines) continue
            val mantissa = lines.scientific
                .removePrefix("= ")
                .substringBefore("\u00D7")
                .replace(" ", "")
                .toDouble()
            assertTrue("the mantissa of $v came out as $mantissa", Math.abs(mantissa) < 10.0)
        }
    }

    @Test fun aNegativeBigAnswerKeepsItsSign() {
        val lines = AnswerStyle.lines(-281474976710656.0, "-281 474 976 710 656")
        assertTrue(lines.isTwoLines)
        assertTrue("the sign is lost: '${lines.scientific}'", lines.scientific.contains("-"))
    }
}
