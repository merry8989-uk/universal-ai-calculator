package com.merry8989uk.universalcalculator.calc

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** A complex number, with the arithmetic the calculator needs. */
data class Complex(val re: Double, val im: Double) {

    operator fun plus(o: Complex) = Complex(re + o.re, im + o.im)
    operator fun minus(o: Complex) = Complex(re - o.re, im - o.im)
    operator fun times(o: Complex) = Complex(re * o.re - im * o.im, re * o.im + im * o.re)
    operator fun div(o: Complex): Complex {
        val d = o.re * o.re + o.im * o.im
        if (d == 0.0) throw Evaluator.EvalException("Division by zero")
        return Complex((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d)
    }

    fun abs(): Double = hypot(re, im)

    /** Argument (angle) in radians. */
    fun arg(): Double = atan2(im, re)

    fun conj() = Complex(re, -im)
    fun neg() = Complex(-re, -im)
    fun isReal(eps: Double = 1e-12) = kotlin.math.abs(im) < eps

    fun pow(n: Int): Complex {
        var r = Complex(1.0, 0.0)
        val base = if (n < 0) Complex(1.0, 0.0).div(this) else this
        repeat(kotlin.math.abs(n)) { r = r.times(base) }
        return r
    }

    fun sqrt(): Complex {
        val r = abs()
        val reNew = sqrt(((r + re) / 2.0).coerceAtLeast(0.0))
        var imNew = sqrt(((r - re) / 2.0).coerceAtLeast(0.0))
        if (im < 0) imNew = -imNew
        return Complex(reNew, imNew)
    }

    override fun toString(): String = text(this)

    companion object {
        val ZERO = Complex(0.0, 0.0)
        fun of(re: Double, im: Double = 0.0) = Complex(re, im)

        /** Parses "3+4i", "2i", "-5", "1 - 2i", "3+4j" and so on. */
        fun parse(input: String): Complex {
            var s = input.trim().lowercase().replace(" ", "").replace("j", "i")
            if (s.isEmpty()) throw Evaluator.EvalException("Enter a complex number")
            val sb = StringBuilder()
            for ((idx, ch) in s.withIndex()) {
                if (ch == '-' && idx > 0 && sb.isNotEmpty() && sb.last() != 'e') sb.append('+')
                sb.append(ch)
            }
            var re = 0.0
            var im = 0.0
            for (part in sb.toString().split('+').filter { it.isNotEmpty() }) {
                if (part.contains('i')) {
                    val coeff = part.replace("i", "")
                    im += when (coeff) {
                        "", "+" -> 1.0
                        "-" -> -1.0
                        else -> coeff.toDoubleOrNull()
                            ?: throw Evaluator.EvalException("Bad imaginary part '$part'")
                    }
                } else {
                    re += part.toDoubleOrNull()
                        ?: throw Evaluator.EvalException("Bad number '$part'")
                }
            }
            return Complex(re, im)
        }

        /** Renders a complex number the way a person would write it. */
        fun text(c: Complex): String {
            val r = if (kotlin.math.abs(c.re) < 1e-12) 0.0 else c.re
            val i = if (kotlin.math.abs(c.im) < 1e-12) 0.0 else c.im
            if (i == 0.0) return Format.number(r)
            val imPart = when {
                kotlin.math.abs(i - 1.0) < 1e-12 -> "i"
                kotlin.math.abs(i + 1.0) < 1e-12 -> "-i"
                else -> Format.number(i) + "i"
            }
            if (r == 0.0) return imPart
            return Format.number(r) + (if (i > 0) " + " else " - ") +
                imPart.removePrefix("-")
        }

        /** Polar form: magnitude and angle. */
        fun polar(c: Complex, degrees: Boolean = true): Pair<Double, Double> {
            val a = if (degrees) c.arg() * 180.0 / PI else c.arg()
            return c.abs() to a
        }

        fun fromPolar(r: Double, angleDeg: Double): Complex {
            val a = angleDeg * PI / 180.0
            return Complex(r * cos(a), r * sin(a))
        }
    }
}
