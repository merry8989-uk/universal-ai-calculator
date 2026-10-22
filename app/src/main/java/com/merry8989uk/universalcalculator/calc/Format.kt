package com.merry8989uk.universalcalculator.calc

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/** Number formatting used across every screen. */
object Format {

    /** A clean, human result string: trims noise, uses scientific for extremes. */
    fun number(v: Double): String {
        if (v.isNaN()) return "NaN"
        if (v.isInfinite()) return if (v > 0) "Infinity" else "-Infinity"
        if (v == 0.0) return "0"
        val a = abs(v)
        if (a >= 1e15 || a < 1e-9) {
            return String.format("%.6e", v).replace("e+", "e").replace("E", "e")
        }
        val rounded = BigDecimal(v.toString()).round(MathContext(12)).stripTrailingZeros()
        return rounded.toPlainString()
    }

    /** Fixed-decimal formatting for inputs/table cells. */
    fun decimal(v: Double, places: Int = 4): String =
        BigDecimal(v.toString()).setScale(places, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
