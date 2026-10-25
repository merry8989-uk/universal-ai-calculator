package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.CalcInfo
import com.merry8989uk.universalcalculator.calc.ImpliedMultiplication
import com.merry8989uk.universalcalculator.calc.PercentageMode
import com.merry8989uk.universalcalculator.ui.label
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The keyboard and display settings.
 *
 * The two signs are what the keys *show* — what they insert is always `/` and
 * `*`, because that is what the evaluator reads. That separation is the thing
 * worth testing: a pretty label must never change the arithmetic.
 */
class KeyboardDisplayTest {

    // ---------------- the two signs ----------------

    @Test fun theDivideKeyShowsTheChosenSign() {
        assertEquals("\u00F7", label("/", divisionSign = "\u00F7"))
        assertEquals("/", label("/", divisionSign = "/"))
    }

    @Test fun theMultiplyKeyShowsTheChosenSign() {
        assertEquals("\u00D7", label("*", multiplicationSign = "\u00D7"))
        assertEquals("*", label("*", multiplicationSign = "*"))
    }

    @Test fun theTwoSignsAreIndependent() {
        // changing one must not change the other
        assertEquals("\u00F7", label("/", divisionSign = "\u00F7", multiplicationSign = "*"))
        assertEquals("*", label("*", divisionSign = "\u00F7", multiplicationSign = "*"))
        assertEquals("/", label("/", divisionSign = "/", multiplicationSign = "\u00D7"))
        assertEquals("\u00D7", label("*", divisionSign = "/", multiplicationSign = "\u00D7"))
    }

    @Test fun theDefaultIsThePrintedSigns() {
        assertEquals("\u00F7", label("/"))
        assertEquals("\u00D7", label("*"))
    }

    @Test fun everyOtherKeyIsUnaffected() {
        for (k in listOf("7", "del", "C", "%", "+", "-", "=", "ans", "sin", "sqrt")) {
            assertEquals("$k changed with the sign setting", label(k), label(k, "/", "*"))
        }
    }

    // ---------------- the note under the result ----------------

    @Test fun aMissingSignIsExplained() {
        assertEquals(
            "a missing \u00D7 counts as \u00D7",
            CalcInfo.explain("2pi", implied = ImpliedMultiplication.SAME_PRECEDENCE),
        )
        assertEquals(
            "a missing \u00D7 is worked out first",
            CalcInfo.explain("2pi", implied = ImpliedMultiplication.HIGHER_PRECEDENCE),
        )
    }

    @Test fun aBracketWithoutASignIsExplained() {
        assertTrue(CalcInfo.explain("3(4+5)").contains("missing"))
        assertTrue(CalcInfo.explain("(2)(3)").contains("missing"))
    }

    @Test fun aPercentageIsExplainedByWhatItWasTakenOf() {
        assertEquals(
            "20% of 100 = 20",
            CalcInfo.explain("100 + 20%", PercentageMode.MULTIPLICATIVE),
        )
        assertEquals(
            "20% off 100 = 20",
            CalcInfo.explain("100 - 20%", PercentageMode.MULTIPLICATIVE),
        )
    }

    @Test fun aPercentageAsAPlainNumberIsExplainedDifferently() {
        assertEquals(
            "20% is 0.2, so the answer is 100.2",
            CalcInfo.explain("100 + 20%", PercentageMode.ADDITIVE),
        )
    }

    @Test fun anExpressionThatReadsObviousHasNoNote() {
        for (e in listOf("2+3", "10*4", "100/8", "2^10", "(1+2)*3", "200 * 10%")) {
            assertEquals("$e should need no note", "", CalcInfo.explain(e))
        }
    }

    @Test fun bothNotesCanAppearTogether() {
        // a missing sign and a percentage in one expression
        val note = CalcInfo.explain("2pi + 20%", PercentageMode.MULTIPLICATIVE)
        assertTrue("expected both notes, got '$note'", note.contains("missing") && note.contains("%"))
    }

    @Test fun theNoteUsesTheSameWordingForEitherSetting() {
        // the note must change when the setting changes, or it is not telling the truth
        val same = CalcInfo.explain("1/2pi", implied = ImpliedMultiplication.SAME_PRECEDENCE)
        val higher = CalcInfo.explain("1/2pi", implied = ImpliedMultiplication.HIGHER_PRECEDENCE)
        assertFalse("the note is the same either way", same == higher)
    }

    // ---------------- the settings the sync carries ----------------

    @Test fun everySyncableSettingHasBothDirections() {
        // the export and import lists are read straight from the source, so a
        // setting added to one and not the other is caught here
        val source = java.io.File(
            "src/main/java/com/merry8989uk/universalcalculator/settings/SettingsRepository.kt",
        ).let { if (it.exists()) it else java.io.File(
            "app/src/main/java/com/merry8989uk/universalcalculator/settings/SettingsRepository.kt",
        ) }
        assertTrue("could not find SettingsRepository", source.exists())
        val text = source.readText()
        val exported = Regex("put\\(\"([^\"]+)\"").findAll(
            text.substringAfter("suspend fun exportSyncable").substringBefore("suspend fun importSyncable"),
        ).map { it.groupValues[1] }.toSet()
        val imported = Regex("(?:str|bool|num|flt)\\(\"([^\"]+)\"").findAll(
            text.substringAfter("suspend fun importSyncable"),
        ).map { it.groupValues[1] }.toSet()

        assertTrue("nothing is exported", exported.isNotEmpty())
        assertEquals(
            "these are exported but never imported: ${exported - imported}",
            emptySet<String>(), exported - imported,
        )
        assertEquals(
            "these are imported but never exported: ${imported - exported}",
            emptySet<String>(), imported - exported,
        )
    }
}
