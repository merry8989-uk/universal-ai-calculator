package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The three India-first calculators, worked out by hand.
 *
 * A slab bill, an HRA exemption and land in units that are not square feet are
 * the three sums people here do on paper and get wrong. Each one is a pure
 * function, so the expected numbers below were computed by hand rather than by
 * the code they are testing.
 */
class IndiaCalculatorsTest {

    private fun calc(id: String, vararg inputs: Pair<String, Double>): Map<String, Double> {
        val f = FormulaLibrary.formulas.first { it.id == id }
        val values = f.inputs.associate { it.key to it.default }.toMutableMap()
        inputs.forEach { (k, v) -> values[k] = v }
        return f.compute(values).associate { it.label to it.value }
    }

    @Test fun theSlabBillChargesEachSlabOnce() {
        // 100 units at 3.5 = 350, nothing above the first slab, plus 50 fixed
        val small = calc("electricity_bill_in", "units" to 100.0)
        assertEquals(350.0, small["Energy charge"]!!, 0.001)
        assertEquals(400.0, small["Total"]!!, 0.001)

        // 250 units: 100 at 3.5 = 350, 150 at 4.5 = 675, so 1025 + 50
        val mid = calc("electricity_bill_in", "units" to 250.0)
        assertEquals(1025.0, mid["Energy charge"]!!, 0.001)
        assertEquals(1075.0, mid["Total"]!!, 0.001)

        // 350 units: 100 at 3.5 = 350, 200 at 4.5 = 900, the last 50 at 6.0 = 300
        val high = calc("electricity_bill_in", "units" to 350.0)
        assertEquals(1550.0, high["Energy charge"]!!, 0.001)
        assertEquals(1600.0, high["Total"]!!, 0.001)
        assertEquals(1600.0 / 350.0, high["Average rate"]!!, 0.001)
    }

    @Test fun theHraExemptionIsTheLeastOfThreeLimits() {
        // salary 60,000; HRA 24,000; rent 20,000; metro
        // limit 1 = 24,000
        // limit 2 = 20,000 - 6,000 = 14,000
        // limit 3 = 50% of 60,000 = 30,000
        val metro = calc("hra_exemption")
        assertEquals(14_000.0, metro["Exempt"]!!, 0.001)
        assertEquals(10_000.0, metro["Taxable HRA"]!!, 0.001)

        // the same rent in a non-metro city drops the third limit to 24,000,
        // so the second limit still decides
        val other = calc("hra_exemption", "metro" to 0.0)
        assertEquals(14_000.0, other["Exempt"]!!, 0.001)

        // a rent below 10% of salary leaves nothing exempt at all
        val cheap = calc("hra_exemption", "rent" to 5_000.0)
        assertEquals(0.0, cheap["Exempt"]!!, 0.001)
        assertEquals(24_000.0, cheap["Taxable HRA"]!!, 0.001)
    }

    @Test fun landConvertsOnTheStandardFactors() {
        // one acre is 43,560 sq ft, 40 guntha, 8 kanal and 1.6 bigha in UP
        val acre = calc("land_area", "value" to 1.0, "unit" to 6.0)
        assertEquals(43_560.0, acre["Square feet"]!!, 0.5)
        assertEquals(40.0, acre["Guntha"]!!, 0.001)
        assertEquals(8.0, acre["Kanal"]!!, 0.001)
        assertEquals(1.6, acre["Bigha (UP)"]!!, 0.001)
        assertEquals(4_046.86, acre["Square metres"]!!, 0.5)

        // and back: 5 kanal is 27,225 sq ft, which is one bigha in UP
        val kanal = calc("land_area", "value" to 5.0, "unit" to 5.0)
        assertEquals(27_225.0, kanal["Square feet"]!!, 0.5)
        assertEquals(1.0, kanal["Bigha (UP)"]!!, 0.001)
    }

    @Test fun everyNewCalculatorIsInTheLibrary() {
        val ids = FormulaLibrary.formulas.map { it.id }.toSet()
        for (id in listOf("electricity_bill_in", "hra_exemption", "land_area")) {
            assertTrue("$id is missing from the hub", id in ids)
        }
    }
}
