package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Evaluator
import com.merry8989uk.universalcalculator.calc.ImpliedMultiplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Shorthand, so the expectations below stay readable. */
private typealias IM = ImpliedMultiplication

private const val PI = Math.PI

/**
 * Where a multiplication sign that was not written down sits.
 *
 * The two settings differ only when a division is nearby. Everything else —
 * `2π` on its own, `3(4+5)`, `(2)(3)`, and any explicit `×` — must come out the
 * same either way, and that is the larger half of this file.
 */
class ImpliedMultiplicationTest {

    private fun of(expr: String, mode: IM) = Evaluator.evaluate(expr, implied = mode)

    // ---- the difference, which is the whole point ----

    @Test fun aFractionTimesPi() {
        assertEquals(PI / 2, of("1/2pi", IM.SAME_PRECEDENCE), 1e-12)
        assertEquals(1 / (2 * PI), of("1/2pi", IM.HIGHER_PRECEDENCE), 1e-12)
    }

    @Test fun aFractionTimesABracket() {
        assertEquals(1.5, of("1/2(3)", IM.SAME_PRECEDENCE), 1e-12)
        assertEquals(1.0 / 6.0, of("1/2(3)", IM.HIGHER_PRECEDENCE), 1e-12)
    }

    @Test fun theQuestionEveryoneArguesAbout() {
        // 6/2(1+2): nine or one, depending on where the missing sign sits
        assertEquals(9.0, of("6/2(1+2)", IM.SAME_PRECEDENCE), 1e-12)
        assertEquals(1.0, of("6/2(1+2)", IM.HIGHER_PRECEDENCE), 1e-12)
    }

    @Test fun aFractionOverPiSquared() {
        assertEquals(PI * PI / 2, of("1/2pi^2", IM.SAME_PRECEDENCE), 1e-12)
        assertEquals(1 / (2 * PI * PI), of("1/2pi^2", IM.HIGHER_PRECEDENCE), 1e-12)
    }

    @Test fun theDefaultIsTheWayTextbooksWriteIt() {
        // no mode given means same precedence, which is the older behaviour
        assertEquals(PI / 2, Evaluator.evaluate("1/2pi"), 1e-12)
    }

    // ---- what must not change ----

    @Test fun aMissingSignOnItsOwnIsTheSameEitherWay() {
        for (expr in listOf("2pi", "3pi", "2(3)", "3(4+5)", "(2)(3)", "pi(2)")) {
            assertEquals(
                "$expr changed with the setting",
                of(expr, IM.SAME_PRECEDENCE), of(expr, IM.HIGHER_PRECEDENCE), 1e-12,
            )
        }
    }

    @Test fun anExplicitSignIsAlwaysJustAMultiply() {
        // 1/2*pi is (1/2)*pi however the setting is left
        assertEquals(PI / 2, of("1/2*pi", IM.SAME_PRECEDENCE), 1e-12)
        assertEquals(PI / 2, of("1/2*pi", IM.HIGHER_PRECEDENCE), 1e-12)
        assertEquals(PI / 2, of("1/2*pi", IM.HIGHER_PRECEDENCE), 1e-12)
    }

    @Test fun multiplicationAndAdditionAreUntouched() {
        for (expr in listOf("2+3", "10*4", "100/8", "2^10", "(1+2)*3", "2*3+4")) {
            assertEquals(
                "$expr changed with the setting",
                of(expr, IM.SAME_PRECEDENCE), of(expr, IM.HIGHER_PRECEDENCE), 1e-12,
            )
        }
    }

    @Test fun powersStillBindTighterThanBoth() {
        // ^ is above * and /, so it is above a missing sign too
        assertEquals(2 * PI * PI, of("2pi^2", IM.SAME_PRECEDENCE), 1e-12)
        assertEquals(2 * PI * PI, of("2pi^2", IM.HIGHER_PRECEDENCE), 1e-12)
    }

    @Test fun unaryMinusStillReadsTheWholeTerm() {
        assertEquals(-2 * PI, of("-2pi", IM.SAME_PRECEDENCE), 1e-12)
        assertEquals(-2 * PI, of("-2pi", IM.HIGHER_PRECEDENCE), 1e-12)
    }

    @Test fun aMissingSignBeforeAFunctionIsTheSameEitherWay() {
        for (expr in listOf("2sqrt(9)", "3sin(0)", "2log(100)")) {
            assertEquals(
                "$expr changed with the setting",
                of(expr, IM.SAME_PRECEDENCE), of(expr, IM.HIGHER_PRECEDENCE), 1e-12,
            )
        }
    }

    @Test fun aChainOfMissingSignsIsTheSameEitherWay() {
        assertEquals(2 * PI * 3, of("2pi3", IM.SAME_PRECEDENCE), 1e-12)
        assertEquals(2 * PI * 3, of("2pi3", IM.HIGHER_PRECEDENCE), 1e-12)
    }

    // ---- the settings round trip ----

    @Test fun everyModeHasALabelAnExampleAndADescription() {
        val thin = IM.all.filter { it.label.isBlank() || it.example.isBlank() || it.blurb.isBlank() }
        assertTrue("modes with nothing to show: $thin", thin.isEmpty())
        assertEquals(2, IM.all.size)
    }

    @Test fun theExamplePrintedInSettingsIsTheTruth() {
        // "1/2pi = 1/2xpi = 1.5708" must be what the mode really does
        assertEquals(
            1.5708,
            of("1/2pi", IM.SAME_PRECEDENCE).let { Math.round(it * 10000) / 10000.0 },
            1e-9,
        )
        assertEquals(
            0.1592,
            of("1/2pi", IM.HIGHER_PRECEDENCE).let { Math.round(it * 10000) / 10000.0 },
            1e-9,
        )
    }

    @Test fun theStoredNameIsReadTolerantly() {
        assertEquals(IM.HIGHER_PRECEDENCE, IM.of("higher_precedence"))
        assertEquals(IM.HIGHER_PRECEDENCE, IM.of("higher"))
        assertEquals(IM.HIGHER_PRECEDENCE, IM.of("implicit"))
        assertEquals(IM.SAME_PRECEDENCE, IM.of("same_precedence"))
        assertEquals(IM.SAME_PRECEDENCE, IM.of("nonsense"))
        assertEquals(IM.SAME_PRECEDENCE, IM.of(""))
    }

    @Test fun everyModeSurvivesTheRoundTripThroughItsName() {
        for (mode in IM.all) assertEquals(mode, IM.of(mode.name))
    }
}
