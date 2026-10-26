package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Distributions
import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Statistics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The seven defects an outside review found, pinned so they cannot come back.
 *
 * Every expected value here came from SciPy, not from reasoning about the code —
 * which is the point. The old suite compared the code against the same author's
 * arithmetic, so a wrong formula and a wrong expectation agreed with each other
 * and the tests passed.
 */
class BugFixesTest {

    // ---------------- 1. round, and the three functions built on it ----------------

    @Test fun roundTakesAHalfAwayFromZero() {
        // the old code used Math.rint, which takes a half to the EVEN number
        assertEquals(1.0, Evaluator.evaluate("round(0.5)"), 1e-12)
        assertEquals(3.0, Evaluator.evaluate("round(2.5)"), 1e-12)
        assertEquals(5.0, Evaluator.evaluate("round(4.5)"), 1e-12)
        assertEquals(-1.0, Evaluator.evaluate("round(-0.5)"), 1e-12)
        assertEquals(-3.0, Evaluator.evaluate("round(-2.5)"), 1e-12)
    }

    @Test fun roundStillLeavesANonTieAlone() {
        assertEquals(2.0, Evaluator.evaluate("round(2.4)"), 1e-12)
        assertEquals(3.0, Evaluator.evaluate("round(2.6)"), 1e-12)
        assertEquals(-2.0, Evaluator.evaluate("round(-2.4)"), 1e-12)
    }

    @Test fun rintIsStillHalfToEven() {
        // rint was never the broken one, and it must not change
        assertEquals(2.0, Evaluator.evaluate("rint(2.5)"), 1e-12)
        assertEquals(4.0, Evaluator.evaluate("rint(4.5)"), 1e-12)
    }

    @Test fun mroundKeepsItsMultiple() {
        // the old code gave 0 for this, losing the multiple entirely
        assertEquals(5.0, Evaluator.evaluate("mround(2.5,5)"), 1e-12)
        assertEquals(1.0, Evaluator.evaluate("mround(0.5,1)"), 1e-12)
        assertEquals(-5.0, Evaluator.evaluate("mround(-2.5,5)"), 1e-12)
        // 7 is nearer to 5 than to 10, so 5 is the right multiple
        assertEquals(5.0, Evaluator.evaluate("mround(7,5)"), 1e-12)
        assertEquals(10.0, Evaluator.evaluate("mround(8,5)"), 1e-12)
    }

    @Test fun evenRoundsAwayFromZero() {
        assertEquals(2.0, Evaluator.evaluate("even(1)"), 1e-12)
        assertEquals(2.0, Evaluator.evaluate("even(2)"), 1e-12)
        assertEquals(4.0, Evaluator.evaluate("even(2.5)"), 1e-12)
        assertEquals(-2.0, Evaluator.evaluate("even(-1)"), 1e-12)
        assertEquals(0.0, Evaluator.evaluate("even(0)"), 1e-12)
    }

    @Test fun oddRoundsAwayFromZero() {
        assertEquals(1.0, Evaluator.evaluate("odd(1)"), 1e-12)
        assertEquals(3.0, Evaluator.evaluate("odd(2)"), 1e-12)
        assertEquals(1.0, Evaluator.evaluate("odd(0)"), 1e-12)
        assertEquals(3.0, Evaluator.evaluate("odd(2.5)"), 1e-12)
        assertEquals(-1.0, Evaluator.evaluate("odd(-1)"), 1e-12)
        assertEquals(-3.0, Evaluator.evaluate("odd(-2)"), 1e-12)
    }

    // ---------------- 2. skewness ----------------

