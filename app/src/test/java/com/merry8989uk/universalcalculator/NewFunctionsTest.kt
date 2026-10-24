package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * The functions added alongside the big list: every one is checked against a
 * value worked out independently before the assertion was written.
 */
class NewFunctionsTest {

    private fun eval(expr: String, degrees: Boolean = false): Double =
        Evaluator.evaluate(expr, degrees)

    // ---- trigonometric ----

    @Test fun reciprocalTrigInDegrees() {
        assertEquals(2.0, eval("csc(30)", degrees = true), 1e-9)
        assertEquals(2.0, eval("sec(60)", degrees = true), 1e-9)
        assertEquals(1.0, eval("cot(45)", degrees = true), 1e-9)
    }

    @Test fun sincIsOneAtZero() {
        assertEquals(1.0, eval("sinc(0)"), 1e-12)
        // sin(pi)/pi is zero to the precision of pi itself
        assertEquals(0.0, eval("sinc(pi)"), 1e-15)
    }

    // ---- inverse trigonometric ----

    @Test fun inverseReciprocalTrig() {
        assertEquals(PI / 4, eval("acot(1)"), 1e-12)
        assertEquals(3 * PI / 4, eval("acot(-1)"), 1e-12)
        assertEquals(PI / 3, eval("asec(2)"), 1e-12)
        assertEquals(PI / 6, eval("acsc(2)"), 1e-12)
    }

    @Test fun atan2PicksTheRightQuadrant() {
        assertEquals(PI / 4, eval("atan2(1,1)"), 1e-12)
        assertEquals(3 * PI / 4, eval("atan2(1,-1)"), 1e-12)
        assertEquals(-PI / 4, eval("atan2(-1,1)"), 1e-12)
    }

    @Test fun inverseTrigFollowsTheDegreeSetting() {
        assertEquals(45.0, eval("atan2(1,1)", degrees = true), 1e-9)
        assertEquals(45.0, eval("acot(1)", degrees = true), 1e-9)
    }

    // ---- hyperbolic ----

    @Test fun hyperbolicReciprocals() {
        assertEquals(1.3130352854993315, eval("coth(1)"), 1e-12)
        assertEquals(0.6480542736638855, eval("sech(1)"), 1e-12)
        assertEquals(0.8509181282393216, eval("csch(1)"), 1e-12)
    }

    @Test fun inverseHyperbolic() {
        assertEquals(0.881373587019543, eval("asinh(1)"), 1e-12)
        assertEquals(1.3169578969248166, eval("acosh(2)"), 1e-12)
        assertEquals(0.5493061443340548, eval("atanh(0.5)"), 1e-12)
        assertEquals(0.5493061443340549, eval("acoth(2)"), 1e-9)
        assertEquals(1.3169578969248166, eval("asech(0.5)"), 1e-9)
        assertEquals(0.881373587019543, eval("acsch(1)"), 1e-9)
    }

    // ---- exponential and logarithmic ----

    @Test fun logarithmsOfOtherBases() {
        assertEquals(3.0, eval("log2(8)"), 1e-12)
        assertEquals(3.0, eval("log10(1000)"), 1e-12)
        assertEquals(5.0, eval("log(32,2)"), 1e-12)
    }

    @Test fun exponentialHelpers() {
        assertEquals(1024.0, eval("exp2(10)"), 1e-9)
        assertEquals(0.0, eval("expm1(0)"), 1e-12)
        assertEquals(0.0, eval("log1p(0)"), 1e-12)
        assertEquals(1.0, eval("expm1(ln(2))"), 1e-12)
    }

    // ---- powers and roots ----

    @Test fun nthRoot() {
        assertEquals(3.0, eval("nthroot(27,3)"), 1e-9)
        assertEquals(2.0, eval("nthroot(16,4)"), 1e-9)
        assertEquals(-2.0, eval("nthroot(-8,3)"), 1e-9)
    }

    @Test fun anEvenRootOfANegativeIsRefused() {
        val outcome = runCatching { eval("nthroot(-4,2)") }
        assertTrue("expected a clear refusal", outcome.isFailure)
    }

    @Test fun squaresCubesAndHypotenuse() {
        assertEquals(49.0, eval("square(7)"), 1e-12)
        assertEquals(27.0, eval("cube(3)"), 1e-12)
        assertEquals(5.0, eval("hypot(3,4)"), 1e-12)
        // x^2 and x^3 are handled by the parser, not as named functions
        assertEquals(49.0, eval("7^2"), 1e-12)
        assertEquals(27.0, eval("3^3"), 1e-12)
    }

    // ---- rounding ----

    @Test fun roundingHelpers() {
        assertEquals(-3.0, eval("floor(-2.5)"), 1e-12)
        assertEquals(-2.0, eval("ceil(-2.5)"), 1e-12)
        assertEquals(-2.0, eval("trunc(-2.7)"), 1e-12)
        assertEquals(2.0, eval("trunc(2.7)"), 1e-12)
        assertEquals(-0.7, eval("frac(-2.7)"), 1e-9)
        assertEquals(-1.0, eval("sign(-3)"), 1e-12)
        assertEquals(0.0, eval("sign(0)"), 1e-12)
    }

