package com.merry8989uk.universalcalculator.diagnostics

import android.content.Context
import android.os.Build
import com.merry8989uk.universalcalculator.BuildConfig
import com.merry8989uk.universalcalculator.diag.DiagCategory
import com.merry8989uk.universalcalculator.diag.DiagContext
import com.merry8989uk.universalcalculator.diag.DiagEvent
import com.merry8989uk.universalcalculator.diag.DiagJson
import com.merry8989uk.universalcalculator.diag.DiagSeverity
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The structured event log.
 *
 * [DiagnosticsLogger] keeps a human-readable line for the live box on the
 * diagnostics screen. This keeps the *machine-readable* record the logging
 * architecture asks for: one JSON object per event, with a schema version, an
 * event name, a severity, correlation ids and a flat bag of fields, so events
 * can be counted, grouped and compared between releases instead of only read.
 *
 * Everything stays on the device. Nothing is uploaded; the user exports the file
 * and nothing leaves the phone until they do.
 */
object EventLog {

    const val DIR = "logs"
    const val NAME = "events.jsonl"

    private const val MAX_BYTES = 2_000_000L
    private const val KEEP_ROTATED = 3

    /** High-volume events are sampled so one busy screen cannot fill the file. */
    private const val SAMPLE_CAP_PER_EVENT = 400

    private val lock = Any()
    private var file: File? = null
    private var appContext: Context? = null

    /** Kept in memory so the diagnostics screen can show recent events at once. */
    private val recent = ArrayDeque<String>()
    private const val RECENT_MAX = 200

    private val counts = ConcurrentHashMap<String, Int>()

    @Volatile private var installId: String = ""
    @Volatile private var sessionId: String = UUID.randomUUID().toString()
    @Volatile private var traceId: String = ""
    @Volatile private var requestId: String = ""

    /** Mirrors the one switch that governs all storing. */
    @Volatile var enabled: Boolean = true

    fun init(context: Context) {
        synchronized(lock) {
            appContext = context.applicationContext
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            file = File(dir, NAME)
            if (installId.isEmpty()) installId = readOrMakeInstallId(context)
        }
    }

    /**
     * A random id for this install.
     *
     * Deliberately not the Android ID, not the advertising id and not anything
     * derived from the phone — the architecture is explicit that a direct
     * identifier must never be used as an event id. The user can clear it from
     * the diagnostics screen, which starts a new one.
     */
    private fun readOrMakeInstallId(context: Context): String {
        val prefs = context.getSharedPreferences("diag_ids", Context.MODE_PRIVATE)
        val existing = prefs.getString("install_id", null)
        if (existing != null && existing.length >= 16) return existing
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString("install_id", fresh).apply()
        return fresh
    }

    /** A new install id, so the record from before cannot be joined to the one after. */
    fun rotateInstallId(context: Context) {
        val prefs = context.getSharedPreferences("diag_ids", Context.MODE_PRIVATE)
        prefs.edit().putString("install_id", UUID.randomUUID().toString()).apply()
        installId = readOrMakeInstallId(context)
    }

    fun newSession() {
        sessionId = UUID.randomUUID().toString()
    }

    /** Called at the start of a user-initiated action; every event in it carries the id. */
    fun beginTrace(): String {
        traceId = UUID.randomUUID().toString().take(8)
        return traceId
    }

    fun endTrace() {
        traceId = ""
    }

    /** Called around one assistant request. */
    fun beginRequest(): String {
        requestId = UUID.randomUUID().toString().take(8)
        return requestId
    }

    fun endRequest() {
        requestId = ""
    }

    fun contextNow(): DiagContext = DiagContext(
        installId = installId.ifEmpty { "unknown" },
        sessionId = sessionId,
        requestId = requestId,
        traceId = traceId,
        appVersion = BuildConfig.VERSION_NAME,
        platform = "android",
        environment = if (BuildConfig.DEBUG) "debug" else "production",
    )

    /**
     * Records one event.
     *
     * Returns the JSON line, which the tests and the screen both use. A failure
     * to write is swallowed: an app that stops working because its notebook is
     * full is a worse app.
     */
    fun record(
        name: String,
        category: DiagCategory,
        severity: DiagSeverity = DiagSeverity.INFO,
        fields: Map<String, String> = emptyMap(),
    ): String {
        val event = DiagEvent(name, category, severity, System.currentTimeMillis(), fields)
        val line = DiagJson.line(event, contextNow())
        if (!enabled) return line

        val seen = counts.merge(name, 1, Int::plus) ?: 1
        if (seen > SAMPLE_CAP_PER_EVENT && severity != DiagSeverity.ERROR && severity != DiagSeverity.FATAL) {
            return line
        }

        synchronized(lock) {
            recent.addLast(line)
            while (recent.size > RECENT_MAX) recent.removeFirst()
            val f = file ?: return line
            try {
                if (f.exists() && f.length() > MAX_BYTES) rotate(f)
                f.appendText(line + "\n")
            } catch (e: Exception) {
                // never let logging break the app
            }
        }
        return line
    }

