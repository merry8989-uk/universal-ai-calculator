package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test

/** The number-format and decimal-places settings. */
class FormatGroupingTest {

    @Test fun indianGrouping() {
        assertEquals("30,00,000.5", Format.grouped("3000000.5", "Indian"))
        assertEquals("1,00,000", Format.grouped("100000", "Indian"))
        assertEquals("12,34,56,789", Format.grouped("123456789", "Indian"))
        assertEquals("999", Format.grouped("999", "Indian"))
        assertEquals("1,000", Format.grouped("1000", "Indian"))
    }

    @Test fun internationalGrouping() {
        assertEquals("3,000,000.5", Format.grouped("3000000.5", "International"))
        assertEquals("12,345,678", Format.grouped("12345678", "International"))
        assertEquals("1,000", Format.grouped("1000", "International"))
    }

    @Test fun europeanGrouping() {
        assertEquals("3.000.000,5", Format.grouped("3000000.5", "European"))
        assertEquals("1.000", Format.grouped("1000", "European"))
    }

    @Test fun noneLeavesItAlone() {
        assertEquals("3000000.5", Format.grouped("3000000.5", "None"))
    }

    @Test fun decimalPlaces() {
        assertEquals("3.14", Format.grouped("3.14159", "None", 2))
        assertEquals("3.142", Format.grouped("3.14159", "None", 3))
        assertEquals("3", Format.grouped("3.14159", "None", 0))
        assertEquals("3,000,000.50", Format.grouped("3000000.5", "International", 2))
        // automatic keeps what the value needs
        assertEquals("3.14159", Format.grouped("3.14159", "None", -1))
    }

    @Test fun negativesAndSmallNumbers() {
        assertEquals("-30,00,000.5", Format.grouped("-3000000.5", "Indian"))
        assertEquals("-3.14", Format.grouped("-3.14159", "None", 2))
        assertEquals("0.5", Format.grouped("0.5", "Indian"))
    }

    @Test fun nonNumbersAreLeftAlone() {
        assertEquals("error", Format.grouped("error", "Indian"))
        assertEquals("NaN", Format.grouped("NaN", "Indian"))
        assertEquals("2+3", Format.grouped("2+3", "Indian"))
    }
}
