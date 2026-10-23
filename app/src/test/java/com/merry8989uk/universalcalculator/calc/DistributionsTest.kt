package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistributionsTest {

    @Test fun errorFunction() {
        assertEquals(0.0, Distributions.erf(0.0), 1e-9)
        assertEquals(0.8427008, Distributions.erf(1.0), 1e-6)
        assertEquals(-0.8427008, Distributions.erf(-1.0), 1e-6)
    }

    @Test fun normal() {
        assertEquals(0.3989423, Distributions.normalPdf(0.0), 1e-6)
        assertEquals(0.5, Distributions.normalCdf(0.0), 1e-9)
        assertEquals(0.9750021, Distributions.normalCdf(1.96), 1e-4)
        assertEquals(0.0249979, Distributions.normalCdf(-1.96), 1e-4)
        assertEquals(1.959964, Distributions.normalInv(0.975), 1e-3)
        assertEquals(0.0, Distributions.normalInv(0.5), 1e-6)
    }

    @Test fun studentT() {
        assertEquals(0.389108, Distributions.tPdf(0.0, 10.0), 1e-5)
        assertEquals(0.5, Distributions.tCdf(0.0, 10.0), 1e-6)
        assertEquals(0.975, Distributions.tCdf(2.228, 10.0), 2e-3)
        assertEquals(2.228, Distributions.tInv(0.975, 10.0), 5e-3)
        assertEquals(1.812, Distributions.tInv(0.95, 10.0), 1e-2)
    }

    @Test fun binomial() {
        assertEquals(0.3125, Distributions.binomialPmf(2, 5, 0.5), 1e-9)
        assertEquals(0.5, Distributions.binomialCdf(2, 5, 0.5), 1e-9)
        assertEquals(1.0, Distributions.binomialCdf(5, 5, 0.5), 1e-9)
        assertEquals(0.0, Distributions.binomialPmf(6, 5, 0.5), 1e-9)
    }

    @Test fun poisson() {
        assertEquals(0.2706706, Distributions.poissonPmf(2, 2.0), 1e-6)
        assertEquals(0.1353353, Distributions.poissonCdf(0, 2.0), 1e-6)
    }

    @Test fun oneSampleTTest() {
        val r = Distributions.tTestOneSample(listOf(1.0, 2.0, 3.0, 4.0, 5.0), 3.0)
        assertEquals(0.0, r["t statistic"]!!, 1e-6)
        assertEquals(1.0, r["p value (two-tailed)"]!!, 1e-3)
        assertEquals(4.0, r["Degrees of freedom"]!!, 1e-9)
    }

    @Test fun twoSampleTTest() {
        val r = Distributions.tTestTwoSample(
            listOf(1.0, 2.0, 3.0, 4.0, 5.0),
            listOf(1.0, 2.0, 3.0, 4.0, 5.0),
        )
        assertEquals(0.0, r["t statistic"]!!, 1e-6)
        assertEquals(1.0, r["p value (two-tailed)"]!!, 1e-3)
    }

    @Test fun confidenceInterval() {
        val r = Distributions.confidenceIntervalMean(listOf(1.0, 2.0, 3.0, 4.0, 5.0), 0.95)
        assertEquals(3.0, r["Sample mean"]!!, 1e-9)
        assertEquals(1.0368, r["Lower bound"]!!, 1e-2)
        assertEquals(4.9632, r["Upper bound"]!!, 1e-2)
        assertTrue(r["Upper bound"]!! > r["Lower bound"]!!)
    }

    @Test fun zPValue() {
        assertEquals(0.05, Distributions.pValueFromZ(1.96), 1e-3)
        assertEquals(1.0, Distributions.pValueFromZ(0.0), 1e-6)
    }

    @Test fun badInputIsRejected() {
        for (f in listOf<() -> Any>(
            { Distributions.normalPdf(0.0, 0.0, 0.0) },
            { Distributions.tCdf(0.0, 0.0) },
            { Distributions.normalInv(1.5) },
            { Distributions.confidenceIntervalMean(listOf(1.0)) },
        )) {
            try { f(); throw AssertionError("expected failure") }
            catch (e: Evaluator.EvalException) { /* expected */ }
        }
    }
}
