package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.round

/**
 * A small symbolic engine for polynomials: parse, simplify, differentiate,
 * integrate, evaluate, factor and find roots. Polynomials are held as
 * coefficient arrays where index i is the coefficient of x^i.
 */
object Symbolic {

    /** Removes trailing zero coefficients. */
    fun trim(p: DoubleArray): DoubleArray {
        var n = p.size
        while (n > 1 && abs(p[n - 1]) < 1e-12) n--
        return if (n == p.size) p else p.copyOf(n)
    }

    fun degree(p: DoubleArray): Int = trim(p).size - 1

    fun isZero(p: DoubleArray): Boolean = trim(p).size == 1 && abs(trim(p)[0]) < 1e-12

    fun add(a: DoubleArray, b: DoubleArray): DoubleArray {
        val n = maxOf(a.size, b.size)
        return trim(DoubleArray(n) { (if (it < a.size) a[it] else 0.0) + (if (it < b.size) b[it] else 0.0) })
    }

    fun subtract(a: DoubleArray, b: DoubleArray): DoubleArray {
        val n = maxOf(a.size, b.size)
        return trim(DoubleArray(n) { (if (it < a.size) a[it] else 0.0) - (if (it < b.size) b[it] else 0.0) })
    }

    fun multiply(a: DoubleArray, b: DoubleArray): DoubleArray {
        if (a.isEmpty() || b.isEmpty()) return doubleArrayOf(0.0)
        val out = DoubleArray(a.size + b.size - 1)
        for (i in a.indices) for (j in b.indices) out[i + j] += a[i] * b[j]
        return trim(out)
    }

    fun scale(a: DoubleArray, k: Double): DoubleArray = trim(DoubleArray(a.size) { a[it] * k })

    fun derivative(p: DoubleArray): DoubleArray {
        val t = trim(p)
        if (t.size <= 1) return doubleArrayOf(0.0)
        return trim(DoubleArray(t.size - 1) { t[it + 1] * (it + 1) })
    }

    fun integral(p: DoubleArray, constant: Double = 0.0): DoubleArray {
        val t = trim(p)
        val out = DoubleArray(t.size + 1)
        out[0] = constant
        for (i in t.indices) out[i + 1] = t[i] / (i + 1)
        return out
    }

    fun evaluate(p: DoubleArray, x: Double): Double {
        var acc = 0.0
        for (i in p.indices.reversed()) acc = acc * x + p[i]
        return acc
    }

    /** (a + b·x)^n, expanded. */
    fun expandBinomial(a: Double, b: Double, n: Int): DoubleArray {
        if (n < 0) throw Evaluator.EvalException("The power must not be negative")
        val out = DoubleArray(n + 1)
        for (k in 0..n) {
            out[k] = Evaluator.combinations(n.toDouble(), k.toDouble()) * Math.pow(a, (n - k).toDouble()) * Math.pow(b, k.toDouble())
        }
        return trim(out)
    }

    // ---------------- parsing ----------------

    /** Parses "2x^3 - 5x + 1", "x^2", "4", "-x", "3x". */
    fun parse(text: String): DoubleArray {
        val s = text.trim().lowercase().replace(" ", "").replace("*", "")
        if (s.isEmpty()) throw Evaluator.EvalException("Enter a polynomial in x")
        val terms = splitTerms(s)
        if (terms.isEmpty()) throw Evaluator.EvalException("Enter a polynomial in x")
        var maxDeg = 0
        val parsed = ArrayList<Pair<Double, Int>>(terms.size)
        for ((sign, body) in terms) {
            val t = parseTerm(body)
            val scaled = (t.first * sign) to t.second
            maxDeg = maxOf(maxDeg, scaled.second)
            parsed.add(scaled)
        }
        val out = DoubleArray(maxDeg + 1)
        for ((coeff, deg) in parsed) out[deg] += coeff
        return trim(out)
    }

