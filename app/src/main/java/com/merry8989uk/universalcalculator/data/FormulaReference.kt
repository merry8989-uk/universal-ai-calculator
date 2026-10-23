package com.merry8989uk.universalcalculator.data

import android.content.Context
import org.json.JSONObject

/** One entry of the built-in formula reference. */
data class RefFormula(
    val category: String,
    val title: String,
    val formula: String,
)

/**
 * The reference covers the formulas that cannot be a calculator: identities,
 * rules, tables and theorems. It is loaded from the bundled asset so it works
 * with no network.
 */
object FormulaReference {

    @Volatile
    private var cache: List<RefFormula>? = null

    fun load(context: Context): List<RefFormula> {
        cache?.let { return it }
        val text = context.assets.open("formulas.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(text).getJSONArray("formulas")
        val list = ArrayList<RefFormula>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(RefFormula(o.getString("c"), o.getString("t"), o.getString("f")))
        }
        cache = list
        return list
    }

    /** Categories in the order they appear in the data file. */
    fun categories(list: List<RefFormula>): List<String> {
        val seen = LinkedHashSet<String>()
        list.forEach { seen.add(it.category) }
        return seen.toList()
    }

    /** Case-insensitive search over the title, the formula and the category. */
    fun search(list: List<RefFormula>, query: String, category: String?): List<RefFormula> {
        val q = query.trim().lowercase()
        return list.filter { f ->
            (category == null || f.category == category) &&
                (q.isEmpty() ||
                    f.title.lowercase().contains(q) ||
                    f.formula.lowercase().contains(q) ||
                    f.category.lowercase().contains(q))
        }
    }
}
