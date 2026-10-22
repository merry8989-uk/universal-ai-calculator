package com.merry8989uk.universalcalculator.calc

import java.math.BigInteger

/** Base conversion and bitwise maths for the Programmer tool. */
object Programmer {

    fun toBase(value: Long, base: Int): String =
        BigInteger.valueOf(value).toString(base).uppercase()

    fun fromBase(text: String, base: Int): Long {
        val t = text.trim().replace(" ", "")
        if (t.isEmpty()) throw Evaluator.EvalException("Empty value")
        return try {
            BigInteger(t, base).toLong()
        } catch (e: NumberFormatException) {
            throw Evaluator.EvalException("'$t' is not a valid base-$base number")
        }
    }

    fun and(a: Long, b: Long) = a and b
    fun or(a: Long, b: Long) = a or b
    fun xor(a: Long, b: Long) = a xor b
    fun not(a: Long) = a.inv()
    fun shl(a: Long, b: Long) = a shl b.toInt()
    fun shr(a: Long, b: Long) = a shr b.toInt()
    fun ushr(a: Long, b: Long) = a ushr b.toInt()
    fun rol(a: Long, b: Long) = java.lang.Long.rotateLeft(a, b.toInt())
    fun ror(a: Long, b: Long) = java.lang.Long.rotateRight(a, b.toInt())
    fun popcount(a: Long) = java.lang.Long.bitCount(a)
    fun leadingZeros(a: Long) = java.lang.Long.numberOfLeadingZeros(a)
    fun trailingZeros(a: Long) = java.lang.Long.numberOfTrailingZeros(a)
}
