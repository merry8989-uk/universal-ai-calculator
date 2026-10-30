package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.formula.RawFormula

/**
 * Just enough JSON to read the bundled formula file.
 *
 * org.json is an Android stub and is not available in a unit test, and pulling a
 * JSON library onto the test classpath to read three string fields would be more
 * moving parts than the job needs. This handles strings with escapes, numbers,
 * objects and arrays — enough for this file and honest about it.
 */
object MiniJson {

    fun readFormulas(text: String): List<RawFormula> {
        val out = ArrayList<RawFormula>()
        var i = text.indexOf("\"formulas\"")
        if (i < 0) return out
        i = text.indexOf('[', i)
        if (i < 0) return out
        i++
        while (i < text.length) {
            while (i < text.length && (text[i].isWhitespace() || text[i] == ',')) i++
            if (i >= text.length || text[i] == ']') break
            if (text[i] != '{') break
            val entry = readObject(text, i) ?: break
            i = entry.second
            val fields = entry.first
            val c = fields["c"] ?: continue
            val t = fields["t"] ?: continue
            val f = fields["f"] ?: continue
            out.add(RawFormula(c, t, f))
        }
        return out
    }

    /** Reads one object, returning its string fields and where it ended. */
    private fun readObject(text: String, start: Int): Pair<Map<String, String>, Int>? {
        val fields = LinkedHashMap<String, String>()
        var i = start + 1
        while (i < text.length) {
            while (i < text.length && (text[i].isWhitespace() || text[i] == ',')) i++
            if (i >= text.length) return null
            if (text[i] == '}') return fields to (i + 1)
            if (text[i] != '"') return null
            val key = readString(text, i) ?: return null
            i = key.second
            while (i < text.length && (text[i].isWhitespace() || text[i] == ':')) i++
            if (i >= text.length) return null
            if (text[i] == '"') {
                val value = readString(text, i) ?: return null
                fields[key.first] = value.first
                i = value.second
            } else {
                // not a string: skip it, we only want the three we know
                while (i < text.length && text[i] != ',' && text[i] != '}') i++
            }
        }
        return null
    }

    private fun readString(text: String, start: Int): Pair<String, Int>? {
        if (start >= text.length || text[start] != '"') return null
        val out = StringBuilder()
        var i = start + 1
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' -> return out.toString() to (i + 1)
                c == '\\' -> {
                    if (i + 1 >= text.length) return null
                    when (val e = text[i + 1]) {
                        '"' -> out.append('"')
                        '\\' -> out.append('\\')
                        '/' -> out.append('/')
                        'n' -> out.append('\n')
                        't' -> out.append('\t')
                        'r' -> out.append('\r')
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'u' -> {
                            if (i + 5 >= text.length) return null
                            val code = text.substring(i + 2, i + 6).toIntOrNull(16) ?: return null
                            out.append(code.toChar())
                            i += 4
                        }
                        else -> out.append(e)
                    }
                    i += 2
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
        return null
    }
}