    @Test fun skewnessUsesTheSampleStandardDeviation() {
        // scipy.stats.skew(..., bias=False)
        assertEquals(1.697056274847714, Statistics.skewness(listOf(1.0, 2.0, 3.0, 4.0, 10.0)), 1e-12)
        assertEquals(0.818487553356800, Statistics.skewness(listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)), 1e-12)
    }

    @Test fun aFlatSetHasNoSkew() {
        // both the right and the wrong formula give zero here, which is why the
        // old test suite could not tell them apart
        assertEquals(0.0, Statistics.skewness(listOf(1.0, 2.0, 3.0, 4.0, 5.0)), 1e-12)
    }

    // ---------------- 3. kurtosis ----------------

    @Test fun kurtosisUsesTheSampleStandardDeviation() {
        // the worst of the old answers: a flat set reported as +2.625, i.e.
        // strongly peaked, when it is flat
        assertEquals(-1.2, Statistics.kurtosis(listOf(1.0, 2.0, 3.0, 4.0, 5.0)), 1e-12)
        assertEquals(3.152, Statistics.kurtosis(listOf(1.0, 2.0, 3.0, 4.0, 10.0)), 1e-12)
        assertEquals(0.940625, Statistics.kurtosis(listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)), 1e-12)
    }

    // ---------------- 4. the t distribution's CDF ----------------

    @Test fun theTCdfKeepsItsLeftTail() {
        // scipy.stats.t.cdf
        assertEquals(0.75, Distributions.tCdf(1.0, 1.0), 1e-12)
        assertEquals(0.937167041810999, Distributions.tCdf(5.0, 1.0), 1e-12)
        assertEquals(0.952267016866645, Distributions.tCdf(3.0, 2.0), 1e-12)
        assertEquals(0.971165557188781, Distributions.tCdf(3.0, 3.0), 1e-12)
        assertEquals(0.963305982614630, Distributions.tCdf(2.0, 10.0), 1e-12)
    }

    @Test fun theTCdfIsSymmetricAboutZero() {
        assertEquals(0.5, Distributions.tCdf(0.0, 1.0), 1e-12)
        assertEquals(0.25, Distributions.tCdf(-1.0, 1.0), 1e-12)
        assertEquals(
            Distributions.tCdf(-3.0, 2.0),
            1.0 - Distributions.tCdf(3.0, 2.0),
            1e-12,
        )
    }

    // ---------------- 5. the t distribution's inverse ----------------

    @Test fun theTCriticalValuesAreRightAtSmallDf() {
        // scipy.stats.t.ppf — the old code was 19% out at df = 1
        assertEquals(6.313751514675, Distributions.tInv(0.95, 1.0), 1e-9)
        assertEquals(12.706204736175, Distributions.tInv(0.975, 1.0), 1e-9)
        assertEquals(4.302652729749, Distributions.tInv(0.975, 2.0), 1e-8)
        assertEquals(2.228138851986, Distributions.tInv(0.975, 10.0), 1e-8)
    }

    @Test fun theInverseUndoesTheCdf() {
        for (df in listOf(1.0, 2.0, 5.0, 10.0, 30.0)) {
            for (p in listOf(0.5, 0.9, 0.975, 0.995)) {
                val t = Distributions.tInv(p, df)
                assertEquals("df=$df p=$p", p, Distributions.tCdf(t, df), 1e-9)
            }
        }
    }

    // ---------------- 6. the upper tails ----------------

    @Test fun theChiSquareUpperTailDoesNotCollapseToZero() {
        // scipy.stats.chi2.sf — the old code returned exactly 0.0 for all of these
        assertEquals(1.5239706048320995e-23, Distributions.chiSquareUpperTail(100.0, 1.0), 1e-35)
        assertEquals(2.0884875837625688e-45, Distributions.chiSquareUpperTail(200.0, 1.0), 1e-57)
        assertEquals(2.6691902155412655e-109, Distributions.chiSquareUpperTail(500.0, 2.0), 1e-121)
        assertEquals(6.0100776879208495e-214, Distributions.chiSquareUpperTail(1000.0, 5.0), 1e-226)
    }

    @Test fun theChiSquareTailStillAgreesAtOrdinaryValues() {
        assertEquals(0.050013683763956804, Distributions.chiSquareUpperTail(3.841, 1.0), 1e-12)
        assertTrue(Distributions.chiSquareUpperTail(0.0, 1.0) == 1.0)
    }

    @Test fun theFUpperTailDoesNotCollapseToZero() {
        // scipy.stats.f.sf
        assertEquals(1.8980332457517617e-19, Distributions.fUpperTail(1e8, 1.0, 5.0), 1e-31)
        assertEquals(1.8980131131979721e-14, Distributions.fUpperTail(1e6, 1.0, 5.0), 1e-26)
        assertEquals(3.0480333386234549e-12, Distributions.fUpperTail(1000.0, 2.0, 10.0), 1e-24)
    }

    @Test fun theFUpperTailStillAgreesAtOrdinaryValues() {
        assertEquals(0.031882570564059021, Distributions.fUpperTail(4.96, 2.0, 10.0), 1e-12)
        assertTrue(Distributions.fUpperTail(0.0, 1.0, 5.0) == 1.0)
    }

    // ---------------- 7. the decimal separator ----------------

    @Test fun aScientificResultUsesADotWhateverTheDeviceLocale() {
        val saved = Locale.getDefault()
        try {
            for (tag in listOf("de-DE", "fr-FR", "hi-IN", "en-US")) {
                Locale.setDefault(Locale.forLanguageTag(tag))
                val text = Format.number(1e15)
                assertTrue("under $tag the result was '$text'", text.contains("."))
                assertTrue("under $tag the result was '$text'", !text.contains(","))
            }
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test fun aSmallScientificResultAlsoUsesADot() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("de-DE"))
            val text = Format.number(1.2345e-10)
            assertTrue("the result was '$text'", text.contains("."))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
