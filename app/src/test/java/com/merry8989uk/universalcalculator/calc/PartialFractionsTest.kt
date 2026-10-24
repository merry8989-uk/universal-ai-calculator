package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Splitting a rational function into partial fractions. */
class PartialFractionsTest {

    // Coefficients run low to high: [c, x, x^2, ...]
    private val x = doubleArrayOf(0.0, 1.0)

    /** (x - r) */
    private fun minusRoot(r: Double) = doubleArrayOf(-r, 1.0)

    /** p * q */
    private fun times(a: DoubleArray, b: DoubleArray): DoubleArray {
        val out = DoubleArray(a.size + b.size - 1)
        for (i in a.indices) for (j in b.indices) out[i + j] += a[i] * b[j]
        return out
    }

    private fun valueAt(p: DoubleArray, x: Double): Double =
        p.foldIndexed(0.0) { i, acc, c -> acc + c * Math.pow(x, i.toDouble()) }

    /** The decomposition must rebuild the original fraction at any other point. */
    private fun assertRebuilds(num: DoubleArray, den: DoubleArray) {
        val outcome = PartialFractions.decompose(num, den)
        assertTrue("expected a decomposition, got $outcome", outcome is PartialFractions.Outcome.Decomposed)
        val d = outcome as PartialFractions.Outcome.Decomposed
        for (probe in listOf(-7.3, -3.5, -0.4, 0.9, 2.7, 6.1)) {
            // skip anywhere the denominator vanishes
            if (abs(valueAt(den, probe)) < 1e-6) continue
            if (d.terms.any { abs(it.root - probe) < 1e-6 }) continue
            val original = valueAt(num, probe) / valueAt(den, probe)
            var rebuilt = valueAt(d.polynomial, probe)
            d.terms.forEach { t ->
                rebuilt += t.coefficient / Math.pow(probe - t.root, t.power.toDouble())
            }
            assertEquals("does not rebuild at x = $probe", original, rebuilt, 1e-6)
        }
    }

    @Test fun aSimpleSingleFactor() {
        val outcome = PartialFractions.decompose(doubleArrayOf(1.0), minusRoot(2.0))
        val d = outcome as PartialFractions.Outcome.Decomposed
        assertEquals(1, d.terms.size)
        assertEquals(1.0, d.terms[0].coefficient, 1e-9)
        assertEquals(2.0, d.terms[0].root, 1e-9)
        assertEquals(1, d.terms[0].power)
        assertRebuilds(doubleArrayOf(1.0), minusRoot(2.0))
    }

    @Test fun twoDistinctFactors() {
        // 1/(x^2 - 1) = -1/2 / (x+1) + 1/2 / (x-1)
        val den = times(minusRoot(-1.0), minusRoot(1.0))
        val d = PartialFractions.decompose(doubleArrayOf(1.0), den)
            as PartialFractions.Outcome.Decomposed
        assertEquals(2, d.terms.size)
        val byRoot = d.terms.associateBy { it.root }
        assertEquals(-0.5, byRoot[-1.0]!!.coefficient, 1e-9)
        assertEquals(0.5, byRoot[1.0]!!.coefficient, 1e-9)
        assertRebuilds(doubleArrayOf(1.0), den)
    }

    @Test fun aFactorOfXCounts() {
        // 1/(x^2 + x) = 1/x - 1/(x+1)
        val den = doubleArrayOf(0.0, 1.0, 1.0)
        val d = PartialFractions.decompose(doubleArrayOf(1.0), den)
            as PartialFractions.Outcome.Decomposed
        val byRoot = d.terms.associateBy { it.root }
        assertEquals(1.0, byRoot[0.0]!!.coefficient, 1e-9)
        assertEquals(-1.0, byRoot[-1.0]!!.coefficient, 1e-9)
        assertRebuilds(doubleArrayOf(1.0), den)
    }

    @Test fun aRepeatedFactor() {
        // 1/x^2 stays as it is: the power is carried, not split further.
        val d = PartialFractions.decompose(doubleArrayOf(1.0), doubleArrayOf(0.0, 0.0, 1.0))
            as PartialFractions.Outcome.Decomposed
        assertEquals(1, d.terms.size)
        assertEquals(1.0, d.terms[0].coefficient, 1e-9)
        assertEquals(2, d.terms[0].power)
    }

