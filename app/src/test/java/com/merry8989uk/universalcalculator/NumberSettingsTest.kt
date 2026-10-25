package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.BitWidth
import com.merry8989uk.universalcalculator.calc.ComplexFormat
import com.merry8989uk.universalcalculator.calc.IntervalNotation
import com.merry8989uk.universalcalculator.calc.NumberStyle
import com.merry8989uk.universalcalculator.calc.WeightedStatistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The six settings that shape a result rather than the arithmetic: the bit
 * width, the complex form, the interval style, the frequency column, the text
 * size and CAS mode.
 *
 * Each expectation here was worked out by hand first.
 */
class NumberSettingsTest {

    // ---------------- bit width ----------------

    @Test fun thirtyTwoBitsWrapThroughAnInt() {
        assertEquals(0L, BitWidth.BITS_32.fit(0L))
        assertEquals(-1L, BitWidth.BITS_32.fit(-1L))
        assertEquals(-1L, BitWidth.BITS_32.fit(0xFFFFFFFFL))
        assertEquals(0L, BitWidth.BITS_32.fit(0x100000000L))
    }

    @Test fun thirtyTwoBitsHoldTheIntLimits() {
        assertEquals(2147483647L, BitWidth.BITS_32.fit(2147483647L))
        assertEquals(-2147483648L, BitWidth.BITS_32.fit(2147483648L))
    }

    @Test fun sixtyFourBitsLeaveEverythingAlone() {
        for (v in listOf(0L, -1L, 0xFFFFFFFFL, 0x100000000L, Long.MAX_VALUE, Long.MIN_VALUE)) {
            assertEquals("$v changed at 64 bits", v, BitWidth.BITS_64.fit(v))
        }
    }

    @Test fun notZeroIsMinusOneAtBothWidths() {
        // the case the setting exists for: a machine NOT is not a maths NOT
        assertEquals(-1L, BitWidth.BITS_32.fit(0L.inv()))
        assertEquals(-1L, BitWidth.BITS_64.fit(0L.inv()))
    }

    @Test fun notZeroAtThirtyTwoBitsIsTheThirtyTwoBitAnswer() {
        // 0xFFFFFFFF at 32 bits reads as -1, which is what the screen shows
        assertEquals(-1L, BitWidth.BITS_32.fit(0xFFFFFFFFL))
        assertTrue(BitWidth.BITS_32.bits == 32)
        assertTrue(BitWidth.BITS_64.bits == 64)
    }

    @Test fun theWidthIsReadTolerantly() {
        assertEquals(BitWidth.BITS_32, BitWidth.valueOf("BITS_32"))
        assertEquals(BitWidth.BITS_64, BitWidth.valueOf("BITS_64"))
    }

    // ---------------- complex form ----------------

    @Test fun theRectangularFormIsTheUsualOne() {
        assertEquals("3+4i", NumberStyle.complex(3.0, 4.0, ComplexFormat.RECTANGULAR))
        assertEquals("1", NumberStyle.complex(1.0, 0.0, ComplexFormat.RECTANGULAR))
    }

    @Test fun thePolarFormGivesTheMagnitudeAndTheAngle() {
        assertEquals("5\u222053.1301023542\u00B0", NumberStyle.complex(3.0, 4.0, ComplexFormat.POLAR))
        assertEquals("1.41421356237\u222045\u00B0", NumberStyle.complex(1.0, 1.0, ComplexFormat.POLAR))
        assertEquals("1\u222090\u00B0", NumberStyle.complex(0.0, 1.0, ComplexFormat.POLAR))
        assertEquals("1\u2220180\u00B0", NumberStyle.complex(-1.0, 0.0, ComplexFormat.POLAR))
    }

    @Test fun thePolarFormCanSpeakInRadians() {
        val text = NumberStyle.complex(0.0, 1.0, ComplexFormat.POLAR, degrees = false)
        assertTrue("expected radians, got '$text'", text.contains(" rad"))
        assertFalse(text.contains("\u00B0"))
    }

    @Test fun bothFormsDescribeTheSameNumber() {
        // the magnitude of 3+4i is 5, and that must show in both
        assertTrue(NumberStyle.complex(3.0, 4.0, ComplexFormat.RECTANGULAR).contains("3"))
        assertTrue(NumberStyle.complex(3.0, 4.0, ComplexFormat.POLAR).startsWith("5"))
    }

    // ---------------- interval notation ----------------

    private fun interval(lo: Double, hi: Double, incLo: Boolean, incHi: Boolean, n: IntervalNotation) =
        NumberStyle.interval(lo, hi, incLo, incHi, n)

    @Test fun theParenthesisStyle() {
        assertEquals("(-2, 2)", interval(-2.0, 2.0, false, false, IntervalNotation.PARENTHESES))
        assertEquals("[-2, 2]", interval(-2.0, 2.0, true, true, IntervalNotation.PARENTHESES))
        assertEquals("[-2, 2)", interval(-2.0, 2.0, true, false, IntervalNotation.PARENTHESES))
    }

    @Test fun thePlainStyleUsesSemicolons() {
        assertEquals("(-2; 2)", interval(-2.0, 2.0, false, false, IntervalNotation.PLAIN))
        assertEquals("[-2; 2]", interval(-2.0, 2.0, true, true, IntervalNotation.PLAIN))
    }

