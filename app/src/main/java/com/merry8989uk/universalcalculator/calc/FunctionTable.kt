package com.merry8989uk.universalcalculator.calc

/** One row of the table: an x and what each function made of it. */
data class TableRow(val x: Double, val f: Double?, val g: Double?)

/** A whole table, ready to draw. */
data class FunctionTable(
    val rows: List<TableRow>,
    /** True when the range was cut short because the entry limit was reached. */
    val truncated: Boolean,
    /** The step that was actually used, after the direction was worked out. */
    val step: Double,
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}

/**
 * Building a table of values for one or two functions.
 *
 * This is the TABLE mode a scientific calculator has: give it `f(x)`, a range and
 * a step, and it writes out what the function does across that range. The second
 * function is optional, which is what the `g(x)` switch in the settings is for.
 *
 * A value the expression cannot produce — a division by zero, a log of a negative
 * — leaves that cell empty rather than failing the whole table, because one bad
 * point in a hundred is not a reason to show nothing.
 */
object FunctionTableBuilder {

    /** How many rows are built before the range is cut short. */
    const val DEFAULT_MAX_ENTRIES = 200

    /** A ceiling on the ceiling, so a silly limit cannot hang the app. */
    const val HARD_MAX_ENTRIES = 5000

    /**
     * Builds the table.
     *
     * [step] may be given negative when [start] is above [stop]; the direction is
     * worked out from the two ends so the caller does not have to get it right.
     */
    fun build(
        fExpression: String,
        gExpression: String = "",
        start: Double,
        stop: Double,
        step: Double,
        maxEntries: Int = DEFAULT_MAX_ENTRIES,
        degrees: Boolean = false,
        angle: AngleUnit = AngleUnit.DEGREE,
        percentage: PercentageMode = PercentageMode.MULTIPLICATIVE,
        implied: ImpliedMultiplication = ImpliedMultiplication.SAME_PRECEDENCE,
    ): FunctionTable {
        val f = fExpression.trim()
        val g = gExpression.trim()
        if (f.isEmpty()) return FunctionTable(emptyList(), false, step)
        if (!start.isFinite() || !stop.isFinite() || !step.isFinite()) {
            return FunctionTable(emptyList(), false, step)
        }
        if (step == 0.0) return FunctionTable(emptyList(), false, step)

        // the direction follows the two ends, so a negative step is never needed
        val magnitude = kotlin.math.abs(step)
        val goingUp = stop >= start
        val actualStep = if (goingUp) magnitude else -magnitude

        val limit = maxEntries.coerceIn(1, HARD_MAX_ENTRIES)
        val rows = ArrayList<TableRow>(minOf(limit, 64))
        var truncated = false
        var x = start
        var count = 0

        // A tolerance so a step that lands just past the end is not dropped.
        val tolerance = magnitude * 1e-9

        while (count < limit) {
            if (goingUp && x > stop + tolerance) break
            if (!goingUp && x < stop - tolerance) break

            rows.add(
                TableRow(
                    x = round(x),
                    f = value(f, x, degrees, angle, percentage, implied),
                    g = if (g.isEmpty()) null else value(g, x, degrees, angle, percentage, implied),
                ),
            )
            count++
            x += actualStep
        }

        // If the loop stopped because of the limit rather than the range, say so.
        if (count >= limit) {
            val more = if (goingUp) x <= stop + tolerance else x >= stop - tolerance
            if (more) truncated = true
        }

        return FunctionTable(rows, truncated, actualStep)
    }

    /** Evaluates one expression at one x, or null when it has no value there. */
    private fun value(
        expression: String,
        x: Double,
        degrees: Boolean,
        angle: AngleUnit,
        percentage: PercentageMode,
        implied: ImpliedMultiplication,
    ): Double? = try {
        val v = Evaluator.evaluate(
            expression,
            degrees = degrees,
            vars = mapOf("x" to x),
            unit = angle,
            percentage = percentage,
            implied = implied,
        )
        if (v.isFinite()) round(v) else null
    } catch (e: Exception) {
        null
    }

    /** Trims the floating-point dust that a step of 0.1 leaves behind. */
    private fun round(v: Double): Double {
        val r = Math.round(v * 1e10) / 1e10
        return if (r == -0.0) 0.0 else r
    }

    /**
     * How many rows a range will produce, so the screen can warn before building.
     */
    fun countFor(start: Double, stop: Double, step: Double): Int {
        if (step == 0.0 || !start.isFinite() || !stop.isFinite() || !step.isFinite()) return 0
        val span = kotlin.math.abs(stop - start)
        val size = kotlin.math.abs(step)
        return (span / size).toInt() + 1
    }
}
