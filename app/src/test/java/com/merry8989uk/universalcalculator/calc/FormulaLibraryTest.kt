package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulaLibraryTest {

    private fun run(id: String): List<OutputSpec> {
        val f = FormulaLibrary.formulas.firstOrNull { it.id == id }
            ?: throw AssertionError("no formula with id '$id'")
        return f.compute(f.inputs.associate { it.key to it.default })
    }

    private fun value(id: String, index: Int = 0) = run(id)[index].value

    @Test fun idsAreUnique() {
        val ids = FormulaLibrary.formulas.map { it.id }
        assertEquals("duplicate formula ids", ids.size, ids.toSet().size)
    }

    @Test fun everyFormulaIsInADeclaredCategory() {
        for (f in FormulaLibrary.formulas) {
            assertTrue("'${f.id}' has category '${f.category}' which is not listed",
                f.category in FormulaLibrary.categories)
        }
    }

    @Test fun everyCategoryHasFormulas() {
        for (c in FormulaLibrary.categories) {
            assertTrue("category '$c' has no formulas", FormulaLibrary.byCategory(c).isNotEmpty())
        }
    }

    /** The important one: every formula must run with its defaults. */
    @Test fun everyFormulaRunsWithDefaults() {
        val broken = mutableListOf<String>()
        val notFinite = mutableListOf<String>()
        val nan = mutableListOf<String>()
        for (f in FormulaLibrary.formulas) {
            val defaults = f.inputs.associate { it.key to it.default }
            try {
                val out = f.compute(defaults)
                if (out.isEmpty()) broken.add("${f.id}: returned no outputs")
                for (o in out) {
                    if (o.value.isNaN()) nan.add("${f.id}: ${o.label}")
                    else if (o.value.isInfinite()) notFinite.add("${f.id}: ${o.label}")
                }
            } catch (e: Exception) {
                broken.add("${f.id}: ${e::class.simpleName}: ${e.message}")
            }
        }
        if (broken.isNotEmpty()) println("THREW:\n" + broken.joinToString("\n"))
        assertTrue("formulas that threw:\n" + broken.joinToString("\n"), broken.isEmpty())
        assertTrue("non-finite outputs:\n" + notFinite.joinToString("\n"), notFinite.isEmpty())
        assertTrue("NaN outputs:\n" + nan.joinToString("\n"), nan.isEmpty())
    }

    @Test fun inputsHaveLabelsAndUniqueKeys() {
        for (f in FormulaLibrary.formulas) {
            val keys = f.inputs.map { it.key }
            assertEquals("duplicate input keys in ${f.id}", keys.size, keys.toSet().size)
            for (i in f.inputs) assertTrue("${f.id}: blank label", i.label.isNotBlank())
        }
    }

    // ---- spot checks against known values ----

    @Test fun everyday() {
        assertEquals(30.0, value("percent_of"), 1e-9)          // 15% of 200
        assertEquals(12.5, value("what_percent"), 1e-9)        // 25 of 200
        assertEquals(25.0, value("percent_change"), 1e-9)      // 100 -> 125
        assertEquals(300.0, value("discount"), 1e-9)           // save 20% of 1500
        assertEquals(1100.0, value("tip_split", 1), 1e-9)      // 1000 + 10% tip
        assertEquals(180.0, value("gst_add"), 1e-9)            // 18% GST on 1000
        assertEquals(1000.0, value("gst_remove"), 1e-6)        // back to the base
        assertEquals(200.0, value("profit"), 1e-9)             // 800 -> 1000
    }

    @Test fun finance() {
        assertEquals(40000.0, value("simple_interest"), 1e-6)  // 1L @8% for 5y
        assertEquals(10379.18, value("emi"), 0.5)             // 5L @9% for 5y
        assertEquals(50.0, value("roi", 1), 1e-6)              // 1L -> 1.5L
        assertEquals(500.0, value("break_even"), 1e-6)         // 1L / (500-300)
    }

    @Test fun health() {
        assertEquals(24.2214, value("bmi"), 1e-3)              // 70 kg, 170 cm
        assertEquals(1.8181, value("bsa"), 1e-3)               // Mosteller
        assertEquals(97.2222, value("creatinine_clearance"), 1e-3) // 40y, 70kg, Cr 1.0, male
    }

    @Test fun geometry() {
        assertEquals(78.5398, value("circle"), 1e-3)           // r = 5
        assertEquals(24.0, value("rectangle"), 1e-9)           // 4 x 6
        assertEquals(6.0, value("triangle_heron"), 1e-6)       // 3,4,5
        assertEquals(5.0, value("pythagoras"), 1e-9)
        assertEquals(523.5988, value("sphere"), 1e-3)          // r = 5
    }

    @Test fun physics() {
        assertEquals(2.0, value("ohms_law"), 1e-9)             // 12V / 6 ohm
        assertEquals(100.0, value("kinetic_energy"), 1e-9)     // 2 kg, 10 m/s
        assertEquals(196.2, value("potential_energy"), 1e-6)   // 2 kg, 10 m
        assertEquals(50.0, value("work"), 1e-9)                // 10 N x 5 m
        assertEquals(50.0, value("power"), 1e-9)               // 500 J / 10 s
        assertEquals(5.0, value("density"), 1e-9)              // 10 kg / 2 m3
        assertEquals(50.0, value("pressure"), 1e-9)            // 100 N / 2 m2
    }

    @Test fun mathAndEconomics() {
        assertEquals(6.0, value("gcd_lcm"), 1e-9)
        assertEquals(10.0, value("ncr"), 1e-9)
        assertEquals(2.0, value("quadratic", 1), 1e-9)         // root of x^2-3x+2
        assertEquals(1650.0, value("gdp_expenditure"), 1e-6)   // 1000+400+300+200-250
        assertEquals(10.0, value("money_multiplier"), 1e-9)    // 10% reserve
        assertEquals(0.8, value("mpc_mps"), 1e-9)
        assertEquals(5.0, value("spending_multiplier"), 1e-9)  // MPC 0.8
        assertEquals(-1.2222, value("elasticity_demand"), 1e-3)
    }

    @Test fun engineeringAndConstruction() {
        assertEquals(6.469, value("three_phase"), 1e-3)        // 415V, 10A, pf 0.9
        assertEquals(150.0, value("led_resistor"), 1e-9)       // (5-2) / 0.02
        assertEquals(8.0, value("voltage_divider"), 1e-6)      // 12 x 2000/3000
        assertEquals(16.0, value("aspect_ratio", 1), 1e-9)     // 1920x1080 -> 16:9
        assertEquals(2.25, value("concrete"), 1e-6)            // 5 x 3 x 0.15
        assertEquals(1.5, value("far"), 1e-9)                  // 300 / 200
    }
}
