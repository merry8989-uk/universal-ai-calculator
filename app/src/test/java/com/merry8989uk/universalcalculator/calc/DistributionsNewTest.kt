package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chi-square, F, ANOVA and the incomplete gamma/beta helpers. */
class DistributionsNewTest {

    @Test fun chiSquareCriticalValues() {
        assertEquals(0.95, Distributions.chiSquareCdf(3.841, 1.0), 2e-3)
        assertEquals(0.95, Distributions.chiSquareCdf(5.991, 2.0), 2e-3)
        assertEquals(0.95, Distributions.chiSquareCdf(7.815, 3.0), 2e-3)
        assertEquals(0.95, Distributions.chiSquareCdf(9.488, 4.0), 2e-3)
        assertEquals(0.0, Distributions.chiSquareCdf(0.0, 3.0), 1e-12)
        assertEquals(0.0, Distributions.chiSquareCdf(-1.0, 3.0), 1e-12)
    }

    @Test fun chiSquareUpperTail() {
        assertEquals(0.00017, Distributions.chiSquareUpperTail(20.0, 3.0), 1e-4)
        assertEquals(0.05, Distributions.chiSquareUpperTail(3.841, 1.0), 2e-3)
    }

    @Test fun fCriticalValues() {
        assertEquals(0.05, Distributions.fUpperTail(5.1433, 2.0, 6.0), 1e-3)
        assertEquals(0.05, Distributions.fUpperTail(3.0984, 3.0, 20.0), 1e-3)
        assertEquals(0.5, Distributions.betai(0.5, 0.5, 0.5), 1e-9)
    }

    @Test fun incompleteGamma() {
        assertEquals(0.5, Distributions.gammp(1.0, Math.log(2.0)), 1e-9)   // P(1, ln2) = 1 - e^-ln2
        assertEquals(1.0, Distributions.gammp(1.0, 60.0), 1e-9)
        assertEquals(0.0, Distributions.gammp(2.0, 0.0), 1e-12)
    }

    @Test fun oneWayAnova() {
        val r = Distributions.oneWayAnova(
            listOf(
                listOf(1.0, 2.0, 3.0),
                listOf(4.0, 5.0, 6.0),
                listOf(7.0, 8.0, 9.0),
            ),
        )
        assertEquals(27.0, r["F statistic"]!!, 1e-6)
        assertEquals(2.0, r["df between"]!!, 1e-9)
        assertEquals(6.0, r["df within"]!!, 1e-9)
        assertEquals(54.0, r["SS between"]!!, 1e-9)
        assertEquals(6.0, r["SS within"]!!, 1e-9)
        assertEquals(0.001, r["p value"]!!, 1e-3)
    }

    @Test fun goodnessOfFit() {
        val r = Distributions.chiSquareGoodnessOfFit(
            listOf(10.0, 20.0, 30.0, 40.0),
            listOf(25.0, 25.0, 25.0, 25.0),
        )
        assertEquals(20.0, r["Chi-square"]!!, 1e-9)
        assertEquals(3.0, r["Degrees of freedom"]!!, 1e-9)
        assertEquals(0.00017, r["p value"]!!, 1e-4)
    }

    @Test fun independence() {
        val r = Distributions.chiSquareIndependence(
            listOf(listOf(10.0, 20.0), listOf(30.0, 40.0)),
        )
        assertEquals(0.793651, r["Chi-square"]!!, 1e-6)
        assertEquals(1.0, r["Degrees of freedom"]!!, 1e-9)
        assertEquals(0.373, r["p value"]!!, 5e-3)
    }

    @Test fun badInputIsRejected() {
        for (f in listOf<() -> Any>(
            { Distributions.chiSquareCdf(1.0, 0.0) },
            { Distributions.fUpperTail(1.0, 0.0, 2.0) },
            { Distributions.oneWayAnova(listOf(listOf(1.0))) },
            { Distributions.chiSquareGoodnessOfFit(listOf(1.0), listOf(1.0, 2.0)) },
            { Distributions.chiSquareIndependence(listOf(listOf(1.0, 2.0))) },
            { Distributions.gammp(0.0, 1.0) },
        )) {
            try { f(); throw AssertionError("expected failure") }
            catch (e: Evaluator.EvalException) { /* expected */ }
        }
    }
}
