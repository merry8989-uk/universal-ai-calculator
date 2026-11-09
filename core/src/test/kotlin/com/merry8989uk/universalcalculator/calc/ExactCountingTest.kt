package com.merry8989uk.universalcalculator.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * Counting functions that used to answer with a wrapped number.
 *
 * Two of these had the same fault in different clothes. `nCr` went through three
 * factorials and their quotient, so `nCr(171, 1)` was refused as "Factorial is
 * too large" although the answer is 171 — the refusal was about the working, not
 * about the result. `fib` counted in a Long, and `fib(93)` is 12200160415121876738,
 * one past the largest signed 64-bit number, so it came back as −6.2e18: a
 * negative count of rabbits, reported as an answer.
 *
 * The fix is to count in exact integers and to hand the answer over to the
 * display only where the display can carry it. So these tests are in two halves:
 * the numbers that were wrong and are now right, and the neighbours either side
 * of them that were already right and have to stay that way. Where a count is
 * genuinely past what the answer field can hold — a double tops out just under
 * 2^1024 — the calculator says "too large to display" instead of inventing a
 * number, and that refusal is tested too, because a refusal that nobody checks
 * is a refusal that quietly becomes a wrap again.
 */
class ExactCountingTest {

    private fun ev(expression: String) = Evaluator.evaluate(expression)

    /** What the calculator answered, or the words it refused in. */
    private fun outcomeOf(expression: String): String = try {
        Evaluator.evaluate(expression).toString()
    } catch (e: Evaluator.EvalException) {
        e.message ?: e.toString()
    }

    private fun big(n: Long) = BigInteger.valueOf(n)

    // ---------------- the cases the tracker reported ----------------

    @Test fun choosingOneOfAHundredAndSeventyOneIsAChoiceOfOneHundredAndSeventyOne() {
        // the refusal here was about 171! being asked for as working, not about
        // the answer, which is 171 and always was
        assertEquals(171.0, ev("nCr(171,1)"), 0.0)
        assertEquals(171.0, ev("nCr(171,170)"), 0.0)
        assertEquals(big(171), Evaluator.combinationsExact(big(171), BigInteger.ONE))
        assertEquals(171.0, ev("nPr(171,1)"), 0.0)
    }

    @Test fun theNinetyThirdFibonacciNumberIsPastTheWordAndStillExact() {
        // 12200160415121876738, which a signed 64-bit word cannot hold
        assertEquals(BigInteger("12200160415121876738"), Evaluator.fibonacciExact(93))
        assertTrue("fib(93) is still negative: ${ev("fib(93)")}", ev("fib(93)") > 0)
        assertEquals(1.2200160415121877E19, ev("fib(93)"), 0.0)
    }

    @Test fun aCombinationTooBigForItsFactorialsButSmallEnoughForTheDisplayIsAnswered() {
        // 200 choose 100 is 9.05e58: three factorials would each have overflowed,
        // while the combination itself is nowhere near the ceiling
        val expected = 9.054851465610328E58
        assertEquals(expected, ev("nCr(200,100)"), expected * 1e-15)
    }

    // ---------------- the neighbours, which must not change ----------------

    @Test fun theSmallCountsThatAlwaysWorkedAreUnchanged() {
        assertEquals(10.0, ev("nCr(5,2)"), 0.0)
        assertEquals(20.0, ev("nPr(5,2)"), 0.0)
        assertEquals(1.0, ev("nCr(5,0)"), 0.0)
        assertEquals(1.0, ev("nCr(0,0)"), 0.0)
        assertEquals(6765.0, ev("fib(20)"), 0.0)
        assertEquals(16796.0, ev("catalan(10)"), 0.0)
    }

    @Test fun theNinetySecondFibonacciNumberIsTheNumberItAlwaysWas() {
        // fib(92) fits a Long and was right before this change: the old code
        // reached it as a Long and then widened it to a double, which is exactly
        // the value asserted here, so nothing has moved
        assertEquals(BigInteger("7540113804746346429"), Evaluator.fibonacciExact(92))
        assertEquals(7540113804746346429L.toDouble(), ev("fib(92)"), 0.0)
    }

    @Test fun theWidestCombinationThatFitsADoubleExactlyIsUnchanged() {
        // 126410606437752 is below 2^53, so it is a double with no rounding at all
        assertEquals(126_410_606_437_752.0, ev("nCr(50,25)"), 0.0)
        assertEquals(126_410_606_437_752.0, ev("nCr(25,25)*nCr(50,25)"), 0.0)
    }

    @Test fun theCatalanNumbersThatFitAWordAreUnchanged() {
        assertEquals(2.0, ev("catalan(2)"), 0.0)
        assertEquals(16796.0, ev("catalan(10)"), 0.0)
        // catalan(33) is 212336130412243110: it fits a Long, but the running
        // (n+1)-multiple on the way there does not, and this used to come back
        // as 8.07e15
        assertEquals(BigInteger("212336130412243110"), Evaluator.catalanExact(33))
        assertEquals(2.1233613041224311E17, ev("catalan(33)"), 0.0)
    }

    // ---------------- the refusals ----------------

    @Test fun aCountPastTheAnswerFieldIsRefusedByName() {
        // 10000 choose 5000 has 3009 digits, and there is no double that is it
        assertEquals("too large to display", outcomeOf("nCr(10000,5000)"))
        assertEquals("too large to display", outcomeOf("nPr(1000,999)"))
        assertEquals("too large to display", outcomeOf("catalan(600)"))
    }

    @Test fun theRefusalIsItsOwnKindOfAnswerRatherThanBadInput() {
        val thrown = try {
            Evaluator.evaluate("nCr(10000,5000)")
            null
        } catch (e: Exception) {
            e
        }
        assertTrue(
            "nCr(10000,5000) was not refused as too large: $thrown",
            thrown is Evaluator.TooLargeToDisplay,
        )
    }

    @Test fun theFibonacciNumberJustInsideTheAnswerFieldIsStillAnswered() {
        // fib(1476) is 1.31e308, the last one a double can carry; fib(1477) is
        // past it and is refused
        assertEquals(1.3069892237633993E308, ev("fib(1476)"), 0.0)
        assertEquals("too large to display", outcomeOf("fib(1477)"))
    }

    @Test fun askingForTheFibonacciNumberOfABillionIsRefusedRatherThanCounted() {
        // the walk is abandoned at the ceiling, so this answers at once instead
        // of counting a billion steps first
        assertEquals("too large to display", outcomeOf("fib(1000000000)"))
    }

    @Test fun aFactorialPastTheAnswerFieldIsRefusedInTheSameWords() {
        // 170! is 7.26e306 and fits; 171! is the first that does not
        assertEquals(7.257415615307999E306, ev("170!"), 0.0)
        assertEquals("too large to display", outcomeOf("171!"))
    }

    // ---------------- the arguments, which are not results ----------------

    @Test fun anArgumentThatIsNotACountIsRefusedAsBefore() {
        assertEquals("nCr needs 0 <= r <= n", outcomeOf("nCr(2,5)"))
        assertEquals("nCr needs whole numbers", outcomeOf("nCr(5.5,2)"))
        assertEquals("nPr needs 0 <= r <= n", outcomeOf("nPr(2,5)"))
    }

    @Test fun anArgumentAHairOffAWholeNumberIsStillForgiven() {
        // 5 - 1e-15 arrives as 4.999999999999999 from ordinary arithmetic, and
        // the user meant five of them
        assertEquals(10.0, ev("nCr(4.999999999999999,2)"), 0.0)
    }
}