    @Test fun aRepeatedFactorAlongsideASimpleOne() {
        // 1 / (x (x+1)^2) has three terms: A/x + B/(x+1) + C/(x+1)^2
        val den = times(doubleArrayOf(0.0, 1.0), times(minusRoot(-1.0), minusRoot(-1.0)))
        val d = PartialFractions.decompose(doubleArrayOf(1.0), den)
            as PartialFractions.Outcome.Decomposed
        assertEquals(3, d.terms.size)
        assertRebuilds(doubleArrayOf(1.0), den)
    }

    @Test fun anImproperFractionKeepsItsPolynomialPart() {
        // x^3 / (x - 1) = x^2 + x + 1 + 1/(x - 1)
        val num = doubleArrayOf(0.0, 0.0, 0.0, 1.0)
        val den = minusRoot(1.0)
        val d = PartialFractions.decompose(num, den) as PartialFractions.Outcome.Decomposed
        assertEquals(3, Symbolic.degree(d.polynomial))
        assertEquals(1.0, d.polynomial[0], 1e-9)
        assertEquals(1.0, d.polynomial[1], 1e-9)
        assertEquals(1.0, d.polynomial[2], 1e-9)
        assertEquals(1, d.terms.size)
        assertEquals(1.0, d.terms[0].coefficient, 1e-9)
        assertRebuilds(num, den)
    }

    @Test fun aNonTrivialNumerator() {
        // (x + 5)/(x^2 + 3x + 2) = 4/(x+1) - 3/(x+2)
        val num = doubleArrayOf(5.0, 1.0)
        val den = doubleArrayOf(2.0, 3.0, 1.0)
        val d = PartialFractions.decompose(num, den) as PartialFractions.Outcome.Decomposed
        val byRoot = d.terms.associateBy { it.root }
        assertEquals(4.0, byRoot[-1.0]!!.coefficient, 1e-9)
        assertEquals(-3.0, byRoot[-2.0]!!.coefficient, 1e-9)
        assertRebuilds(num, den)
    }

    @Test fun aDenominatorWithNoRealRootsIsReported() {
        // x^2 + 1 cannot be split over the reals.
        val outcome = PartialFractions.decompose(doubleArrayOf(1.0), doubleArrayOf(1.0, 0.0, 1.0))
        assertTrue(outcome is PartialFractions.Outcome.NotPossible)
    }

    @Test fun aConstantDenominatorIsReported() {
        val outcome = PartialFractions.decompose(doubleArrayOf(1.0), doubleArrayOf(5.0))
        assertTrue(outcome is PartialFractions.Outcome.NotPossible)
    }

    @Test fun aZeroNumeratorGivesNothing() {
        val d = PartialFractions.decompose(doubleArrayOf(0.0), minusRoot(3.0))
            as PartialFractions.Outcome.Decomposed
        assertTrue(d.terms.isEmpty())
        assertTrue(Symbolic.isZero(d.polynomial))
    }

    @Test fun theTermsReadBackAsText() {
        val terms = listOf(
            PartialFractions.Term(1.0, 0.0, 1),
            PartialFractions.Term(-1.0, -1.0, 1),
        )
        assertEquals("1/(x-0) + -1/(x+1)", PartialFractions.format(terms))
    }

    @Test fun aPlainTermReadsAsAFraction() {
        val terms = listOf(PartialFractions.Term(1.0, 2.0, 1))
        assertEquals("1/(x-2)", PartialFractions.format(terms))
    }

    @Test fun aPowerIsShown() {
        val terms = listOf(PartialFractions.Term(3.0, 0.0, 2))
        assertEquals("3/(x-0)^2", PartialFractions.format(terms))
    }

    @Test fun theXIsSubstitutedOnlyWhenItStandsAlone() {
        // "exp" must not lose its x.
        assertEquals("(1/t)+2", Limits.substituteX("x+2", "(1/t)"))
        // the replacement keeps its own brackets, so this doubles up harmlessly
        assertEquals("exp((1/t))", Limits.substituteX("exp(x)", "(1/t)"))
        assertEquals("2*(1/t)", Limits.substituteX("2*x", "(1/t)"))
        assertEquals("max", Limits.substituteX("max", "(1/t)"))
    }
}