    @Test fun theBracketedStyleReversesAnOpenEnd() {
        assertEquals("]-2; 2[", interval(-2.0, 2.0, false, false, IntervalNotation.BRACKETED))
        assertEquals("[-2; 2]", interval(-2.0, 2.0, true, true, IntervalNotation.BRACKETED))
    }

    @Test fun anInfinityNeverGetsAClosedBracket() {
        for (n in IntervalNotation.entries) {
            val open = interval(Double.NEGATIVE_INFINITY, 2.0, true, true, n)
            assertFalse("$n closed an infinity: $open", open.startsWith("["))
        }
    }

    @Test fun theInfinitySignFollowsTheStyle() {
        assertEquals("+\u221E", NumberStyle.infinity(true, IntervalNotation.PARENTHESES))
        assertEquals("\u221E", NumberStyle.infinity(true, IntervalNotation.PLAIN))
        assertEquals("-\u221E", NumberStyle.infinity(false, IntervalNotation.PARENTHESES))
        assertEquals("-\u221E", NumberStyle.infinity(false, IntervalNotation.PLAIN))
    }

    @Test fun theUnionJoinsTheTwoHalves() {
        val left = interval(Double.NEGATIVE_INFINITY, -2.0, false, false, IntervalNotation.PARENTHESES)
            .replace("-\u221E", NumberStyle.infinity(false, IntervalNotation.PARENTHESES))
        val right = interval(2.0, Double.POSITIVE_INFINITY, false, false, IntervalNotation.PARENTHESES)
            .replace("\u221E", NumberStyle.infinity(true, IntervalNotation.PARENTHESES))
        assertEquals("(-\u221E, -2) \u222A (2, +\u221E)", NumberStyle.union(left, right))
    }

    @Test fun everyNotationHasAWorkedExample() {
        for (n in IntervalNotation.entries) {
            assertTrue("$n has no example", n.label.contains("["))
        }
    }

    // ---------------- the frequency column ----------------

    @Test fun withoutWeightsTheMeanIsTheOrdinaryOne() {
        assertEquals(7.5, WeightedStatistics.mean(listOf(7.0, 8.0)), 1e-12)
    }

    @Test fun aValueSeenFourTimesCountsFourTimes() {
        // 7 seen four times and 8 once is (7*4 + 8) / 5
        assertEquals(7.2, WeightedStatistics.mean(listOf(7.0, 8.0), listOf(4.0, 1.0)), 1e-12)
    }

    @Test fun theCountWithWeightsIsTheSumOfTheWeights() {
        assertEquals(2.0, WeightedStatistics.count(listOf(7.0, 8.0), null), 1e-12)
        assertEquals(5.0, WeightedStatistics.count(listOf(7.0, 8.0), listOf(4.0, 1.0)), 1e-12)
    }

    @Test fun theVarianceIsTheOrdinaryOneWithoutWeights() {
        // the textbook set: mean 5, population variance 4
        val xs = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        assertEquals(5.0, WeightedStatistics.mean(xs), 1e-12)
        assertEquals(4.0, WeightedStatistics.variance(xs), 1e-12)
        assertEquals(2.0, WeightedStatistics.sd(xs), 1e-12)
    }

    @Test fun theSampleVarianceDividesByOneLess() {
        val xs = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        assertEquals(4.0 * 8 / 7, WeightedStatistics.sampleVariance(xs), 1e-12)
    }

    @Test fun weightsChangeTheMean() {
        val xs = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)
        val w = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0)
        assertEquals(5.944444444444445, WeightedStatistics.mean(xs, w), 1e-12)
    }

    @Test fun theWeightedMedianWalksTheRunningTotal() {
        // weights 1, 3, 1 add to 5, so the middle is the second value
        assertEquals(2.0, WeightedStatistics.median(listOf(1.0, 2.0, 3.0), listOf(1.0, 3.0, 1.0)), 1e-12)
    }

    @Test fun theOrdinaryMedianAveragesTheMiddlePair() {
        assertEquals(2.5, WeightedStatistics.median(listOf(1.0, 2.0, 3.0, 4.0)), 1e-12)
        assertEquals(2.0, WeightedStatistics.median(listOf(1.0, 2.0, 3.0)), 1e-12)
    }

    @Test fun theEmptySetGivesNoAnswerRatherThanCrashing() {
        assertTrue(WeightedStatistics.mean(emptyList()).isNaN())
        assertTrue(WeightedStatistics.median(emptyList()).isNaN())
        assertTrue(WeightedStatistics.variance(emptyList()).isNaN())
    }

    @Test fun aWeightOfZeroTakesTheValueOut() {
        // the second value carries no weight, so it must not move the mean
        assertEquals(1.0, WeightedStatistics.mean(listOf(1.0, 100.0), listOf(1.0, 0.0)), 1e-12)
    }

    // ---------------- the settings round trip ----------------

    @Test fun everySettingIsReadTolerantly() {
        assertEquals(BitWidth.BITS_32, BitWidth.valueOf("BITS_32"))
        assertEquals(ComplexFormat.POLAR, ComplexFormat.valueOf("POLAR"))
        assertEquals(IntervalNotation.PLAIN, IntervalNotation.valueOf("PLAIN"))
    }
}
