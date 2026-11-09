package com.merry8989uk.universalcalculator.calc

/**
 * What a failed calculation is allowed to do to the screen.
 *
 * Three wrongs used to come out of the same catch block. The exception's own
 * text replaced the expression, so what the user typed was gone; that same text
 * was written into the history tape as though it were the sum and its answer;
 * and the answer to the previous calculation stayed on screen beside it. None of
 * the three is a fact about the sum the user asked for, so the decision is made
 * here, once, where it can be tested without a device.
 *
 * A failure is not a result. It has no answer and no history line — both are
 * properties that are always null rather than fields, so a call site has nothing
 * it could accidentally fill in. It does not carry the expression away either:
 * [expression] is the text exactly as it was typed, handed straight back.
 *
 * The one thing a failure adds is [notice], and it is written for a person.
 * [Evaluator.EvalException]'s message already is — "Division by zero", "Missing
 * ')'" — so it is used as it stands. Anything else is a fault the app did not
 * plan for, and its message ("For input string: \"abc\"", a stack frame) tells
 * the user nothing they can act on, so it becomes one plain sentence instead.
 * What the exception actually said is kept in [logDetail] to be logged and never
 * shown.
 */
data class CalcFailure(
    /** The expression exactly as typed, so the call site cannot lose it. */
    val expression: String,
    /** The one sentence shown under the expression. */
    val notice: String,
    /** The exception's class, for the log. */
    val logCode: String,
    /** The exception's own message, for the log only — never for the screen. */
    val logDetail: String?,
) {
    /**
     * A failed sum has no answer. Always null, and deliberately not a field: the
     * screen shows nothing where the answer would be.
     */
    val answer: String?
        get() = null

    /**
     * A failed sum is not a calculation that happened, so it is not recorded.
     * Always null, for the same reason as [answer].
     */
    val historyLine: String?
        get() = null

    companion object {
        /** The one sentence for a fault that was not written for a person. */
        const val PLAIN = "That calculation could not be completed."

        /**
         * Decides what [thrown] does to the screen, given the [typed] expression.
         *
         * The expression is returned untouched and nothing is recorded; only the
         * sentence varies, and it varies by whether the exception was written for
         * a person or not — not by how it reads.
         */
        fun of(typed: String, thrown: Throwable): CalcFailure {
            val said = thrown.message?.trim()?.takeIf { it.isNotEmpty() }
            // EvalException is thrown by this app, for this screen; its message is
            // already the sentence. Everything else is a fault nobody wrote a
            // message for, so it gets the plain one.
            val written = thrown is Evaluator.EvalException
            return CalcFailure(
                expression = typed,
                notice = if (written) (said ?: PLAIN) else PLAIN,
                logCode = thrown.javaClass.simpleName.ifEmpty { thrown.javaClass.name },
                logDetail = said,
            )
        }
    }
}
