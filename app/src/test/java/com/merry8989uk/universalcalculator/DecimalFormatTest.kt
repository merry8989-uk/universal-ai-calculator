package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.DecimalFormat
import com.merry8989uk.universalcalculator.calc.Format
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Shorthand, so the expectations below stay readable. */
private typealias DF = DecimalFormat

/**
 * The five ways a number can be written out.
 *
 * Each mode is checked against a value worked out by hand, because a wrong
 * mantissa or a missing prefix is the sort of thing that looks fine and is not.
 */
class DecimalFormatTest {

    // ---- Normal, unchanged ----

    @Test fun normalTrimsTheNoise() {
        assertEquals("3.5", Format.number(3.5, DF.NORMAL))
        assertEquals("1234.5678", Format.number(1234.5678, DF.NORMAL))
        assertEquals("0", Format.number(0.0, DF.NORMAL))
    }

    @Test fun normalIsTheDefault() {
        assertEquals(Format.number(2.5), Format.number(2.5, DF.NORMAL))
    }

    // ---- Fixed ----

    @Test fun fixedKeepsExactlyTheChosenPlaces() {
        assertEquals("1234.57", Format.number(1234.5678, DF.FIXED, 2))
        assertEquals("1234.5678", Format.number(1234.5678, DF.FIXED, 4))
        assertEquals("1235", Format.number(1234.5678, DF.FIXED, 0))
    }

    @Test fun fixedPadsRatherThanTrims() {
        assertEquals("3.10", Format.number(3.1, DF.FIXED, 2))
        assertEquals("0.00", Format.number(0.0, DF.FIXED, 2))
    }

    // ---- Scientific ----

    @Test fun scientificPutsOneDigitBeforeThePoint() {
        assertEquals("1.2346\u00D710\u00B3", Format.number(1234.5678, DF.SCIENTIFIC, 4))
        assertEquals("1.234\u00D710\u207B\u2074", Format.number(0.0001234, DF.SCIENTIFIC, 4))
        assertEquals("5\u00D710\u2070", Format.number(5.0, DF.SCIENTIFIC, 4))
    }

    @Test fun scientificCarriesWhenTheMantissaRoundsUp() {
        // 9.99999 to four places is ten, which is one times ten to the one
        assertEquals("1\u00D710\u00B9", Format.number(9.99999, DF.SCIENTIFIC, 4))
    }

    // ---- Engineering ----

    @Test fun engineeringPutsTheExponentOnAMultipleOfThree() {
        assertEquals("1.2346\u00D710\u00B3", Format.number(1234.5678, DF.ENGINEERING, 4))
        assertEquals("12.3457\u00D710\u2076", Format.number(12345678.9, DF.ENGINEERING, 4))
        assertEquals("123.4\u00D710\u207B\u2076", Format.number(0.0001234, DF.ENGINEERING, 4))
    }

    @Test fun engineeringExponentIsAlwaysAMultipleOfThree() {
        for (v in listOf(1.0, 12.0, 123.0, 1234.0, 12345.0, 123456.0, 1234567.0)) {
            val text = Format.number(v, DF.ENGINEERING, 4)
            val power = text.substringAfter("\u00D710")
            assertTrue("$v produced no power of ten: $text", power.isNotEmpty())
        }
    }

    // ---- Engineering SI ----

    @Test fun engineeringSiUsesThePrefixInsteadOfThePower() {
        assertEquals("1.2346k", Format.number(1234.5678, DF.ENGINEERING_SI, 4))
        assertEquals("12.3457M", Format.number(12345678.9, DF.ENGINEERING_SI, 4))
        assertEquals("123.4\u00B5", Format.number(0.0001234, DF.ENGINEERING_SI, 4))
        assertEquals("1.5G", Format.number(1_500_000_000.0, DF.ENGINEERING_SI, 4))
        assertEquals("2n", Format.number(2e-9, DF.ENGINEERING_SI, 4))
    }

    @Test fun engineeringSiKeepsSmallNumbersReadable() {
        assertEquals("999.999k", Format.number(999999.0, DF.ENGINEERING_SI, 4))
        assertEquals("1k", Format.number(1000.0, DF.ENGINEERING_SI, 4))
    }

    // ---- the awkward inputs ----

    @Test fun theAwkwardNumbersAreStillHandled() {
        for (mode in DF.all) {
            assertEquals("NaN", Format.number(Double.NaN, mode, 4))
            assertEquals("Infinity", Format.number(Double.POSITIVE_INFINITY, mode, 4))
            assertEquals("-Infinity", Format.number(Double.NEGATIVE_INFINITY, mode, 4))
        }
    }

    @Test fun negativeNumbersKeepTheirSign() {
        assertEquals("-1234.57", Format.number(-1234.5678, DF.FIXED, 2))
        assertEquals("-1.2346k", Format.number(-1234.5678, DF.ENGINEERING_SI, 4))
        assertTrue(Format.number(-1234.5678, DF.SCIENTIFIC, 4).startsWith("-1.2346"))
    }

    @Test fun theDigitCountIsKeptWithinReason() {
        // 0 to 16 is what the slider offers; anything else is clamped, not crashed on
        assertEquals("5", Format.number(5.0, DF.FIXED, 0))
        assertTrue(Format.number(5.0, DF.FIXED, 99).startsWith("5.0"))
        assertTrue(Format.number(5.0, DF.FIXED, -3).isNotEmpty())
    }

    // ---- the settings round trip ----

    @Test fun everyModeHasALabel() {
        val blank = DF.all.filter { it.label.isBlank() }
        assertTrue("modes with no name: $blank", blank.isEmpty())
        assertEquals(5, DF.all.size)
    }

    @Test fun theStoredNameIsReadTolerantly() {
        assertEquals(DF.FIXED, DF.of("fixed"))
        assertEquals(DF.SCIENTIFIC, DF.of("Scientific"))
        assertEquals(DF.ENGINEERING, DF.of("ENGINEERING"))
        assertEquals(DF.ENGINEERING_SI, DF.of("Engineering SI"))
        assertEquals(DF.ENGINEERING_SI, DF.of("eng_si"))
        assertEquals(DF.NORMAL, DF.of("nonsense"))
        assertEquals(DF.NORMAL, DF.of(""))
    }

    @Test fun everyModeSurvivesTheRoundTripThroughItsName() {
        for (mode in DF.all) {
            assertEquals(mode, DF.of(mode.name))
        }
    }

    @Test fun groupingLeavesTheWrittenFormsAlone() {
        // the grouping separators must not be pushed into a mantissa or a prefix
        val sci = Format.number(12345678.9, DF.SCIENTIFIC, 4)
        assertEquals(sci, Format.grouped(sci, "Indian", -1))
        val si = Format.number(12345678.9, DF.ENGINEERING_SI, 4)
        assertEquals(si, Format.grouped(si, "Indian", -1))
    }
}
