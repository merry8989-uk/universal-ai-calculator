package com.merry8989uk.universalcalculator.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The assistant must never show the user raw Markdown or LaTeX — and must never
 * quietly throw a formula away while tidying one.
 *
 * The first half of this file is about what the user sees: a real answer that
 * shipped to a device with ** and $$ still visible, and the Markdown and LaTeX
 * that had to stop reaching the screen. The second half is about what the user
 * no longer sees but needs: `\sqrt{4}` used to arrive as `4`, because the
 * commands without a mapping were deleted rather than written out. A flattened
 * formula is not untidy, it is wrong, and it is wrong in the direction nobody
 * catches — it still looks like an answer. Every case below is a shape that came
 * out of the model and has to survive the tidying.
 */
class AnswerFormatTest {

    private val realAnswer = """
        **Calculation:**

        * **Area:** 1,200 sq ft
        * **Rate:** ₹2,500 per sq ft

        ${'$'}${'$'}\text{Total Cost} = \text{Area} \times \text{Rate}${'$'}${'$'}
        ${'$'}${'$'}\text{Total Cost} = 1,200 \times 2,500 = \text{₹} 30,00,000${'$'}${'$'}

        *Note: This calculation covers the base price.*

        **Final Answer:**
        **₹30,00,000** (₹30 Lakh)
    """.trimIndent()

    /** Maths the model writes, and the plain maths it has to become. */
    private val maths: List<Pair<String, String>> = listOf(
        "\\sqrt{4}" to "sqrt(4)",
        "\\sqrt{4} = 2" to "sqrt(4) = 2",
        "\\sqrt{x+1}" to "sqrt(x+1)",
        "\\sqrt[3]{8}" to "(8)^(1/3)",
        "\\sqrt{\\frac{1}{2}}" to "sqrt(1/2)",
        "\\frac{\\sqrt{4}}{2}" to "sqrt(4)/2",
        "\\frac{1}{2}" to "1/2",
        "\\foo{2}" to "foo(2)",
        "\\alpha + \\beta" to "alpha + beta",
        "\\pi r^2" to "pi r^2",
        "x^{2n}" to "x^(2n)",
        "\\text{Area} \\times \\text{Rate}" to "Area x Rate",
        "\\operatorname{sin} x" to "sin x",
        "\\overline{AB} = 5" to "AB = 5",
        "\\left( \\frac{a}{b} \\right)" to "( a/b )",
    )

    @Test fun realAnswerComesOutClean() {
        val out = AnswerFormat.clean(realAnswer)
        assertFalse("no bold markers", out.contains("**"))
        assertFalse("no dollar maths", out.contains("$"))
        assertFalse("no backslashes", out.contains("\\"))
        assertFalse("no braces", out.contains("{"))
        assertFalse("no italic markers", out.contains("*"))
        assertTrue("the numbers survive", out.contains("30,00,000"))
        assertTrue("the bullet survives", out.contains("\u2022"))
    }

    @Test fun markdownIsStripped() {
        assertEquals("bold", AnswerFormat.clean("**bold**"))
        assertEquals("italic", AnswerFormat.clean("*italic*"))
        assertEquals("Heading", AnswerFormat.clean("# Heading"))
        assertEquals("\u2022 item", AnswerFormat.clean("* item"))
        assertEquals("code", AnswerFormat.clean("`code`"))
        assertEquals("gone", AnswerFormat.clean("~~gone~~"))
    }

    @Test fun latexIsWrittenOutRatherThanShownRaw() {
        assertEquals("x", AnswerFormat.clean("\$\$x\$\$"))
        assertEquals("1/2", AnswerFormat.clean("\\frac{1}{2}"))
        assertEquals("Area x Rate", AnswerFormat.clean("\\text{Area} \\times \\text{Rate}"))
        assertEquals("a / b", AnswerFormat.clean("a \\div b"))
        assertEquals("<= 5", AnswerFormat.clean("\\le 5"))
    }

    // ---- the maths is rewritten, never deleted ----

    @Test fun aRootKeepsItsNameAndItsArgument() {
        // the case this change exists for: \sqrt{4} arrived on the screen as "4"
        assertEquals("sqrt(4)", AnswerFormat.clean("\\sqrt{4}"))
        assertEquals("sqrt(4) = 2", AnswerFormat.clean("\\sqrt{4} = 2"))
    }

