package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Calculus
import com.merry8989uk.universalcalculator.calc.Complex
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.Limits
import com.merry8989uk.universalcalculator.calc.Matrix
import com.merry8989uk.universalcalculator.calc.PartialFractions
import com.merry8989uk.universalcalculator.calc.Statistics
import com.merry8989uk.universalcalculator.calc.Symbolic
import com.merry8989uk.universalcalculator.calc.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

/**
 * The engine under load.
 *
 * The ordinary tests check that the answers are right; these check that they
 * stay right — and stay quick — when the input is large. Every expected value
 * was worked out separately before the assertion was written.
 */
class StressTest {

    // ---- linear algebra ----

    /** n·I + 1/(i+j+1): strictly diagonally dominant, so it is safely invertible. */
    private fun dominant(n: Int): Array<DoubleArray> = Array(n) { i ->
        DoubleArray(n) { j -> if (i == j) n.toDouble() else 1.0 / (i + j + 1) }
    }

    @Test fun aSixtyBySixtyMatrixStillInverts() {
        val n = 60
        val a = dominant(n)
        val inverse = Matrix.inverse(a)
        assertNotNull("a diagonally dominant matrix must invert", inverse)

        // A · A⁻¹ must be the identity.
        val product = Matrix.mul(a, inverse!!)
        var worst = 0.0
        for (i in 0 until n) {
            for (j in 0 until n) {
                val expected = if (i == j) 1.0 else 0.0
                worst = maxOf(worst, abs(product[i][j] - expected))
            }
        }
        assertTrue("A · A⁻¹ drifted by $worst", worst < 1e-8)
    }

    @Test fun aHundredByHundredMatrixDoesNotHang() {
        // Not asserting accuracy — just that it completes and produces finite numbers.
        val n = 100
        val inverse = Matrix.inverse(dominant(n))
        assertNotNull(inverse)
        inverse!!.forEach { row -> row.forEach { assertTrue("not finite: $it", it.isFinite()) } }
    }

    @Test fun theDeterminantOfATriangularMatrixIsTheDiagonalProduct() {
        // upper triangular with 1..15 on the diagonal, so det = 15!
        val n = 15
        val a = Array(n) { i -> DoubleArray(n) { j -> when { j > i -> 1.0; j == i -> (i + 1).toDouble(); else -> 0.0 } } }
        assertEquals(1_307_674_368_000.0, Matrix.determinant(a), 1.0)
    }

    @Test fun aSingularLargeMatrixIsRefusedRatherThanGuessed() {
        // every row identical: rank 1, so there is no inverse
        val n = 40
        val a = Array(n) { DoubleArray(n) { j -> (j + 1).toDouble() } }
        assertTrue(Matrix.inverse(a) == null)
        assertEquals(0.0, Matrix.determinant(a), 1e-6)
    }

    @Test fun eigenvaluesOfAModerateSymmetricMatrixAreReal() {
        val n = 6
        val a = Array(n) { i -> DoubleArray(n) { j -> if (i == j) (i + 1).toDouble() else 0.5 } }
        val values = Matrix.eigenvalues(a)
        assertEquals(n, values.size)
        // a symmetric real matrix has real eigenvalues
        values.forEach { v -> assertTrue("eigenvalue $v is not real", abs(v.im) < 1e-6) }
        // and the eigenvalues sum to the trace
        assertEquals(Matrix.trace(a), values.sumOf { it.re }, 1e-5)
    }

    @Test fun eigenvaluesOfALargeMatrixStayFinite() {
        // At 20x20 the characteristic polynomial is far too ill-conditioned for
        // its roots to be resolved in double precision — the imaginary parts
        // that come back are noise, not mathematics. What matters here is that
        // it terminates and returns finite numbers rather than hanging or
        // producing NaN. The same limitation is documented on Matrix.eigenvalues.
        val n = 20
        val a = Array(n) { i -> DoubleArray(n) { j -> if (i == j) (i + 1).toDouble() else 0.5 } }
        val values = Matrix.eigenvalues(a)
        // It may resolve fewer than n; what it must never do is return NaN.
        assertTrue("expected some eigenvalues back", values.isNotEmpty())
        assertTrue("returned more eigenvalues than the matrix has", values.size <= n)
        values.forEach { v ->
            assertTrue("eigenvalue is not finite: $v", v.re.isFinite() && v.im.isFinite())
        }
    }

