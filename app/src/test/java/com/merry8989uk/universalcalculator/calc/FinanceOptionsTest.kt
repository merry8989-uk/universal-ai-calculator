package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test

/** The finance and option-pricing calculators taken from the formulae handbooks. */
class FinanceOptionsTest {

    private fun run(id: String): List<OutputSpec> {
        val f = FormulaLibrary.formulas.firstOrNull { it.id == id }
            ?: throw AssertionError("no formula with id '$id'")
        return f.compute(f.inputs.associate { it.key to it.default })
    }

    private fun value(id: String, index: Int = 0) = run(id)[index].value

    @Test fun interestAndCapital() {
        assertEquals(12.6825, value("effective_apr"), 1e-3)     // 12% compounded monthly
        assertEquals(12.7497, value("effective_apr", 1), 1e-3)  // continuous
        assertEquals(9.6, value("wacc"), 1e-6)
        assertEquals(83.3333, value("dividend_discount"), 1e-3)
    }

    @Test fun annuities() {
        assertEquals(7586.7958, value("sinking_fund"), 1e-3)
        assertEquals(7721.7349, value("annuity_pv"), 1e-3)
        assertEquals(12577.8925, value("annuity_fv"), 1e-3)
        assertEquals(20000.0, value("perpetuity_pv"), 1e-6)     // 1000 / 5%
    }

    @Test fun indexNumbers() {
        assertEquals(125.0, value("price_index"), 1e-9)
        assertEquals(125.0, value("quantity_index"), 1e-9)
        assertEquals(4000.0, value("real_value_index"), 1e-6)   // 5000 / 125 x 100
        assertEquals(113.6842, value("laspeyres_index"), 1e-3)
    }

    @Test fun macroFormulas() {
        assertEquals(42000.0, value("consumption_function"), 1e-6)
        assertEquals(8000.0, value("consumption_function", 1), 1e-6)
        assertEquals(2980000.0, value("gnp_gni"), 1e-6)
        assertEquals(2995000.0, value("gnp_gni", 1), 1e-6)
        assertEquals(0.5, value("sharpe_ratio"), 1e-9)
    }

    @Test fun blackScholesMatchesTheTextbook() {
        // S = K = 100, r = 5%, sigma = 20%, T = 1
        assertEquals(10.4506, value("black_scholes"), 1e-3)      // call
        assertEquals(5.5735, value("black_scholes", 1), 1e-3)    // put
        assertEquals(0.35, value("black_scholes", 2), 1e-6)      // d1
        assertEquals(0.15, value("black_scholes", 3), 1e-6)      // d2
        assertEquals(0.0, value("black_scholes", 4), 1e-9)       // parity holds
    }

    @Test fun greeks() {
        assertEquals(0.6368, value("option_greeks"), 1e-4)       // delta
        assertEquals(0.018762, value("option_greeks", 1), 1e-6)  // gamma
        assertEquals(37.5240, value("option_greeks", 2), 1e-3)   // vega
    }

    @Test fun parityAndTrees() {
        assertEquals(5.5729, value("put_call_parity"), 1e-3)
        assertEquals(0.0, value("put_call_parity", 2), 1e-9)
        assertEquals(1.2214, value("binomial_option"), 1e-4)     // u
        assertEquals(0.5775, value("binomial_option", 2), 1e-4)  // p
        assertEquals(12.1623, value("binomial_option", 3), 1e-3) // call
    }

    @Test fun forwardsAndBonds() {
        assertEquals(103.0455, value("forward_price"), 1e-4)
        assertEquals(1077.9458, value("bond_price"), 1e-3)
    }

    @Test fun everyNewFormulaIsInADeclaredCategory() {
        for (id in listOf(
            "effective_apr", "wacc", "dividend_discount", "sinking_fund", "annuity_pv",
            "annuity_fv", "perpetuity_pv", "price_index", "quantity_index", "real_value_index",
            "laspeyres_index", "consumption_function", "gnp_gni", "sharpe_ratio",
            "black_scholes", "option_greeks", "put_call_parity", "binomial_option",
            "forward_price", "bond_price",
        )) {
            val f = FormulaLibrary.formulas.first { it.id == id }
            assert(f.category in FormulaLibrary.categories) { "$id has category ${f.category}" }
        }
    }
}
