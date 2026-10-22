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
 * Supports + - * / % ^, parentheses, unary minus, postfix factorial (!),
 * a rich function library and the constants pi, e, tau, phi and ans.
 * It is the numeric core behind Standard, Scientific and the formula engine.
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

        private fun peek(): Char? = if (i < s.length) s[i] else null
        private fun skipSpaces() { while (i < s.length && s[i].isWhitespace()) i++ }

        fun expectEnd() {
            skipSpaces()
            if (i < s.length) throw EvalException("Unexpected '${s[i]}' at position $i")
        }

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '+' -> { i++; value += parseTerm() }
                    '-' -> { i++; value -= parseTerm() }
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '*' -> { i++; value *= parseUnary() }
                    '/' -> {
                        i++
                        val d = parseUnary()
                        if (d == 0.0) throw EvalException("Division by zero")
                        value /= d
                    }
                    '%' -> {
                        i++
                        val d = parseUnary()
                        if (d == 0.0) throw EvalException("Modulo by zero")
                        value %= d
                    }
                    else -> return value
                }
            }
        }

        private fun parseUnary(): Double {
            skipSpaces()
            when (peek()) {
                '-' -> { i++; return -parseUnary() }
                '+' -> { i++; return parseUnary() }
            }
            return parsePower()
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            skipSpaces()
            if (peek() == '^') {
                i++
                val exponent = parseUnary()
                return base.pow(exponent)
            }
            return base
        }

        private fun parsePostfix(): Double {
            var v = parseAtom()
            skipSpaces()
            while (peek() == '!') {
                i++
                v = factorial(v)
                skipSpaces()
            }
            return v
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
