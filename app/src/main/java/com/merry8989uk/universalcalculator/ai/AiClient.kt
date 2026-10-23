package com.merry8989uk.universalcalculator.ai

import android.content.Context
import android.util.Base64
import com.merry8989uk.universalcalculator.diagnostics.Analytics
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the app's own AI backend — a Cloudflare Worker that holds the Gemini
 * key server-side. The app never holds the key and the user never enters one.
 */
object AiClient {

    /** Fixed assistant endpoint (Cloudflare Worker). Not user-editable. */
    private const val ENDPOINT = "https://nexus-calc-ai.br19shc39.workers.dev"

    private var appContext: Context? = null

    /** Call once, from the Application, so calls can be counted locally. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun count(kind: String) {
        appContext?.let { Analytics.event(it, "ai:$kind") }
    }

    /** Either the expression read from a photo, or why it could not be read. */
    data class PhotoResult(
        val expression: String? = null,
        val error: String? = null,
        /** True when the reads disagreed, so the text may be wrong. */
        val lowConfidence: Boolean = false,
    )

    /**
     * Sends a JPEG photo of some arithmetic and returns the expression read from
     * it. The reading happens on the Worker with a vision model, so no OCR
     * library has to ship inside the app.
     */
    suspend fun solveImage(jpeg: ByteArray): PhotoResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val payload = JSONObject()
                .put("image", Base64.encodeToString(jpeg, Base64.NO_WRAP))
                .toString()
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 60_000
                setRequestProperty("Content-Type", "application/json")
            }
            count("photo")
            DiagnosticsLogger.log("AI", "Photo solve: ${jpeg.size} bytes")
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (code in 200..299) {
                val expr = json?.optString("expression")?.takeIf { it.isNotBlank() }
                if (expr != null) {
                    val low = json?.optString("confidence") == "low"
                    DiagnosticsLogger.log("AI", "Photo read as: $expr (low=$low)")
                    PhotoResult(expression = expr, lowConfidence = low)
                } else {
                    PhotoResult(error = "Could not read maths in that photo.")
                }
            } else {
                PhotoResult(
                    error = json?.optString("error")?.takeIf { it.isNotBlank() }
                        ?: "Could not read the photo ($code)",
                )
            }
        } catch (e: Exception) {
            DiagnosticsLogger.log("AI", "Photo solve failed")
            PhotoResult(error = "Could not reach the reader. Check your connection.")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Reads a page image. With mode "text" the whole page is transcribed; with
     * "expression" (the default) only the calculation on it is returned.
     */
    suspend fun readImage(jpeg: ByteArray, mode: String = "expression"): String? =
        withContext(Dispatchers.IO) {
            count(if (mode == "text") "pdf" else "photo")
            var conn: HttpURLConnection? = null
            try {
                val payload = JSONObject()
                    .put("image", Base64.encodeToString(jpeg, Base64.NO_WRAP))
                    .put("mode", mode)
                    .toString()
                conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 20_000
                    readTimeout = 90_000
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (code !in 200..299) return@withContext null
                if (mode == "text") json?.optString("text")?.takeIf { it.isNotBlank() }
                else json?.optString("expression")?.takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                DiagnosticsLogger.log("AI", "Page read failed")
                null
            } finally {
                conn?.disconnect()
            }
        }

    /** Transcribes an audio clip on the Worker (Whisper), or null on failure. */
    suspend fun transcribe(audio: ByteArray): String? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val payload = JSONObject()
                .put("audio", Base64.encodeToString(audio, Base64.NO_WRAP))
                .toString()
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 90_000
                setRequestProperty("Content-Type", "application/json")
            }
            count("audio")
            DiagnosticsLogger.log("AI", "Transcribe: ${audio.size} bytes")
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (code in 200..299) json?.optString("text")?.takeIf { it.isNotBlank() } else null
        } catch (e: Exception) {
            DiagnosticsLogger.log("AI", "Transcribe failed")
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** Sends a question (with optional app context) and returns the answer text. */
    suspend fun ask(question: String, context: String = ""): String = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val payload = JSONObject().put("question", question).apply {
                if (context.isNotBlank()) put("context", context)
            }.toString()

            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 45_000
                setRequestProperty("Content-Type", "application/json")
            }
            count("ask")
            DiagnosticsLogger.log("AI", "Ask: ${question.take(140)}")
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(text) }.getOrNull()

            if (code in 200..299) {
                val ans = json?.optString("answer")?.takeIf { it.isNotBlank() }
                DiagnosticsLogger.log("AI", "Answer received (${ans?.length ?: 0} chars)")
                ans ?: "The assistant returned no answer."
            } else {
                DiagnosticsLogger.log("AI", "Error $code")
                "Could not reach the assistant: " +
                    (json?.optString("error")?.takeIf { it.isNotBlank() } ?: "request failed ($code)")
            }
        } catch (e: Exception) {
            DiagnosticsLogger.log("AI", "Network error")
            "Could not reach the assistant. Check your internet connection and try again."
        } finally {
            conn?.disconnect()
        }
    }
}
