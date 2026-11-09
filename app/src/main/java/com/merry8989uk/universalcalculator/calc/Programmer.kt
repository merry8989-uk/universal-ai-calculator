package com.merry8989uk.universalcalculator.calc

import java.math.BigInteger

/**
 * Base conversion and bitwise maths for the Programmer tool.
 *
 * The word here is 64 bits, because that is the machine the tool is modelling
 * and the width the screen prints. A number that does not fit the word has no
 * answer in this tool, so it is refused by name rather than cut down to its low
 * 64 bits: typing 2^64 used to come back as 0, which is the one number it
 * certainly is not. Reading the text as a [BigInteger] first is what makes that
 * distinction possible — a [Long] cannot tell a number that fits from one that
 * was truncated into fitting.
 */
object Programmer {

    /** The bits a signed 64-bit word holds, sign bit included. */
    private const val WORD_BITS = 64

    fun toBase(value: Long, base: Int): String =
        BigInteger.valueOf(value).toString(base).uppercase()

    fun fromBase(text: String, base: Int): Long {
        val t = text.trim().replace(" ", "")
        if (t.isEmpty()) throw Evaluator.EvalException("Empty value")
        val v = try {
            BigInteger(t, base)
        } catch (e: NumberFormatException) {
            throw Evaluator.EvalException("'$t' is not a valid base-$base number")
        }
        // One bit too many and this is not a number the word holds at all.
        // 2^63 and above are past it: a signed word stops one short of 2^63,
        // and asking for 2^64 in a 64-bit calculator is asking for a number
        // that has no bits left to sit in.
        if (v.bitLength() > WORD_BITS - 1) throw Evaluator.TooLargeToDisplay()
        return v.toLong()
    }

    fun and(a: Long, b: Long) = a and b
    fun or(a: Long, b: Long) = a or b
    fun xor(a: Long, b: Long) = a xor b
    fun not(a: Long) = a.inv()

    fun shl(a: Long, b: Long) = a shl shift(b)
    fun shr(a: Long, b: Long) = a shr shift(b)
    fun ushr(a: Long, b: Long) = a ushr shift(b)

    fun rol(a: Long, b: Long) = java.lang.Long.rotateLeft(a, b.toInt())
    fun ror(a: Long, b: Long) = java.lang.Long.rotateRight(a, b.toInt())
    fun popcount(a: Long) = java.lang.Long.bitCount(a)
    fun leadingZeros(a: Long) = java.lang.Long.numberOfLeadingZeros(a)
    fun trailingZeros(a: Long) = java.lang.Long.numberOfTrailingZeros(a)

    /**
     * How far a shift moves, or a refusal when it moves further than the word.
     *
     * A shift count is not a bit pattern, it is a distance, and a 64-bit word
     * has no bit 64 to move into. Kotlin's `shl` quietly took the count modulo
     * 64, so 1 << 64 came back as 1 and 1 << 65 as 2 — answers to a different
     * question than the one that was asked. Rotations are the exception and keep
     * their own arithmetic, because a rotation is cyclic by definition: moving
     * 64 bits in a circle of 64 bits is moving nowhere, which is what
     * [rol] and [ror] already do.
     */
    private fun shift(b: Long): Int {
        if (b < 0 || b >= WORD_BITS) {
            throw Evaluator.EvalException("shift count must be 0 to ${WORD_BITS - 1}")
        }
        return b.toInt()
    }
}
