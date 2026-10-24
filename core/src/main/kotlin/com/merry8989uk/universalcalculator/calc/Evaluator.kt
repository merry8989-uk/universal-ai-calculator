package com.merry8989uk.universalcalculator.calc

import kotlin.math.PI
import kotlin.math.E
import kotlin.math.acosh
import kotlin.math.asinh
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * A small, dependency-free recursive-descent expression evaluator.
 *
 * Supports + - * / ^, parentheses, unary minus, postfix factorial (!) and a
 * proper percentage key (%):
 *   - `200 + 10%` = 220      (10% of 200, added)
 *   - `200 - 10%` = 180
 *   - `200 * 10%` = 20
 *   - `50%`       = 0.5
 * Use `mod(a, b)` for a remainder.
 */
object Evaluator {

    class EvalException(message: String) : Exception(message)

    /** Evaluates [expression]. [degrees] switches trig between degrees and radians. */
    fun evaluate(
        expression: String,
        degrees: Boolean = false,
        vars: Map<String, Double> = emptyMap(),
    ): Double {
        val parser = Parser(expression, degrees, vars)
        val v = parser.parseExpression()
        parser.expectEnd()
        if (v.isNaN()) throw EvalException("Result is not a number")
        if (v.isInfinite()) throw EvalException("Result is infinite (overflow)")
        return v
    }