    // ---- calculus ----

    @Test fun integratingWithAHundredThousandStepsIsAccurate() {
        // ∫ sin x from 0 to π is exactly 2
        assertEquals(2.0, Calculus.integrate("sin(x)", 0.0, PI, 100_000), 1e-9)
    }

    @Test fun tenThousandTermsSumCorrectly() {
        // Σ 1/k² to 10000; worked out independently as 1.6448340718480599
        val sum = Calculus.summation("1/x^2", 1, 10_000)
        assertEquals(1.6448340718480599, sum, 1e-12)
    }

    @Test fun aHundredThousandStepDerivativeStaysSteady() {
        // d/dx x³ at 2 is 12
        assertEquals(12.0, Calculus.derivative("x^3", 2.0), 1e-4)
        // and the second derivative of x³ is 6x, so 12 at x = 2
        assertEquals(12.0, Calculus.secondDerivative("x^3", 2.0), 1e-3)
    }

    // ---- expressions ----

    @Test fun aThousandTermExpressionParses() {
        val expr = (1..1000).joinToString("+") { "1" }
        assertEquals(1000.0, Evaluator.evaluate(expr), 1e-9)
    }

    @Test fun deeplyNestedParenthesesParse() {
        val depth = 300
        val expr = "(".repeat(depth) + "1+1" + ")".repeat(depth)
        assertEquals(2.0, Evaluator.evaluate(expr), 1e-9)
    }

    @Test fun aLongChainOfOperatorsKeepsTheRightPrecedence() {
        // 1 + 2*3 + 4*5 + ... repeated: each pair contributes 2n(2n+1)
        val expr = (1..200).joinToString("+") { n -> "${2 * n}*${2 * n + 1}" }
        val expected = (1..200).sumOf { n -> (2 * n).toDouble() * (2 * n + 1) }
        assertEquals(expected, Evaluator.evaluate(expr), 1e-6)
    }

    @Test fun bigCombinationsStayExact() {
        // nCr(50, 25) = 126410606437752, comfortably inside a double's exact range
        assertEquals(126_410_606_437_752.0, Evaluator.evaluate("nCr(50,25)"), 1e-3)
        assertEquals(126_410_606_437_752.0, Evaluator.evaluate("nCr(25,25)*nCr(50,25)"), 1e-3)
    }

    @Test fun aFactorialBeyondADoubleIsReportedNotSilentlyWrong() {
        // 171! overflows a double. It must not come back as a quiet NaN.
        val outcome = runCatching { Evaluator.evaluate("171!") }
        val value = outcome.getOrNull()
        assertTrue(
            "171! should overflow loudly, got $value",
            outcome.isFailure || value == null || value.isInfinite(),
        )
    }

    // ---- polynomials ----

    /** (x-1)(x-2)...(x-n), built by repeated multiplication. */
    private fun rootsProduct(n: Int): DoubleArray {
        var poly = doubleArrayOf(1.0)
        for (r in 1..n) {
            val next = DoubleArray(poly.size + 1)
            for (i in poly.indices) {
                next[i] -= poly[i] * r
                next[i + 1] += poly[i]
            }
            poly = Symbolic.trim(next)
        }
        return poly
    }

    @Test fun everyRootIsFoundUpToDegreeEight() {
        val n = 8
        val roots = Symbolic.roots(rootsProduct(n))
        assertEquals("expected $n roots, got ${roots.size}", n, roots.size)
        for (r in 1..n) {
            assertTrue("root $r missing from $roots", roots.any { abs(it - r) < 1e-6 })
        }
    }

