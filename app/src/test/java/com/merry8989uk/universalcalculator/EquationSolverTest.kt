package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.EquationSolver
import com.merry8989uk.universalcalculator.calc.LinearSolution
import com.merry8989uk.universalcalculator.calc.ResultDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The equation solver.
 *
 * Every root and every solution in here was worked out by hand first, because a
 * root finder that is subtly wrong still returns a plausible-looking list.
 */
class EquationSolverTest {

    private fun rootsOf(vararg c: Double) =
        EquationSolver.polynomial(c.toList().toDoubleArray())!!

    // ---------------- polynomial ----------------

    @Test fun aLinearEquation() {
        // 2x - 6 = 0
        val s = rootsOf(2.0, -6.0)
        assertEquals(1, s.degree)
        assertEquals("Linear", EquationSolver.degreeName(1))
        assertEquals(1, s.roots.size)
        assertEquals(3.0, s.roots[0].re, 1e-9)
        assertTrue(s.allReal)
    }

    @Test fun aQuadraticWithTwoRealRoots() {
        // x^2 - 4 = 0
        val s = rootsOf(1.0, 0.0, -4.0)
        assertEquals(2, s.degree)
        assertEquals(listOf(-2.0, 2.0), s.realRoots)
        assertEquals(0.0, s.sumOfRoots!!, 1e-9)
        assertEquals(-4.0, s.productOfRoots!!, 1e-9)
    }

    @Test fun aQuadraticWithNoRealRoots() {
        // x^2 + 1 = 0
        val s = rootsOf(1.0, 0.0, 1.0)
        assertFalse(s.allReal)
        assertEquals(0, s.realRoots.size)
        assertEquals(2, s.roots.size)
        s.roots.forEach { assertEquals(1.0, Math.abs(it.im), 1e-9) }
    }

    @Test fun aQuadraticWithARepeatedRoot() {
        // x^2 - 4x + 4 = (x-2)^2
        val s = rootsOf(1.0, -4.0, 4.0)
        assertEquals(2, s.roots.size)
        s.roots.forEach { assertEquals(2.0, it.re, 1e-6) }
        assertEquals(4.0, s.sumOfRoots!!, 1e-9)
        assertEquals(4.0, s.productOfRoots!!, 1e-9)
    }

    @Test fun aCubic() {
        // (x-1)(x-2)(x-3) = x^3 - 6x^2 + 11x - 6
        val s = rootsOf(1.0, -6.0, 11.0, -6.0)
        assertEquals(3, s.degree)
        assertEquals("Cubic", EquationSolver.degreeName(3))
        assertEquals(listOf(1.0, 2.0, 3.0), s.realRoots)
        assertEquals(6.0, s.sumOfRoots!!, 1e-6)
        assertEquals(6.0, s.productOfRoots!!, 1e-6)
    }

    @Test fun aCubicWithOneRealRootAndTwoComplex() {
        // x^3 - 1 = (x-1)(x^2+x+1)
        val s = rootsOf(1.0, 0.0, 0.0, -1.0)
        assertEquals(3, s.degree)
        assertEquals(listOf(1.0), s.realRoots)
        assertEquals(2, s.roots.count { !it.isReal })
        assertFalse(s.allReal)
    }

