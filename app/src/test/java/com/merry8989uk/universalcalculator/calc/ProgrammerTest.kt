package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgrammerTest {

    @Test fun baseConversion() {
        assertEquals("FF", Programmer.toBase(255, 16))
        assertEquals("11111111", Programmer.toBase(255, 2))
        assertEquals("377", Programmer.toBase(255, 8))
        assertEquals("255", Programmer.toBase(255, 10))
        assertEquals("Z", Programmer.toBase(35, 36))
        assertEquals(255L, Programmer.fromBase("FF", 16))
        assertEquals(255L, Programmer.fromBase("11111111", 2))
        assertEquals(35L, Programmer.fromBase("Z", 36))
    }

    @Test fun roundTripEveryBase() {
        for (base in listOf(2, 8, 10, 16, 36)) {
            for (v in listOf(0L, 1L, 255L, 1234567L, Long.MAX_VALUE)) {
                assertEquals(v, Programmer.fromBase(Programmer.toBase(v, base), base))
            }
        }
    }

    @Test fun bitwise() {
        assertEquals(8L, Programmer.and(12, 10))
        assertEquals(14L, Programmer.or(12, 10))
        assertEquals(6L, Programmer.xor(12, 10))
        assertEquals(-1L, Programmer.not(0))
        assertEquals(16L, Programmer.shl(1, 4))
        assertEquals(1L, Programmer.shr(16, 4))
        assertEquals(8, Programmer.popcount(255))
        assertEquals(63, Programmer.leadingZeros(1))
    }

    @Test fun badInputIsRejected() {
        try { Programmer.fromBase("ZZ", 16); throw AssertionError("expected failure") }
        catch (e: Evaluator.EvalException) { /* expected */ }
    }
}
