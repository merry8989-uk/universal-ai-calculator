package com.merry8989uk.universalcalculator.diagnostics

import android.content.Context

/**
 * Local usage counters.
 *
 * Everything stays on the device, in this app's own preferences. Nothing is
 * uploaded; the numbers only leave the phone if the user exports the
 * diagnostics log themselves. This is the "analytics" half of diagnostics.
 */
object Analytics {

    private const val PREFS = "usage_counters"
    private const val PREFIX = "c_"

    /** Human-readable group names for the keys we record. */
    private val GROUPS = mapOf(
        "tab" to "Tabs opened",
        "calc" to "Calculators used",
        "tool" to "Tools used",
        "ai" to "AI requests",
        "update" to "Update checks",
        "app" to "App events",
        "error" to "Errors",
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Counts one event. [key] looks like "calc:emi" or "ai:photo". */
    fun event(context: Context, key: String) {
        if (!DiagnosticsLogger.enabled) return
        val store = prefs(context)
        val name = PREFIX + key
        store.edit().putInt(name, store.getInt(name, 0) + 1).apply()
    }

    /** Every counter, biggest first. */
    fun counters(context: Context): List<Pair<String, Int>> =
        prefs(context).all
            .filter { it.key.startsWith(PREFIX) && it.value is Int }
            .map { it.key.removePrefix(PREFIX) to (it.value as Int) }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

    /** Just the totals per group, e.g. "Calculators used" -> 142. */
    fun groupTotals(context: Context): List<Pair<String, Int>> {
        val totals = HashMap<String, Int>()
        counters(context).forEach { (key, count) ->
            val group = key.substringBefore(':')
            totals[group] = (totals[group] ?: 0) + count
        }
        return GROUPS.entries
            .mapNotNull { (group, label) -> totals[group]?.let { label to it } }
            .sortedByDescending { it.second }
    }

    fun groupLabel(group: String): String = GROUPS[group] ?: group

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /** The counters as plain text, for the exported log. */
    fun report(context: Context): String = buildString {
        appendLine("--- usage counters ---")
        val all = counters(context)
        if (all.isEmpty()) {
            appendLine("(nothing recorded yet)")
        } else {
            all.forEach { (key, count) -> appendLine("$key = $count") }
        }
    }
}