    /** Splits into (sign, body) pairs. */
    private fun splitTerms(s: String): List<Pair<Double, String>> {
        val out = ArrayList<Pair<Double, String>>()
        val sb = StringBuilder()
        var sign = 1.0
        var i = 0
        if (s[0] == '+' || s[0] == '-') {
            sign = if (s[0] == '-') -1.0 else 1.0
            i = 1
        }
        while (i < s.length) {
            val c = s[i]
            if ((c == '+' || c == '-') && sb.isNotEmpty()) {
                out.add(sign to sb.toString())
                sign = if (c == '-') -1.0 else 1.0
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty()) out.add(sign to sb.toString())
        return out.filter { it.second.isNotEmpty() }
    }

    /** Returns (coefficient, exponent). */
    private fun parseTerm(body: String): Pair<Double, Int> {
        if (!body.contains('x')) {
            val c = body.toDoubleOrNull() ?: throw Evaluator.EvalException("'$body' is not a term")
            return c to 0
        }
        val idx = body.indexOf('x')
        val coeffPart = body.substring(0, idx)
        val coeff = when (coeffPart) {
            "", "+" -> 1.0
            "-" -> -1.0
            else -> coeffPart.toDoubleOrNull() ?: throw Evaluator.EvalException("Bad coefficient '$coeffPart'")
        }
        val after = body.substring(idx + 1)
        val exp = when {
            after.isEmpty() -> 1
            after.startsWith("^") -> after.substring(1).toIntOrNull()
                ?: throw Evaluator.EvalException("Bad power '$after'")
            else -> throw Evaluator.EvalException("Cannot read '$body'")
        }
        if (exp < 0) throw Evaluator.EvalException("Powers must not be negative")
        return coeff to exp
    }

    // ---------------- formatting ----------------

    /** "2x^3 - 5x + 1" — the simplified form. */
    fun format(p: DoubleArray): String {
        val t = trim(p)
        if (isZero(t)) return "0"
        val parts = ArrayList<String>()
        for (i in t.indices.reversed()) {
            val c = t[i]
            if (abs(c) < 1e-12) continue
            val mag = Format.number(abs(c))
            val body = when (i) {
                0 -> mag
                1 -> if (abs(abs(c) - 1.0) < 1e-12) "x" else "${mag}x"
                else -> if (abs(abs(c) - 1.0) < 1e-12) "x^$i" else "${mag}x^$i"
            }
            val prefix = when {
                parts.isEmpty() -> if (c < 0) "-" else ""
                c < 0 -> " - "
                else -> " + "
            }
            parts.add(prefix + body)
        }
        return parts.joinToString("")
    }

    /** "x^2 + x" style for an integral, with "+ C". */
    fun formatWithConstant(p: DoubleArray): String = format(p) + " + C"

    // ---------------- roots and factors ----------------

    /** All real roots. */
    fun roots(p: DoubleArray): List<Double> {
        val t = trim(p)
        val d = t.size - 1
        if (d < 1) return emptyList()
        val rational = rationalRoots(t)
        if (rational.size == d) return rational.sorted()

        // deflate by the rational roots, then finish numerically
        var remaining = t
        for (r in rational) remaining = divideByRoot(remaining, r)
        val out = ArrayList<Double>(rational)
        if (remaining.size == 2) {
            // remaining is c + b·x, so the root is −c/b
            val c = remaining[0]; val b = remaining[1]
            if (abs(b) > 1e-12) out.add(-c / b)
        } else if (remaining.size > 2) {
            val coeffsHighFirst = remaining.reversed().toDoubleArray()
            Matrix.polyRoots(coeffsHighFirst).forEach { c ->
                if (abs(c.im) < 1e-6) out.add(round(c.re * 1e9) / 1e9)
            }
        }
        // A very ill-conditioned polynomial can make the numeric pass diverge.
        // A root that is not a finite number is not a root, so it is dropped
        // rather than handed to the caller as NaN.
        return out.filter { it.isFinite() }.sorted()
    }

    private fun rationalRoots(p: DoubleArray): List<Double> {
        val t = trim(p)
        val a0 = t[0]
        val an = t[t.size - 1]
        if (abs(a0) < 1e-12) {
            // x is a factor: factor it out and look again
            var shift = 0
            while (shift < t.size && abs(t[shift]) < 1e-12) shift++
            val reduced = t.copyOfRange(shift, t.size)
            return ArrayList<Double>(rationalRoots(reduced)).also { it.add(0.0) }
        }
        // Rational root theorem: p divides the constant term, q divides the
        // leading coefficient, and the root is p / q.
        val ps = divisors(a0)
        val qs = divisors(an)
        val found = ArrayList<Double>()
        for (q in qs) for (pp in ps) {
            if (abs(q) < 1e-12) continue
            val cand = pp / q
            if (abs(evaluate(t, cand)) < 1e-9) {
                if (found.none { abs(it - cand) < 1e-9 }) found.add(cand)
            }
        }
        return found
    }

    /** Positive and negative divisors of an integer-ish value. */
    private fun divisors(v: Double): List<Double> {
        val n = abs(round(v)).toLong()
        if (n == 0L) return listOf(1.0)
        val out = LinkedHashSet<Double>()
        var d = 1L
        while (d * d <= n) {
            if (n % d == 0L) {
                out.add(d.toDouble()); out.add(-d.toDouble())
                out.add((n / d).toDouble()); out.add(-(n / d).toDouble())
            }
            d++
        }
        return out.toList()
    }

    private fun divideByRoot(p: DoubleArray, root: Double): DoubleArray {
        val t = trim(p)
        if (t.size <= 1) return doubleArrayOf(0.0)
        val n = t.size - 1
        val out = DoubleArray(n)
        var carry = t[n]
        for (i in n - 1 downTo 0) {
            out[i] = carry
            carry = t[i] + carry * root
        }
        return trim(out)
    }

    /**
     * The factored form, e.g. "(x - 2)(x + 3)". Any part that has no rational
     * root is left as a polynomial factor.
     */
    fun factor(p: DoubleArray): String {
        val t = trim(p)
        if (t.size <= 1) return format(t)
        val lead = t[t.size - 1]
        val rs = rationalRoots(t).sorted()
        var remaining = t
        val factors = ArrayList<String>()
        for (r in rs) {
            remaining = divideByRoot(remaining, r)
            factors.add(rootFactor(r))
        }
        val sb = StringBuilder()
        if (abs(abs(lead) - 1.0) > 1e-12 && factors.isNotEmpty()) {
            sb.append(Format.number(lead))
        }
        factors.forEach { sb.append(it) }
        if (trim(remaining).size > 1) {
            sb.append("(").append(format(remaining)).append(")")
        }
        return if (sb.isEmpty()) format(t) else sb.toString()
    }

    private fun rootFactor(r: Double): String = when {
        abs(r) < 1e-12 -> "(x)"
        r > 0 -> "(x - ${Format.number(r)})"
        else -> "(x + ${Format.number(abs(r))})"
    }
}
