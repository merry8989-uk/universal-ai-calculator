package com.merry8989uk.universalcalculator.data

/**
 * Folding text down to a single line, and back.
 *
 * Several of the app's stored formats are one field per line, so newlines and
 * backslashes inside a value have to be escaped. Shared so the writer and the
 * reader can never drift apart.
 */
internal object LineText {

    fun escape(s: String): String = buildString {
        s.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> Unit
                else -> append(ch)
            }
        }
    }

    fun unescape(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> { append('\n'); i += 2 }
                    '\\' -> { append('\\'); i += 2 }
                    else -> { append(ch); i++ }
                }
            } else {
                append(ch)
                i++
            }
        }
    }
}
