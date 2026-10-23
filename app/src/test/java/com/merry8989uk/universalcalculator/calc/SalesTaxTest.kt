package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class SalesTaxTest {

    private fun run(id: String) = FormulaLibrary.formulas.first { it.id == id }
        .let { it.compute(it.inputs.associate { i -> i.key to i.default }) }

    @Test fun taxOnAPrice() {
        val r = run("sales_tax")
        assertEquals(180.0, r[0].value, 1e-9)     // 18% of 1000
        assertEquals(1180.0, r[1].value, 1e-9)    // total
        assertEquals(1000.0, r[2].value, 1e-6)    // base backed out
        assertEquals(180.0, r[3].value, 1e-6)
    }
}