    private class Parser(
        private val s: String,
        private val degrees: Boolean,
        private val vars: Map<String, Double>,
    ) {
        private var i = 0

        /** A value plus whether it was written with a trailing percent sign. */
        private data class Operand(val value: Double, val isPercent: Boolean)

        private fun peek(): Char? = if (i < s.length) s[i] else null
        private fun skipSpaces() { while (i < s.length && s[i].isWhitespace()) i++ }

        fun expectEnd() {
            skipSpaces()
            if (i < s.length) throw EvalException("Unexpected '${s[i]}' at position $i")
        }

        fun parseExpression(): Double {
            var acc = parseTerm()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '+' -> {
                        i++
                        val r = parseTerm()
                        acc = Operand(
                            if (r.isPercent) acc.value + acc.value * r.value else acc.value + r.value,
                            false,
                        )
                    }
                    '-' -> {
                        i++
                        val r = parseTerm()
                        acc = Operand(
                            if (r.isPercent) acc.value - acc.value * r.value else acc.value - r.value,
                            false,
                        )
                    }
                    else -> return acc.value
                }
            }
        }

        private fun parseTerm(): Operand {
            var acc = parseUnary()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '*' -> { i++; val r = parseUnary(); acc = Operand(acc.value * r.value, false) }
                    '/' -> {
                        i++
                        val r = parseUnary()
                        if (r.value == 0.0) throw EvalException("Division by zero")
                        acc = Operand(acc.value / r.value, false)
                    }
                    // Implicit multiplication: 2pi, 3(4+5), (2)(3)
                    null -> return acc
                    else -> {
                        if (peek()!!.isLetter() || peek() == '(') {
                            val r = parseUnary()
                            acc = Operand(acc.value * r.value, false)
                        } else return acc
                    }
                }
            }
        }

        private fun parseUnary(): Operand {
            skipSpaces()
            when (peek()) {
                '-' -> { i++; val v = parseUnary(); return Operand(-v.value, v.isPercent) }
                '+' -> { i++; return parseUnary() }
            }
            return parsePower()
        }

        private fun parsePostfix(): Operand {
            var v = parseAtom()
            var percent = false
            skipSpaces()
            while (peek() == '%') {
                i++
                percent = true
                skipSpaces()
            }
            while (peek() == '!') {
                i++
                v = factorial(v)
                skipSpaces()
            }
            // A degree sign marks the number as an angle: in DEG mode it is left
            // alone (trig already reads degrees), in RAD mode it becomes radians.
            while (peek() == '\u00B0') {
                i++
                v = if (degrees) v else v * PI / 180.0
                skipSpaces()
            }
            return Operand(if (percent) v / 100.0 else v, percent)
        }

        private fun parsePower(): Operand {
            val base = parsePostfix()
            skipSpaces()
            if (peek() == '^') {
                i++
                val exponent = parseUnary()
                return Operand(base.value.pow(exponent.value), false)
            }
            return base
        }

        private fun parseAtom(): Double {
            skipSpaces()
            val c = peek() ?: throw EvalException("Unexpected end of expression")
            if (c == '(') {
                i++
                val v = parseExpression()
                skipSpaces()
                if (peek() != ')') throw EvalException("Missing ')'")
                i++
                return v
            }
            if (c.isDigit() || c == '.') return parseNumber()
            if (c.isLetter()) return parseIdentifier()
            throw EvalException("Unexpected '$c' at position $i")
        }

        private fun parseNumber(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                val save = i
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                if (i < s.length && s[i].isDigit()) {
                    while (i < s.length && s[i].isDigit()) i++
                } else {
                    i = save
                }
            }
            val text = s.substring(start, i)
            return text.toDoubleOrNull() ?: throw EvalException("Invalid number '$text'")
        }

        private fun parseIdentifier(): Double {
            val start = i
            while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_')) i++
            val name = s.substring(start, i).lowercase()
            skipSpaces()
            if (peek() == '(') {
                i++
                val args = mutableListOf<Double>()
                skipSpaces()
                if (peek() != ')') {
                    args.add(parseExpression())
                    skipSpaces()
                    while (peek() == ',') {
                        i++
                        args.add(parseExpression())
                        skipSpaces()
                    }
                }
                if (peek() != ')') throw EvalException("Missing ')' after $name")
                i++
                return applyFunction(name, args)
            }
            return when (name) {
                "pi" -> PI
                "e" -> E
                "tau" -> 2 * PI
                "phi" -> (1 + sqrt(5.0)) / 2
                "ans" -> vars["ans"] ?: 0.0
                else -> vars[name]
                    // Identifiers are folded to lower case above, so a caller
                    // that stored its variables as "R" would otherwise be told
                    // there is no such name. Match without regard to case.
                    ?: vars.entries.firstOrNull { it.key.lowercase() == name }?.value
                    ?: throw EvalException("Unknown name '$name'")
            }
        }

        private fun applyFunction(name: String, a: List<Double>): Double {
            fun one(): Double = a.firstOrNull() ?: throw EvalException("$name() needs an argument")
            fun two(): Pair<Double, Double> = if (a.size >= 2) {
                a[0] to a[1]
            } else {
                throw EvalException("$name() needs two arguments")
            }
            fun toRad(x: Double) = if (degrees) Math.toRadians(x) else x
            fun fromRad(x: Double) = if (degrees) Math.toDegrees(x) else x

            return when (name) {
                // ---- trigonometric ----
                "sin" -> sin(toRad(one()))
                "cos" -> cos(toRad(one()))
                "tan" -> tan(toRad(one()))
                "csc" -> 1.0 / sin(toRad(one()))
                "sec" -> 1.0 / cos(toRad(one()))
                "cot" -> 1.0 / tan(toRad(one()))
                "sinc" -> {
                    val x = one()
                    if (x == 0.0) 1.0 else sin(x) / x
                }

                // ---- inverse trigonometric ----
                "asin" -> fromRad(asin(one()))
                "acos" -> fromRad(acos(one()))
                "atan" -> fromRad(atan(one()))
                "acot" -> {
                    val x = one()
                    val r = atan(1.0 / x)
                    fromRad(if (x < 0) r + PI else r)
                }
                "asec" -> fromRad(acos(1.0 / one()))
                "acsc" -> fromRad(asin(1.0 / one()))
                "atan2" -> {
                    val (y, x) = two()
                    fromRad(atan2(y, x))
                }

                // ---- hyperbolic ----
                "sinh" -> sinh(one())
                "cosh" -> cosh(one())
                "tanh" -> tanh(one())
                "coth" -> 1.0 / tanh(one())
                "sech" -> 1.0 / cosh(one())
                "csch" -> 1.0 / sinh(one())

                // ---- inverse hyperbolic ----
                "asinh" -> asinh(one())
                "acosh" -> acosh(one())
                "atanh" -> atanh(one())
                "acoth" -> 0.5 * ln((one() + 1.0) / (one() - 1.0))
                "asech" -> acosh(1.0 / one())
                "acsch" -> asinh(1.0 / one())

                // ---- exponential and logarithmic ----
                "ln" -> ln(one())
                "log" -> if (a.size == 2) log(a[0], a[1]) else log10(one())
                "log10" -> log10(one())
                "log2" -> log2(one())
                "log1p" -> ln(1.0 + one())
                "exp" -> exp(one())
                "exp2" -> 2.0.pow(one())
                "expm1" -> exp(one()) - 1.0

                // ---- powers and roots ----
                "sqrt" -> sqrt(one())
                "cbrt" -> cbrt(one())
                "square" -> one().pow(2.0)
                "cube" -> one().pow(3.0)
                "nthroot" -> {
                    val (x, n) = two()
                    if (n == 0.0) throw EvalException("nthroot() needs a non-zero root")
                    if (x < 0 && abs(n % 2.0) == 1.0) -(-x).pow(1.0 / n)
                    else if (x < 0) throw EvalException("Even root of a negative number")
                    else x.pow(1.0 / n)
                }
                "hypot" -> {
                    val (x, y) = two()
                    hypot(x, y)
                }
                "pow" -> {
                    val (x, y) = two()
                    x.pow(y)
                }

                // ---- rounding and number handling ----
                "abs" -> abs(one())
                "floor" -> floor(one())
                "ceil" -> ceil(one())
                "round" -> round(one())
                "trunc" -> {
                    val x = one()
                    if (x >= 0) floor(x) else ceil(x)
                }
                "frac" -> {
                    val x = one()
                    x - (if (x >= 0) floor(x) else ceil(x))
                }
                "sign" -> when {
                    one() > 0 -> 1.0
                    one() < 0 -> -1.0
                    else -> 0.0
                }
                "rint" -> Math.rint(one())
                "clamp" -> {
                    if (a.size < 3) throw EvalException("clamp() needs three arguments")
                    a[0].coerceIn(a[1], a[2])
                }
                "mround" -> {
                    val (x, m) = two()
                    if (m == 0.0) throw EvalException("mround() needs a non-zero multiple")
                    round(x / m) * m
                }
                "even" -> 2.0 * round(one() / 2.0)
                "odd" -> 2.0 * round((one() - 1.0) / 2.0) + 1.0

                // ---- number theory ----
                "fact" -> factorial(one())
                "gcd" -> {
                    val (x, y) = two()
                    gcd(x.toLong(), y.toLong()).toDouble()
                }
                "lcm" -> {
                    val (x, y) = two()
                    lcm(x.toLong(), y.toLong()).toDouble()
                }
                "mod" -> {
                    val (x, y) = two()
                    if (y == 0.0) throw EvalException("Modulo by zero")
                    x % y
                }
                "isprime" -> if (isPrime(one().toLong())) 1.0 else 0.0
                "nextprime" -> nextPrime(one().toLong()).toDouble()
                "totient" -> totient(one().toLong()).toDouble()
                "isqrt" -> floor(sqrt(one()))
                "fib" -> fibonacci(one().toInt()).toDouble()
                "catalan" -> catalan(one().toInt()).toDouble()

                // ---- combinatorics ----
                "ncr" -> {
                    val (n, r) = two()
                    combinations(n, r)
                }
                "npr" -> {
                    val (n, r) = two()
                    permutations(n, r)
                }

                // ---- statistics over the arguments ----
                "sum" -> a.sum()
                "prod" -> a.fold(1.0) { acc, v -> acc * v }
                "count" -> a.size.toDouble()
                "mean" -> if (a.isEmpty()) 0.0 else a.sum() / a.size
                "min" -> a.minOrNull() ?: 0.0
                "max" -> a.maxOrNull() ?: 0.0
                "range" -> if (a.isEmpty()) 0.0 else (a.maxOrNull() ?: 0.0) - (a.minOrNull() ?: 0.0)
                "median" -> medianOf(a)
                "mode" -> modeOf(a)
                "var" -> varianceOf(a)
                "sd" -> sqrt(varianceOf(a))
                "geomean" -> {
                    if (a.isEmpty() || a.any { it <= 0 }) {
                        throw EvalException("geomean() needs positive values")
                    }
                    exp(a.sumOf { ln(it) } / a.size)
                }
                "harmmean" -> {
                    if (a.isEmpty() || a.any { it == 0.0 }) {
                        throw EvalException("harmmean() needs non-zero values")
                    }
                    a.size / a.sumOf { 1.0 / it }
                }

                // ---- special functions ----
                "gamma" -> SpecialFunctions.gamma(one())
                "lgamma" -> SpecialFunctions.logGamma(one())
                "beta" -> {
                    val (x, y) = two()
                    SpecialFunctions.beta(x, y)
                }
                "erf" -> SpecialFunctions.erf(one())
                "erfc" -> SpecialFunctions.erfc(one())
                "zeta" -> SpecialFunctions.zeta(one())
                "lambertw" -> SpecialFunctions.lambertW(one())
                "digamma" -> SpecialFunctions.digamma(one())

                // ---- complex, for real inputs ----
                "real" -> one()
                "imag" -> 0.0
                "conj" -> one()
                "arg" -> if (one() >= 0) 0.0 else PI

                // ---- misc ----
                "rand" -> Math.random()
                "heaviside" -> when {
                    one() < 0 -> 0.0
                    one() > 0 -> 1.0
                    else -> 0.5
                }
                "radians" -> Math.toRadians(one())
                "degrees" -> Math.toDegrees(one())
                "deg2rad" -> Math.toRadians(one())
                "rad2deg" -> Math.toDegrees(one())
                "pct" -> one() / 100.0

                else -> throw EvalException("Unknown function '$name'")
            }
        }

        // ---- helpers the function table leans on ----

        private fun medianOf(values: List<Double>): Double {
            if (values.isEmpty()) return 0.0
            val sorted = values.sorted()
            val mid = sorted.size / 2
            return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
        }

        private fun modeOf(values: List<Double>): Double {
            if (values.isEmpty()) return 0.0
            val counts = HashMap<Double, Int>()
            values.forEach { counts[it] = (counts[it] ?: 0) + 1 }
            // the most frequent, and the smallest of those if there is a tie
            return counts.entries
                .sortedWith(compareByDescending<Map.Entry<Double, Int>> { it.value }.thenBy { it.key })
                .first()
                .key
        }

        /** Population variance. */
        private fun varianceOf(values: List<Double>): Double {
            if (values.size < 1) return 0.0
            val mean = values.sum() / values.size
            return values.sumOf { (it - mean) * (it - mean) } / values.size
        }

        private fun isPrime(n: Long): Boolean {
            if (n < 2) return false
            if (n % 2 == 0L) return n == 2L
            if (n % 3 == 0L) return n == 3L
            var d = 5L
            while (d * d <= n) {
                if (n % d == 0L || n % (d + 2) == 0L) return false
                d += 6
            }
            return true
        }

        private fun nextPrime(after: Long): Long {
            var n = if (after < 2) 2 else after + 1
            while (!isPrime(n)) n++
            return n
        }

        private fun totient(n: Long): Long {
            if (n < 1) return 0
            var result = n
            var remaining = n
            var p = 2L
            while (p * p <= remaining) {
                if (remaining % p == 0L) {
                    while (remaining % p == 0L) remaining /= p
                    result -= result / p
                }
                p++
            }
            if (remaining > 1) result -= result / remaining
            return result
        }

        private fun fibonacci(n: Int): Long {
            if (n <= 0) return 0
            var a = 0L
            var b = 1L
            repeat(n - 1) {
                val next = a + b
                a = b
                b = next
            }
            return b
        }

        /** The n-th Catalan number. */
        private fun catalan(n: Int): Long {
            if (n < 0) return 0
            var result = 1L
            for (i in 0 until n) result = result * (2L * n - i) / (i + 1)
            return result / (n + 1)
        }
    }

    fun factorial(x: Double): Double {
        if (x < 0 || x != floor(x)) throw EvalException("Factorial needs a non-negative whole number")
        if (x > 170) throw EvalException("Factorial is too large")
        var r = 1.0
        var k = 2.0
        while (k <= x) { r *= k; k += 1.0 }
        return r
    }

    fun combinations(n: Double, r: Double): Double {
        if (r < 0 || n < 0 || r > n) throw EvalException("nCr needs 0 <= r <= n")
        return factorial(n) / (factorial(r) * factorial(n - r))
    }

    fun permutations(n: Double, r: Double): Double {
        if (r < 0 || n < 0 || r > n) throw EvalException("nPr needs 0 <= r <= n")
        return factorial(n) / factorial(n - r)
    }

    fun gcd(a: Long, b: Long): Long {
        var x = abs(a); var y = abs(b)
        while (y != 0L) { val t = x % y; x = y; y = t }
        return x
    }

    fun lcm(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        return abs(a / gcd(a, b) * b)
    }
}
