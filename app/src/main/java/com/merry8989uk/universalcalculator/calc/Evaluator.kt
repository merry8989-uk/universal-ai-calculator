package com.merry8989uk.universalcalculator.calc

import kotlin.math.PI
import kotlin.math.E
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
                    else -> return acc
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
                else -> vars[name] ?: throw EvalException("Unknown name '$name'")
            }
        }

        private fun applyFunction(name: String, a: List<Double>): Double {
            fun one(): Double = a.firstOrNull() ?: throw EvalException("$name() needs an argument")
            fun toRad(x: Double) = if (degrees) Math.toRadians(x) else x
            fun fromRad(x: Double) = if (degrees) Math.toDegrees(x) else x
            return when (name) {
                "sin" -> sin(toRad(one()))
                "cos" -> cos(toRad(one()))
                "tan" -> tan(toRad(one()))
                "asin" -> fromRad(asin(one()))
                "acos" -> fromRad(acos(one()))
                "atan" -> fromRad(atan(one()))
                "sinh" -> sinh(one())
                "cosh" -> cosh(one())
                "tanh" -> tanh(one())
                "ln" -> ln(one())
                "log" -> if (a.size == 2) log(a[0], a[1]) else log10(one())
                "log10" -> log10(one())
                "log2" -> log2(one())
                "sqrt" -> sqrt(one())
                "cbrt" -> cbrt(one())
                "abs" -> abs(one())
                "exp" -> exp(one())
                "floor" -> floor(one())
                "ceil" -> ceil(one())
                "round" -> round(one())
                "fact" -> factorial(one())
                "pow" -> a[0].pow(a[1])
                "hypot" -> hypot(a[0], a[1])
                "min" -> a.minOrNull() ?: 0.0
                "max" -> a.maxOrNull() ?: 0.0
                "ncr" -> combinations(a[0], a[1])
                "npr" -> permutations(a[0], a[1])
                "gcd" -> gcd(a[0].toLong(), a[1].toLong()).toDouble()
                "lcm" -> lcm(a[0].toLong(), a[1].toLong()).toDouble()
                "mod" -> {
                    if (a.size < 2) throw EvalException("mod() needs two arguments")
                    if (a[1] == 0.0) throw EvalException("Modulo by zero")
                    a[0] % a[1]
                }
                "pct" -> one() / 100.0
                else -> throw EvalException("Unknown function '$name'")
            }
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
