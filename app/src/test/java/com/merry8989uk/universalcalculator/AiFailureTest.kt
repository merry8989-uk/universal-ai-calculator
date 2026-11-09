package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ai.AiResult
import com.merry8989uk.universalcalculator.ai.AskOutcome
import com.merry8989uk.universalcalculator.ai.askOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A failure is not an answer, and must never be kept as one.
 *
 * AiClient.ask used to hand its error prose back on the success channel, so the
 * screen had one string and no way to know which of the two it was holding: it
 * appended the prose as an assistant turn and wrote it to the chat archive, and
 * "couldn't reach the server" became a permanent part of the transcript.
 *
 * The decision now lives in one pure function, askOutcome, which turns a result
 * into either a turn that may be kept or a card that may not. Everything below
 * runs without a network, an emulator or a device: what a killed connection
 * does to the archive is a property of that function, not of the phone.
 */
class AiFailureTest {

    private val asked = "What is 12% of 450?"

    private fun source(path: String): String? = listOf(File(path), File("../$path"))
        .firstOrNull { it.exists() }?.readText()

    private val aiClient: String?
        get() = source("app/src/main/java/com/merry8989uk/universalcalculator/ai/AiClient.kt")

    private val screen: String?
        get() = source("app/src/main/java/com/merry8989uk/universalcalculator/ui/AskAiScreen.kt")

    private val result: String?
        get() = source("app/src/main/java/com/merry8989uk/universalcalculator/ai/AiResult.kt")

    // ---- what a result becomes ----

    @Test fun aFailedResultIsNeverArchived() {
        val outcome = askOutcome(AiResult.Failed("request failed (500)", "ab12cd34", 500), asked)
        assertNull("a failure was turned into an assistant turn", outcome.turn)
        assertFalse("a failure was allowed into the archive", outcome.saveToArchive)
        assertFalse(outcome.isAnswer)
        val card = outcome.failure
        assertNotNull("a failure produced no card", card)
        assertEquals("request failed (500)", card?.message)
    }

    @Test fun anOfflineResultIsNeverArchived() {
        val outcome = askOutcome(AiResult.Offline("Check your connection.", "ab12cd34"), asked)
        assertNull("a dropped connection was turned into an assistant turn", outcome.turn)
        assertFalse("a dropped connection was allowed into the archive", outcome.saveToArchive)
        assertNotNull("a dropped connection produced no card", outcome.failure)
    }

    @Test fun anAnswerIsArchived() {
        val outcome = askOutcome(AiResult.Ok("12% of 450 is **54**.", "ab12cd34"), asked)
        assertEquals("assistant" to "12% of 450 is 54.", outcome.turn)
        assertTrue("a real answer was not allowed into the archive", outcome.saveToArchive)
        assertNull("an answer produced a failure card", outcome.failure)
        assertTrue(outcome.isAnswer)
    }

    @Test fun anEmptyAnswerIsNotATurnEither() {
        // A 200 with nothing in it is the same bug in a different coat: the old
        // code turned it into the sentence "The assistant returned no answer."
        // and kept it.
        val outcome = askOutcome(AiResult.Ok("   ", "ab12cd34"), asked)
        assertNull("an empty answer became a turn", outcome.turn)
        assertFalse(outcome.saveToArchive)
        assertNotNull(outcome.failure)
    }

    @Test fun theCardCarriesTheRequestIdAndTheQuestionToAskAgain() {
        val outcome = askOutcome(AiResult.Offline("Check your connection.", "ab12cd34"), asked)
        val card = outcome.failure
        assertEquals("the card lost the request id", "ab12cd34", card?.requestId)
        assertEquals(
            "Retry would ask something other than the question that failed",
            asked,
            card?.retryQuestion,
        )
    }

    @Test fun anOutcomeCannotBeBothAnAnswerAndAFailure() {
        // The two channels are exclusive by construction, so no caller can draw
        // a failure as an answer or keep an answer as a failure.
        assertThrows(IllegalArgumentException::class.java) {
            AskOutcome(
                turn = "assistant" to "54",
                failure = com.merry8989uk.universalcalculator.ai.AskFailure(
                    "check your connection",
                    "ab12cd34",
                    "What is 12% of 450?",
                ),
                saveToArchive = true,
            )
        }
        // and an outcome that is neither would be a result nobody handled
        assertThrows(IllegalArgumentException::class.java) {
            AskOutcome(turn = null, failure = null, saveToArchive = false)
        }
    }

    // ---- the type, so prose cannot pass for a reply ----

    @Test fun theAskReturnsAResultRatherThanText() {
        val text = aiClient ?: return
        assertTrue(
            "ask still returns a string, so a failure and a reply are the same value",
            text.contains("): AiResult = withContext(Dispatchers.IO)"),
        )
        // the two lines that put the error prose on the success channel
        assertFalse(
            "the failure text is still returned as the answer",
            text.contains("ans ?: \"The assistant returned no answer.\""),
        )
        // the error prose is now the message inside a result, never the result
        assertTrue("a failure is not built as a result", text.contains("AiResult.Failed("))
        assertTrue("a dropped connection is not built as a result", text.contains("AiResult.Offline("))
    }

    @Test fun everyOutcomeIsCoveredWithoutADefaultBranch() {
        val text = result ?: return
        assertTrue("the result is not a sealed type", text.contains("sealed interface AiResult"))
        // No `else` in the decision: adding an outcome has to be handled, it
        // cannot quietly fall into whatever the last branch did.
        val decision = text.substringAfter("fun askOutcome(")
        assertFalse("the decision has a catch-all branch", decision.contains("else ->"))
        for (branch in listOf("is AiResult.Ok", "is AiResult.Failed", "is AiResult.Offline")) {
            assertTrue("$branch is not handled", decision.contains(branch))
        }
    }

    @Test fun theFailureLogLineStillCarriesTheRequestId() {
        val text = aiClient ?: return
        // the human-readable line beside the live box
        assertTrue(
            "the HTTP failure log line lost the request id",
            text.contains("Error \$code (request \$requestId)"),
        )
        assertTrue(
            "the network failure log line lost the request id",
            text.contains("(request \$requestId): "),
        )
        // and the structured event, on every failure path
        val failureEvents = Regex("\"request_id\" to requestId").findAll(text).count()
        assertTrue("only $failureEvents events carry the id", failureEvents >= 3)
    }

    // ---- the screen ----

    @Test fun theScreenKeepsNothingWhenNothingWasSaid() {
        val text = screen ?: return
        // the decision is the pure function, not an inline guess at the screen
        assertTrue("the screen does not use askOutcome", text.contains("askOutcome("))
        assertFalse(
            "the screen cleans and archives the reply in one step again",
            text.contains("AnswerFormat.clean(AiClient.ask("),
        )
        // the archive write sits inside the answer branch, and a failure only
        // ever reaches the card
        assertTrue("the failure has no card", text.contains("AiFailureCard("))
        assertTrue("the card has no Retry", text.contains("onRetry = { retry() }"))
        assertTrue(
            "a failure does not reach the failure state",
            text.contains("failure = outcome.failure"),
        )
        // the screen never builds an assistant turn itself: it can only add the
        // one askOutcome handed it, which a failure never has
        assertFalse(
            "the screen makes its own assistant turn",
            text.contains("turns + (\"assistant\""),
        )
        assertTrue(
            "the screen does not add the turn it was given",
            text.contains("turns = turns + turn"),
        )
    }
}