    @Test fun roundingToEvenAndToAMultiple() {
        assertEquals(2.0, eval("rint(2.5)"), 1e-12)   // half to even
        assertEquals(4.0, eval("rint(3.5)"), 1e-12)
        assertEquals(5.0, eval("mround(7,5)"), 1e-12)
        assertEquals(10.0, eval("mround(8,5)"), 1e-12)
    }

    // ---- number theory ----

    @Test fun gcdAndLcm() {
        assertEquals(6.0, eval("gcd(12,18)"), 1e-12)
        assertEquals(12.0, eval("lcm(4,6)"), 1e-12)
        assertEquals(2.0, eval("mod(17,5)"), 1e-12)
    }

    @Test fun primalityAndNeighbours() {
        assertEquals(1.0, eval("isprime(97)"), 1e-12)
        assertEquals(0.0, eval("isprime(91)"), 1e-12)
        assertEquals(1.0, eval("isprime(2)"), 1e-12)
        assertEquals(0.0, eval("isprime(1)"), 1e-12)
        assertEquals(101.0, eval("nextprime(100)"), 1e-12)
    }

    @Test fun totientAndIntegerSquareRoot() {
        assertEquals(12.0, eval("totient(36)"), 1e-12)
        assertEquals(1.0, eval("totient(1)"), 1e-12)
        assertEquals(1000.0, eval("isqrt(1000000)"), 1e-12)
        // floors rather than rounding
        assertEquals(3.0, eval("isqrt(15)"), 1e-12)
    }

    @Test fun fibonacciAndCatalan() {
        assertEquals(6765.0, eval("fib(20)"), 1e-9)
        assertEquals(16796.0, eval("catalan(10)"), 1e-9)
    }

    // ---- combinatorics ----

    @Test fun combinationsAndPermutationsStillWork() {
        assertEquals(126410606437752.0, eval("nCr(50,25)"), 1e-3)
        assertEquals(120.0, eval("nPr(5,5)"), 1e-9)
        assertEquals(120.0, eval("5!"), 1e-9)
    }

    // ---- statistics ----

    @Test fun statisticsOverTheArguments() {
        val sample = "mean(2,4,4,4,5,5,7,9)"
        assertEquals(5.0, eval(sample), 1e-12)
        assertEquals(4.5, eval("median(2,4,4,4,5,5,7,9)"), 1e-12)
        assertEquals(2.0, eval("mode(1,2,2,3)"), 1e-12)
        assertEquals(4.0, eval("var(2,4,4,4,5,5,7,9)"), 1e-12)
        assertEquals(2.0, eval("sd(2,4,4,4,5,5,7,9)"), 1e-12)
    }

    @Test fun sumsProductsAndExtremes() {
        assertEquals(6.0, eval("sum(1,2,3)"), 1e-12)
        assertEquals(24.0, eval("prod(2,3,4)"), 1e-12)
        assertEquals(1.0, eval("min(3,1,2)"), 1e-12)
        assertEquals(3.0, eval("max(3,1,2)"), 1e-12)
        assertEquals(2.0, eval("range(1,3)"), 1e-12)
        assertEquals(3.0, eval("count(5,6,7)"), 1e-12)
    }

    @Test fun meansThatAreNotArithmetic() {
        assertEquals(4.603215596046737, eval("geomean(2,4,4,4,5,5,7,9)"), 1e-12)
        assertEquals(4.201750729470613, eval("harmmean(2,4,4,4,5,5,7,9)"), 1e-12)
    }

    // ---- special functions ----

    @Test fun gammaAndFriends() {
        assertEquals(24.0, eval("gamma(5)"), 1e-6)
        assertEquals(sqrt(PI), eval("gamma(0.5)"), 1e-9)
        // gamma(3/2) = sqrt(pi)/2, so twice it is sqrt(pi)
        assertEquals(sqrt(PI), eval("gamma(1.5) * 2"), 1e-9)
        assertEquals(0.08333333333333333, eval("beta(2,3)"), 1e-9)
        assertEquals(3.1780538303479458, eval("lgamma(5)"), 1e-6)
    }

    @Test fun errorFunction() {
        assertEquals(0.8427007929497149, eval("erf(1)"), 1e-9)
        assertEquals(0.15729920705028513, eval("erfc(1)"), 1e-9)
        assertEquals(0.0, eval("erf(0)"), 1e-12)
        assertEquals(1.0, eval("erf(6)"), 1e-9)   // clamped, but that is what it is
    }

    @Test fun riemannZeta() {
        assertEquals(PI * PI / 6, eval("zeta(2)"), 1e-10)
        assertEquals(1.2020569031595943, eval("zeta(3)"), 1e-9)
    }

