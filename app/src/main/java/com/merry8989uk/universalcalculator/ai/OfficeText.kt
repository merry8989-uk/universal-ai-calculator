package com.merry8989uk.universalcalculator.ai

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * The words inside a Word, Excel or PowerPoint file.
 *
 * All three are a zip with XML inside, so reading them needs no library and no
 * network — which matters because the alternative is a dependency that reads
 * formats this app will never otherwise touch. Until now a .docx was refused
 * with "Unsupported file type", which is a poor answer to somebody who has just
 * attached their homework.
 *
 * Only the text is taken. Formatting, images, charts and the exact layout are
 * not something the assistant needs to answer a question, and pretending to
 * preserve them would produce a document that looks wrong.
 */
object OfficeText {

    /** A zip bomb is a zip that expands to fill the disk; this is the guard. */
    private const val MAX_ENTRY_BYTES = 8 * 1024 * 1024
    private const val MAX_ENTRIES = 400

    /** True when this file name is one of the three we can read. */
    fun handles(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".docx") || lower.endsWith(".xlsx") ||
            lower.endsWith(".pptx") || lower.endsWith(".xlsm") || lower.endsWith(".pptm")
    }

    /**
     * The text of the document, or null when it cannot be read.
     *
     * Null rather than an empty string, so the caller can tell "this file has no
     * words in it" from "this file is not what it claims to be".
     */
    fun extract(bytes: ByteArray, name: String): String? {
        val entries = unzip(bytes) ?: return null
        val lower = name.lowercase()
        val text = when {
            lower.endsWith(".docx") -> docx(entries)
            lower.endsWith(".xlsx") || lower.endsWith(".xlsm") -> xlsx(entries)
            else -> pptx(entries)
        }
        return text?.trim()?.takeIf { it.isNotEmpty() }
    }

    // ---- the three formats ----

    private fun docx(entries: Map<String, String>): String? {
        val body = entries["word/document.xml"] ?: return null
        val out = StringBuilder()
        // one paragraph per line, which is what a reader expects of a document
        for (para in blocks(body, "w:p")) {
            val line = texts(para, "w:t").joinToString("") { it }
            if (line.isNotBlank()) out.append(line.trim()).append('\n')
        }
        // tables keep their cells apart, so a markdown table is not one long word
        if (out.isEmpty()) {
            val cells = texts(body, "w:t")
            if (cells.isEmpty()) return null
            out.append(cells.joinToString(" "))
        }
        return out.toString()
    }

    private fun pptx(entries: Map<String, String>): String? {
        // slide1, slide2 … slide10 — sorted by number, not as text
        val slides = entries.keys
            .filter { it.startsWith("ppt/slides/slide") && it.endsWith(".xml") }
            .sortedBy { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        if (slides.isEmpty()) return null
        val out = StringBuilder()
        slides.forEachIndexed { index, path ->
            val text = texts(entries.getValue(path), "a:t").joinToString(" ")
            if (text.isNotBlank()) {
                out.append("Slide ").append(index + 1).append(": ").append(text.trim()).append('\n')
            }
        }
        return out.toString()
    }

    private fun xlsx(entries: Map<String, String>): String? {
        // the strings live in one table and the cells point into it
        val shared = entries["xl/sharedStrings.xml"]
            ?.let { sst -> blocks(sst, "si").map { si -> texts(si, "t").joinToString("") } }
            ?: emptyList()

        val sheets = entries.keys
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .sortedBy { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        if (sheets.isEmpty()) return null

        val out = StringBuilder()
        sheets.forEachIndexed { index, path ->
            val xml = entries.getValue(path)
            out.append("Sheet ").append(index + 1).append(":\n")
            for (row in blocks(xml, "row")) {
                val cells = ArrayList<String>()
                for (cell in blocks(row, "c")) {
                    val ref = attribute(cell, "r")
                    val kind = attribute(cell, "t")
                    val raw = texts(cell, "v").firstOrNull()
                        ?: texts(cell, "t").firstOrNull()
                        ?: continue
                    val value = if (kind == "s") {
                        raw.toIntOrNull()?.let { shared.getOrNull(it) } ?: raw
                    } else {
                        raw
                    }
                    if (value.isNotBlank()) {
                        cells.add(if (ref.isEmpty()) value else "$ref: $value")
                    }
                }
                if (cells.isNotEmpty()) out.append(cells.joinToString(" | ")).append('\n')
            }
        }
        return out.toString()
    }

    // ---- the zip ----

    /** Every XML entry in the archive, by path. Null when it is not a zip. */
    private fun unzip(bytes: ByteArray): Map<String, String>? {
        val out = LinkedHashMap<String, String>()
        var count = 0
        return try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (++count > MAX_ENTRIES) break
                    val name = entry.name
                    if (!entry.isDirectory && name.endsWith(".xml") && out.size < 40) {
                        out[name] = readLimited(zip)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            out
        } catch (e: Exception) {
            null
        }
    }

    private fun readLimited(zip: ZipInputStream): String {
        val buffer = ByteArray(8192)
        val out = StringBuilder()
        var total = 0
        while (total < MAX_ENTRY_BYTES) {
            val read = zip.read(buffer)
            if (read <= 0) break
            total += read
            out.append(String(buffer, 0, read, Charsets.UTF_8))
        }
        return out.toString()
    }

    // ---- the XML, without a parser ----

    /**
     * Every complete `<tag …>…</tag>` in [xml], in order.
     *
     * Blocks are needed as well as leaf text because a shared-string entry and a
     * spreadsheet row both wrap other elements: asking for their inner text gets
     * nothing, and the cell that pointed at them then reads as empty.
     */
    private fun blocks(xml: String, tag: String): List<String> {
        val out = ArrayList<String>()
        var from = 0
        while (true) {
            val start = openingTag(xml, tag, from) ?: break
            val gt = xml.indexOf('>', start)
            if (gt < 0) break
            val close = "</$tag>"
            val end = xml.indexOf(close, gt)
            if (end < 0) break
            out.add(xml.substring(start, end + close.length))
            from = end + close.length
        }
        return out
    }

    /** The text inside every leaf `<tag …>text</tag>` in [xml]. */
    private fun texts(xml: String, tag: String): List<String> {
        val out = ArrayList<String>()
        var from = 0
        while (true) {
            val start = openingTag(xml, tag, from) ?: break
            val gt = xml.indexOf('>', start)
            if (gt < 0) break
            val close = "</$tag>"
            val end = xml.indexOf(close, gt)
            if (end < 0) break
            out.add(unescape(xml.substring(gt + 1, end)))
            from = end + close.length
        }
        return out
    }

    /**
     * Where `<tag` starts at or after [from], as an exact name.
     *
     * The name has to end there: <w:tbl> begins with the same three characters as
     * <w:t, and reading a table as a text run loses the words inside it.
     */
    private fun openingTag(xml: String, tag: String, from: Int): Int? {
        val open = "<$tag"
        var at = from
        while (true) {
            val start = xml.indexOf(open, at)
            if (start < 0) return null
            val after = start + open.length
            val next = xml.getOrNull(after)
            if (next == null || next == '>' || next == ' ' || next == '/') return start
            at = after
        }
    }

    private fun attribute(element: String, name: String): String {
        val at = element.indexOf(" $name=\"")
        if (at < 0) return ""
        val start = at + name.length + 3
        val end = element.indexOf('"', start)
        return if (end < 0) "" else element.substring(start, end)
    }

    private fun unescape(s: String): String {
        if (!s.contains('&')) return s
        return s
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }
}
