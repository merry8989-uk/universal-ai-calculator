package com.merry8989uk.universalcalculator.calc

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One thing said in a conversation: who said it, and what. */
data class ChatTurn(val role: String, val text: String) {
    val isUser: Boolean get() = role == "u"
}

/**
 * A conversation that was kept.
 *
 * This is deliberately a separate thing from [ArchiveEntry]. The archive is a
 * record of calculations; this is a record of conversations. They share a folder
 * on the phone and nothing else — a chat is never written into a day file, and
 * clearing one never touches the other.
 */
data class SavedChat(
    val title: String,
    val started: String,
    val model: String,
    val turns: List<ChatTurn>,
) {
    /** The first thing the user asked, for a one-line list. */
    val opening: String get() = turns.firstOrNull { it.isUser }?.text.orEmpty()

    val asked: Int get() = turns.count { it.isUser }
}

/**
 * The on-disk shape of a saved chat.
 *
 * One file per conversation, so a conversation can be opened, deleted or copied
 * on its own without rewriting a shared file. The format is a header line and
 * then one line per turn, with the three characters that would break a
 * line-and-pipe format escaped.
 */
object ChatFormat {

    const val HEADER = "NEXUSCHAT1"
    const val FIELD = '|'

    private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.UK)

    /**
     * Makes a piece of text safe to sit in a pipe-separated line.
     *
     * The backslash goes first, or unescaping would turn a real `\n` into a line
     * break. This is the bug that turns a transcript into a single mangled line.
     */
    fun escape(text: String): String = text
        .replace("\\", "\\\\")
        .replace("|", "\\|")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")

    /** The inverse of [escape]. */
    fun unescape(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                when (text[i + 1]) {
                    'n' -> out.append('\n')
                    '|' -> out.append('|')
                    '\\' -> out.append('\\')
                    else -> {
                        out.append(c)
                        out.append(text[i + 1])
                    }
                }
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    /** Splits a line on unescaped pipes. */
    fun split(line: String): List<String> {
        val out = ArrayList<String>()
        val field = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\\' && i + 1 < line.length) {
                field.append(c).append(line[i + 1])
                i += 2
            } else if (c == FIELD) {
                out.add(field.toString())
                field.setLength(0)
                i++
            } else {
                field.append(c)
                i++
            }
        }
        out.add(field.toString())
        return out
    }

    /** A file name that sorts by date and still says what the chat was about. */
    fun fileName(at: LocalDateTime, title: String): String =
        at.format(STAMP) + " - " + slug(title) + ".txt"

    /**
     * A short, file-safe version of a title.
     *
     * Anything that is not a letter, a digit or a space becomes a dash, because
     * a question mark in a file name is fine on Android and not fine once the
     * file is exported or read on a desktop.
     */
    fun slug(title: String): String {
        val cleaned = title
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty()) return "Chat"
        return cleaned.take(48).trim().ifEmpty { "Chat" }
    }

    /** The title of a chat, taken from the first thing the user asked. */
    fun titleOf(turns: List<ChatTurn>): String {
        val first = turns.firstOrNull { it.isUser }?.text?.trim().orEmpty()
        if (first.isEmpty()) return "Chat"
        val oneLine = first.replace(Regex("\\s+"), " ").trim()
        return if (oneLine.length <= 60) oneLine else oneLine.take(57).trimEnd() + "..."
    }

    /** The whole file. */
    fun write(chat: SavedChat): String = buildString {
        append(HEADER).append('\n')
        append(escape(chat.title)).append(FIELD)
        append(escape(chat.started)).append(FIELD)
        append(escape(chat.model)).append('\n')
        for (turn in chat.turns) {
            if (turn.text.isBlank()) continue
            append(if (turn.isUser) "u" else "a").append(FIELD)
            append(escape(turn.text)).append('\n')
        }
    }

    /**
     * Reads a file back.
     *
     * Returns null for anything that is not a chat file, so a stray file in the
     * folder — or a half-written one from a crash — is skipped rather than
     * shown as an empty conversation.
     */
    fun parse(text: String): SavedChat? {
        val lines = text.split('\n')
        if (lines.isEmpty() || lines[0].trim() != HEADER) return null
        if (lines.size < 2) return null

        val head = split(lines[1])
        if (head.size < 3) return null
        val title = unescape(head[0])
        val started = unescape(head[1])
        val model = unescape(head[2])

        val turns = ArrayList<ChatTurn>()
        for (i in 2 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val parts = split(line)
            if (parts.size < 2) continue
            val role = if (parts[0] == "u") "u" else "a"
            val said = unescape(parts[1])
            if (said.isNotBlank()) turns.add(ChatTurn(role, said))
        }
        if (turns.isEmpty()) return null
        return SavedChat(title, started, model, turns)
    }
}