    @Test fun aQuartic() {
        // (x-1)(x-2)(x-3)(x-4)
        val s = rootsOf(1.0, -10.0, 35.0, -50.0, 24.0)
        assertEquals(4, s.degree)
        assertEquals("Quartic", EquationSolver.degreeName(4))
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0), s.realRoots)
        assertEquals(10.0, s.sumOfRoots!!, 1e-5)
        assertEquals(24.0, s.productOfRoots!!, 1e-5)
    }

    @Test fun aQuintic() {
        // x^5 - 1: one real root and two complex pairs
        val s = rootsOf(1.0, 0.0, 0.0, 0.0, 0.0, -1.0)
        assertEquals(5, s.degree)
        assertEquals("Quintic", EquationSolver.degreeName(5))
        assertEquals(5, s.roots.size)
        assertEquals(1, s.realRoots.size)
        assertEquals(1.0, s.realRoots[0], 1e-6)
        assertEquals(4, s.roots.count { !it.isReal })
    }

    @Test fun aRepeatedRootIsStillReportedOncePerDegree() {
        // x^3 - x^2 = x^2 (x - 1), so zero counts twice
        val s = rootsOf(1.0, -1.0, 0.0, 0.0)
        assertEquals(3, s.degree)
        assertEquals(3, s.roots.size)
        assertEquals(listOf(0.0, 0.0, 1.0), s.realRoots.sorted())
    }

    @Test fun leadingZerosDoNotChangeTheDegree() {
        // 0x^3 + x^2 - 4 is really just x^2 - 4
        val s = rootsOf(0.0, 1.0, 0.0, -4.0)
        assertEquals(2, s.degree)
        assertEquals(listOf(-2.0, 2.0), s.realRoots)
    }

    @Test fun anEquationWithNoTermsIsRefused() {
        assertNull(EquationSolver.polynomial(doubleArrayOf(0.0, 0.0, 0.0)))
        assertNull(EquationSolver.polynomial(doubleArrayOf(0.0)))
    }

    @Test fun everyDegreeHasAName() {
        assertEquals("Linear", EquationSolver.degreeName(1))
        assertEquals("Quadratic", EquationSolver.degreeName(2))
        assertEquals("Cubic", EquationSolver.degreeName(3))
        assertEquals("Quartic", EquationSolver.degreeName(4))
        assertEquals("Quintic", EquationSolver.degreeName(5))
    }

    // ---------------- simultaneous ----------------

    private fun system(a: Array<DoubleArray>, b: DoubleArray) = EquationSolver.simultaneous(a, b)

    @Test fun twoEquationsTwoUnknowns() {
        // x + y = 10, x - y = 2
        val s = system(arrayOf(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, -1.0)), doubleArrayOf(10.0, 2.0))
        assertTrue(s is LinearSolution.Unique)
        assertEquals(6.0, (s as LinearSolution.Unique).values[0], 1e-9)
        assertEquals(4.0, s.values[1], 1e-9)
    }

    @Test fun threeEquationsThreeUnknowns() {
        val s = system(
            arrayOf(
                doubleArrayOf(1.0, 1.0, 1.0),
                doubleArrayOf(0.0, 2.0, 5.0),
                doubleArrayOf(2.0, 5.0, -1.0),
            ),
            doubleArrayOf(6.0, -4.0, 27.0),
        )
        assertTrue(s is LinearSolution.Unique)
        val v = (s as LinearSolution.Unique).values
        assertEquals(5.0, v[0], 1e-9)
        assertEquals(3.0, v[1], 1e-9)
        assertEquals(-2.0, v[2], 1e-9)
    }

    @Test fun fourEquationsFourUnknowns() {
        val s = system(
            arrayOf(
                doubleArrayOf(1.0, 1.0, 1.0, 1.0),
                doubleArrayOf(1.0, 2.0, 3.0, 4.0),
                doubleArrayOf(1.0, 4.0, 9.0, 16.0),
                doubleArrayOf(1.0, 8.0, 27.0, 64.0),
            ),
            doubleArrayOf(10.0, 30.0, 100.0, 354.0),
        )
        assertTrue(s is LinearSolution.Unique)
        val v = (s as LinearSolution.Unique).values
        for (i in v.indices) assertEquals((i + 1).toDouble(), v[i], 1e-6)
    }

    @Test fun contradictingEquationsHaveNoAnswer() {
        // x + y = 1 and x + y = 2 cannot both hold
        val s = system(arrayOf(doubleArrayOf(1.0, 1.0), doubleArrayOf(1.0, 1.0)), doubleArrayOf(1.0, 2.0))
        assertTrue("expected none, got $s", s is LinearSolution.None)
    }

    @Test fun aRepeatedEquationHasEndlessAnswers() {
        // the second is just twice the first
        val s = system(arrayOf(doubleArrayOf(1.0, 1.0), doubleArrayOf(2.0, 2.0)), doubleArrayOf(2.0, 4.0))
        assertTrue("expected infinite, got $s", s is LinearSolution.Infinite)
    }

    @Test fun aRowOfZerosIsNotAnAnswer() {
        // 0x + 0y = 5 is impossible
        val s = system(arrayOf(doubleArrayOf(1.0, 1.0), doubleArrayOf(0.0, 0.0)), doubleArrayOf(2.0, 5.0))
        assertTrue("expected none, got $s", s is LinearSolution.None)
    }

    @Test fun aRaggedSystemIsRefused() {
        assertTrue(system(arrayOf(doubleArrayOf(1.0, 2.0, 3.0)), doubleArrayOf(1.0)) is LinearSolution.None)
        assertTrue(system(emptyArray(), doubleArrayOf()) is LinearSolution.None)
    }

    // ---------------- inequalities ----------------

    @Test fun aLinearInequality() {
        // 2x - 4 < 0  =>  x < 2
        assertEquals("x < 2\n(-\u221E, 2)", EquationSolver.linearInequality(2.0, -4.0, lessThan = true))
    }

    @Test fun aLinearInequalityFlipsWhenTheCoefficientIsNegative() {
        // -2x - 4 < 0  =>  x > -2
        assertEquals("x > -2\n(-2, \u221E)", EquationSolver.linearInequality(-2.0, -4.0, lessThan = true))
    }

    @Test fun aLinearInequalityWithNoX() {
        assertEquals("No value of x works", EquationSolver.linearInequality(0.0, 1.0, lessThan = true))
        assertEquals("True for every x", EquationSolver.linearInequality(0.0, -1.0, lessThan = true))
    }

    @Test fun aQuadraticBetweenItsRoots() {
        // x^2 - 4 < 0  =>  -2 < x < 2
        assertEquals(
            "-2 < x < 2\n(-2, 2)",
            EquationSolver.quadraticInequality(1.0, 0.0, -4.0, lessThan = true),
        )
    }

    @Test fun aQuadraticOutsideItsRoots() {
        // x^2 - 4 > 0  =>  x < -2 or x > 2
        assertEquals(
            "x < -2 or x > 2\n(-\u221E, -2) \u222A (2, \u221E)",
            EquationSolver.quadraticInequality(1.0, 0.0, -4.0, lessThan = false),
        )
    }

    @Test fun aQuadraticThatNeverMeetsTheAxis() {
        // x^2 + 1 is always positive
        assertEquals("No value of x works", EquationSolver.quadraticInequality(1.0, 0.0, 1.0, lessThan = true))
        assertEquals("True for every x", EquationSolver.quadraticInequality(1.0, 0.0, 1.0, lessThan = false))
    }

    @Test fun aDownwardParabolaFlipsEverything() {
        // -x^2 - 1 is always negative
        assertEquals("True for every x", EquationSolver.quadraticInequality(-1.0, 0.0, -1.0, lessThan = true))
        assertEquals("No value of x works", EquationSolver.quadraticInequality(-1.0, 0.0, -1.0, lessThan = false))
    }

    @Test fun aQuadraticWithARepeatedRoot() {
        // (x-2)^2 touches the axis at 2 and is positive everywhere else
        assertEquals(
            "True for every x except x = 2",
            EquationSolver.quadraticInequality(1.0, -4.0, 4.0, lessThan = false),
        )
        assertEquals(
            "Only x = 2, and only if the inequality is not strict",
            EquationSolver.quadraticInequality(1.0, -4.0, 4.0, lessThan = true),
        )
    }

    @Test fun aQuadraticThatIsReallyLinearFallsBack() {
        assertEquals(
            EquationSolver.linearInequality(3.0, -6.0, lessThan = true),
            EquationSolver.quadraticInequality(0.0, 3.0, -6.0, lessThan = true),
        )
    }

    // ---------------- the detail panel ----------------

    @Test fun divisorsOfAWholeNumber() {
        assertEquals(listOf(1L, 2L, 3L, 4L, 6L, 12L), ResultDetail.divisors(12L))
        assertEquals(listOf(1L, 97L), ResultDetail.divisors(97L))
        assertEquals(listOf(1L), ResultDetail.divisors(1L))
        assertEquals(emptyList<Long>(), ResultDetail.divisors(0L))
    }

    @Test fun divisorsCountToTheSquareRootOnly() {
        // a perfect square must not list its square root twice
        val d = ResultDetail.divisors(36L)
        assertEquals(listOf(1L, 2L, 3L, 4L, 6L, 9L, 12L, 18L, 36L), d)
        assertEquals(d.size, d.distinct().size)
    }

    @Test fun primeFactorisation() {
        assertEquals("2^2 \u00D7 3", ResultDetail.primeFactorisation(12L))
        assertEquals("2^3 \u00D7 3^2 \u00D7 5", ResultDetail.primeFactorisation(360L))
        assertEquals("2^10", ResultDetail.primeFactorisation(1024L))
        assertEquals("97", ResultDetail.primeFactorisation(97L))
        assertEquals("1", ResultDetail.primeFactorisation(1L))
        assertEquals("0", ResultDetail.primeFactorisation(0L))
    }

    @Test fun primality() {
        assertTrue(ResultDetail.isPrime(2L))
        assertTrue(ResultDetail.isPrime(97L))
        assertTrue(ResultDetail.isPrime(7919L))
        assertFalse(ResultDetail.isPrime(1L))
        assertFalse(ResultDetail.isPrime(0L))
        assertFalse(ResultDetail.isPrime(9L))
        assertFalse(ResultDetail.isPrime(1024L))
    }

    @Test fun theOtherBases() {
        assertEquals("1010", ResultDetail.binary(10L))
        assertEquals("12", ResultDetail.octal(10L))
        assertEquals("A", ResultDetail.hex(10L))
        assertEquals("FF", ResultDetail.hex(255L))
        assertEquals("1100100", ResultDetail.binary(100L))
    }

    @Test fun onlyWholeNumbersGetADetailPanel() {
        assertEquals(5L, ResultDetail.asWhole(5.0))
        assertEquals(0L, ResultDetail.asWhole(0.0))
        assertNull(ResultDetail.asWhole(5.5))
        assertNull(ResultDetail.asWhole(Double.NaN))
        assertNull(ResultDetail.asWhole(Double.POSITIVE_INFINITY))
    }

    @Test fun theDetailPanelMatchesTheRootsItIsShownFor() {
        // the solver and the detail panel must agree about what a whole number is
        val s = rootsOf(1.0, -10.0, 35.0, -50.0, 24.0)
        val whole = s.roots.filter { it.isWholeNumber }.map { Math.round(it.re) }
        assertEquals(listOf(1L, 2L, 3L, 4L), whole)
        whole.forEach { assertNotNull(ResultDetail.primeFactorisation(it)) }
    }
}
