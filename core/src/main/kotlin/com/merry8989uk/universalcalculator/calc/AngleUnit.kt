package com.merry8989uk.universalcalculator.calc

import kotlin.math.PI

/**
 * How trigonometry reads its arguments.
 *
 * Degrees and radians are the usual two; gradians are the third the classic
 * scientific calculators offer, where a right angle is 100 rather than 90 or
 * π/2. A full turn is 360, 2π or 400 respectively.
 */
enum class AngleUnit {
    DEGREE,
    RADIAN,
    GRADIAN;

    /** The number of units in a full turn. */
    val perTurn: Double
        get() = when (this) {
            DEGREE -> 360.0
            RADIAN -> 2.0 * PI
            GRADIAN -> 400.0
        }

    /** An angle in this unit, expressed in radians. */
    fun toRadians(value: Double): Double = when (this) {
        RADIAN -> value
        else -> value * 2.0 * PI / perTurn
    }

    /** Radians, expressed in this unit. */
    fun fromRadians(value: Double): Double = when (this) {
        RADIAN -> value
        else -> value * perTurn / (2.0 * PI)
    }

    companion object {
        /** Tolerant of how the value was stored, e.g. "grad" or "GRADIAN". */
        fun of(name: String): AngleUnit = when (name.trim().uppercase()) {
            "RADIAN", "RAD", "R" -> RADIAN
            "GRADIAN", "GRAD", "GRA", "G" -> GRADIAN
            else -> DEGREE
        }
    }
}
