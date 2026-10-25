package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * One move in a worked solution.
 *
 * [piece] is the bit that was worked out, [value] is what it came to, and
 * [result] is the whole expression afterwards — so the reader can see the
 * expression shrinking one step at a time.
 */
data class SolutionStep(
    /** What was worked out, as it appeared. */
    val piece: String,
    /** What it came to. */
    val value: String,
    /** The whole expression after that step. */
    val result: String,
    /** Why this piece went first, in a few words. */
    val why: String,
)

/**
 * Works an expression out a step at a time, in the order the calculator does.
 *
 * This is not a second evaluator. Each move takes one operation, works it out,
 * and puts the answer back into the expression, so the final move must land on
 * the same answer [Evaluator] gives. When it does not — because the two disagree
 * about something subtle like whether powers group to the right — the steps are
 * thrown away rather than shown, because wrong working is worse than none.
 */
object StepSolver {

    private const val NUMBER = "\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?"

    /** The functions a single move may work out on its own. */
    private fun call(name: String, x: Double, degrees: Boolean): Double? {
        val angle = if (degrees) Math.toRadians(x) else x
        return when (name.lowercase()) {
            "sin" -> sin(angle)
            "cos" -> cos(angle)
            "tan" -> tan(angle)
            "sqrt" -> if (x < 0) null else sqrt(x)
            "cbrt" -> Math.cbrt(x)
            "ln" -> if (x <= 0) null else ln(x)
            "log" -> if (x <= 0) null else log10(x)
            "log2" -> if (x <= 0) null else ln(x) / ln(2.0)
            "exp" -> exp(x)
            "abs" -> abs(x)
            else -> null
        }
    }

    /** A number written the way the calculator writes one. */
    private fun written(v: Double): String = Format.number(v)

    private fun findPiece(
        expression: String,
        degrees: Boolean,
    ): Triple<Int, Int, String>? {
        // a function call on a plain number, which is the innermost thing there is
        val call = Regex("([a-zA-Z]+)\\(\\s*($NUMBER)\\s*\\)").find(expression)
        if (call != null) {
            val name = call.groupValues[1]
            val arg = call.groupValues[2].toDoubleOrNull()
            if (arg != null && call(name, arg, degrees) != null) {
                return Triple(call.range.first, call.range.last + 1, "call")
            }
        }
        // the innermost bracket
        val open = ArrayList<Int>()
        for ((i, c) in expression.withIndex()) {
            if (c == '(') open.add(i)
            if (c == ')' && open.isNotEmpty()) {
                val a = open.removeAt(open.size - 1)
                val inner = expression.substring(a + 1, i)
                for (pattern in listOf(
                    "($NUMBER)\\s*(\\^|[*\\/])\\s*($NUMBER)",
                    "($NUMBER)\\s*([+-])\\s*($NUMBER)",
                )) {
                    val m = Regex(pattern).find(inner)
                    if (m != null) {
                        return Triple(a + 1 + m.range.first, a + 1 + m.range.last + 1, "op")
                    }
                }
                // a bracket holding only a number can go, unless it is a
                // function's argument — sin(30) must keep its brackets
                if (Regex("\\s*$NUMBER\\s*").matches(inner)) {
                    val before = if (a > 0) expression[a - 1] else ' '
                    if (!before.isLetter()) return Triple(a, i + 1, "group")
                }
            }
        }
        // nothing bracketed left: go by precedence, powers first
        for (ops in listOf("\\^", "[*/]", "[+-]")) {
            val m = Regex("($NUMBER)\\s*($ops)\\s*($NUMBER)").find(expression)
            if (m != null) return Triple(m.range.first, m.range.last + 1, "op")
        }
        return null
    }

    private fun whyFirst(kind: String, piece: String): String = when {
        kind == "call" -> "work out the function first"
        kind == "group" -> "the brackets have done their job"
        piece.contains('^') -> "powers come before multiplying"
        piece.contains('*') || piece.contains('/') -> "multiplying and dividing come before adding"
        else -> "adding and subtracting last"
    }

    private fun reduceOnce(
        expression: String,
        degrees: Boolean,
    ): Triple<String, String, String>? {
        val found = findPiece(expression, degrees) ?: return null
        val (a, b, kind) = found
        val piece = expression.substring(a, b)
        val value: String = when (kind) {
            "call" -> {
                val m = Regex("([a-zA-Z]+)\\(\\s*($NUMBER)\\s*\\)").find(piece) ?: return null
                val name = m.groupValues[1]
                val arg = m.groupValues[2].toDoubleOrNull() ?: return null
                val v = call(name, arg, degrees) ?: return null
                written(v)
            }
            "group" -> piece.substring(1, piece.length - 1).trim()
            else -> {
                val m = Regex("\\s*($NUMBER)\\s*([\\^*/+-])\\s*($NUMBER)\\s*").find(piece)
                    ?: return null
                val x = m.groupValues[1].toDoubleOrNull() ?: return null
                val op = m.groupValues[2]
                val y = m.groupValues[3].toDoubleOrNull() ?: return null
                val v = when (op) {
                    "+" -> x + y
                    "-" -> x - y
                    "*" -> x * y
                    "/" -> if (y == 0.0) return null else x / y
                    "^" -> x.pow(y)
                    else -> return null
                }
                if (!v.isFinite()) return null
                written(v)
            }
        }
        val result = expression.substring(0, a) + value + expression.substring(b)
        return Triple(piece.trim(), value, result)
    }

    /**
     * The working, or an empty list when it cannot be shown honestly.
     *
     * [answer] is the number the calculator already gave. The steps must arrive
     * at the same one; if they do not, there is nothing to show.
     */
    fun steps(
        expression: String,
        answer: Double?,
        degrees: Boolean = false,
        limit: Int = 14,
    ): List<SolutionStep> {
        val start = expression.replace(" ", "")
        if (start.isEmpty()) return emptyList()
        val out = ArrayList<SolutionStep>()
        var current = start
        repeat(limit) {
            val move = reduceOnce(current, degrees) ?: return@repeat
            val (piece, value, result) = move
            out.add(SolutionStep(piece, value, result, whyFirst("op", piece)))
            current = result
            if (result == value) return@repeat
        }
        if (out.isEmpty()) return emptyList()
        // the working has to agree with the answer the calculator gave
        val arrived = current.toDoubleOrNull() ?: return emptyList()
        if (answer != null && abs(arrived - answer) > 1e-6 * maxOf(1.0, abs(answer))) {
            return emptyList()
        }
        return out
    }
}
