package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConvertersTest {

    @Test fun romanNumerals() {
        assertEquals("MCMXCIV", Converters.toRoman(1994))
        assertEquals("IV", Converters.toRoman(4))
        assertEquals("IX", Converters.toRoman(9))
        assertEquals("MMXXVI", Converters.toRoman(2026))
        assertEquals(1994, Converters.fromRoman("MCMXCIV"))
        for (n in listOf(1, 4, 9, 40, 90, 400, 900, 1994, 2026, 3999)) {
            assertEquals(n, Converters.fromRoman(Converters.toRoman(n)))
        }
    }

    @Test fun romanRejectsBadInput() {
        for (bad in listOf("", "ABC", "IIIIII")) {
            try {
                Converters.fromRoman(bad)
                if (bad.isNotEmpty()) throw AssertionError("expected failure for '$bad'")
            } catch (e: Evaluator.EvalException) { /* expected */ }
        }
        try { Converters.toRoman(0); throw AssertionError("0 should fail") }
        catch (e: Evaluator.EvalException) { /* expected */ }
    }

    @Test fun words() {
        assertEquals("zero", Converters.numberToWords(0))
        assertEquals("one lakh twenty-five thousand", Converters.numberToWords(125000))
        assertEquals("ten lakh", Converters.numberToWords(1000000))
        assertEquals("one hundred", Converters.numberToWords(100))
        assertTrue(Converters.numberToWords(-5).startsWith("minus"))
    }

    @Test fun textEncodings() {
        assertEquals("01001000 01101001", Converters.textToBinary("Hi"))
        assertEquals("Hi", Converters.binaryToText("01001000 01101001"))
        assertEquals("48 69", Converters.textToHex("Hi"))
        assertEquals("Hi", Converters.hexToText("48 69"))
        assertEquals("72 105", Converters.textToAscii("Hi"))
        assertEquals("Hi", Converters.asciiToText("72 105"))
        val sample = "Nexus Calc 123 !"
        assertEquals(sample, Converters.binaryToText(Converters.textToBinary(sample)))
        assertEquals(sample, Converters.hexToText(Converters.textToHex(sample)))
        assertEquals(sample, Converters.asciiToText(Converters.textToAscii(sample)))
    }

    @Test fun colours() {
        assertEquals("#1565C0", Converters.rgbToHex(21, 101, 192))
        val (r, g, b) = Converters.hexToRgb("#1565C0")
        assertEquals(21, r); assertEquals(101, g); assertEquals(192, b)
        assertEquals("#1565C0", Converters.rgbToHex(r, g, b))
    }

    @Test fun timestampsRoundTrip() {
        val stamp = Converters.dateToTimestamp("2026-10-23")
        assertTrue(Converters.timestampToDate(stamp).startsWith("2026-10-23"))
    }
}
