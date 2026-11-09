package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.ResultCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What the shared picture says.
 *
 * The drawing is a canvas and cannot be checked here; the words can. They are
 * built by a pure function, and the rules that matter are the ones that stop a
 * card looking broken: a long value is cut rather than run off the edge, a
 * newline does not break the layout, and the inputs are not sent.
 */
class ResultCardTest {

    @Test fun theCardCarriesTheTitleTheCategoryAndTheAnswer() {
        val card = ResultCard.content(
            title = "Loan EMI",
            category = "Finance",
            results = listOf("Instalment" to "8,791.59", "Total" to "5,27,495"),
        )
        assertEquals("Loan EMI", card.title)
        assertEquals("Finance", card.subtitle)
        assertEquals(2, card.rows.size)
        assertEquals("Instalment" to "8,791.59", card.rows[0])
        assertEquals("Nexus Calc", card.footer)
    }

    @Test fun aLongValueIsCutRatherThanRunOffTheEdge() {
        val long = "1,234,567,890,123,456,789,012,345,678,901,234,567"
        val card = ResultCard.content("x", "Math", listOf("Result" to long))
        val shown = card.rows.single().second
        assertTrue("the value was not cut: ${shown.length}", shown.length <= 34)
        assertTrue("the cut is not marked", shown.endsWith("\u2026"))
    }

    @Test fun aNewlineInAnAnswerDoesNotBreakTheCard() {
        val card = ResultCard.content("x", "Math", listOf("Working" to "a = 1\nb = 2"))
        assertTrue("the newline is still there", !card.rows.single().second.contains("\n"))
    }

    @Test fun atMostSevenAnswersFit() {
        val many = (1..20).map { "Result $it" to "$it" }
        val card = ResultCard.content("x", "Math", many)
        assertEquals(7, card.rows.size)
    }

    @Test fun theInputsAreNotInThePicture() {
        // the values that produced an answer are often a salary or a dose, and
        // the card is the thing people forward
        val card = ResultCard.content("Loan EMI", "Finance", listOf("Instalment" to "8,791"))
        assertTrue("the card repeats the inputs", card.rows.none { it.first.startsWith("Principal") })
    }
}
