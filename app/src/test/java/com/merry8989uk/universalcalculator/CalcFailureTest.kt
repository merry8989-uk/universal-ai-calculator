package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.CalcFailure
import com.merry8989uk.universalcalculator.calc.Evaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a failed sum is allowed to do to the screen.
 *
 * The reported bug had three faces at once: the exception's text replaced the
 * expression, the same text was written into the history tape as if it were the
 * sum, and an answer that belonged to a different calculation was left sitting
 * beside it. These tests hold the decision to all three — the expression comes
 * back untouched, there is no answer, there is no history line — and to the
 * fourth rule, that the only words the user sees are ones written for a person.
 *
 * The last of those is the one worth guarding hardest. "For input string: abc"
 * and a stack frame are both what an exception says, and neither is something
 * the user can act on, so neither may reach the screen.
 */
class CalcFailureTest {

    // ---------------- the three wrongs on the same catch block ----------------

    @Test fun aFailedSumKeepsTheTypedExpression() {
        val typed = "1/0+2"
        val failure = CalcFailure.of(typed, Evaluator.EvalException("Division by zero"))
        assertEquals("the expression the user typed was not handed back", typed, failure.expression)
    }

    @Test fun theExpressionComesBackExactlyAsTyped() {
        // Spaces, a half-typed bracket and a sign the parser would have to think
        // about: whatever it was, it is what the user is still looking at.
        val typed = " 2 + sin(  "
        assertEquals(typed, CalcFailure.of(typed, RuntimeException("boom")).expression)
    }

    @Test fun aFailedSumShowsNoAnswer() {
        assertNull(
            "a failed sum offered an answer to show",
            CalcFailure.of("1/0", Evaluator.EvalException("Division by zero")).answer,
        )
    }

    @Test fun aFailedSumWritesNoHistoryEntry() {
        assertNull(
            "a failed sum offered a line for the tape",
            CalcFailure.of("1/0", Evaluator.EvalException("Division by zero")).historyLine,
        )
    }

    @Test fun aFailureCarriesNothingButASentence() {
        // One decision, all three rules, so a call site cannot take one and leave
        // another: what it gets back is a sentence and nothing writable.
        val failure = CalcFailure.of("ln(0)", Evaluator.EvalException("ln of zero"))
        assertEquals("ln(0)", failure.expression)
        assertNull(failure.answer)
        assertNull(failure.historyLine)
        assertEquals("ln of zero", failure.notice)
    }

    // ---------------- the words on screen ----------------

    @Test fun aRefusalSaysWhatTheEvaluatorSaid() {
        // EvalException is thrown by this app for this screen, so its message is
        // already the sentence and is used as it stands.
        val failure = CalcFailure.of("1/0", Evaluator.EvalException("Division by zero"))
        assertEquals("Division by zero", failure.notice)
    }

    @Test fun anyOtherFaultGetsOnePlainSentence() {
        val failure = CalcFailure.of("2+2", NumberFormatException("For input string: \"2+\""))
        assertEquals(CalcFailure.PLAIN, failure.notice)
    }

    @Test fun everyOtherFaultGetsTheSamePlainSentence() {
        // One sentence, not one per exception: the user is told the sum did not
        // work, not which class of object was unhappy.
        val plain = listOf(
            IllegalStateException("state"),
            ArithmeticException("/ by zero"),
            RuntimeException(),
            Throwable("something"),
        ).map { CalcFailure.of("2+2", it).notice }.distinct()
        assertEquals("the plain sentence varies by fault: $plain", 1, plain.size)
        assertEquals(CalcFailure.PLAIN, plain.single())
    }

    @Test fun thePlainSentenceIsASentence() {
        val notice = CalcFailure.of("2+2", RuntimeException("boom")).notice
        assertTrue("the sentence does not end in a full stop: '$notice'", notice.endsWith("."))
        assertTrue("the sentence does not start with a capital: '$notice'", notice.first().isUpperCase())
        assertFalse("the sentence is a fragment: '$notice'", notice.length < 10)
    }

    @Test fun theNoticeIsNeverAStackTrace() {
        val thrown = IllegalStateException("java.lang.IllegalStateException: null\n\tat com.foo.Bar")
        val notice = CalcFailure.of("2+2", thrown).notice
        assertFalse("the notice carries a newline: '$notice'", notice.contains("\n"))
        assertFalse("the notice carries a stack frame: '$notice'", notice.contains("\tat "))
        assertFalse("the notice names an exception: '$notice'", notice.contains("Exception"))
        assertFalse("the notice carries a package name: '$notice'", notice.contains("java."))
    }

    @Test fun theNoticeNeverCarriesTheFaultsOwnWords() {
        // The message of an unexpected fault is exactly what must not be shown,
        // even when it looks harmless.
        val said = "For input string: \"2+\""
        val notice = CalcFailure.of("2+2", NumberFormatException(said)).notice
        assertFalse("the exception's own words reached the screen: '$notice'", notice.contains(said))
        assertFalse("part of the exception's words reached the screen: '$notice'", notice.contains("For input"))
    }

    @Test fun aRefusalWithNothingToSayStillGetsASentence() {
        // EvalException("") would otherwise leave the row empty and look like a
        // layout fault rather than a refusal.
        assertEquals(CalcFailure.PLAIN, CalcFailure.of("1/0", Evaluator.EvalException("   ")).notice)
    }

    // ---------------- what the log gets instead ----------------

    @Test fun theFaultIsKeptForTheLogButNotForTheScreen() {
        val failure = CalcFailure.of("2+2", NumberFormatException("For input string: \"2+\""))
        assertEquals("NumberFormatException", failure.logCode)
        assertEquals("For input string: \"2+\"", failure.logDetail)
        assertFalse("the log's detail is on screen", failure.notice.contains("2+"))
    }

    @Test fun anUnnamedFaultStillHasACodeForTheLog() {
        val failure = CalcFailure.of("2+2", object : RuntimeException("anonymous") {})
        assertTrue("the log has no code for this fault", failure.logCode.isNotBlank())
        assertEquals(CalcFailure.PLAIN, failure.notice)
    }
}
