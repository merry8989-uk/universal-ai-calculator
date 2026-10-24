package com.merry8989uk.universalcalculator.data

import com.merry8989uk.universalcalculator.calc.CustomCalculator
import com.merry8989uk.universalcalculator.calc.CustomInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Saving and loading the user's calculators. No Android needed. */
class CustomCalcStoreTest {

    private val one = CustomCalculator(
        id = "c1",
        name = "Loan EMI",
        description = "Monthly payment",
        inputs = listOf(
            CustomInput("P", "Principal", 100000.0),
            CustomInput("r", "Rate", 0.01),
        ),
        expression = "P*r",
        unit = "\u20B9",
    )

    @Test fun oneCalculatorSurvivesARoundTrip() {
        assertEquals(listOf(one), CustomCalcStore.parse(CustomCalcStore.serialize(listOf(one))))
    }

    @Test fun severalCalculatorsSurviveInOrder() {
        val two = one.copy(id = "c2", name = "Other", inputs = emptyList(), expression = "1+1")
        val back = CustomCalcStore.parse(CustomCalcStore.serialize(listOf(one, two)))
        assertEquals(2, back.size)
        assertEquals("c1", back[0].id)
        assertEquals("c2", back[1].id)
    }

    @Test fun anEmptyListRoundTrips() {
        assertTrue(CustomCalcStore.parse(CustomCalcStore.serialize(emptyList())).isEmpty())
    }

    @Test fun newlinesInTheDescriptionSurvive() {
        val tricky = one.copy(description = "line one\nline two")
        val back = CustomCalcStore.parse(CustomCalcStore.serialize(listOf(tricky)))
        assertEquals("line one\nline two", back[0].description)
    }

    @Test fun backslashesSurvive() {
        val tricky = one.copy(description = "a \\ b \\n c")
        val back = CustomCalcStore.parse(CustomCalcStore.serialize(listOf(tricky)))
        assertEquals("a \\ b \\n c", back[0].description)
    }

    @Test fun pipesAndEqualsSignsSurvive() {
        val tricky = one.copy(expression = "P*r|2", description = "x = y | z")
        val back = CustomCalcStore.parse(CustomCalcStore.serialize(listOf(tricky)))
        assertEquals("P*r|2", back[0].expression)
        assertEquals("x = y | z", back[0].description)
    }

    @Test fun aFileWithoutTheHeaderIsIgnored() {
        assertTrue(CustomCalcStore.parse("something else\nc1\nname").isEmpty())
    }

    @Test fun anEmptyFileIsIgnored() {
        assertTrue(CustomCalcStore.parse("").isEmpty())
    }

    @Test fun aMissingFinalNewlineIsStillACompleteRecord() {
        // Cutting only the trailing newline loses nothing.
        val cut = CustomCalcStore.serialize(listOf(one)).substringBeforeLast("\n")
        assertEquals(listOf(one), CustomCalcStore.parse(cut))
    }

    @Test fun aRecordMissingItsLastFieldIsDropped() {
        // A genuinely partial record must be dropped, not half-read.
        val cut = CustomCalcStore.serialize(listOf(one)).substringBeforeLast("\n0.01")
        assertTrue("a partial record must not be returned", CustomCalcStore.parse(cut).isEmpty())
    }

    @Test fun aCorruptCountKeepsTheEntryWithNoInputs() {
        // A count that will not parse is treated as zero rather than losing the
        // calculator altogether.
        val text = "NEXUSCUSTOM1\nc1\nName\n\n\n1+1\nnotanumber\n"
        val back = CustomCalcStore.parse(text)
        assertEquals(1, back.size)
        assertEquals("c1", back[0].id)
        assertTrue(back[0].inputs.isEmpty())
        assertEquals("1+1", back[0].expression)
    }

    @Test fun aNonNumericDefaultBecomesZero() {
        val text = "NEXUSCUSTOM1\nc1\nName\n\n\nx\n1\nx\nValue\nabc\n"
        val back = CustomCalcStore.parse(text)
        assertEquals(1, back.size)
        assertEquals(0.0, back[0].inputs[0].default, 1e-9)
    }

    @Test fun itKeepsTheUnit() {
        val back = CustomCalcStore.parse(CustomCalcStore.serialize(listOf(one)))
        assertEquals("\u20B9", back[0].unit)
    }
}
