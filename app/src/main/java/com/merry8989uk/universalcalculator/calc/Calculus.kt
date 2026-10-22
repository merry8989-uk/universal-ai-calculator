package com.merry8989uk.universalcalculator.calc

/** Numerical calculus over a single-variable expression in x. */
object Calculus {

    private fun f(expr: String, x: Double): Double =
        Evaluator.evaluate(expr, degrees = false, vars = mapOf("x" to x))

    /** Central-difference derivative of [expr] at [at]. */
    fun derivative(expr: String, at: Double, h: Double = 1e-5): Double =
        (f(expr, at + h) - f(expr, at - h)) / (2 * h)

    /** Second derivative by central difference. */
    fun secondDerivative(expr: String, at: Double, h: Double = 1e-4): Double =
        (f(expr, at + h) - 2 * f(expr, at) + f(expr, at - h)) / (h * h)

    /** Definite integral from [a] to [b] using composite Simpson's rule. */
    fun integrate(expr: String, a: Double, b: Double, n: Int = 1000): Double {
        val steps = if (n % 2 == 0) n else n + 1
        val h = (b - a) / steps
        var sum = f(expr, a) + f(expr, b)
        for (i in 1 until steps) {
            val x = a + i * h
            sum += if (i % 2 == 1) 4 * f(expr, x) else 2 * f(expr, x)
        }
        return sum * h / 3.0
    }

    /** Sum of f(x) for x = from..to step 1. */
    fun summation(expr: String, from: Int, to: Int): Double {
        var s = 0.0
        for (i in from..to) s += f(expr, i.toDouble())
        return s
    }
}
