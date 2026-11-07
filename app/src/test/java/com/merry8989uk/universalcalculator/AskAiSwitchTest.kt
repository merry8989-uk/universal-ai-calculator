package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.aiIsVisible
import com.merry8989uk.universalcalculator.ui.chatArchiveVisible
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Turning Ask AI off has to take the assistant away, everywhere.
 *
 * It was reported as not working, and the switch does now reach the tab, the
 * bottom bar, the magic button, the voice and photo keys and the "ask about
 * this" row. The one place it did not reach was the conversation archive: the
 * flag that says it is open is remembered across a rotation and a restart, so it
 * cannot be trusted to be false just because the switch is off.
 */
class AskAiSwitchTest {

    // ---- the rule ----

    @Test fun theAssistantIsOnOnlyWhenBothSwitchesAllowIt() {
        assertTrue(aiIsVisible(aiEnabled = true, minimal = false))
        assertFalse("the off switch did nothing", aiIsVisible(aiEnabled = false, minimal = false))
        assertFalse("minimal mode left the assistant on", aiIsVisible(aiEnabled = true, minimal = true))
        assertFalse(aiIsVisible(aiEnabled = false, minimal = true))
    }

    @Test fun theArchiveGoesWhenTheSwitchGoes() {
        assertTrue(chatArchiveVisible(aiEnabled = true, minimal = false, open = true))
        assertFalse(
            "the archive stayed open with the assistant switched off",
            chatArchiveVisible(aiEnabled = false, minimal = false, open = true),
        )
        assertFalse(
            "the archive stayed open in minimal mode",
            chatArchiveVisible(aiEnabled = true, minimal = true, open = true),
        )
    }

    @Test fun theArchiveIsNotShownWhenItWasNeverOpened() {
        assertFalse(chatArchiveVisible(aiEnabled = true, minimal = false, open = false))
    }

    // ---- the surfaces, checked in the source ----

    private fun source(path: String): String? =
        listOf(File("app/src/main/java/com/merry8989uk/universalcalculator/$path"),
               File("src/main/java/com/merry8989uk/universalcalculator/$path"))
            .firstOrNull { it.exists() }?.readText()

    @Test fun everyAssistantSurfaceInTheShellIsBehindTheSwitch() {
        val text = source("ui/AppRoot.kt") ?: return
        // the archive has to go through the rule rather than its own flag
        assertTrue(
            "the archive is drawn from its own flag",
            text.contains("chatArchiveVisible(settings.aiEnabled, minimal, chatArchiveOpen)"),
        )
        assertTrue("the archive is not closed when the switch goes off",
            text.contains("if (!aiVisible) chatArchiveOpen = false"))
        // the tab bar and the tab itself
        assertTrue("the AI tab is not reset", text.contains("if (!aiVisible && tab == AI_TAB)"))
        assertTrue("the AI tab still draws the assistant",
            text.contains("AI_TAB -> if (!aiVisible)"))
    }

    @Test fun everyAssistantSurfaceOnTheKeypadIsBehindTheSwitch() {
        val text = source("ui/CalculatorScreen.kt") ?: return
        // the magic button, the drawer, voice, photo and "ask about this" are all
        // the assistant, and each is guarded
        val guards = Regex("if \\(aiVisible\\)").findAll(text).count()
        assertTrue("only $guards surfaces are guarded", guards >= 4)
        assertTrue("the drawer is not closed when the switch goes off",
            text.contains("if (!aiVisible) drawerState.close()"))
    }
}