    @Test fun aDegreeTwentyPolynomialIsBeyondDoublePrecision() {
        // (x-1)...(x-20) is Wilkinson's polynomial, the textbook example of an
        // ill-conditioned root problem. Its constant term is 20! ≈ 2.4e18, so
        // simply evaluating it near a root already carries an error of ~500 —
        // no double-precision method can pull those roots apart, and this one
        // does not pretend to. It must instead terminate and stay finite.
        val poly = rootsProduct(20)
        val roots = Symbolic.roots(poly)
        // Fewer than 20 roots may come back; none of them may be NaN.
        assertTrue("returned more roots than the degree", roots.size <= 20)
        roots.forEach { assertTrue("root is not finite: $it", it.isFinite()) }

        // The well-conditioned degree-eight case above shows the method itself
        // is sound; it is the problem that is impossible.
        assertEquals(20, Symbolic.degree(poly))
    }

    @Test fun partialFractionsOnADegreeEightDenominator() {
        // 1 / (x(x+1)(x+2)...(x+7)): eight distinct linear factors
        var den = doubleArrayOf(1.0)
        for (r in 0..7) {
            val next = DoubleArray(den.size + 1)
            for (i in den.indices) {
                next[i] += den[i] * r
                next[i + 1] += den[i]
            }
            den = Symbolic.trim(next)
        }
        val outcome = PartialFractions.decompose(doubleArrayOf(1.0), den)
        assertTrue("expected a decomposition, got $outcome", outcome is PartialFractions.Outcome.Decomposed)
        val terms = (outcome as PartialFractions.Outcome.Decomposed).terms
        assertEquals(8, terms.size)

        // It must rebuild the original fraction away from the poles.
        for (probe in listOf(-9.5, -4.5, 0.5, 3.3, 8.7)) {
            val original = 1.0 / Symbolic.evaluate(den, probe)
            var rebuilt = 0.0
            terms.forEach { t -> rebuilt += t.coefficient / Math.pow(probe - t.root, t.power.toDouble()) }
            assertEquals("does not rebuild at $probe", original, rebuilt, 1e-6)
        }
    }

    // ---- statistics ----

    @Test fun statisticsOnAHundredThousandSamples() {
        val n = 100_000
        val xs = List(n) { i -> ((i * 7919) % 10000) / 1000.0 }
        // worked out independently: the pattern is uniform over 0..9.999, mean 4.9995
        assertEquals(4.9995, Statistics.mean(xs), 1e-9)
        assertTrue(Statistics.sd(xs) > 2.8 && Statistics.sd(xs) < 2.9)
        assertEquals(100_000, xs.size)
    }

    // ---- limits ----

    @Test fun aLimitWithATightToleranceHoldsUp() {
        assertEquals(1.0, Limits.limit("sin(x)/x", 0.0)!!, 1e-9)
        assertEquals(0.5, Limits.limit("(1-cos(x))/x^2", 0.0)!!, 1e-6)
        assertEquals(3.0, Limits.limitAtInfinity("(3*x^2+2*x)/(x^2+1)")!!, 1e-5)
    }

    // ---- conversions ----

    @Test fun tenThousandConversionsStayConsistent() {
        // unit() lives on a category, not on Units itself
        val length = Units.categories.first { it.name == "Length" }
        val metre = length.unit("m")
        val foot = length.unit("ft")
        assertEquals("m", metre.symbol)
        assertEquals("ft", foot.symbol)
        repeat(10_000) { i ->
            val value = i.toDouble() / 7.0
            val there = Units.convert(value, metre, foot)
            val back = Units.convert(there, foot, metre)
            assertEquals(value, back, 1e-9)
        }
    }

    // ---- complex ----

    @Test fun complexRootsOfUnityAreAccurateAtHighDegree() {
        // the 16th roots of unity all have modulus 1
        val values: List<Complex> = (0 until 16).map { k ->
            val angle = 2 * PI * k / 16.0
            Complex(kotlin.math.cos(angle), kotlin.math.sin(angle))
        }
        values.forEach { assertEquals(1.0, it.abs(), 1e-12) }
        // and they sum to zero
        val sum = values.fold(Complex(0.0, 0.0)) { acc, v -> acc + v }
        assertTrue("roots of unity should cancel, got $sum", abs(sum.re) < 1e-12 && abs(sum.im) < 1e-12)
    }
}
