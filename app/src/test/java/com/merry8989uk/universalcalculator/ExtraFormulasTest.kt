package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every calculator built from the reference, checked against a value worked out
 * separately before the assertion was written.
 */
class ExtraFormulasTest {

    private val expected: Map<String, Triple<Map<String, Double>, Double, Double>> = mapOf(
        "pythagoras_hyp" to Triple(mapOf("a" to 3.0, "b" to 4.0), 5.0, 1e-6),
        "tri_area_bh" to Triple(mapOf("b" to 6.0, "h" to 4.0), 12.0, 1e-6),
        "heron" to Triple(mapOf("a" to 3.0, "b" to 4.0, "c" to 5.0), 6.0, 1e-6),
        "tri_area_sas" to Triple(mapOf("a" to 5.0, "b" to 7.0, "c" to 40.0), 11.248783169514438, 1e-6),
        "cosine_rule_side" to Triple(mapOf("a" to 5.0, "b" to 7.0, "c" to 60.0), 6.244997998398397, 1e-6),
        "tri_median" to Triple(mapOf("a" to 6.0, "b" to 5.0, "c" to 7.0), 5.291502622129181, 1e-6),
        "tri_inradius" to Triple(mapOf("a" to 3.0, "b" to 4.0, "c" to 5.0), 1.0, 1e-6),
        "tri_circumradius" to Triple(mapOf("a" to 3.0, "b" to 4.0, "c" to 5.0), 2.5, 1e-6),
        "polygon_angle_sum" to Triple(mapOf("n" to 6.0), 720.0, 1e-6),
        "polygon_regular_angle" to Triple(mapOf("n" to 6.0), 120.0, 1e-6),
        "circle_area" to Triple(mapOf("r" to 5.0), 78.53981633974483, 1e-6),
        "circle_circumference" to Triple(mapOf("r" to 5.0), 31.41592653589793, 1e-6),
        "arc_length" to Triple(mapOf("r" to 5.0, "t" to 60.0), 5.235987755982989, 1e-6),
        "sector_area" to Triple(mapOf("r" to 5.0, "t" to 60.0), 13.08996938995747, 1e-6),
        "chord_length" to Triple(mapOf("r" to 5.0, "t" to 60.0), 4.999999999999999, 1e-6),
        "trapezoid_area" to Triple(mapOf("a" to 8.0, "b" to 5.0, "h" to 4.0), 26.0, 1e-6),
        "regular_polygon_area" to Triple(mapOf("n" to 6.0, "r" to 5.0), 64.9519052838329, 1e-6),
        "regular_polygon_perimeter" to Triple(mapOf("n" to 6.0, "r" to 5.0), 29.999999999999996, 1e-6),
        "ellipse_area" to Triple(mapOf("a" to 5.0, "b" to 3.0), 47.12388980384689, 1e-6),
        "ellipse_circumference" to Triple(mapOf("a" to 5.0, "b" to 3.0), 25.526986393758545, 1e-6),
        "brahmagupta" to Triple(mapOf("a" to 4.0, "b" to 5.0, "c" to 6.0, "d" to 7.0), 28.982753492378876, 1e-6),
        "cone_frustum_volume" to Triple(mapOf("r" to 2.0, "R" to 4.0, "h" to 6.0), 175.92918860102841, 1e-6),
        "spherical_segment_volume" to Triple(mapOf("r" to 3.0, "h" to 2.0), 32.46312408709453, 1e-6),
        "spherical_sector_volume" to Triple(mapOf("R" to 5.0, "h" to 2.0), 104.71975511965977, 1e-6),
        "pyramid_volume" to Triple(mapOf("b" to 36.0, "h" to 7.0), 84.0, 1e-6),
        "pyramid_frustum_volume" to Triple(mapOf("b1" to 36.0, "b2" to 9.0, "h" to 5.0), 105.0, 1e-6),
        "dist_2d" to Triple(mapOf("x1" to 1.0, "y1" to 2.0, "x2" to 4.0, "y2" to 6.0), 5.0, 1e-6),
        "dist_3d" to Triple(mapOf("x1" to 1.0, "y1" to 2.0, "z1" to 3.0, "x2" to 4.0, "y2" to 6.0, "z2" to 8.0), 7.0710678118654755, 1e-6),
        "slope_2pts" to Triple(mapOf("x1" to 1.0, "y1" to 2.0, "x2" to 4.0, "y2" to 8.0), 2.0, 1e-6),
        "tri_area_vertices" to Triple(mapOf("x1" to 0.0, "y1" to 0.0, "x2" to 4.0, "y2" to 0.0, "x3" to 0.0, "y3" to 3.0), 6.0, 1e-6),
        "circle_3pts_r" to Triple(mapOf("x1" to 0.0, "y1" to 0.0, "x2" to 4.0, "y2" to 0.0, "x3" to 0.0, "y3" to 3.0), 2.5, 1e-6),
        "ellipse_eccentricity" to Triple(mapOf("a" to 5.0, "b" to 3.0), 0.8, 1e-6),
        "hyperbola_eccentricity" to Triple(mapOf("a" to 3.0, "b" to 4.0), 1.6666666666666667, 1e-6),
        "parabola_latus_rectum" to Triple(mapOf("a" to 2.0), 8.0, 1e-6),
        "vec_magnitude" to Triple(mapOf("a1" to 2.0, "a2" to 3.0, "a3" to 6.0), 7.0, 1e-6),
        "vec_dot" to Triple(mapOf("a1" to 1.0, "a2" to 2.0, "a3" to 3.0, "b1" to 4.0, "b2" to 5.0, "b3" to 6.0), 32.0, 1e-6),
        "vec_cross_magnitude" to Triple(mapOf("a1" to 1.0, "a2" to 2.0, "a3" to 3.0, "b1" to 4.0, "b2" to 5.0, "b3" to 6.0), 7.3484692283495345, 1e-6),
        "vec_angle" to Triple(mapOf("a1" to 1.0, "a2" to 0.0, "a3" to 0.0, "b1" to 0.0, "b2" to 1.0, "b3" to 0.0), 90.0, 1e-6),
        "vec_projection" to Triple(mapOf("a1" to 1.0, "a2" to 2.0, "a3" to 3.0, "b1" to 4.0, "b2" to 5.0, "b3" to 6.0), 3.6467384467084143, 1e-6),
        "vec_triple" to Triple(mapOf("a1" to 1.0, "a2" to 0.0, "a3" to 0.0, "b1" to 0.0, "b2" to 1.0, "b3" to 0.0, "c1" to 0.0, "c2" to 0.0, "c3" to 1.0), 1.0, 1e-6),
        "ap_nth" to Triple(mapOf("a" to 2.0, "d" to 3.0, "n" to 10.0), 29.0, 1e-6),
        "ap_sum" to Triple(mapOf("a" to 2.0, "d" to 3.0, "n" to 10.0), 155.0, 1e-6),
        "gp_nth" to Triple(mapOf("a" to 2.0, "r" to 3.0, "n" to 5.0), 162.0, 1e-6),
        "gp_sum" to Triple(mapOf("a" to 2.0, "r" to 3.0, "n" to 5.0), 242.0, 1e-6),
        "gp_sum_infinite" to Triple(mapOf("a" to 2.0, "r" to 0.5), 4.0, 1e-6),
        "series_harmonic_mean" to Triple(mapOf("a" to 3.0, "b" to 6.0), 4.0, 1e-6),
        "sum_first_n" to Triple(mapOf("n" to 100.0), 5050.0, 1e-6),
        "sum_first_n_squares" to Triple(mapOf("n" to 10.0), 385.0, 1e-6),
        "sum_first_n_cubes" to Triple(mapOf("n" to 10.0), 3025.0, 1e-6),
        "perm_npr_calc" to Triple(mapOf("n" to 10.0, "r" to 3.0), 720.0, 1e-6),
        "comb_ncr_calc" to Triple(mapOf("n" to 10.0, "r" to 3.0), 120.0, 1e-6),
        "perm_repetition" to Triple(mapOf("n" to 5.0, "r" to 3.0), 125.0, 1e-6),
        "circular_permutations" to Triple(mapOf("n" to 6.0), 120.0, 1e-6),
        "derangements" to Triple(mapOf("n" to 5.0), 44.0, 1e-6),
        "binom_prob" to Triple(mapOf("n" to 10.0, "k" to 3.0, "p" to 0.5), 0.1171875, 1e-6),
        "binom_mean" to Triple(mapOf("n" to 10.0, "p" to 0.5), 5.0, 1e-6),
        "binom_variance" to Triple(mapOf("n" to 10.0, "p" to 0.5), 2.5, 1e-6),
        "bayes_calc" to Triple(mapOf("pa" to 0.01, "pb_given_a" to 0.9, "pb_given_not_a" to 0.05), 0.15384615384615385, 1e-6),
        "odds_from_prob" to Triple(mapOf("p" to 0.25), 0.3333333333333333, 1e-6),
        "prob_from_odds" to Triple(mapOf("o" to 3.0), 0.75, 1e-6),
        "bs_d1" to Triple(mapOf("s" to 100.0, "k" to 100.0, "r" to 0.05, "sig" to 0.2, "t" to 1.0), 0.35000000000000003, 1e-6),
        "bs_d2" to Triple(mapOf("s" to 100.0, "k" to 100.0, "r" to 0.05, "sig" to 0.2, "t" to 1.0), 0.15000000000000002, 1e-6),
        "forward_price_yield" to Triple(mapOf("s" to 100.0, "r" to 0.05, "d" to 0.02, "t" to 1.0), 103.0454533953517, 1e-6),
        "option_delta_call" to Triple(mapOf("s" to 100.0, "k" to 100.0, "r" to 0.05, "sig" to 0.2, "t" to 1.0), 0.6368306511756191, 1e-6),
    )

