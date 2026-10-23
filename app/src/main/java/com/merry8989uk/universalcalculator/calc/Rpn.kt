package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Reverse-Polish (stack) arithmetic. Pure functions over an immutable stack so
 * the whole mode is unit-tested without a screen.
 */
object Rpn {

    /** Every operation the RPN keypad offers, in display order. */
    val operations: List<String> = listOf(
        "+", "-", "*", "/", "^",
        "sqrt", "inv", "neg", "abs",
        "ln", "log", "exp",
        "swap", "drop", "dup",
    )

    private val binary = setOf("+", "-", "*", "/", "^")
    private val unary = setOf("sqrt", "inv", "neg", "abs", "ln", "log", "exp")

    fun push(stack: List<Double>, value: Double): List<Double> = stack + value

    /** Applies one operation, returning the new stack. */
    fun apply(stack: List<Double>, op: String): List<Double> {
        when (op) {
            "swap" -> {
                if (stack.size < 2) throw Evaluator.EvalException("swap needs two values on the stack")
                val n = stack.size
                return stack.toMutableList().also { it[n - 1] = stack[n - 2]; it[n - 2] = stack[n - 1] }
            }
            "drop" -> {
                if (stack.isEmpty()) throw Evaluator.EvalException("drop needs a value on the stack")
                return stack.dropLast(1)
            }
            "dup" -> {
                if (stack.isEmpty()) throw Evaluator.EvalException("dup needs a value on the stack")
                return stack + stack.last()
            }
            "clear" -> return emptyList()
        }

        if (op in unary) {
            if (stack.isEmpty()) throw Evaluator.EvalException("$op needs a value on the stack")
            val x = stack.last()
            val r = when (op) {
                "sqrt" -> {
                    if (x < 0) throw Evaluator.EvalException("sqrt of a negative number")
                    sqrt(x)
                }
                "inv" -> {
                    if (x == 0.0) throw Evaluator.EvalException("Division by zero")
                    1.0 / x
                }
                "neg" -> -x
                "abs" -> abs(x)
                "ln" -> {
                    if (x <= 0) throw Evaluator.EvalException("ln needs a positive number")
                    ln(x)
                }
                "log" -> {
                    if (x <= 0) throw Evaluator.EvalException("log needs a positive number")
                    log10(x)
                }
                "exp" -> exp(x)
                else -> throw Evaluator.EvalException("Unknown operation '$op'")
            }
            return stack.dropLast(1) + r
        }

        if (op in binary) {
            if (stack.size < 2) throw Evaluator.EvalException("$op needs two values on the stack")
            val b = stack[stack.size - 1]
            val a = stack[stack.size - 2]
            val r = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> {
                    if (b == 0.0) throw Evaluator.EvalException("Division by zero")
                    a / b
                }
                "^" -> a.pow(b)
                else -> throw Evaluator.EvalException("Unknown operation '$op'")
            }
            return stack.dropLast(2) + r
        }

        throw Evaluator.EvalException("Unknown operation '$op'")
    }
}
