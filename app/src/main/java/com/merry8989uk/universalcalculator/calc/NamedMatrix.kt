package com.merry8989uk.universalcalculator.calc

/**
 * A matrix the user has given a name to, so it can be used again.
 *
 * The reference keeps four of these plus whatever the last answer was. A named
 * matrix is the difference between working something out and keeping it: without
 * a name, every calculation starts from an empty grid.
 */
data class NamedMatrix(val name: String, val rows: List<List<Double>>) {

    val height: Int get() = rows.size
    val width: Int get() = rows.firstOrNull()?.size ?: 0

    /** "3×3", for the list. */
    val shape: String get() = "${height}\u00D7$width"

    val isSquare: Boolean get() = height > 0 && height == width
    val isEmpty: Boolean get() = height == 0 || width == 0

    fun toArray(): Array<DoubleArray> = rows.map { it.toDoubleArray() }.toTypedArray()

    /** The rows written out, one per line, the way the editor shows them. */
    fun text(): String = rows.joinToString("\n") { row ->
        row.joinToString(" ") { Format.number(it) }
    }

    companion object {
        /** The names the reference offers, so the app feels the same. */
        val DEFAULT_NAMES = listOf("MatA", "MatB", "MatC", "MatD", "MatAns")

        fun of(name: String, a: Array<DoubleArray>): NamedMatrix =
            NamedMatrix(name, a.map { it.toList() })

        /**
         * The matrix an answer leaves behind.
         *
         * It replaces whatever `MatAns` held, which is what makes it useful: the
         * result of one operation becomes an input to the next.
         */
        fun answer(a: Array<DoubleArray>): NamedMatrix = of("MatAns", a)
    }
}

/**
 * Reading and writing the named matrices as text.
 *
 * Plain lines, so the file can be read by eye if it ever has to be. A line
 * that does not fit the shape it declares is dropped rather than trusted.
 */
object NamedMatrixText {

    const val HEADER = "NEXUSMATRIX1"

    fun serialize(matrices: List<NamedMatrix>): String = buildString {
        appendLine(HEADER)
        for (m in matrices) {
            appendLine("${m.name}|${m.height}|${m.width}")
            for (row in m.rows) {
                appendLine(row.joinToString(" ") { Format.number(it) })
            }
        }
    }

    fun parse(text: String): List<NamedMatrix> {
        val lines = text.lines()
        if (lines.isEmpty() || lines.first().trim() != HEADER) return emptyList()
        val out = ArrayList<NamedMatrix>()
        var i = 1
        while (i < lines.size) {
            val head = lines[i].trim()
            i++
            if (head.isEmpty()) continue
            val parts = head.split("|")
            if (parts.size != 3) continue
            val name = parts[0].trim()
            val h = parts[1].trim().toIntOrNull() ?: continue
            val w = parts[2].trim().toIntOrNull() ?: continue
            if (name.isEmpty() || h <= 0 || w <= 0 || h > 50 || w > 50) continue
            val rows = ArrayList<List<Double>>(h)
            var ok = true
            for (r in 0 until h) {
                if (i >= lines.size) { ok = false; break }
                val cells = lines[i].trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                i++
                if (cells.size != w) { ok = false; break }
                val values = cells.map { it.toDoubleOrNull() }
                if (values.any { it == null }) { ok = false; break }
                rows.add(values.map { it!! })
            }
            if (ok) out.add(NamedMatrix(name, rows))
        }
        return out
    }
}

/** The named matrices a fresh install starts with, so the screen is not empty. */
fun defaultNamedMatrices(): List<NamedMatrix> = listOf(
    NamedMatrix("MatA", listOf(listOf(1.0, 0.0, 0.0), listOf(0.0, 1.0, 0.0), listOf(0.0, 0.0, 1.0))),
    NamedMatrix("MatB", listOf(listOf(1.0, 0.0, 0.0), listOf(0.0, 1.0, 0.0), listOf(0.0, 0.0, 1.0))),
    NamedMatrix("MatC", listOf(listOf(0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0))),
    NamedMatrix("MatD", listOf(listOf(0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0))),
    NamedMatrix("MatAns", listOf(listOf(0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0), listOf(0.0, 0.0, 0.0))),
)
