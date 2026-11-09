package com.merry8989uk.universalcalculator.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two blocks the worker asks for, and the sources it returns, must survive
 * the trip to the screen.
 *
 * The worker's ANSWER_FORMAT gives every reply the same shape: a line saying
 * ANSWERS, one short line per question under it, a line saying EXPLANATIONS, and
 * the reasoning. The app drew all of that as one paragraph, so the answers a
 * user came for and the reasoning they may skip ran together — and the search
 * queries the worker returned in `searched` were counted in AiClient and
 * dropped, so an answer that used the web had no way to say so.
 *
 * Everything here is a plain function on text, so the decision about what the
 * screen draws is checked without a device, a network or a Compose runtime.
 */
class AnswerBlocksTest {

    private val asked = "Solve these two."

    private val reply = """
        **ANSWERS**
        Q1: 36
        Q2: B - 7

        **EXPLANATIONS**
        Q1: 15/100 x 240 = 36.
        Q2: 4 + 3 is 7, so B.
    """.trimIndent()

    // ---- the split ----

    @Test fun theAnswersAndTheReasoningAreSeparateBlocks() {
        val blocks = answerBlocks(AnswerFormat.clean(reply))
        assertEquals("Q1: 36\nQ2: B - 7", blocks.answer)
        assertEquals("Q1: 15/100 x 240 = 36.\nQ2: 4 + 3 is 7, so B.", blocks.explanation)
        assertTrue(blocks.hasExplanation)
    }

    @Test fun neitherHeadingIsShownToTheUser() {
        val blocks = answerBlocks(AnswerFormat.clean(reply))
        for (block in listOf(blocks.answer, blocks.explanation)) {
            assertFalse("a heading reached the screen: '$block'", block.contains("ANSWERS"))
            assertFalse("a heading reached the screen: '$block'", block.contains("EXPLANATIONS"))
        }
    }

    @Test fun aReplyWithNoHeadingsIsAllAnswer() {
        // the model does not always follow the format, and text that was shown
        // before must not go missing now
        val blocks = answerBlocks("54 rupees, worked out from 12 percent of 450.")
        assertEquals("54 rupees, worked out from 12 percent of 450.", blocks.answer)
        assertEquals("", blocks.explanation)
        assertFalse(blocks.hasExplanation)
    }

    @Test fun theHeadingMayCarryAColonAndAnyCase() {
        val blocks = answerBlocks("Answers:\nQ1: 36\nExplanations:\nbecause 15/100 x 240 is 36.")
        assertEquals("Q1: 36", blocks.answer)
        assertEquals("because 15/100 x 240 is 36.", blocks.explanation)
    }

    @Test fun aReplyWithOnlyReasoningIsShownAsTheAnswer() {
        // an empty answer box above a paragraph is worse than the paragraph
        val blocks = answerBlocks("EXPLANATIONS:\nbecause 15/100 x 240 is 36.")
        assertEquals("because 15/100 x 240 is 36.", blocks.answer)
        assertEquals("", blocks.explanation)
        assertFalse("the heading is still on screen", blocks.answer.contains("EXPLANATIONS"))
    }

    @Test fun copyAndShareAndTheArchiveGetBothBlocks() {
        val blocks = answerBlocks(AnswerFormat.clean(reply))
        val text = blocks.text
        assertTrue("the answers are missing from the copied text", text.contains("Q1: 36"))
        assertTrue("the reasoning is missing from the copied text", text.contains("15/100 x 240"))
        assertEquals("the blocks are not separated", 2, text.split("\n\n").size)
    }

    // ---- the sources ----

    @Test fun theSourcesAreKeptRatherThanCounted() {
        val blocks = answerBlocks(
            "Q1: 36",
            listOf("gst rate india", "cbse class 10 maths"),
        )
        assertEquals(listOf("gst rate india", "cbse class 10 maths"), blocks.sources)
        assertEquals("gst rate india \u00B7 cbse class 10 maths", blocks.sourcesLine)
    }

    @Test fun aRepeatedOrEmptySourceIsNotListedTwice() {
        val blocks = answerBlocks("Q1: 36", listOf("gst rate india", "gst rate india", "  ", ""))
        assertEquals(listOf("gst rate india"), blocks.sources)
    }

    @Test fun noSourcesMeansNoLineRatherThanAnEmptyOne() {
        assertNull(answerBlocks("Q1: 36").sourcesLine)
        assertNull(answerBlocks("Q1: 36", listOf("   ")).sourcesLine)
    }

    @Test fun theSourcesAreNotPartOfTheAnswer() {
        // a citation inside the answer would be copied into the next sum
        val blocks = answerBlocks("Q1: 36", listOf("gst rate india"))
        assertFalse(blocks.answer.contains("gst"))
        assertFalse(blocks.text.contains("gst rate india"))
    }

    // ---- what the result becomes ----

    @Test fun anAnswerCarriesItsBlocksAndItsSources() {
        val outcome = askOutcome(
            AiResult.Ok(reply, "ab12cd34", listOf("gst rate india")),
            asked,
        )
        val blocks = outcome.blocks
        assertNotNull("the screen was given no blocks to draw", blocks)
        assertEquals("Q1: 36\nQ2: B - 7", blocks?.answer)
        assertTrue("the reasoning did not arrive", blocks?.hasExplanation == true)
        assertEquals(listOf("gst rate india"), blocks?.sources)
        // and the turn — the history and the archive — still holds everything
        assertEquals("assistant" to blocks?.text, outcome.turn)
    }

    @Test fun aFailureHasNoBlocksToDraw() {
        val outcome = askOutcome(AiResult.Failed("request failed (500)", "ab12cd34", 500), asked)
        assertNull(outcome.blocks)
        assertNull(outcome.turn)
    }
}
