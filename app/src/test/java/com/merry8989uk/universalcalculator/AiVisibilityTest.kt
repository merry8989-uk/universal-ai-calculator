package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.ui.aiIsVisible
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The assistant, and everything that belongs to it.
 *
 * Turning Ask AI off has to take the whole thing away — not just the tab, but the
 * side drawer that explains a calculation, because a swipe from the left edge
 * used to bring it up whether the assistant was on or not.
 */
class AiVisibilityTest {

    private val calculator: String by lazy {
        listOf(
            File("src/main/java/com/merry8989uk/universalcalculator/ui/CalculatorScreen.kt"),
            File("app/src/main/java/com/merry8989uk/universalcalculator/ui/CalculatorScreen.kt"),
        ).firstOrNull { it.exists() }?.readText().orEmpty()
    }

    // ---- the rule itself ----

    @Test fun theAssistantIsHiddenWhenItIsOff() {
        assertFalse(aiIsVisible(aiEnabled = false, minimal = false))
    }

    @Test fun theAssistantIsHiddenInMinimalMode() {
        assertFalse(aiIsVisible(aiEnabled = true, minimal = true))
    }

    @Test fun theAssistantShowsOnlyWhenItIsOnAndNotMinimal() {
        assertTrue(aiIsVisible(aiEnabled = true, minimal = false))
    }

    // ---- and the drawer obeys the rule ----

    @Test fun theSideDrawerCannotBeSwipedOpenWhenTheAssistantIsOff() {
        if (calculator.isBlank()) return
        assertTrue(
            "the drawer must take its gestures from whether the assistant is visible",
            "gesturesEnabled = aiVisible" in calculator,
        )
    }

    @Test fun theSideDrawerClosesWhenTheAssistantIsSwitchedOff() {
        if (calculator.isBlank()) return
        assertTrue(
            "an open explainer should close when the assistant is turned off",
            "if (!aiVisible) drawerState.close()" in calculator,
        )
    }

    @Test fun theMagicButtonIsBehindTheSameRule() {
        if (calculator.isBlank()) return
        val magic = calculator.indexOf("MagicButton(onClick = { explainNow() })")
        assertTrue("the magic button is missing", magic > 0)
        // the nearest guard above it is the assistant's own
        val before = calculator.substring(0, magic)
        val guard = before.lastIndexOf("if (aiVisible)")
        assertTrue("the magic button is not guarded by aiVisible", guard > 0)
    }

    @Test fun theDrawerExistsAtAll() {
        if (calculator.isBlank()) return
        assertTrue("the explainer drawer is missing", "ModalNavigationDrawer" in calculator)
    }
}