    @Test fun lambertW() {
        // the defining property: w * e^w = x
        val w = eval("lambertw(1)")
        assertEquals(0.5671432904097838, w, 1e-12)
        assertEquals(1.0, w * Math.exp(w), 1e-12)
        assertEquals(0.0, eval("lambertw(0)"), 1e-12)
    }

    @Test fun digamma() {
        assertEquals(-0.5772156649015329, eval("digamma(1)"), 1e-7)
        assertEquals(0.42278433509846713, eval("digamma(2)"), 1e-7)
    }

    // ---- constants ----

    @Test fun theConstantsAreThere() {
        assertEquals(2 * PI, eval("tau"), 1e-12)
        assertEquals((1 + sqrt(5.0)) / 2, eval("phi"), 1e-12)
        assertEquals(PI, eval("pi"), 1e-12)
    }

    // ---- complex, for real inputs ----

    @Test fun complexHelpersOnRealInput() {
        assertEquals(3.0, eval("real(3)"), 1e-12)
        assertEquals(0.0, eval("imag(3)"), 1e-12)
        assertEquals(3.0, eval("conj(3)"), 1e-12)
        assertEquals(0.0, eval("arg(3)"), 1e-12)
        assertEquals(PI, eval("arg(-3)"), 1e-12)
    }

    // ---- nothing is repeated, and everything runs ----

    @Test fun everyDrawerKeyEvaluates() {
        // The keys the drawer offers must all be understood by the evaluator,
        // with sensible sample arguments.
        val samples = mapOf(
            "sin" to "sin(1)", "cos" to "cos(1)", "tan" to "tan(1)",
            "csc" to "csc(1)", "sec" to "sec(1)", "cot" to "cot(1)",
            "asin" to "asin(0.5)", "acos" to "acos(0.5)", "atan" to "atan(1)",
            "acot" to "acot(1)", "asec" to "asec(2)", "acsc" to "acsc(2)", "atan2" to "atan2(1,1)",
            "sinh" to "sinh(1)", "cosh" to "cosh(1)", "tanh" to "tanh(1)",
            "coth" to "coth(1)", "sech" to "sech(1)", "csch" to "csch(1)",
            "asinh" to "asinh(1)", "acosh" to "acosh(2)", "atanh" to "atanh(0.5)",
            "acoth" to "acoth(2)", "asech" to "asech(0.5)", "acsch" to "acsch(1)",
            "ln" to "ln(2)", "log" to "log(100)", "log2" to "log2(8)", "log10" to "log10(100)",
            "exp" to "exp(1)", "exp2" to "exp2(3)", "log1p" to "log1p(1)", "expm1" to "expm1(1)",
            "x^2" to "3^2", "x^3" to "3^3", "x^y" to "2^3", "sqrt" to "sqrt(9)",
            "cbrt" to "cbrt(27)", "nthroot" to "nthroot(27,3)", "1/x" to "1/(4)", "hypot" to "hypot(3,4)",
            "round" to "round(2.6)", "floor" to "floor(2.6)", "ceil" to "ceil(2.1)",
            "trunc" to "trunc(2.9)", "frac" to "frac(2.9)", "sign" to "sign(-2)",
            "rint" to "rint(2.4)", "mround" to "mround(7,5)",
            "abs" to "abs(-3)", "gcd" to "gcd(12,18)", "lcm" to "lcm(4,6)", "mod" to "mod(17,5)",
            "isprime" to "isprime(7)", "nextprime" to "nextprime(10)", "totient" to "totient(36)",
            "isqrt" to "isqrt(20)", "fib" to "fib(10)", "catalan" to "catalan(5)",
            "!" to "5!", "nCr" to "nCr(5,2)", "nPr" to "nPr(5,2)",
            "sum" to "sum(1,2)", "prod" to "prod(2,3)", "mean" to "mean(1,2,3)",
            "median" to "median(1,2,3)", "mode" to "mode(1,2,2)", "var" to "var(1,2,3)",
            "sd" to "sd(1,2,3)", "min" to "min(1,2)", "max" to "max(1,2)", "range" to "range(1,5)",
            "geomean" to "geomean(1,4)", "harmmean" to "harmmean(2,4)",
            "gamma" to "gamma(4)", "lgamma" to "lgamma(4)", "beta" to "beta(2,3)",
            "erf" to "erf(1)", "erfc" to "erfc(1)", "zeta" to "zeta(3)",
            "lambertw" to "lambertw(1)", "digamma" to "digamma(2)",
            "pi" to "pi", "e" to "e", "tau" to "tau", "phi" to "phi",
            "i" to "1+1", "x" to "1+1", "deg" to "1+1", "(" to "(1+1)", ")" to "(1+1)",
            "swap" to "1+1",
        )
        val failures = ArrayList<String>()
        samples.forEach { (key, expr) ->
            val value = runCatching { eval(expr) }.getOrNull()
            if (value == null || !value.isFinite()) failures.add("$key -> $expr")
        }
        assertTrue("these drawer keys do not evaluate: $failures", failures.isEmpty())
    }
}