    private fun formula(id: String) =
        FormulaLibrary.formulas.firstOrNull { it.id == id }

    @Test fun everyOneIsRegistered() {
        val missing = expected.keys.filter { formula(it) == null }
        assertTrue("these calculators are not in the library: $missing", missing.isEmpty())
    }

    @Test fun everyOneComputesTheRightNumber() {
        val wrong = ArrayList<String>()
        expected.forEach { (id, spec) ->
            val (inputs, want, tol) = spec
            val f = formula(id) ?: return@forEach
            val got = try {
                f.compute(inputs).firstOrNull()?.value
            } catch (e: Exception) {
                null
            }
            if (got == null || kotlin.math.abs(got - want) > tol) {
                wrong.add("$id: got $got, wanted $want")
            }
        }
        assertTrue("these calculators give the wrong answer: $wrong", wrong.isEmpty())
    }

    @Test fun everyOneHasInputsAndAnOutput() {
        val problems = ArrayList<String>()
        expected.keys.forEach { id ->
            val f = formula(id) ?: return@forEach
            if (f.inputs.isEmpty()) problems.add("$id has no inputs")
            if (f.title.isBlank()) problems.add("$id has no title")
            if (f.description.isBlank()) problems.add("$id has no description")
        }
        assertTrue("$problems", problems.isEmpty())
    }

    @Test fun noIdIsUsedTwice() {
        val ids = expected.keys.toList()
        assertEquals(ids.size, ids.distinct().size)
        val all = FormulaLibrary.formulas.map { it.id }
        assertEquals("duplicate ids in the library", all.size, all.distinct().size)
    }

    @Test fun everyInputOfEveryCalculatorCanBeFound() {
        // The expression refers to inputs by key; a typo would throw at runtime.
        val broken = ArrayList<String>()
        expected.keys.forEach { id ->
            val f = formula(id) ?: return@forEach
            val values = f.inputs.associate { it.key to it.default }
            try {
                f.compute(values)
            } catch (e: Exception) {
                broken.add("$id: ${e.message}")
            }
        }
        assertTrue("these calculators throw on their own defaults: $broken", broken.isEmpty())
    }
}
