package com.merry8989uk.universalcalculator.calc

/**
 * One place a search can land: a unit, and the category it lives in.
 *
 * The screen keeps its selection as two positions, so a hit is two positions
 * too. Handing back the unit and category objects instead would only make the
 * caller look up the indices again.
 */
data class UnitHit(val categoryIndex: Int, val unitIndex: Int) {
    val category: UnitCategory get() = Units.categories[categoryIndex]
    val unit: Unit get() = category.units[unitIndex]
}

/**
 * Finding a unit by typing part of what it is called.
 *
 * The converter offered only the category chips and the unit dropdown, so a
 * unit far down the list — "Concentration (molar)" was the complaint — cost a
 * long hunt through both. A person looking for a unit knows one of three
 * things about it, so all three are searched: the category it belongs to
 * ("concentration"), its name ("molar") and its symbol ("mol/L").
 *
 * Ranking is what lets one field serve all three. An exact symbol comes first,
 * because "m" should mean the metre and not the micrometre; then a symbol that
 * starts with what was typed; then a name that starts with it. The looser
 * "appears somewhere" matches follow, and a category match comes last of all,
 * since it returns a whole category and is the least specific answer a query
 * can give.
 *
 * Matching folds case, the degree sign and the punctuation inside names, so
 * "°C", "c" and "celsius" all reach the same unit, and "concentration molar"
 * typed without the brackets still finds that category. Folding only ever
 * widens what matches; the order the results come back in is unaffected.
 */
object UnitSearch {

    /** Lower is a better answer. */
    private const val SYMBOL_EXACT = 0
    private const val SYMBOL_STARTS = 1
    private const val NAME_STARTS = 2
    private const val SYMBOL_CONTAINS = 3
    private const val NAME_CONTAINS = 4
    private const val CATEGORY_CONTAINS = 5

    /**
     * The form both the query and the text are compared in. Removing the spaces
     * is what lets "light year" and "Light year" meet, and removing the brackets
     * is what lets "concentration molar" reach "Concentration (molar)".
     */
    private fun fold(text: String): String =
        text.lowercase()
            .replace("\u00B0", "")
            .replace("(", "")
            .replace(")", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()

    private fun rank(query: String, symbol: String, name: String, category: String): Int? {
        val s = fold(symbol)
        val n = fold(name)
        return when {
            s == query -> SYMBOL_EXACT
            s.startsWith(query) -> SYMBOL_STARTS
            n.startsWith(query) -> NAME_STARTS
            s.contains(query) -> SYMBOL_CONTAINS
            n.contains(query) -> NAME_CONTAINS
            fold(category).contains(query) -> CATEGORY_CONTAINS
            else -> null
        }
    }

    /**
     * Every unit that answers [query], best first. A blank query matches
     * nothing rather than everything: an empty field means the caller is
     * browsing, and the full list is already on screen.
     */
    fun hits(
        query: String,
        categories: List<UnitCategory> = Units.categories,
        limit: Int = Int.MAX_VALUE,
    ): List<UnitHit> {
        val q = fold(query)
        if (q.isEmpty() || limit <= 0) return emptyList()
        val found = ArrayList<Ranked>(32)
        for (ci in categories.indices) {
            val category = categories[ci]
            for (ui in category.units.indices) {
                val unit = category.units[ui]
                val rank = rank(q, unit.symbol, unit.name, category.name) ?: continue
                found.add(Ranked(rank, fold(unit.symbol).length, UnitHit(ci, ui)))
            }
        }
        // Shorter symbols first within a rank, so "mm" comes before "mol/L"
        // when both only start with the typed "m"; the indices then make the
        // order stable instead of depending on the sort algorithm.
        found.sortWith(
            compareBy<Ranked> { it.rank }
                .thenBy { it.symbolLength }
                .thenBy { it.hit.categoryIndex }
                .thenBy { it.hit.unitIndex },
        )
        return found.take(limit).map { it.hit }
    }

    private class Ranked(val rank: Int, val symbolLength: Int, val hit: UnitHit)
}
