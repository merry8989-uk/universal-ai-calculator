package com.merry8989uk.universalcalculator.diag

import com.merry8989uk.universalcalculator.calc.Evaluator

/**
 * Checks an assistant's answer against the calculator.
 *
 * The logging architecture asks for a mismatch indicator between the model and a
 * trusted deterministic solver, and it is the most useful number in the whole
 * set: a thumbs-up does not prove an answer is right, but the calculator does.
 *
 * This only speaks when it can speak with certainty. If the question is a plain
 * arithmetic expression the app can evaluate itself, and the answer contains a
 * single number, the two are compared. Anything else — a word problem, a proof,
 * an essay — is reported as not applicable rather than guessed at. A wrong
 * "mismatch" is worse than no data.
 */
object Verification {

    /** A pure arithmetic expression: digits and operators and nothing else. */
    private val ARITHMETIC = Regex("^[0-9+\\-*/^().%\\s]+$")

    private val NUMBER = Regex("-?\\d+(?:\\.\\d+)?")

    data class Result(val state: String, val detail: String = "")

    fun check(question: String, answer: String): Result {
        val q = question.trim()
        if (q.isEmpty() || q.length > 200) return Result("not_applicable")
        if (!ARITHMETIC.matches(q)) return Result("not_applicable")
        if (q.count { it.isDigit() } == 0) return Result("not_applicable")

        val expected = runCatching { Evaluator.evaluate(q) }.getOrNull()
        if (expected == null || expected.isNaN() || expected.isInfinite()) {
            return Result("not_applicable")
        }

        // The answer's numbers, in order. The last one is the result when the
        // answer works the sum out step by step, which is the usual shape.
        val numbers = NUMBER.findAll(answer)
            .mapNotNull { it.value.toDoubleOrNull() }
            .filter { !it.isNaN() && !it.isInfinite() }
            .toList()
        if (numbers.isEmpty()) return Result("unverified", "no number in the answer")

        val close = numbers.any { same(it, expected) }
        return if (close) {
            Result("match", "")
        } else {
            Result("mismatch", "calculator says ${trim(expected)}, answer says ${numbers.last()}")
        }
    }

    /**
     * Two numbers are the same if they round to the same thing at the precision
     * either one was written at — 1/3 and 0.333 are not a mismatch.
     */
    private fun same(a: Double, b: Double): Boolean {
        if (a == b) return true
        val scale = maxOf(Math.abs(a), Math.abs(b), 1.0)
        return Math.abs(a - b) / scale < 0.005
    }

    private fun trim(v: Double): String =
        if (v == Math.floor(v) && Math.abs(v) < 1e15) v.toLong().toString()
        else String.format("%.6g", v)
}
