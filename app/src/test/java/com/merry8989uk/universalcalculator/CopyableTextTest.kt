package com.merry8989uk.universalcalculator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Text on the reference and converter screens has to be takeable.
 *
 * A formula is something people want in a message or a note, and a plain Text in
 * Compose is not selectable at all — long-press did nothing. These checks are on
 * the source because the behaviour itself lives in Compose's SelectionContainer
 * and in the system clipboard, neither of which runs in a unit test.
 */
class CopyableTextTest {

    private fun source(path: String): String? =
        listOf(File("app/src/main/java/com/merry8989uk/universalcalculator/$path"),
               File("src/main/java/com/merry8989uk/universalcalculator/$path"))
            .firstOrNull { it.exists() }?.readText()

    @Test fun theSharedCopyableIsASelectionContainer() {
        val text = source("ui/Copyable.kt") ?: return
        // without this, long-press does nothing and there are no handles
        assertTrue("Copyable does not wrap its text in a SelectionContainer",
            text.contains("SelectionContainer"))
        assertTrue("there is no clipboard write", text.contains("ClipData.newPlainText"))
        assertTrue("sharing does not start with NEW_TASK",
            text.contains("FLAG_ACTIVITY_NEW_TASK"))
    }

    @Test fun theFormulaPageLetsTheReaderHoldAndCopy() {
        // The formula screen is a page now, and selection there is the phone's
        // own: hold the text, drag the handles, Copy or Share from the toolbar.
        // That is why there are no Copy and Share buttons on a card — the two
        // buttons were the thing that made it feel unlike every other app.
        val page = source("../../assets/formula-site.html")
            ?: java.io.File("app/src/main/assets/formula-site.html").takeIf { it.exists() }?.readText()
            ?: return
        assertTrue("the formula text is not selectable", page.contains("-webkit-user-select:text"))
        assertTrue("there is a Copy button on the card", !page.contains("class=\"act copy\""))
        assertTrue("the page does not ask the app for the working", page.contains("Android.calculate"))
        assertTrue("the working is not rendered", page.contains("steps"))
    }

    @Test fun bothConvertersAreSelectable() {
        val units = source("ui/ConvertScreen.kt") ?: return
        assertTrue("the unit answer is not copyable", units.contains("Copyable("))
        assertTrue("there is no Copy/Share row on the unit answer", units.contains("CopyShareRow("))

        val text = source("ui/ConverterScreen.kt") ?: return
        assertTrue("the converted text is not copyable", text.contains("Copyable("))
        assertTrue("there is no Copy/Share row on the converted text", text.contains("CopyShareRow("))

        val currency = source("ui/CurrencyScreen.kt") ?: return
        assertTrue("the converted amount is not copyable", currency.contains("Copyable("))
    }

    @Test fun aCopySaysItWorked() {
        val text = source("ui/Copyable.kt") ?: return
        // the clipboard is invisible; a button with no confirmation reads as broken
        assertTrue("copying gives no confirmation", text.contains("\"Copied\""))
    }
}
