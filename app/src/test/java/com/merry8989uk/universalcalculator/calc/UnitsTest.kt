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

    @Test fun fuelEconomyHandlesTheReciprocalUnit() {
        // L/100km is inversely related to km/L, not a straight ratio
        assertEquals(100.0, conv("Fuel economy", 1.0, "L/100km", "km/L"), 1e-9)
        assertEquals(20.0, conv("Fuel economy", 5.0, "L/100km", "km/L"), 1e-9)
        assertEquals(20.0, conv("Fuel economy", 5.0, "km/L", "L/100km"), 1e-9)
        assertEquals(8.0, conv("Fuel economy", 12.5, "L/100km", "km/L"), 1e-9)
    }

    @Test fun electricalQuantitiesAreSeparate() {
        assertEquals(1000.0, conv("Current", 1.0, "A", "mA"), 1e-9)
        assertEquals(1000.0, conv("Voltage", 1.0, "kV", "V"), 1e-9)
        assertEquals(1000.0, conv("Resistance", 1.0, "kohm", "ohm"), 1e-9)
        assertEquals(1e6, conv("Capacitance", 1.0, "uF", "pF"), 1e-3)
        assertEquals(1000.0, conv("Charge", 1.0, "Ah", "mAh"), 1e-9)
        // the categories must be distinct, so volts can never become ohms
        val names = Units.categories.map { it.name }
        assertTrue("Current" in names && "Voltage" in names && "Resistance" in names)
        assertTrue("Electric" !in names)
    }

    @Test fun radiationAndDose() {
        assertEquals(3.7e10, conv("Radioactivity", 1.0, "Ci", "Bq"), 1.0)
        assertEquals(100.0, conv("Absorbed dose", 1.0, "Gy", "rad"), 1e-9)
        assertEquals(100.0, conv("Equivalent dose", 1.0, "Sv", "rem"), 1e-9)
        assertEquals(5000.0, conv("Equivalent dose", 50.0, "mSv", "mrem"), 1e-6)
    }

    @Test fun levelsAndAngles() {
        assertEquals(10.0, conv("Sound level", 1.0, "bel", "dB"), 1e-9)
        assertEquals(0.1, conv("Sound level", 1.0, "dB", "bel"), 1e-9)
        assertEquals(Math.PI, conv("Angle", 180.0, "deg", "rad"), 1e-9)
        assertEquals(90.0, conv("Angle", 1.0, "quadrant", "deg"), 1e-9)
    }

    @Test fun viscosityAndConcentration() {
        assertEquals(100.0, conv("Dynamic viscosity", 1.0, "P", "cP"), 1e-9)
        assertEquals(1e-6, conv("Kinematic viscosity", 1.0, "cSt", "m2/s"), 1e-18)
        assertEquals(10.0, conv("Concentration (mass)", 1.0, "g/dL", "g/L"), 1e-9)
        assertEquals(1000.0, conv("Concentration (molar)", 1.0, "mol/L", "mmol/L"), 1e-9)
    }

    @Test fun converterHasAHealthyNumberOfUnits() {
        assertTrue("expected 500+ units, found ${Units.unitCount}", Units.unitCount >= 500)
        assertTrue("expected 40+ categories", Units.categories.size >= 40)
    }
}
