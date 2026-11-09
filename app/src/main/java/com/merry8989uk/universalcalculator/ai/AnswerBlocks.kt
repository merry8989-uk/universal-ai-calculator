package com.merry8989uk.universalcalculator.ai

/**
 * One answer, read apart into the two blocks the worker asks the model for, and
 * the places it looked.
 *
 * The worker's ANSWER_FORMAT makes every reply the same shape: a line saying
 * ANSWERS, one short line per question under it, then a line saying
 * EXPLANATIONS and the reasoning. The app threw that shape away on arrival —
 * the reply was one string, and the screen drew it as one paragraph, so the
 * lines a user came for and the reasoning they may want to skip ran together
 * into a wall of text, and the questions they had just asked were somewhere in
 * the middle of it.
 *
 * Reading the shape back is a pure function on the cleaned text, so what the
 * screen draws can be decided and tested without a device, a network or a
 * Compose runtime. [sources] rides along here for the same reason: it used to be
 * counted in AiClient and dropped, and an answer that came off the web arrived
 * with no way to say so.
 */
data class AnswerBlocks(
    /** The ANSWERS block: what the question was, and the answer, one per line. */
    val answer: String,
    /** The EXPLANATIONS block. Empty when the reply had no reasoning in it. */
    val explanation: String,
    /** What the assistant looked up to answer, in the order it looked. */
    val sources: List<String> = emptyList(),
) {

    val hasExplanation: Boolean get() = explanation.isNotBlank()

    /**
     * Both blocks as one piece of text: what Copy, Share and the chat archive
     * get, because a copy that lost the working would be the flattening again
     * in a different place.
     */
    val text: String
        get() = listOf(answer, explanation)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")

    /** Where the answer came from, as one line, or null when it came from nowhere. */
    val sourcesLine: String?
        get() = sources.takeIf { it.isNotEmpty() }?.joinToString(" \u00B7 ")
}

/** The heading the model is told to start with, on a line of its own. */
private val ANSWER_HEAD = Regex("""(?i)^\s*answers?\s*:?\s*$""")

/** The heading the model is told to put before its reasoning. */
private val EXPLANATION_HEAD = Regex("""(?i)^\s*explanations?\s*:?\s*$""")

/** More than a handful of lookups is a list nobody reads. */
private const val MAX_SOURCES = 8

/**
 * Splits [cleaned] at the two headings, and keeps [sources] with the result.
 *
 * Three shapes have to survive, because the model does not always follow the
 * format it was given: both headings, the explanation heading alone, and
 * neither. None of them may lose text — a reply with no headings is all answer,
 * which is what the screen drew before and is still the honest reading of it.
 */
fun answerBlocks(cleaned: String, sources: List<String> = emptyList()): AnswerBlocks {
    val kept = sources.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(MAX_SOURCES)
    val lines = cleaned.split("\n")
    val explanationAt = lines.indexOfFirst { EXPLANATION_HEAD.matches(it) }
    val answerAt = lines.indexOfFirst { ANSWER_HEAD.matches(it) }

    val body = if (explanationAt >= 0) lines.subList(0, explanationAt) else lines
    val answer = body.drop(if (answerAt in body.indices) answerAt + 1 else 0)
        .joinToString("\n")
        .trim()
    val explanation = if (explanationAt >= 0) {
        lines.drop(explanationAt + 1).joinToString("\n").trim()
    } else {
        ""
    }

    if (answer.isNotBlank()) return AnswerBlocks(answer, explanation, kept)

    // Nothing above the heading. The reply is the reasoning and nothing else, so
    // it is shown as the answer rather than as an empty box under a heading the
    // user cannot do anything with.
    if (explanation.isNotBlank()) return AnswerBlocks(explanation, "", kept)

    return AnswerBlocks(cleaned.trim(), "", kept)
}
