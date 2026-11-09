package com.merry8989uk.universalcalculator.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * An answer has to be able to go back into the calculator.
 *
 * The row under an answer offered Copy, Share and Clear, and nothing else: the
 * only way from a result to the expression line was to read it and retype it,
 * which is the one thing a calculator exists to save you. Insert is the fourth
 * action, and it is a callback rather than a clipboard write because the
 * expression lives in the calculator screen, not in the row and not in this one.
 *
 * The behaviour itself is Compose and cannot run here, so what is checked is the
 * wiring: that the row offers the action, that the screen passes it on, and that
 * the thing handed over is the answer rather than the explanation. The line the
 * calculator screen still needs is written down in SUMMARY.md, because that file
 * belongs to another change.
 */
class InsertRouteTest {

    private fun source(path: String): String? =
        listOf(File("app/src/main/java/com/merry8989uk/universalcalculator/$path"),
               File("src/main/java/com/merry8989uk/universalcalculator/$path"))
            .firstOrNull { it.exists() }?.readText()

    private val actions: String? get() = source("ui/AnswerActions.kt")
    private val screen: String? get() = source("ui/AskAiScreen.kt")

    private val strings: String? =
        listOf(File("app/src/main/res/values/strings.xml"),
               File("src/main/res/values/strings.xml"))
            .firstOrNull { it.exists() }?.readText()

    @Test fun theAnswerRowOffersAnInsert() {
        val text = actions ?: return
        assertTrue(
            "AnswerActions takes no Insert callback",
            text.contains("onInsert: (() -> Unit)? = null"),
        )
        assertTrue("the Insert button is not drawn", text.contains("R.string.insert"))
        assertTrue(
            "the button is drawn without asking whether there is anywhere to insert",
            text.contains("if (insert != null)"),
        )
        // Copy and Share must stay: Insert is a fourth action, not a replacement
        for (action in listOf("R.string.copy", "R.string.share", "R.string.clear")) {
            assertTrue("$action was dropped from the row", text.contains(action))
        }
    }

    @Test fun theScreenTakesTheCallbackAndDefaultsToDoingNothing() {
        val text = screen ?: return
        assertTrue(
            "AskAiScreen has no Insert callback with a no-op default",
            text.contains("onInsert: (String) -> Unit = {}"),
        )
        assertTrue(
            "the screen never passes the callback to the answer row",
            text.contains("onInsert = { onInsert("),
        )
    }

    @Test fun theAnswerIsWhatGetsInserted() {
        val text = screen ?: return
        // a paragraph of reasoning is not arithmetic
        assertTrue(
            "the whole reply is handed to the calculator, reasoning and all",
            text.contains("onInsert(shown.answer)"),
        )
        assertFalse(
            "the explanation is being inserted",
            text.contains("onInsert(shown.explanation)"),
        )
    }

    @Test fun theInsertLabelExistsInEnglish() {
        val text = strings ?: return
        assertTrue(
            "there is no insert string, so the button would show its name or nothing",
            text.contains("<string name=\"insert\">"),
        )
    }
}
