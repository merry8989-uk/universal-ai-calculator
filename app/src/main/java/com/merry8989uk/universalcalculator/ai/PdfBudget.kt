package com.merry8989uk.universalcalculator.ai

/**
 * How much of a long PDF is worth reading.
 *
 * Every page costs a picture and a call to the vision model, so a hundred-page
 * book is not a hundred pages of waiting — it is a phone that appears to have
 * hung. The old answer was a flat twenty pages, which is wrong twice: a
 * forty-page question paper got half its questions answered with no way to ask
 * for the rest, and a five-page handout waited for nothing.
 *
 * Three budgets instead, whichever runs out first, and the note at the top of
 * the text says which one it was and what was missed. Silence about a skipped
 * half is the part that actually misleads.
 */
object PdfBudget {

    /** The most pages that will ever be attempted. */
    const val MAX_PAGES = 60

    /** Long enough for a slow phone, short enough that nobody gives up. */
    const val TIME_BUDGET_MS = 90_000L

    /** The text still has to fit in the model's context with the question. */
    const val TEXT_BUDGET_CHARS = 55_000

    /** Why the reading stopped. Null means keep going. */
    sealed interface Stop {
        /** Every page was read. */
        data object Finished : Stop

        /** The page cap was reached. */
        data object PageCap : Stop

        /** The time budget ran out. */
        data object Time : Stop

        /** The text budget filled up. */
        data object Text : Stop
    }

    /**
     * Whether to stop before reading the next page.
     *
     * [pagesRead] is how many have been read, [elapsedMs] how long it has taken,
     * [chars] how much text has come back, and [totalPages] what the document
     * holds.
     */
    fun stopReason(pagesRead: Int, elapsedMs: Long, chars: Int, totalPages: Int): Stop? = when {
        pagesRead >= totalPages -> Stop.Finished
        pagesRead >= MAX_PAGES -> Stop.PageCap
        elapsedMs >= TIME_BUDGET_MS -> Stop.Time
        chars >= TEXT_BUDGET_CHARS -> Stop.Text
        else -> null
    }

    /**
     * The line that goes at the top of what the model is given.
     *
     * It says what was read and what was not, because a document that has been
     * half read and does not say so produces a confident answer to the wrong
     * question.
     */
    fun note(stop: Stop, pagesRead: Int, totalPages: Int): String {
        val pages = if (totalPages == 1) "page" else "pages"
        return when (stop) {
            Stop.Finished ->
                "Read all $totalPages $pages."
            Stop.PageCap ->
                "Read the first $pagesRead of $totalPages $pages \u2014 that is the limit. " +
                    "The rest was not read, so do not assume it is empty."
            Stop.Time ->
                "Read $pagesRead of $totalPages $pages before the time ran out. " +
                    "The rest was not read, so do not assume it is empty."
            Stop.Text ->
                "Read $pagesRead of $totalPages $pages before the text filled up. " +
                    "The rest was not read, so do not assume it is empty."
        }
    }

    /** True when the reading stopped before the end of the document. */
    fun partial(stop: Stop): Boolean = stop != Stop.Finished
}
