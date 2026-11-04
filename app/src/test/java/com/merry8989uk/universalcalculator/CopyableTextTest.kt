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

    @Test fun theFormulaReferenceIsSelectable() {
        val text = source("ui/FormulaReferenceScreen.kt") ?: return
        assertTrue("the formula is not wrapped in Copyable", text.contains("Copyable("))
        assertTrue("there is no Copy/Share row on a formula", text.contains("CopyShareRow("))
        // the answer and the working are the other thing people want out
        assertTrue("the answer is not copyable", text.contains("text = answer"))
        assertTrue("the working is not copyable", text.contains("text = working"))
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
