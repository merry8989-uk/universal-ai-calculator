package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.settings.Choice
import com.merry8989uk.universalcalculator.settings.visibleChoices
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The three-chip rule, and the export bug that made a button look broken.
 */
class SettingsShapeTest {

    private val languages = listOf(
        Choice("en", "English"),
        Choice("hi", "हिन्दी"),
        Choice("ta", "தமிழ்"),
        Choice("bn", "বাংলা"),
        Choice("mr", "मराठी"),
    )

    @Test fun theDefaultAlwaysTakesTheFirstChip() {
        // whatever is selected, the default is visible: a setting should show what
        // most people want without opening anything
        for (picked in languages.map { it.value }) {
            assertEquals("en", visibleChoices(languages, picked)[0].value)
        }
    }

    @Test fun theSecondChipIsTheNextOptionWhenNothingElseIsPicked() {
        val shown = visibleChoices(languages, "en")
        assertEquals(2, shown.size)
        assertEquals("hi", shown[1].value)
    }

    @Test fun theSecondChipShowsWhatIsActuallyPicked() {
        // picking Tamil must not hide it behind More, or all three chips look
        // unselected and the setting looks broken
        val shown = visibleChoices(languages, "ta")
        assertEquals("en", shown[0].value)
        assertEquals("ta", shown[1].value)
    }

    @Test fun neverMoreThanTwoChipsBesideMore() {
        for (picked in languages.map { it.value }) {
            assertTrue("too many chips for '$picked'", visibleChoices(languages, picked).size <= 2)
        }
    }

    @Test fun aSingleOptionShowsOneChip() {
        val one = listOf(Choice("only", "Only"))
        assertEquals(1, visibleChoices(one, "only").size)
        assertTrue(visibleChoices(emptyList(), "x").isEmpty())
    }

    @Test fun anUnknownSelectionFallsBackToTheSecondOption() {
        // a saved value from an older version must not leave the row blank
        val shown = visibleChoices(languages, "zz")
        assertEquals("en", shown[0].value)
        assertEquals("hi", shown[1].value)
    }

    @Test fun theShareSheetIsStartedWithTheNewTaskFlag() {
        // The export came from the application context, and starting an activity
        // from one without this flag throws. The exception was lost inside a
        // coroutine, so the button looked like it did nothing at all.
        val source = listOf(
            File("app/src/main/java/com/merry8989uk/universalcalculator/diagnostics/LogExporter.kt"),
            File("src/main/java/com/merry8989uk/universalcalculator/diagnostics/LogExporter.kt"),
        ).firstOrNull { it.exists() } ?: return
        val text = source.readText()
        assertTrue("the chooser is started without FLAG_ACTIVITY_NEW_TASK",
            text.contains("FLAG_ACTIVITY_NEW_TASK"))
        assertTrue("share no longer reports a failure to the caller",
            text.contains("fun share(") && text.contains("): String?"))
    }

    @Test fun theExportResultIsShownToTheUser() {
        // a button that appears to do nothing is indistinguishable from a broken one
        val source = listOf(
            File("app/src/main/java/com/merry8989uk/universalcalculator/diagnostics/DiagnosticsScreen.kt"),
            File("src/main/java/com/merry8989uk/universalcalculator/diagnostics/DiagnosticsScreen.kt"),
        ).firstOrNull { it.exists() } ?: return
        val text = source.readText()
        assertTrue("the export message is never rendered", text.contains("vm.message?.let"))
        assertTrue("the exported path is never shown", text.contains("lastExportPath"))
    }

    @Test fun diagnosticsHasItsTabsInOneScreen() {
        val source = listOf(
            File("app/src/main/java/com/merry8989uk/universalcalculator/diagnostics/DiagnosticsScreen.kt"),
            File("src/main/java/com/merry8989uk/universalcalculator/diagnostics/DiagnosticsScreen.kt"),
        ).firstOrNull { it.exists() } ?: return
        val text = source.readText()
        for (tab in listOf("Logs", "Usage", "Connection", "Export")) {
            assertTrue("the $tab tab is missing", text.contains("\"$tab\""))
        }
    }
}
