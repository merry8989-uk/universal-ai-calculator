package com.merry8989uk.universalcalculator.data

import android.content.Context
import com.merry8989uk.universalcalculator.calc.CustomCalculator
import com.merry8989uk.universalcalculator.calc.CustomInput
import java.io.File

/**
 * Saves the user's own calculators on the device.
 *
 * The format is a plain line-per-field text file rather than JSON, so the
 * reader and writer are ordinary pure functions that a test can exercise
 * without Android.
 */
object CustomCalcStore {

    private const val HEADER = "NEXUSCUSTOM1"
    private const val FILE = "custom_calculators.txt"

    private fun file(context: Context) = File(context.filesDir, FILE)

    fun load(context: Context): List<CustomCalculator> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return runCatching { parse(f.readText()) }.getOrDefault(emptyList())
    }

    fun save(context: Context, calculators: List<CustomCalculator>) {
        runCatching { file(context).writeText(serialize(calculators)) }
    }

    // ---- pure, testable ----

    fun serialize(calculators: List<CustomCalculator>): String = buildString {
        appendLine(HEADER)
        calculators.forEach { c ->
            appendLine(escape(c.id))
            appendLine(escape(c.name))
            appendLine(escape(c.unit))
            appendLine(escape(c.description))
            appendLine(escape(c.expression))
            appendLine(c.inputs.size.toString())
            c.inputs.forEach { i ->
                appendLine(escape(i.key))
                appendLine(escape(i.label))
                appendLine(i.default.toString())
            }
        }
    }

    /** Anything unreadable is skipped rather than thrown, so one bad entry cannot
     *  lose the rest. */
    fun parse(text: String): List<CustomCalculator> {
        val lines = text.split("\n")
        if (lines.isEmpty() || lines[0].trim() != HEADER) return emptyList()
        val out = ArrayList<CustomCalculator>()
        var i = 1
        while (i < lines.size) {
            if (lines[i].isBlank()) {
                i++
                continue
            }
            // every calculator needs at least the six header fields
            if (i + 5 >= lines.size) break
            val id = unescape(lines[i])
            val name = unescape(lines[i + 1])
            val unit = unescape(lines[i + 2])
            val description = unescape(lines[i + 3])
            val expression = unescape(lines[i + 4])
            val count = lines[i + 5].trim().toIntOrNull() ?: 0
            i += 6
            if (count < 0 || i + count * 3 > lines.size) break
            val inputs = ArrayList<CustomInput>(count)
            repeat(count) {
                val key = unescape(lines[i])
                val label = unescape(lines[i + 1])
                val default = lines[i + 2].trim().toDoubleOrNull() ?: 0.0
                inputs.add(CustomInput(key, label, default))
                i += 3
            }
            out.add(CustomCalculator(id, name, description, inputs, expression, unit))
        }
        return out
    }

    /** One field per line, so newlines and backslashes must be folded away. */
    private fun escape(s: String): String = LineText.escape(s)

    private fun unescape(s: String): String = LineText.unescape(s)
}
