package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Test

/** Spot checks for the formulas added from the maths handbooks. */
class FormulaLibraryNewTest {

    private fun run(id: String): List<OutputSpec> {
        val f = FormulaLibrary.formulas.firstOrNull { it.id == id }
            ?: throw AssertionError("no formula with id '$id'")
        return f.compute(f.inputs.associate { it.key to it.default })
    }

    private fun value(id: String, index: Int = 0) = run(id)[index].value

    @Test fun trigonometry() {
        assertEquals(7.2111025, value("law_of_cosines"), 1e-6)          // c from a=8,b=6,C=60
        assertEquals(20.7846097, value("law_of_cosines", 3), 1e-6)      // area
        assertEquals(13.4729636, value("law_of_sines"), 1e-5)           // b
        assertEquals(22.2738636, value("triangle_sas"), 1e-5)           // area
        assertEquals(6.0, value("triangle_inscribed_circumscribed"), 1e-6)
        assertEquals(1.0, value("triangle_inscribed_circumscribed", 1), 1e-6)   // inradius
        assertEquals(2.5, value("triangle_inscribed_circumscribed", 2), 1e-6)   // circumradius
        assertEquals(5.0, value("right_triangle_solver"), 1e-9)         // hypotenuse
        assertEquals(36.8698976, value("right_triangle_solver", 1), 1e-5)
        assertEquals(10.4719755, value("arc_and_chord"), 1e-5)          // arc
        assertEquals(10.0, value("arc_and_chord", 1), 1e-6)             // chord
        assertEquals(0.7853982, value("angle_converter"), 1e-7)
        assertEquals(50.0, value("angle_converter", 2), 1e-9)           // gradians
        assertEquals(30.0, value("inverse_trig_values"), 1e-6)
        assertEquals(1.1752012, value("hyperbolic_values"), 1e-6)
        assertEquals(28.8675135, value("height_by_elevation"), 1e-5)
        assertEquals(90.0, value("angle_between_lines"), 1e-6)
    }

    @Test fun analyticGeometry() {
        assertEquals(2.0, value("line_from_two_points"), 1e-9)          // slope
        assertEquals(0.0, value("line_from_two_points", 1), 1e-9)       // intercept
        assertEquals(6.7082039, value("line_from_two_points", 2), 1e-6)
        assertEquals(2.0, value("point_line_distance"), 1e-9)
        assertEquals(4.0, value("ellipse_properties"), 1e-9)
        assertEquals(0.8, value("ellipse_properties", 1), 1e-9)
        assertEquals(47.1238898, value("ellipse_properties", 3), 1e-5)
        assertEquals(5.0, value("hyperbola_properties"), 1e-9)
        assertEquals(1.6666667, value("hyperbola_properties", 1), 1e-6)
        assertEquals(8.0, value("parabola_properties", 2), 1e-9)
        assertEquals(78.5398163, value("circle_from_centre"), 1e-5)
        assertEquals(-4.0, value("circle_from_centre", 2), 1e-9)
        assertEquals(-12.0, value("circle_from_centre", 4), 1e-9)
        assertEquals(7.0710678, value("distance_3d"), 1e-6)
        assertEquals(7.0, value("direction_cosines"), 1e-9)
        assertEquals(1.0, value("point_plane_distance"), 1e-9)
    }

    @Test fun vectors() {
        assertEquals(32.0, value("vector_dot"), 1e-9)
        assertEquals(3.7416574, value("vector_dot", 1), 1e-6)
        assertEquals(12.9331545, value("vector_dot", 3), 1e-4)          // angle
        assertEquals(-3.0, value("vector_cross"), 1e-9)
        assertEquals(6.0, value("vector_cross", 1), 1e-9)
        assertEquals(7.3484692, value("vector_cross", 3), 1e-6)
        assertEquals(1.0, value("vector_triple_product"), 1e-9)
        assertEquals(13.0, value("unit_vector"), 1e-9)
        assertEquals(0.2307692, value("unit_vector", 1), 1e-6)
        assertEquals(3.0, value("vector_projection"), 1e-9)             // a=(3,4) onto b=(1,0)
    }

