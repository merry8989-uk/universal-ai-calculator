package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The advanced matrix routines the master plan asks for and the app did not have:
 * the Cholesky factorisation, a matrix raised to a power, and the matrix
 * exponential.
 *
 * These build on [Matrix], which already had LU and QR.
 */
object AdvancedMatrix {

    /**
     * The Cholesky factorisation: a symmetric positive-definite matrix as
     * `A = L · Lᵀ` with `L` lower triangular.
     *
     * Returns null when the matrix is not symmetric or not positive definite,
     * which is the honest answer — a Cholesky factor does not exist then, and
     * pretending otherwise would give a wrong triangle.
     */
    fun cholesky(a: Array<DoubleArray>): Array<DoubleArray>? {
        val n = a.size
        if (n == 0 || a.any { it.size != n }) return null
        // symmetry first
        for (i in 0 until n) {
            for (j in 0 until n) {
                if (abs(a[i][j] - a[j][i]) > 1e-9) return null
            }
        }
        val l = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0..i) {
                var sum = a[i][j]
                for (k in 0 until j) sum -= l[i][k] * l[j][k]
                if (i == j) {
                    // a non-positive pivot means the matrix is not positive definite
                    if (sum <= 0.0) return null
                    l[i][i] = sqrt(sum)
                } else {
                    l[i][j] = sum / l[j][j]
                }
            }
        }
        return l
    }

    /** The identity of the same size as [a]. */
    fun identity(n: Int): Array<DoubleArray> =
        Array(n) { r -> DoubleArray(n) { c -> if (r == c) 1.0 else 0.0 } }

    /**
     * `A` raised to a whole power, by repeated squaring.
     *
     * `A^0` is the identity, which is what makes the exponent behave like an
     * exponent. A negative power needs an inverse and returns null without one.
     */
    fun power(a: Array<DoubleArray>, n: Int): Array<DoubleArray>? {
        val size = a.size
        if (size == 0 || a.any { it.size != size }) return null
        if (n == 0) return identity(size)
        if (n < 0) {
            val inv = Matrix.inverse(a) ?: return null
            return power(inv, -n)
        }
        var result = identity(size)
        var base = a
        var e = n
        while (e > 0) {
            if (e and 1 == 1) result = multiply(result, base)
            e = e shr 1
            if (e > 0) base = multiply(base, base)
        }
        return result
    }

    /**
     * The matrix exponential `e^A`, by scaling and squaring.
     *
     * `A` is halved until its norm is small, the Taylor series is summed there,
     * and the result is squared back the same number of times. This is the
     * standard method and is what makes it work for a matrix that is not small.
     */
    fun expm(a: Array<DoubleArray>): Array<DoubleArray>? {
        val n = a.size
        if (n == 0 || a.any { it.size != n }) return null

        // how many halvings bring the norm under a half
        val norm = a.sumOf { row -> row.sumOf { abs(it) } }
        var squarings = 0
        var scale = 1.0
        while (norm * scale > 0.5 && squarings < 40) {
            scale /= 2.0
            squarings++
        }

        var scaled = Array(n) { r -> DoubleArray(n) { c -> a[r][c] * scale } }

        // the Taylor series, which converges fast once the norm is small
        var term = identity(n)
        var sum = identity(n)
        for (k in 1..30) {
            term = multiply(term, scaled)
            term = Array(n) { r -> DoubleArray(n) { c -> term[r][c] / k } }
            sum = add(sum, term)
            // stop once the new term stops moving the sum
            val biggest = term.maxOf { row -> row.maxOf { abs(it) } }
            if (biggest < 1e-16) break
        }

        // square back
        var result = sum
        repeat(squarings) { result = multiply(result, result) }
        return result
    }

    /** Ordinary matrix product, kept here so the routines above read plainly. */
    fun multiply(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> {
        val n = a.size
        val m = b[0].size
        val k = b.size
        return Array(n) { r ->
            DoubleArray(m) { c ->
                var sum = 0.0
                for (t in 0 until k) sum += a[r][t] * b[t][c]
                sum
            }
        }
    }

    fun add(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> =
        Array(a.size) { r -> DoubleArray(a[r].size) { c -> a[r][c] + b[r][c] } }

    /** A matrix written out row by row, for the screen. */
    fun text(a: Array<DoubleArray>, decimals: Int = 4): String =
        a.joinToString("\n") { row ->
            row.joinToString("  ") { Format.number(Math.round(it * 1e10) / 1e10) }
        }
}

/** A term of a Taylor series, and the whole series. */
data class TaylorSeries(
    /** The point the series is taken about. */
    val about: Double,
    /** c₀, c₁, c₂ … so the series is c₀ + c₁(x−a) + c₂(x−a)² + … */
    val coefficients: List<Double>,
    /** The same thing written out. */
    val text: String,
    /** What the series gives at [about], which must be f(about). */
    val valueAtAbout: Double,
    /** The series summed at this point, and what f actually gives there. */
    val checkAt: Double,
    val checkActual: Double,
) {
    val error: Double get() = abs(checkAt - checkActual)
}

/** A point where a curve turns, and what kind of turn it is. */
data class TurningPoint(
    val x: Double,
    val y: Double,
    /** "minimum", "maximum" or "inflection". */
    val kind: String,
)

/**
 * The advanced calculus the master plan asks for: a Taylor series, a derivative
 * of any order, and the points where a curve turns.
 *
 * All of it is numerical, because the app's expression evaluator is numerical.
 * The finite differences use a step that grows with the order, because a high
 * derivative over a tiny step is all rounding error.
 */
object AdvancedCalculus {

    /** The largest derivative order the numerical step can still carry. */
    const val MAX_ORDER = 6

    /**
     * The step to use for each order.
     *
     * A central difference of order n has two errors pulling opposite ways: the
     * truncation error shrinks as h falls, and the rounding error grows as h
     * falls because the n-th difference divides by hⁿ. The best h is where they
     * cross, which is around ε^(1/(n+2)) — measured here rather than guessed, and
     * held as a table because the numbers matter and the formula is only a guide.
     */
    private val STEPS = doubleArrayOf(0.0, 1e-5, 1e-4, 1e-3, 1e-3, 1e-2, 1e-2)

    private fun f(expr: String, x: Double, unit: AngleUnit, percentage: PercentageMode): Double =
        Evaluator.evaluate(expr, vars = mapOf("x" to x), unit = unit, percentage = percentage)

    /**
     * The n-th derivative at a point, by repeated central differences.
     *
     * The step widens as the order climbs: the n-th difference divides by hⁿ, so
     * a small h for a high order is mostly floating-point noise.
     */
    fun nthDerivative(
        expr: String,
        at: Double,
        order: Int,
        unit: AngleUnit = AngleUnit.RADIAN,
        percentage: PercentageMode = PercentageMode.MULTIPLICATIVE,
    ): Double {
        if (order < 0 || order > MAX_ORDER) return Double.NaN
        if (order == 0) return f(expr, at, unit, percentage)
        return difference(expr, at, order, STEPS[order], unit, percentage)
    }

    private fun difference(
        expr: String,
        at: Double,
        order: Int,
        h: Double,
        unit: AngleUnit,
        percentage: PercentageMode,
    ): Double {
        if (order == 1) {
            return (f(expr, at + h, unit, percentage) - f(expr, at - h, unit, percentage)) / (2 * h)
        }
        return (
            difference(expr, at + h, order - 1, h, unit, percentage) -
                difference(expr, at - h, order - 1, h, unit, percentage)
            ) / (2 * h)
    }

    /**
     * The Taylor series of [expr] about [about], to [order] terms.
     *
     * [checkAt] is where the series is compared with the function, so the screen
     * can say how close it came rather than asking the user to trust it.
     */
    fun taylor(
        expr: String,
        about: Double,
        order: Int,
        checkAt: Double,
        unit: AngleUnit = AngleUnit.RADIAN,
        percentage: PercentageMode = PercentageMode.MULTIPLICATIVE,
    ): TaylorSeries? {
        val n = order.coerceIn(0, MAX_ORDER)
        val coefficients = ArrayList<Double>(n + 1)
        var factorial = 1.0
        for (k in 0..n) {
            if (k > 0) factorial *= k
            val d = nthDerivative(expr, about, k, unit, percentage)
            if (!d.isFinite()) return null
            coefficients.add(d / factorial)
        }
        val text = format(coefficients, about)
        val atAbout = f(expr, about, unit, percentage)
        val summed = evaluate(coefficients, about, checkAt)
        val actual = f(expr, checkAt, unit, percentage)
        return TaylorSeries(
            about = about,
            coefficients = coefficients,
            text = text,
            valueAtAbout = atAbout,
            checkAt = summed,
            checkActual = actual,
        )
    }

    /** The series summed at a point. */
    fun evaluate(coefficients: List<Double>, about: Double, x: Double): Double {
        var sum = 0.0
        var power = 1.0
        for (c in coefficients) {
            sum += c * power
            power *= (x - about)
        }
        return sum
    }

    /** The series written out, with the terms that are zero left off. */
    private fun format(coefficients: List<Double>, about: Double): String {
        if (coefficients.isEmpty()) return "0"
        val parts = ArrayList<String>()
        coefficients.forEachIndexed { k, c ->
            if (abs(c) < 1e-12) return@forEachIndexed
            val power = when (k) {
                0 -> ""
                1 -> "x"
                else -> "x^$k"
            }
            val sign = if (c < 0) "\u2212" else if (parts.isEmpty()) "" else "+"
            val magnitude = if (abs(abs(c) - 1.0) < 1e-12 && k > 0) "" else Format.number(abs(c))
            parts.add(listOf(sign, magnitude, power).filter { it.isNotEmpty() }.joinToString(" "))
        }
        if (parts.isEmpty()) return "0"
        val body = parts.joinToString(" ")
        return if (abs(about) < 1e-12) body else "$body   (about x = ${Format.number(about)})"
    }

    /**
     * The points in [from]..[to] where the curve turns.
     *
     * The derivative is scanned for a change of sign and then bisected, which
     * finds a turning point without needing a symbolic derivative. Each is
     * classified by the second derivative.
     */
    fun turningPoints(
        expr: String,
        from: Double,
        to: Double,
        steps: Int = 400,
        unit: AngleUnit = AngleUnit.RADIAN,
        percentage: PercentageMode = PercentageMode.MULTIPLICATIVE,
    ): List<TurningPoint> {
        if (!from.isFinite() || !to.isFinite() || to <= from) return emptyList()
        val n = steps.coerceIn(20, 4000)
        val h = (to - from) / n

        fun d1(x: Double) = nthDerivative(expr, x, 1, unit, percentage)

        val out = ArrayList<TurningPoint>()
        var previousX = from
        var previous = d1(from)
        for (i in 1..n) {
            val x = from + i * h
            val current = d1(x)
            // A derivative that lands exactly on zero is a crossing too — and it
            // is the common case, because a turning point often sits on a step.
            val crossed = previous.isFinite() && current.isFinite() &&
                ((previous < 0 && current >= 0) || (previous > 0 && current <= 0))
            if (crossed) {
                val root = bisect(::d1, previousX, x)
                if (root != null && out.none { abs(it.x - root) < h }) {
                    val second = nthDerivative(expr, root, 2, unit, percentage)
                    val kind = when {
                        second > 1e-7 -> "minimum"
                        second < -1e-7 -> "maximum"
                        else -> "inflection"
                    }
                    out.add(TurningPoint(root, f(expr, root, unit, percentage), kind))
                }
            }
            previousX = x
            previous = current
        }
        return out
    }

    /** The x where [g] crosses zero between two points, by halving the gap. */
    private fun bisect(g: (Double) -> Double, lo: Double, hi: Double): Double? {
        var a = lo
        var b = hi
        var fa = g(a)
        if (!fa.isFinite()) return null
        repeat(80) {
            val mid = (a + b) / 2
            val fm = g(mid)
            if (!fm.isFinite()) return null
            // close enough to zero to be the root, given the noise in the slope
            if (abs(fm) < 1e-12) return mid
            if ((fa < 0) != (fm < 0)) {
                b = mid
            } else {
                a = mid
                fa = fm
            }
        }
        return (a + b) / 2
    }
}

/** The answer to a multiple regression. */
data class MultipleRegression(
    /** One coefficient per predictor, plus the intercept first. */
    val coefficients: List<Double>,
    val rSquared: Double,
    val adjustedRSquared: Double,
    /** The number of observations and the number of predictors. */
    val observations: Int,
    val predictors: Int,
) {
    /** The fitted line written out. */
    fun text(predictorNames: List<String>): String {
        val parts = ArrayList<String>()
        parts.add("${Format.number(coefficients[0])}")
        for (i in 1 until coefficients.size) {
            val c = coefficients[i]
            val name = predictorNames.getOrElse(i - 1) { "x$i" }
            parts.add((if (c < 0) "\u2212 " else "+ ") + "${Format.number(abs(c))} $name")
        }
        return "y = " + parts.joinToString(" ")
    }
}

/**
 * Least-squares regression with more than one predictor.
 *
 * The single-predictor case was already there; this is the general one. It is
 * solved by the QR decomposition [Matrix] already had, which is the numerically
 * sound way to do it — the normal equations square the condition number and lose
 * half the precision.
 */
object AdvancedStatistics {

    /**
     * Fits `y = b₀ + b₁x₁ + b₂x₂ + …`.
     *
     * [predictors] is one list per predictor, each as long as [ys].
     */
    fun multipleRegression(
        predictors: List<List<Double>>,
        ys: List<Double>,
    ): MultipleRegression? {
        val k = predictors.size
        val n = ys.size
        if (k < 1 || n < k + 2) return null
        if (predictors.any { it.size != n }) return null

        // the design matrix, with a leading column of ones for the intercept
        val design = Array(n) { r ->
            DoubleArray(k + 1) { c -> if (c == 0) 1.0 else predictors[c - 1][r] }
        }
        val beta = Matrix.leastSquares(design, ys.toDoubleArray()) ?: return null

        // how much of the variation the fit explains
        val meanY = ys.sum() / n
        var total = 0.0
        var residual = 0.0
        for (r in 0 until n) {
            var fitted = beta[0]
            for (c in 0 until k) fitted += beta[c + 1] * predictors[c][r]
            total += (ys[r] - meanY) * (ys[r] - meanY)
            residual += (ys[r] - fitted) * (ys[r] - fitted)
        }
        val r2 = if (total <= 0.0) 0.0 else 1.0 - residual / total
        val adjusted = if (total <= 0.0 || n - k - 1 <= 0) {
            r2
        } else {
            1.0 - (1.0 - r2) * (n - 1).toDouble() / (n - k - 1)
        }
        return MultipleRegression(
            coefficients = beta.toList(),
            rSquared = r2,
            adjustedRSquared = adjusted,
            observations = n,
            predictors = k,
        )
    }
}
