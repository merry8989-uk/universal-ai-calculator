package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitsTest {

    private fun cat(name: String) = Units.categories.first { it.name == name }
    private fun conv(c: String, v: Double, from: String, to: String): Double {
        val category = cat(c)
        return Units.convert(v, category.unit(from), category.unit(to))
    }

    @Test fun length() {
        assertEquals(1000.0, conv("Length", 1.0, "km", "m"), 1e-6)
        assertEquals(100.0, conv("Length", 1.0, "m", "cm"), 1e-6)
        assertEquals(1609.344, conv("Length", 1.0, "mi", "m"), 1e-3)
        assertEquals(0.3048, conv("Length", 1.0, "ft", "m"), 1e-9)
        assertEquals(1852.0, conv("Length", 1.0, "nmi", "m"), 1e-6)
    }

    @Test fun temperature() {
        assertEquals(32.0, conv("Temperature", 0.0, "C", "F"), 1e-6)
        assertEquals(212.0, conv("Temperature", 100.0, "C", "F"), 1e-6)
        assertEquals(273.15, conv("Temperature", 0.0, "C", "K"), 1e-6)
        assertEquals(0.0, conv("Temperature", 32.0, "F", "C"), 1e-6)
        assertEquals(100.0, conv("Temperature", 373.15, "K", "C"), 1e-6)
    }

    @Test fun massVolumeTimeData() {
        assertEquals(1000.0, conv("Mass", 1.0, "kg", "g"), 1e-6)
        assertEquals(2.2046226218, conv("Mass", 1.0, "kg", "lb"), 1e-6)
        assertEquals(1000.0, conv("Volume", 1.0, "L", "mL"), 1e-6)
        assertEquals(3600.0, conv("Time", 1.0, "h", "s"), 1e-6)
        assertEquals(1024.0, conv("Data", 1.0, "KiB", "B"), 1e-6)
        assertEquals(1000.0, conv("Data", 1.0, "KB", "B"), 1e-6)
    }

    @Test fun everyUnitConvertsToItselfAndBack() {
        for (category in Units.categories) {
            assertTrue("${category.name} needs at least 2 units", category.units.size >= 2)
            for (u in category.units) {
                assertEquals("${category.name}/${u.symbol} identity",
                    1.0, Units.convert(1.0, u, u), 1e-9)
            }
            val a = category.units.first()
            val b = category.units.last()
            val v = 123.456
            assertEquals("${category.name} round trip",
                v, Units.convert(Units.convert(v, a, b), b, a), 1e-6)
        }
    }

    @Test fun symbolsAreUniqueWithinACategory() {
        for (category in Units.categories) {
            val symbols = category.units.map { it.symbol }
            assertEquals("duplicate symbols in ${category.name}", symbols.size, symbols.toSet().size)
        }
    }
}