    @Test fun anUnmappedCommandKeepsItsName() {
        // nothing in the table covers these, and deleting them is what made the
        // answer wrong rather than merely untidy
        assertEquals("foo(2)", AnswerFormat.clean("\\foo{2}"))
        assertEquals("alpha + beta", AnswerFormat.clean("\\alpha + \\beta"))
        assertEquals("pi r^2", AnswerFormat.clean("\\pi r^2"))
    }

    @Test fun noCommandIsEverSilentlyDeleted() {
        for ((input, name) in listOf(
            "\\sqrt{4}" to "sqrt",
            "\\foo{2}" to "foo",
            "\\alpha" to "alpha",
            "\\int_0^1 x^2 dx" to "int",
            "\\frac{\\sqrt{4}}{2}" to "sqrt",
            "\\beta_2" to "beta",
        )) {
            val out = AnswerFormat.clean(input)
            assertTrue("the command name vanished from '$input': '$out'", out.contains(name))
        }
        // said as the number the user was shown: a flattened answer is an answer
        // the model never gave
        assertFalse("the root was flattened again", AnswerFormat.clean("\\sqrt{4}").trim() == "4")
    }

    @Test fun anIndexedRootReadsAsAPower() {
        // something the calculator can work out, rather than a name it cannot
        assertEquals("(8)^(1/3)", AnswerFormat.clean("\\sqrt[3]{8}"))
    }

    @Test fun nestedMathsIsRewrittenFromTheInsideOut() {
        // a [^}]* match stops at the first closing brace and produces nonsense
        assertEquals("sqrt(1/2)", AnswerFormat.clean("\\sqrt{\\frac{1}{2}}"))
        assertEquals("sqrt(4)/2", AnswerFormat.clean("\\frac{\\sqrt{4}}{2}"))
    }

    @Test fun aGroupIsBracketedRatherThanErased() {
        // the braces used to be deleted outright, which silently changed what
        // the power applied to
        assertEquals("x^(2n)", AnswerFormat.clean("x^{2n}"))
        assertEquals("x^2n", AnswerFormat.clean("x^2n"))
    }

    @Test fun anEnvironmentKeepsItsEntriesAndLosesItsName() {
        val out = AnswerFormat.clean("\\begin{matrix} a & b \\\\ c & d \\end{matrix}")
        assertFalse("the environment name is still there: '$out'", out.contains("matrix"))
        for (cell in listOf("a", "b", "c", "d")) {
            assertTrue("the entry '$cell' was lost: '$out'", out.contains(cell))
        }
        assertTrue("the rows ran together: '$out'", out.contains("\n"))
    }

    @Test fun everyShapeOfMathsComesOutAsPlainText() {
        for ((input, expected) in maths) {
            val out = AnswerFormat.clean(input)
            assertEquals("'$input' was rewritten as '$out'", expected, out)
            for (bad in listOf("\\", "{", "}", "$")) {
                assertFalse("'$bad' survived '$input': '$out'", out.contains(bad))
            }
        }
    }

    @Test fun aHalfWrittenCommandDoesNotBecomeSilence() {
        // the model does not always close its braces; what it wrote still has to
        // be on screen
        assertEquals("sqrt(4", AnswerFormat.clean("\\sqrt{4"))
        assertEquals("sqrt 4", AnswerFormat.clean("\\sqrt 4"))
    }

    @Test fun multiplicationWithAsterisksSurvives() {
        // a plain asterisk between numbers is arithmetic, not emphasis
        assertEquals("2 * 3 * 4 = 24", AnswerFormat.clean("2 * 3 * 4 = 24"))
        assertEquals("5 * 4 sq ft", AnswerFormat.clean("5 * 4 sq ft"))
    }

    @Test fun plainTextIsUntouched() {
        val plain = "Total = 1,200 x 2,500 = Rs 30,00,000"
        assertEquals(plain, AnswerFormat.clean(plain))
    }

    @Test fun neverLeavesMarkersBehind() {
        val samples = listOf(
            realAnswer,
            "**a** *b* \$\$c\$\$ `d` # e \\frac{1}{2} \\text{x}",
            "\$\$\\text{Total} = \\text{A} \\times \\text{B}\$\$",
            "### Heading\n* one\n* two",
        )
        for (s in samples) {
            val out = AnswerFormat.clean(s)
            for (bad in listOf("**", "\$\$", "\\text", "\\times", "\\frac", "`")) {
                assertFalse("'$bad' left in: $out", out.contains(bad))
            }
        }
    }

    @Test fun emptyInputIsSafe() {
        assertEquals("", AnswerFormat.clean(""))
    }
}
