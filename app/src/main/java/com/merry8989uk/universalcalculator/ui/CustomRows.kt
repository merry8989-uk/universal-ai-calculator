package com.merry8989uk.universalcalculator.ui

import com.merry8989uk.universalcalculator.calc.CustomCalculator
import com.merry8989uk.universalcalculator.calc.Formula
import com.merry8989uk.universalcalculator.data.CustomCalcStore

/**
 * One saved calculator, as the hub draws it.
 *
 * The row is the only place that knows both things a custom calculator can do:
 * [formula] is the same shape a built-in calculator has, so a tap opens the
 * detail screen the built-ins use, and [calculator] is what the editor needs,
 * so a hold opens it. Keeping both here means the list can never drift into
 * showing a row that runs and a row that edits the same calculator — which is
 * what it used to do, twice over.
 */
data class CustomRow(
    val id: String,
    val title: String,
    val subtitle: String,
    /** Tap: hand this to the detail screen, exactly as a built-in would. */
    val formula: Formula,
    /** Hold: the calculator itself, for the editor. */
    val calculator: CustomCalculator,
)

/**
 * The rows for the "My calculators" category: one per saved calculator.
 *
 * The saved list is the source of truth, but it is a file on the device and a
 * file can come back with the same id twice — a restored backup, a hand-edited
 * entry. Drawing whatever it holds would put the duplicate on screen, so the
 * first id wins and the rest are dropped: a reader who sees two identical rows
 * has no way to tell which one their edits land on.
 *
 * The formula is built here rather than in the composable so that what a tap
 * runs and what the row says are made from the same calculator in the same
 * pass.
 */
fun customRows(calculators: List<CustomCalculator>): List<CustomRow> {
    val seen = HashSet<String>(calculators.size)
    val rows = ArrayList<CustomRow>(calculators.size)
    calculators.forEach { c ->
        if (!seen.add(c.id)) return@forEach
        val formula = c.toFormula()
        rows.add(
            CustomRow(
                id = c.id,
                title = formula.title,
                subtitle = formula.description,
                formula = formula,
                calculator = c,
            ),
        )
    }
    return rows
}

/**
 * The text that carries one calculator to another phone.
 *
 * It is the store's own format, one entry long, so nothing new has to be
 * invented for sharing and an imported calculator is read by exactly the same
 * parser that reads the file on this device.
 */
fun shareText(c: CustomCalculator): String = CustomCalcStore.serialize(listOf(c))

/**
 * What a pasted blob adds, with ids that cannot collide.
 *
 * A calculator travels with its id, and the id is how saving an edit finds the
 * calculator to replace — so importing one that carries an id already on this
 * device would overwrite the local one instead of sitting next to it. A clash
 * is therefore renamed rather than rejected, and anything that does not read
 * back as a usable calculator is skipped, because a half-read entry is worse
 * than the one that did not arrive.
 */
fun importedCalculators(
    blob: String,
    existing: List<CustomCalculator>,
): List<CustomCalculator> {
    val taken = existing.mapTo(HashSet()) { it.id }
    val out = ArrayList<CustomCalculator>()
    CustomCalcStore.parse(blob).forEach { c ->
        val name = c.name.trim()
        if (name.isEmpty() || c.expression.isBlank()) return@forEach
        val base = c.id.trim().ifBlank { "imported" }
        var id = base
        var n = 2
        while (!taken.add(id)) {
            id = "$base-$n"
            n++
        }
        out.add(c.copy(id = id, name = name))
    }
    return out
}
