package com.merry8989uk.universalcalculator.ui

import android.content.Context
import android.net.Uri
import com.merry8989uk.universalcalculator.data.FormulaReference
import com.merry8989uk.universalcalculator.formula.CalculationRequest
import com.merry8989uk.universalcalculator.formula.FormulaCalculator
import com.merry8989uk.universalcalculator.formula.FormulaDefinition
import com.merry8989uk.universalcalculator.calc.FormulaLibrary
import com.merry8989uk.universalcalculator.calc.ToolIndex
import com.merry8989uk.universalcalculator.formula.FormulaType

/**
 * What the formula page inside the app talks to.
 *
 * The page is drawn in HTML, but the numbers come from here — the same engine
 * the rest of the app uses, including the working it produces. That matters for
 * the thing a formula screen is actually for: not just the answer, but how it
 * got there. A second engine in JavaScript would have to be kept in step with
 * this one, and the first time it drifted nobody would notice until a number
 * was wrong.
 */
class FormulaBridge(private val context: Context) {

    /** Built once; the library does not change while the app is running. */
    private val definitions: List<FormulaDefinition> by lazy {
        FormulaReference.definitions(context)
    }

    private val byId: Map<String, FormulaDefinition> by lazy {
        definitions.associateBy { it.id }
    }

    /**
     * The tools, by folded title.
     *
     * A reference entry and a tool can describe the same formula, and until now
     * the page did not know that: somebody searching "determinant" was told it
     * was something to read while a determinant calculator sat one tab away.
     */
    private val tools: Map<String, com.merry8989uk.universalcalculator.calc.Formula> by lazy {
        ToolIndex.byTitle(FormulaLibrary.formulas)
    }

    /**
     * The whole library, as the page wants it.
     *
     * Everything the page needs to draw a card and nothing else: what the book
     * prints, what can be worked out, and the boxes to put numbers in.
     */
    fun libraryJson(): String {
        val out = StringBuilder(1 shl 20)
        out.append("{\"formulas\":[")
        var first = true
        for (d in definitions) {
            if (!first) out.append(',')
            first = false
            val workable = (d.type == FormulaType.DIRECT_CALCULATOR ||
                d.type == FormulaType.CHECKER) && d.expression != null
            out.append('{')
            out.append("\"id\":").append(q(d.id)).append(',')
            out.append("\"title\":").append(q(d.title)).append(',')
            out.append("\"category\":").append(q(d.category)).append(',')
            out.append("\"printed\":").append(q(d.printed)).append(',')
            out.append("\"type\":").append(q(d.type.name)).append(',')
            out.append("\"workable\":").append(workable).append(',')
            out.append("\"out\":").append(q(d.output.label)).append(',')
            out.append("\"unit\":").append(d.output.unit?.let { q(it) } ?: "null").append(',')
            // the calculator that does this formula, when there is one
            val tool = ToolIndex.forTitle(d.title, tools)
            out.append("\"tool\":").append(tool?.let { q(it.id) } ?: "null").append(',')
            out.append("\"inputs\":[")
            d.inputs.forEachIndexed { i, spec ->
                if (i > 0) out.append(',')
                out.append('{')
                out.append("\"id\":").append(q(spec.id)).append(',')
                out.append("\"label\":").append(q(spec.label)).append(',')
                out.append("\"sample\":").append(num(spec.sample))
                out.append('}')
            }
            out.append("]}")
        }
        out.append("]}")
        return out.toString()
    }

    /**
     * Works one formula out, with the working.
     *
     * [inputs] arrives as "a=6&b=9" rather than JSON, because parsing two numbers
     * out of a string should not need a parser.
     */
    fun calculateJson(id: String, inputs: String): String {
        val definition = byId[id]
            ?: return "{\"ok\":false,\"error\":\"That formula is no longer in the library.\"}"

        val values = HashMap<String, Double>()
        for (pair in inputs.split('&')) {
            if (pair.isBlank()) continue
            val at = pair.indexOf('=')
            if (at <= 0) continue
            val key = Uri.decode(pair.substring(0, at))
            val text = Uri.decode(pair.substring(at + 1)).trim()
            val v = text.toDoubleOrNull()
            if (v != null) values[key] = v
        }

        val result = FormulaCalculator.calculate(definition, CalculationRequest(definition.id, values))

        val out = StringBuilder(512)
        out.append('{')
        out.append("\"ok\":").append(result.success).append(',')
        out.append("\"exact\":").append(result.exactValue?.let { q(it) } ?: "null").append(',')
        out.append("\"value\":").append(result.approximateValue?.let { num(it) } ?: "null").append(',')
        out.append("\"unit\":").append(result.outputUnit?.let { q(it) } ?: "null").append(',')
        out.append("\"error\":").append(result.error?.let { q(it) } ?: "null").append(',')
        out.append("\"steps\":[")
        result.steps.forEachIndexed { i, step ->
            if (i > 0) out.append(',')
            out.append('{')
            out.append("\"d\":").append(q(step.description)).append(',')
            out.append("\"e\":").append(q(step.expression)).append(',')
            out.append("\"r\":").append(q(step.result))
            out.append('}')
        }
        out.append("],\"warnings\":[")
        result.warnings.forEachIndexed { i, w ->
            if (i > 0) out.append(',')
            out.append(q(w))
        }
        out.append("]}")
        return out.toString()
    }

    private fun q(s: String): String {
        val b = StringBuilder(s.length + 2)
        b.append('"')
        for (c in s) {
            when (c) {
                '"' -> b.append("\\\"")
                '\\' -> b.append("\\\\")
                '\n' -> b.append("\\n")
                '\r' -> b.append("\\r")
                '\t' -> b.append("\\t")
                else -> if (c < ' ') b.append(String.format("\\u%04x", c.code)) else b.append(c)
            }
        }
        b.append('"')
        return b.toString()
    }

    private fun num(v: Double): String =
        if (v.isNaN() || v.isInfinite()) "null" else v.toString()
}
