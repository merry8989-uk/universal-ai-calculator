package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The programmer tool's word, and what happens at the edges of it.
 *
 * This tool models a 64-bit machine, so its answers are 64 bits wide and no
 * wider. The old code read the typed text straight into a Long, and a Long does
 * not say no: 2^64 was cut down to its low 64 bits and came back as 0, which is
 * the one number it certainly is not. Shifts had the same shape of fault in the
 * other direction — Kotlin takes a shift count modulo the word size, so `1 << 64`
 * was answered as 1 and `1 << 65` as 2, numbers from a question nobody asked.
 *
 * The rule now is that a 64-bit tool refuses what a 64-bit word cannot hold and
 * says why, in the same words the calculator uses when a count is too big for the
 * answer field: "too large to display". Rotations are the exception and keep
 * their arithmetic, because moving a bit 64 places around a circle of 64 places
 * is moving it nowhere — that is what a rotation means, not an overflow.
 */
class ProgrammerWordTest {

    /** What the tool read, or the words it refused in. */
    private fun outcomeOf(text: String, base: Int = 10): String = try {
        Programmer.fromBase(text, base).toString()
    } catch (e: Evaluator.EvalException) {
        e.message ?: e.toString()
    }

    /** What the tool did with an operation, or the words it refused in. */
    private fun operationOutcome(work: () -> Long): String = try {
        work().toString()
    } catch (e: Evaluator.EvalException) {
        e.message ?: e.toString()
    }

    // ---------------- the cases the tracker reported ----------------

    @Test fun twoToTheSixtyFourthIsRefusedRatherThanTruncatedToZero() {
        // 18446744073709551616, which used to arrive as 0
        assertEquals("too large to display", outcomeOf("18446744073709551616"))
        // the same number written in hexadecimal, one digit past the word
        assertEquals("too large to display", outcomeOf("10000000000000000", 16))
    }

    @Test fun twoToTheSixtyThirdIsRefusedBecauseTheWordIsSigned() {
        // 9223372036854775808 used to arrive as -9223372036854775808: the sign
        // bit is the top bit of this word, so 2^63 is the first number that is
        // not in it
        assertEquals("too large to display", outcomeOf("9223372036854775808"))
    }

    @Test fun aShiftFurtherThanTheWordIsRefusedRatherThanMasked() {
        // Kotlin's shl takes the count modulo 64, so these used to be 1, 2 and
        // -1 — the answers to 1 << 0, 1 << 1 and -1 >>> 0
        assertEquals("shift count must be 0 to 63", operationOutcome { Programmer.shl(1, 64) })
        assertEquals("shift count must be 0 to 63", operationOutcome { Programmer.shl(1, 65) })
        assertEquals("shift count must be 0 to 63", operationOutcome { Programmer.shr(-1, 64) })
        assertEquals("shift count must be 0 to 63", operationOutcome { Programmer.ushr(-1, 64) })
        assertEquals("shift count must be 0 to 63", operationOutcome { Programmer.shl(1, -1) })
    }

    // ---------------- the neighbours, which must not change ----------------

    @Test fun theLargestNumberTheWordHoldsIsReadBackExactly() {
        // 2^63 - 1, the last one there is room for
        assertEquals(9223372036854775807L, Programmer.fromBase("9223372036854775807", 10))
        assertEquals(9223372036854775807L, Programmer.fromBase("7FFFFFFFFFFFFFFF", 16))
        assertEquals(Long.MIN_VALUE, Programmer.fromBase("-9223372036854775808", 10))
    }

    @Test fun shiftsInsideTheWordAreUnchanged() {
        assertEquals(16L, Programmer.shl(1, 4))
        assertEquals(1L, Programmer.shr(16, 4))
        assertEquals(0L, Programmer.shr(1, 4))
        assertEquals(1L, Programmer.ushr(-1, 63))
        // the last shift that fits: 1 moved to the top bit, which is the sign
        assertEquals(Long.MIN_VALUE, Programmer.shl(1, 63))
        assertEquals(1L, Programmer.shl(1, 0))
    }

    @Test fun rotationsStillGoRoundBecauseACircleHasNoEdge() {
        assertEquals(1L, Programmer.rol(1, 64))
        assertEquals(1L, Programmer.ror(1, 64))
        assertEquals(Long.MIN_VALUE, Programmer.rol(1, 63))
    }

    @Test fun theRestOfTheToolIsUntouched() {
        assertEquals(8L, Programmer.and(12, 10))
        assertEquals(14L, Programmer.or(12, 10))
        assertEquals(6L, Programmer.xor(12, 10))
        assertEquals(-1L, Programmer.not(0))
        assertEquals(8, Programmer.popcount(255))
        assertEquals(63, Programmer.leadingZeros(1))
        assertEquals("FF", Programmer.toBase(255, 16))
    }

    @Test fun textThatIsNotANumberIsStillRefusedAsText() {
        assertEquals("'ZZ' is not a valid base-16 number", outcomeOf("ZZ", 16))
        assertEquals("Empty value", outcomeOf("   "))
    }

    @Test fun aNumberTooBigForTheWordIsRefusedRatherThanReportedAsText() {
        // the refusal has to be the refusal, not the generic "not a valid
        // number" a screen falls back to when it does not know the difference
        val thrown = try {
            Programmer.fromBase("18446744073709551616", 10)
            null
        } catch (e: Exception) {
            e
        }
        assertTrue(
            "2^64 was not refused as too large: $thrown",
            thrown is Evaluator.TooLargeToDisplay,
        )
    }
}
