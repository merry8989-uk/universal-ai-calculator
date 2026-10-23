package com.merry8989uk.universalcalculator.calc

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * The same expression grammar as [Evaluator], but the arithmetic is done on
 * complex numbers, so the `i` key works on the main calculator.
 */
object ComplexEvaluator {

    /** True when the expression contains a standalone `i` (not part of pi/sin/...). */
    fun hasImaginary(expr: String): Boolean {
        val s = expr.lowercase()
        for (idx in s.indices) {
            if (s[idx] != 'i') continue
            val before = if (idx > 0) s[idx - 1] else ' '
            val after = if (idx + 1 < s.length) s[idx + 1] else ' '
            if (!before.isLetter() && !after.isLetter()) return true
        }
        return false
    }

    fun evaluate(expr: String, degrees: Boolean = false, vars: Map<String, Complex> = emptyMap()): Complex =
        Parser(expr, degrees, vars).parse()

    // ---------- complex elementary functions ----------

    fun ln(z: Complex): Complex {
        if (z.re == 0.0 && z.im == 0.0) throw Evaluator.EvalException("ln of zero")
        return Complex(ln(z.abs()), z.arg())
    }

    fun exp(z: Complex): Complex = Complex(exp(z.re) * cos(z.im), exp(z.re) * sin(z.im))

    fun sin(z: Complex): Complex = Complex(sin(z.re) * cosh(z.im), cos(z.re) * sinh(z.im))

    fun cos(z: Complex): Complex = Complex(cos(z.re) * cosh(z.im), -sin(z.re) * sinh(z.im))

    fun tan(z: Complex): Complex = sin(z).div(cos(z))

    fun sinh(z: Complex): Complex = Complex(sinh(z.re) * cos(z.im), cosh(z.re) * sin(z.im))

    fun cosh(z: Complex): Complex = Complex(cosh(z.re) * cos(z.im), sinh(z.re) * sin(z.im))

    fun tanh(z: Complex): Complex = sinh(z).div(cosh(z))

    fun sqrt(z: Complex): Complex = z.sqrt()

    fun pow(z: Complex, w: Complex): Complex {
        if (w.re == 0.0 && w.im == 0.0) return Complex(1.0, 0.0)
        if (z.re == 0.0 && z.im == 0.0) return Complex(0.0, 0.0)
        return exp(ln(z).times(w))
    }

    fun asin(z: Complex): Complex {
        val one = Complex(1.0, 0.0)
        val iz = Complex(-z.im, z.re)
        val root = pow(one.minus(z.times(z)), Complex(0.5, 0.0))
        val inner = iz.plus(root)
        val l = ln(inner)
        return Complex(l.im, -l.re)   // -i * ln(...)
    }

    fun acos(z: Complex): Complex = Complex(PI / 2.0, 0.0).minus(asin(z))

    fun atan(z: Complex): Complex {
        val i = Complex(0.0, 1.0)
        val num = i.plus(z)
        val den = i.minus(z)
        val l = ln(num.div(den))
        // (i / 2) * ln(...)
        return Complex(-l.im / 2.0, l.re / 2.0)
    }

    // ---------- parser ----------

