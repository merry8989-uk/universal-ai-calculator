package com.merry8989uk.universalcalculator.ai

/**
 * What one ask came back with, said as a fact instead of as a sentence.
 *
 * [AiClient.ask] used to return its error prose on the same channel as the
 * model's reply, so nothing downstream could tell them apart: "couldn't reach
 * the server" was shown as the assistant's answer and then archived as a
 * permanent turn, which is a transcript that lies about what happened. A result
 * now states which of the three things occurred, and because the type is sealed
 * every caller has to deal with all three — there is no branch left where prose
 * can pass for a reply.
 *
 * [requestId] is the id the attempt sent as X-Request-Id. It rides on a failure
 * as well as on a success, because a failure is the case somebody has to look up
 * afterwards: without the id it is a line in a log with a timestamp and nothing
 * to match it against.
 */
sealed interface AiResult {

    /** The id of the attempt this came from, on every outcome. */
    val requestId: String

    /**
     * The model answered. [text] is what it said, before any formatting.
     *
     * [sources] is what it looked up on the way, in the order it looked: the
     * worker returns the search queries in `searched`, and the client used to
     * keep only how many there were. An answer that came off the web then
     * arrived with nothing to say where from, which is the half of a citation
     * that makes the other half worth having.
     */
    data class Ok(
        val text: String,
        override val requestId: String,
        val sources: List<String> = emptyList(),
    ) : AiResult

    /**
     * The server was reached and would not answer: an error status, or a 200
     * with nothing usable in it. [httpCode] is 0 when there was no code to
     * report, so a caller can still tell a refusal from a transport problem.
     */
    data class Failed(
        val message: String,
        override val requestId: String,
        val httpCode: Int = 0,
    ) : AiResult

    /**
     * The request never got out of the device, or never came back: no
     * connection, a refused one, or the read timeout running out.
     *
     * Kept apart from [Failed] because the advice differs — this one is about
     * the connection, not about the question — and because a Retry is worth
     * offering without a change to what was asked.
     */
    data class Offline(val message: String, override val requestId: String) : AiResult
}

/** Said when the server answered but the answer carried nothing. */
internal const val NO_ANSWER = "The assistant returned no answer."

/**
 * The one thing the screen may do with a result.
 *
 * Either the answer that goes into the transcript and the archive, or the card
 * that goes on screen and nowhere else. The two cannot both be present, which is
 * the whole point: a failure has no text to archive, so it cannot reach the
 * transcript even by accident.
 */
data class AskOutcome(
    /** The assistant turn to add, role to text. Null when there was no answer. */
    val turn: Pair<String, String>?,
    /** The card to draw instead. Null when the question was answered. */
    val failure: AskFailure?,
    /** Whether this result may be written to the chat archive. */
    val saveToArchive: Boolean,
    /**
     * The answer read apart, for the screen that draws it: the answers block,
     * the reasoning, and where the reasoning came from. Null on a failure,
     * because a failure has no answer to lay out.
     */
    val blocks: AnswerBlocks? = null,
) {
    init {
        require((turn == null) != (failure == null)) {
            "a result is an answer or a failure, never both and never neither"
        }
        require(blocks == null || turn != null) {
            "only an answer has blocks to draw"
        }
    }

    val isAnswer: Boolean get() = turn != null
}

/** What the failure card says, and what its Retry sends again. */
data class AskFailure(
    val message: String,
    val requestId: String,
    /**
     * The question of the attempt that failed, exactly as it was asked.
     *
     * Retry re-asks this rather than starting over: the user's question is not
     * the thing that was wrong.
     */
    val retryQuestion: String,
)

/**
 * Decides what one result becomes.
 *
 * This is a plain function on purpose. It is the step that used to sit inline in
 * the screen and quietly turned anything it was handed into an assistant turn;
 * as a function it can be tested without a network, an emulator or a device.
 *
 * The `when` has no `else` branch and never should: exhaustiveness over the
 * sealed type is what makes a new outcome impossible to add without deciding,
 * in this one place, whether it may be kept.
 */
fun askOutcome(result: AiResult, asked: String): AskOutcome = when (result) {
    is AiResult.Ok -> {
        // Cleaned first, then read apart: the headings arrive wrapped in the
        // model's Markdown (**ANSWERS**), and a split before cleaning would
        // miss every one of them.
        val blocks = answerBlocks(AnswerFormat.clean(result.text), result.sources)
        // A 200 with an empty answer is not an answer either. Writing "the
        // assistant returned no answer" into the transcript is the same bug in
        // a different coat.
        if (blocks.text.isBlank()) {
            AskOutcome(
                turn = null,
                failure = AskFailure(NO_ANSWER, result.requestId, asked),
                saveToArchive = false,
            )
        } else {
            AskOutcome(
                turn = "assistant" to blocks.text,
                failure = null,
                saveToArchive = true,
                blocks = blocks,
            )
        }
    }
    is AiResult.Failed -> AskOutcome(
        turn = null,
        failure = AskFailure(result.message, result.requestId, asked),
        saveToArchive = false,
    )
    is AiResult.Offline -> AskOutcome(
        turn = null,
        failure = AskFailure(result.message, result.requestId, asked),
        saveToArchive = false,
    )
}
