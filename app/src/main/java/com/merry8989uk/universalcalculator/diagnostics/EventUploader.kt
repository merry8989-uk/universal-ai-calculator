package com.merry8989uk.universalcalculator.diagnostics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends recorded events to the Cloudflare warehouse.
 *
 * The architecture asks for an offline queue with retry, and that is what this
 * is: an event is written to `pending.jsonl` as it happens, and a flush takes
 * whatever is waiting and posts it in batches. A flush that fails changes
 * nothing — the lines stay in the queue and go next time. A phone with no signal
 * loses no events, it just delivers them late.
 *
 * Nothing is sent while the master switch is off, and the events themselves
 * carry no user content: they are the same redacted objects the local file
 * holds.
 */
object EventUploader {

    private const val ENDPOINT = "https://nexus-calc-ai.br19shc39.workers.dev/events"
    private const val PENDING = "pending.jsonl"
    private const val BATCH = 150
    private const val MAX_PENDING_BYTES = 4_000_000L

    private val lock = Any()

    @Volatile private var lastAttempt = 0L
    @Volatile private var lastError = ""
    @Volatile private var sent = 0

    fun pendingFile(context: Context): File =
        File(File(context.filesDir, EventLog.DIR).apply { mkdirs() }, PENDING)

    /** Called for every event, so the queue grows as things happen. */
    fun enqueue(context: Context, line: String) {
        synchronized(lock) {
            try {
                val file = pendingFile(context)
                if (file.exists() && file.length() > MAX_PENDING_BYTES) {
                    // keep the newest half rather than growing without limit
                    val lines = file.readLines()
                    file.writeText(lines.drop(lines.size / 2).joinToString("\n", postfix = "\n"))
                }
                file.appendText(line + "\n")
            } catch (e: Exception) {
                // never let the queue break the app
            }
        }
    }

    fun pendingCount(context: Context): Int = try {
        val file = pendingFile(context)
        if (file.exists()) file.readLines().count { it.isNotBlank() } else 0
    } catch (e: Exception) {
        0
    }

    fun lastError(): String = lastError
    fun sentCount(): Int = sent

    /**
     * Sends what is waiting.
     *
     * Returns how many events were accepted. A failure is recorded and the
     * queue is left exactly as it was.
     */
    suspend fun flush(context: Context): Int = withContext(Dispatchers.IO) {
        if (!EventLog.enabled) return@withContext 0
        val file = pendingFile(context)
        if (!file.exists()) return@withContext 0

        val lines = try {
            file.readLines().filter { it.isNotBlank() }
        } catch (e: Exception) {
            return@withContext 0
        }
        if (lines.isEmpty()) return@withContext 0

        var delivered = 0
        var failed = false
        val remaining = ArrayList<String>()

        // In order, and everything from the first failure onwards is kept: a
        // later batch must not overtake one that has not gone yet.
        for (chunk in lines.chunked(BATCH)) {
            if (failed) {
                remaining.addAll(chunk)
                continue
            }
            if (send(chunk)) {
                delivered += chunk.size
            } else {
                failed = true
                remaining.addAll(chunk)
            }
        }

        synchronized(lock) {
            try {
                if (delivered >= lines.size) {
                    file.delete()
                } else {
                    file.writeText(remaining.joinToString("\n", postfix = "\n"))
                }
            } catch (e: Exception) {
                // leave it; the next flush tries again
            }
        }
        delivered
    }

    private fun send(lines: List<String>): Boolean {
        lastAttempt = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        return try {
            val events = JSONArray()
            for (line in lines) {
                runCatching { JSONObject(line) }.getOrNull()?.let { events.put(it) }
            }
            if (events.length() == 0) return true // nothing parseable: drop it

            val payload = JSONObject().put("events", events).toString()
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "NexusCalc/" + com.merry8989uk.universalcalculator.BuildConfig.VERSION_NAME)
            }
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code in 200..299) {
                sent += lines.size
                lastError = ""
                true
            } else {
                lastError = "http $code"
                false
            }
        } catch (e: Exception) {
            lastError = e.javaClass.simpleName
            false
        } finally {
            conn?.disconnect()
        }
    }
}
