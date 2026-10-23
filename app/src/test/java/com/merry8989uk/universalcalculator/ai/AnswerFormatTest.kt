package com.merry8989uk.universalcalculator.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The assistant must never show the user raw Markdown or LaTeX. These tests use
 * a real answer that shipped to a device with ** and $$ still visible.
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

    @Test fun latexIsStripped() {
        assertEquals("x", AnswerFormat.clean("\$\$x\$\$"))
        assertEquals("1/2", AnswerFormat.clean("\\frac{1}{2}"))
        assertEquals("Area x Rate", AnswerFormat.clean("\\text{Area} \\times \\text{Rate}"))
        assertEquals("a / b", AnswerFormat.clean("a \\div b"))
        assertEquals("<= 5", AnswerFormat.clean("\\le 5"))
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