    private class Parser(
        private val src: String,
        private val degrees: Boolean,
        private val vars: Map<String, Complex>,
    ) {
        private var i = 0
        private fun peek(): Char? = if (i < src.length) src[i] else null
        private fun skip() { while (i < src.length && src[i].isWhitespace()) i++ }

        fun parse(): Complex {
            val v = parseExpression()
            skip()
            if (i < src.length) throw Evaluator.EvalException("Unexpected '${src[i]}' at position $i")
            return v
        }

        private fun parseExpression(): Complex {
            var left = parseTerm()
            while (true) {
                skip()
                when (peek()) {
                    '+' -> { i++; left = left.plus(parseTerm()) }
                    '-' -> { i++; left = left.minus(parseTerm()) }
                    else -> return left
                }
            }
        }

        private fun parseTerm(): Complex {
            var left = parseUnary()
            while (true) {
                skip()
                val c = peek()
                when {
                    c == '*' -> { i++; left = left.times(parseUnary()) }
                    c == '/' -> { i++; left = left.div(parseUnary()) }
                    // Implicit multiplication: 2i, 3(4+5), (2)(3)
                    c != null && (c.isLetter() || c == '(') ->
                        left = left.times(parseUnary())
                    else -> return left
                }
            }
        }

        private fun parseUnary(): Complex {
            skip()
            when (peek()) {
                '-' -> { i++; return parseUnary().neg() }
                '+' -> { i++; return parseUnary() }
            }
            return parsePower()
        }

        private fun parsePower(): Complex {
            val base = parsePostfix()
            skip()
            if (peek() == '^') {
                i++
                return ComplexEvaluator.pow(base, parseUnary())
            }
            return base
        }

        private fun parsePostfix(): Complex {
            var v = parseAtom()
            skip()
            while (peek() == '%') { i++; v = v.div(Complex(100.0, 0.0)); skip() }
            while (peek() == '!') {
                i++
                if (abs(v.im) > 1e-12) throw Evaluator.EvalException("Factorial needs a real number")
                v = Complex(Evaluator.factorial(v.re), 0.0)
                skip()
            }
            while (peek() == '\u00B0') {
                i++
                if (!degrees) v = v.times(Complex(PI / 180.0, 0.0))
                skip()
            }
            return v
        }

        private fun parseAtom(): Complex {
            skip()
            val c = peek() ?: throw Evaluator.EvalException("The expression ends too early")
            if (c == '(') {
                i++
                val v = parseExpression()
                skip()
                if (peek() != ')') throw Evaluator.EvalException("Missing ')'")
                i++
                return v
            }
            if (c.isDigit() || c == '.') return Complex(parseNumber(), 0.0)
            if (c.isLetter()) return parseIdentifier()
            throw Evaluator.EvalException("Unexpected '$c' at position $i")
        }

        private fun parseNumber(): Double {
            val start = i
            while (peek()?.isDigit() == true || peek() == '.') i++
            // scientific notation, e.g. 1.5e-3
            if (peek() == 'e' || peek() == 'E') {
                val save = i
                i++
                if (peek() == '+' || peek() == '-') i++
                if (peek()?.isDigit() == true) {
                    while (peek()?.isDigit() == true) i++
                } else {
                    i = save
                }
            }
            val text = src.substring(start, i)
            return text.toDoubleOrNull() ?: throw Evaluator.EvalException("Bad number '$text'")
        }

        private fun parseIdentifier(): Complex {
            val start = i
            while (peek()?.isLetter() == true) i++
            val name = src.substring(start, i).lowercase()
            skip()
            if (peek() == '(') {
                i++
                val args = ArrayList<Complex>()
                skip()
                if (peek() != ')') {
                    args.add(parseExpression())
                    skip()
                    while (peek() == ',') { i++; args.add(parseExpression()); skip() }
                }
                if (peek() != ')') throw Evaluator.EvalException("Missing ')' after $name")
                i++
                return applyFunction(name, args)
            }
            return when (name) {
                "i" -> Complex(0.0, 1.0)
                "pi" -> Complex(PI, 0.0)
                "e" -> Complex(kotlin.math.E, 0.0)
                else -> vars[name] ?: throw Evaluator.EvalException("Unknown name '$name'")
            }
        }

        private fun applyFunction(name: String, a: List<Complex>): Complex {
            fun one(): Complex {
                if (a.size != 1) throw Evaluator.EvalException("$name takes one argument")
                return a[0]
            }
            fun angle(z: Complex): Complex =
                if (degrees) z.times(Complex(PI / 180.0, 0.0)) else z

            return when (name) {
                "sin" -> ComplexEvaluator.sin(angle(one()))
                "cos" -> ComplexEvaluator.cos(angle(one()))
                "tan" -> ComplexEvaluator.tan(angle(one()))
                "sinh" -> ComplexEvaluator.sinh(one())
                "cosh" -> ComplexEvaluator.cosh(one())
                "tanh" -> ComplexEvaluator.tanh(one())
                "asin" -> ComplexEvaluator.asin(one()).let { if (degrees) it.times(Complex(180.0 / PI, 0.0)) else it }
                "acos" -> ComplexEvaluator.acos(one()).let { if (degrees) it.times(Complex(180.0 / PI, 0.0)) else it }
                "atan" -> ComplexEvaluator.atan(one()).let { if (degrees) it.times(Complex(180.0 / PI, 0.0)) else it }
                "ln" -> ComplexEvaluator.ln(one())
                "log" -> if (a.size == 2) {
                    ComplexEvaluator.ln(a[0]).div(ComplexEvaluator.ln(a[1]))
                } else {
                    ComplexEvaluator.ln(one()).div(Complex(ln(10.0), 0.0))
                }
                "log2" -> ComplexEvaluator.ln(one()).div(Complex(ln(2.0), 0.0))
                "exp" -> ComplexEvaluator.exp(one())
                "sqrt" -> ComplexEvaluator.sqrt(one())
                "cbrt" -> ComplexEvaluator.pow(one(), Complex(1.0 / 3.0, 0.0))
                "abs" -> Complex(one().abs(), 0.0)
                "round" -> Complex(round(one().re), round(one().im))
                "floor" -> Complex(floor(one().re), floor(one().im))
                "ceil" -> Complex(kotlin.math.ceil(one().re), kotlin.math.ceil(one().im))
                "hypot" -> {
                    if (a.size != 2) throw Evaluator.EvalException("hypot takes two arguments")
                    Complex(kotlin.math.hypot(a[0].re, a[1].re), 0.0)
                }
                "re" -> Complex(one().re, 0.0)
                "im" -> Complex(one().im, 0.0)
                "conj" -> one().conj()
                "arg" -> Complex(one().arg(), 0.0)
                else -> throw Evaluator.EvalException("Unknown function '$name'")
            }
        }
    }
}