    @Test fun numberTheory() {
        assertEquals(6.0, value("divisors_sum"), 1e-9)                  // 28 has 6 divisors
        assertEquals(56.0, value("divisors_sum", 1), 1e-9)
        assertEquals(1.0, value("divisors_sum", 2), 1e-9)               // 28 is perfect
        assertEquals(12.0, value("euler_phi"), 1e-9)                    // phi(36)
        assertEquals(3.0, value("modular_power"), 1e-9)                 // 7^128 mod 13
        assertEquals(5.0, value("digit_properties"), 1e-9)
        assertEquals(15.0, value("digit_properties", 1), 1e-9)
        assertEquals(54321.0, value("digit_properties", 2), 1e-9)
        assertEquals(12.0, value("perfect_square_cube"), 1e-9)
        assertEquals(1.0, value("perfect_square_cube", 2), 1e-9)
        assertEquals(55.0, value("triangular_number"), 1e-9)
        assertEquals(111.0, value("collatz_steps"), 1e-9)               // 27 takes 111 steps
    }

    @Test fun probability() {
        assertEquals(0.7, value("probability_union"), 1e-9)
        assertEquals(0.15, value("probability_independent"), 1e-9)
        assertEquals(0.65, value("probability_independent", 1), 1e-9)
        assertEquals(0.4, value("probability_conditional"), 1e-9)
        assertEquals(0.18, value("bayes_theorem"), 1e-9)
        assertEquals(0.6513216, value("probability_at_least_one"), 1e-6)
        assertEquals(0.65, value("probability_complement"), 1e-9)
        assertEquals(0.3, value("odds_converter", 1), 1e-9)
        assertEquals(23.0, value("expected_value"), 1e-9)               // 10*.2+20*.5+30*.3
        assertEquals(49.0, value("expected_value", 1), 1e-9)            // variance
        assertEquals(0.1171875, value("binomial_probability"), 1e-9)
    }

    @Test fun series() {
        assertEquals(385.0, value("sum_of_squares"), 1e-9)
        assertEquals(3025.0, value("sum_of_cubes"), 1e-9)
        assertEquals(100.0, value("sum_of_odds"), 1e-9)
        assertEquals(2.0, value("infinite_gp_sum"), 1e-9)
        assertEquals(2.9289683, value("harmonic_number"), 1e-6)
        assertEquals(10.0, value("binomial_series_term", 1), 1e-9)      // C(5,2)
        assertEquals(Math.E, value("exponential_series"), 1e-8)
        assertEquals(0.4794255, value("sine_series"), 1e-7)
        assertEquals(0.8775826, value("cosine_series"), 1e-7)
        assertEquals(0.6881722, value("alternating_harmonic"), 1e-6)
    }

    @Test fun geometryFromTheHandbooks() {
        assertEquals(15.5884573, value("equilateral_triangle"), 1e-6)
        assertEquals(5.1961524, value("equilateral_triangle", 1), 1e-6)
        assertEquals(3.0, value("isosceles_triangle"), 1e-9)            // height of base 8, leg 5
        assertEquals(30.0, value("kite"), 1e-9)
        assertEquals(40.9878031, value("cyclic_quadrilateral"), 1e-6)
        assertEquals(64.9519053, value("regular_hexagon"), 1e-6)
        assertEquals(24.0, value("rectangular_parallelepiped"), 1e-9)
        assertEquals(52.0, value("rectangular_parallelepiped", 1), 1e-9)
        assertEquals(5.3851648, value("rectangular_parallelepiped", 2), 1e-6)
        assertEquals(12.0, value("prism"), 1e-9)
        assertEquals(7.5424723, value("regular_tetrahedron"), 1e-6)
        assertEquals(27.7128129, value("regular_tetrahedron", 1), 1e-6)
        assertEquals(30.1699035, value("octahedron"), 1e-6)
        assertEquals(55.4256258, value("octahedron", 1), 1e-6)
        assertEquals(435.6014372, value("spherical_cap"), 1e-5)
        assertEquals(251.3274123, value("spherical_cap", 1), 1e-5)
        assertEquals(251.3274123, value("ellipsoid"), 1e-5)
        assertEquals(565.4866776, value("hollow_cylinder"), 1e-5)
        assertEquals(108.0, value("general_prism"), 1e-9)
        assertEquals(126.0, value("general_prism", 1), 1e-9)
        assertEquals(150.0, value("general_prism", 2), 1e-9)
    }
}