    private fun rotate(current: File) {
        // events.jsonl.1 is the newest of the kept copies
        for (i in KEEP_ROTATED - 1 downTo 1) {
            val from = File(current.parentFile, "$NAME.$i")
            val to = File(current.parentFile, "$NAME.${i + 1}")
            if (from.exists()) {
                if (to.exists()) to.delete()
                from.renameTo(to)
            }
        }
        val first = File(current.parentFile, "$NAME.1")
        if (first.exists()) first.delete()
        current.renameTo(first)
    }

    /** Recent events, newest last, for the live box on the diagnostics screen. */
    fun recent(): List<String> = synchronized(lock) { recent.toList() }

    /** How many of each event name have been recorded. */
    fun counts(): List<Pair<String, Int>> = counts.entries
        .map { it.key to it.value }
        .sortedByDescending { it.second }

    fun countOf(name: String): Int = counts[name] ?: 0

    fun totalCount(): Int = counts.values.sum()

    fun file(): File? = file

    fun sizeBytes(): Long = file?.let { if (it.exists()) it.length() else 0L } ?: 0L

    /** Everything recorded, rotated copies first, oldest first. */
    fun readAll(): String {
        val f = file ?: return ""
        val parts = ArrayList<String>()
        for (i in KEEP_ROTATED downTo 1) {
            val old = File(f.parentFile, "$NAME.$i")
            if (old.exists()) parts.add(old.readText())
        }
        if (f.exists()) parts.add(f.readText())
        return parts.joinToString("")
    }

    fun clear() {
        synchronized(lock) {
            recent.clear()
            counts.clear()
            val f = file ?: return
            runCatching { if (f.exists()) f.delete() }
            for (i in 1..KEEP_ROTATED) runCatching { File(f.parentFile, "$NAME.$i").delete() }
        }
    }

    // ---- the six categories, each with the events the architecture asks for ----

    /** A. Calculator engine. */
    fun calc(
        operation: String,
        status: String,
        durationMs: Long,
        errorCode: String? = null,
        fields: Map<String, String> = emptyMap(),
    ) {
        val all = LinkedHashMap<String, String>()
        all["operation"] = operation
        all["status"] = status
        all["duration_ms"] = durationMs.toString()
        if (errorCode != null) all["error_code"] = errorCode
        all.putAll(fields)
        record(
            "calculator.operation.${if (status == "success") "completed" else "failed"}",
            DiagCategory.CALC,
            if (status == "success") DiagSeverity.INFO else DiagSeverity.WARN,
            all,
        )
    }

    /** B. AI solver. */
    fun ai(
        event: String,
        provider: String,
        model: String,
        fields: Map<String, String> = emptyMap(),
    ) {
        val all = LinkedHashMap<String, String>()
        all["provider"] = provider
        all["model"] = model
        all.putAll(fields)
        record("ai.$event", DiagCategory.AI, severityFor(event), all)
    }

    /** C. Camera, OCR and parsing. */
    fun ocr(
        event: String,
        fields: Map<String, String> = emptyMap(),
    ) {
        record("ocr.$event", DiagCategory.OCR, severityFor(event), fields)
    }

    /** D. Performance and reliability. */
    fun perf(
        event: String,
        durationMs: Long? = null,
        fields: Map<String, String> = emptyMap(),
    ) {
        val all = LinkedHashMap<String, String>()
        if (durationMs != null) all["duration_ms"] = durationMs.toString()
        all.putAll(fields)
        record("perf.$event", DiagCategory.PERF, severityFor(event), all)
    }

    /** E. Product usage. */
    fun product(event: String, fields: Map<String, String> = emptyMap()) {
        record("product.$event", DiagCategory.APP, DiagSeverity.INFO, fields)
    }

    /** F. Security and consent. */
    fun security(event: String, fields: Map<String, String> = emptyMap()) {
        record("security.$event", DiagCategory.SECURITY, DiagSeverity.WARN, fields)
    }

    /** What the user said about an answer. */
    fun feedback(verdict: String, fields: Map<String, String> = emptyMap()) {
        val all = LinkedHashMap<String, String>()
        all["verdict"] = verdict
        all.putAll(fields)
        record("solution.feedback_submitted", DiagCategory.FEEDBACK, DiagSeverity.INFO, all)
    }

    private fun severityFor(event: String): DiagSeverity = when {
        event.contains("fail") || event.contains("error") || event.contains("reject") -> DiagSeverity.WARN
        event.contains("crash") || event.contains("fatal") -> DiagSeverity.ERROR
        else -> DiagSeverity.INFO
    }

    /** A report for the diagnostics screen and the export. */
    fun summary(): String = buildString {
        append("Structured events: ").append(totalCount()).append('\n')
        append("File: ").append(sizeBytes() / 1024).append(" KB\n")
        val byCategory = counts().groupBy { name ->
            DiagCategory.entries.firstOrNull { name.first.startsWith(it.key + ".") } ?: DiagCategory.APP
        }
        for ((category, list) in byCategory) {
            append("\n").append(category.key.uppercase()).append(" — ")
                .append(list.sumOf { it.second }).append('\n')
            for ((name, n) in list.take(12)) {
                append("  ").append(name).append("  x").append(n).append('\n')
            }
        }
    }
}
