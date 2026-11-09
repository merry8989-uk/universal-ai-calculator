package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.calc.CustomCalculator
import com.merry8989uk.universalcalculator.calc.CustomInput
import com.merry8989uk.universalcalculator.ui.customRows
import com.merry8989uk.universalcalculator.ui.importedCalculators
import com.merry8989uk.universalcalculator.ui.shareText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rows the hub draws for a person's own calculators.
 *
 * Each saved calculator is one line in the list, and the line has to do both
 * things the calculator can: a tap works it out, a hold opens it for editing.
 * It used to be drawn twice — once from the formula list and once more below it
 * — and both copies opened the editor, so a calculator somebody built could not
 * be used from the very list it lived in.
 *
 * The rules are checked here rather than on a device because they are a
 * question about the list, not about pixels: which calculators become rows, how
 * many rows each one gets, what a tap runs, and what a hold opens.
 */
class CustomRowsTest {

    private fun calculator(
        id: String,
        name: String = "Loan payment",
        expression: String = "p * r",
        inputs: List<CustomInput> = listOf(
            CustomInput("p", "Principal", 1000.0),
            CustomInput("r", "Rate", 0.1),
        ),
    ) = CustomCalculator(
        id = id,
        name = name,
        description = "What the payment comes to.",
        inputs = inputs,
        expression = expression,
    )

    @Test fun everySavedCalculatorIsOneRowAndTappingItRunsIt() {
        val saved = listOf(calculator("c1"), calculator("c2"), calculator("c3"))
        val rows = customRows(saved)

        assertEquals("a saved calculator did not get a row", 3, rows.size)
        assertEquals(
            "the rows are not the saved calculators, in order",
            listOf("c1", "c2", "c3"),
            rows.map { it.id },
        )

        // A tap hands the row's formula to the same detail screen a built-in
        // calculator opens, so the proof that it runs is that formula working
        // out its answer from its own defaults.
        rows.forEach { row ->
            val answer = row.formula.compute(row.formula.inputs.associate { it.key to it.default })
            assertEquals("the row did not produce an answer", 1, answer.size)
            assertEquals("1000 at 10% is not 100", 100.0, answer.first().value, 1e-9)
        }
    }

    @Test fun aSavedListThatRepeatsAnIdStillDrawsOneRow() {
        // A restored backup can bring the same calculator back twice. Two
        // identical rows leave no way to tell which one an edit lands on.
        val rows = customRows(listOf(calculator("c1"), calculator("c1"), calculator("c2")))
        assertEquals(listOf("c1", "c2"), rows.map { it.id })
    }

    @Test fun holdingARowHandsTheEditorThatCalculator() {
        val saved = calculator("c1", name = "EMI")
        val row = customRows(listOf(saved)).single()

        // The hold opens the editor on the calculator itself, not on a copy
        // that would save under a new id.
        assertEquals("the editor would open a different calculator", "c1", row.calculator.id)
        assertEquals("EMI", row.calculator.name)
        assertEquals("p * r", row.calculator.expression)
        // and it is the very same object the saved list holds, so an edit
        // replaces it rather than adding beside it
        assertTrue("the row rebuilt the calculator", row.calculator === saved)
    }

    @Test fun theRowIsNamedAndDescribedTheWayTheCalculatorIs() {
        val row = customRows(listOf(calculator("c1", name = "EMI", expression = "p * r"))).single()
        assertEquals("EMI", row.title)
        assertEquals("What the payment comes to.", row.subtitle)
        // the formula carries the custom category, so the row lands in the same
        // place as the built-ins when it is opened
        assertEquals(CustomCalculator.CATEGORY, row.formula.category)
    }

    @Test fun aSentCalculatorComesBackAsTheSameCalculator() {
        val sent = calculator("c1", name = "EMI")
        val arrived = importedCalculators(shareText(sent), existing = emptyList())
        assertEquals("what was sent is not what arrived", 1, arrived.size)
        assertEquals(sent, arrived.single())
    }

    @Test fun anImportDoesNotOverwriteTheCalculatorAlreadyHere() {
        // Saving an edit finds the calculator by id, so an imported one that
        // carried a local id would replace the local calculator instead of
        // sitting next to it.
        val local = calculator("c1", name = "Mine")
        val arrived = importedCalculators(shareText(calculator("c1", name = "Theirs")), listOf(local))

        assertEquals("the import was dropped", 1, arrived.size)
        assertNotEquals("the import took the local id", "c1", arrived.single().id)
        assertEquals("Theirs", arrived.single().name)
        // and it still runs, under its new id
        val answer = arrived.single().compute(arrived.single().defaults())
        assertEquals(100.0, answer, 1e-9)
    }

    @Test fun nothingIsAddedFromTextThatIsNotACalculator() {
        assertEquals(emptyList<CustomCalculator>(), importedCalculators("", emptyList()))
        assertEquals(
            emptyList<CustomCalculator>(),
            importedCalculators("hello, here is my calculator", emptyList()),
        )
        // an entry with no expression is not a calculator, whatever else it has
        val half = CustomCalculator(id = "c9", name = "Broken", expression = "")
        assertEquals(
            emptyList<CustomCalculator>(),
            importedCalculators(shareText(half), emptyList()),
        )
    }
}
