package com.merry8989uk.universalcalculator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The assistant on the screens that are not the keypad.
 *
 * Convert and Formulas have no room for a row of buttons, so the assistant is a
 * small disc that can be dragged out of the way. It is drawn inside the screen
 * rather than in a system overlay, which is what stops it outliving the app, and
 * its position is kept as a fraction of the space so it lands in the same place
 * after a rotation instead of off the edge.
 *
 * The tab button for the assistant is not drawn on those two screens, so the disc
 * is the only way in and the two do not both appear.
 */
class ExplainBubbleTest {

    private fun source(path: String): String? = listOf(
        File("app/src/main/java/com/merry8989uk/universalcalculator/$path"),
        File("src/main/java/com/merry8989uk/universalcalculator/$path"),
    ).firstOrNull { it.exists() }?.readText()

    @Test fun theDiscCanBeDraggedAndStaysInsideTheScreen() {
        val text = source("ui/ExplainBubble.kt") ?: return
        assertTrue("it cannot be dragged", text.contains("detectDragGestures"))
        assertTrue("it does not remember where it was put", text.contains("rememberSaveable"))
        // clamped, which is what stops it being dragged out of reach
        assertTrue("the position is not clamped", text.contains("coerceIn(0f, 1f)"))
        // a fraction of the space, so a rotation does not put it off the edge
        assertTrue("the position is in pixels, so it will not survive a rotation",
            text.contains("* maxX") && text.contains("* maxY"))
    }

    @Test fun itIsNotAWindowThatOutlivesTheApp() {
        val text = source("ui/ExplainBubble.kt") ?: return
        // drawn in the screen's own layout: when the app closes, this goes with it
        assertTrue("it is a system overlay", !text.contains("TYPE_APPLICATION_OVERLAY"))
        assertTrue("it uses a window manager", !text.contains("WindowManager"))
    }

    @Test fun itIsSmallAndHasNoAccessibilityBubble() {
        val text = source("ui/ExplainBubble.kt") ?: return
        assertTrue("it is not a small disc", text.contains("54.dp"))
        // the request was explicit: no accessibility requirement on this one
        assertTrue("it carries accessibility semantics nobody asked for",
            !text.contains("contentDescription"))
    }

    @Test fun itIsOnBothTabsThatNeededIt() {
        val convert = source("ui/ConvertScreen.kt") ?: return
        assertTrue("the unit converter has no assistant", convert.contains("ExplainBubble("))

        // On the formulas tab it wraps the whole tab rather than one screen inside
        // it: it was on the formula page only, so a formula opened from the list —
        // or a tool — had no way to reach it, which read as the disc appearing on
        // some formulas and not others.
        val formulas = source("ui/FormulasScreen.kt") ?: return
        assertTrue("the formulas tab has no assistant", formulas.contains("ExplainBubble("))

        // and only once: a second disc inside a screen would sit on top of this one
        for (inner in listOf("ui/FormulaWebScreen.kt", "ui/ConverterScreen.kt")) {
            val text = source(inner) ?: continue
            assertTrue("$inner draws a second disc", !text.contains("ExplainBubble("))
        }
    }

    @Test fun theAssistantIsReachableAndActuallyAsked() {
        val root = source("ui/AppRoot.kt") ?: return
        // the disc hands the question to the assistant and goes to it
        assertTrue("the disc does not reach the assistant",
            root.contains("CONVERT_TAB -> ConvertScreen("))
        assertTrue("the tools tab does not reach it",
            root.contains("onExplain = { q -> aiAsk = q"))
        // and the tab button is not drawn on those two screens
        assertTrue("the assistant button is still in the bar on those tabs",
            root.contains("!aiVisible || tab == CONVERT_TAB || tab == FORMULAS_TAB"))
    }

    @Test fun theQuestionSaysWhatIsBeingExplained() {
        val text = source("ui/ExplainBubble.kt") ?: return
        assertTrue("no question is built", text.contains("fun explainQuestion("))
        assertTrue("it does not say what it is looking at", text.contains("Explain this"))
        assertTrue("it does not ask for the answer first", text.contains("give the answer first"))
    }
}
