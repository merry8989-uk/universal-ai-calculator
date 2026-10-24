package com.merry8989uk.universalcalculator.sync

import com.merry8989uk.universalcalculator.calc.CustomCalculator
import com.merry8989uk.universalcalculator.data.CustomCalcStore
import com.merry8989uk.universalcalculator.data.LineText

/**
 * What travels between devices, before it is encrypted.
 *
 * Only things worth carrying are included: the user's own calculators, the
 * settings they chose, and — if they ask for it — the calculation history. The
 * payload is versioned so a future format can be recognised rather than
 * misread.
 */
data class SyncPayload(
    val createdAt: Long,
    val settings: Map<String, String> = emptyMap(),
    val calculators: List<CustomCalculator> = emptyList(),
    val history: String = "",
) {
    /** A one-line summary for the UI. */
    fun summary(): String = buildString {
        append(calculators.size).append(if (calculators.size == 1) " calculator" else " calculators")
        append(", ").append(settings.size).append(" settings")
        if (history.isNotBlank()) append(", history included")
    }
}

/**
 * The payload as text. Line based, reusing the escaping the calculator store
 * already relies on, so the whole thing is a pair of pure functions a test can
 * exercise.
 */
object SyncPayloadCodec {

    private const val HEADER = "NEXUSSYNC1"

    fun encode(payload: SyncPayload): String = buildString {
        appendLine(HEADER)
        appendLine(payload.createdAt.toString())
        appendLine(payload.settings.size.toString())
        payload.settings.forEach { (key, value) ->
            appendLine(LineText.escape(key))
            appendLine(LineText.escape(value))
        }
        appendLine(LineText.escape(CustomCalcStore.serialize(payload.calculators)))
        appendLine(LineText.escape(payload.history))
    }

    /** Null when the text is not a payload this version understands. */
    fun decode(text: String): SyncPayload? {
        val lines = text.split("\n")
        if (lines.isEmpty() || lines[0].trim() != HEADER) return null

        var i = 1
        val createdAt = lines.getOrNull(i)?.trim()?.toLongOrNull() ?: return null
        i++
        val count = lines.getOrNull(i)?.trim()?.toIntOrNull() ?: return null
        i++
        if (count < 0 || i + count * 2 > lines.size) return null

        val settings = LinkedHashMap<String, String>()
        repeat(count) {
            val key = LineText.unescape(lines[i])
            val value = LineText.unescape(lines[i + 1])
            settings[key] = value
            i += 2
        }

        val calculatorsBlock = lines.getOrNull(i) ?: return null
        i++
        val history = lines.getOrNull(i)?.let { LineText.unescape(it) } ?: ""

        return SyncPayload(
            createdAt = createdAt,
            settings = settings,
            calculators = CustomCalcStore.parse(LineText.unescape(calculatorsBlock)),
            history = history,
        )
    }
}
