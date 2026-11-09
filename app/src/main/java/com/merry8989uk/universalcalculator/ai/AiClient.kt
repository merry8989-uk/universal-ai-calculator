package com.merry8989uk.universalcalculator.ai

import android.content.Context
import android.util.Base64
import com.merry8989uk.universalcalculator.diagnostics.Analytics
import com.merry8989uk.universalcalculator.diag.Verification
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import com.merry8989uk.universalcalculator.diagnostics.NetLog
import com.merry8989uk.universalcalculator.diagnostics.EventLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the app's own AI backend.
 *
 * The backend is a Cloudflare Worker running Workers AI, and the model provider
 * is reached from inside that Worker. The app never holds a provider key and the
 * user never enters one; there is nothing in the APK to steal and nothing to
 * configure. The endpoint is fixed in code for the same reason.
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
            val started = System.currentTimeMillis()
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
            NetLog.request("AI", "POST", ENDPOINT, code, System.currentTimeMillis() - started)
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (code in 200..299) {
                val expr = json?.optString("expression")?.takeIf { it.isNotBlank() }
                if (expr != null) {
                    val low = json?.optString("confidence") == "low"
                    // what was read is the user's own arithmetic, so only its
                    // shape is logged — enough to tell a bad read from a good one
                    DiagnosticsLogger.log(
                        "AI",
                        "Photo read: ${expr.length} chars" + (if (low) ", uncertain" else ""),
                    )
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
     * Answers a question about a picture.
     *
     * This is a different job from [readImage]: the picture may be a question
     * paper, a diagram or a table, and the user wants the answer, not an
     * arithmetic string. Sending only the expression that happens to be printed
     * on the page is what made a photograph of an exam paper come back as the
     * marks line in its header.
     */
    suspend fun answerImage(jpeg: ByteArray, question: String): String? =
        withContext(Dispatchers.IO) {
            count("photo")
            var conn: HttpURLConnection? = null
            try {
                val payload = JSONObject()
                    .put("image", Base64.encodeToString(jpeg, Base64.NO_WRAP))
                    .put("mode", "answer")
                    .put("question", question)
                    .toString()
                val started = System.currentTimeMillis()
                conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 20_000
                    readTimeout = 120_000
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                NetLog.request("AI", "POST", ENDPOINT, code, System.currentTimeMillis() - started)
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
                val json = runCatching { JSONObject(body) }.getOrNull()
                if (code !in 200..299) {
                    DiagnosticsLogger.log("AI", "Answer from photo failed: HTTP $code")
                    return@withContext null
                }
                json?.optString("answer")?.takeIf { it.isNotBlank() }
            } catch (e: Exception) {
                DiagnosticsLogger.log("AI", "Answer from photo failed")
                null
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
                val started = System.currentTimeMillis()
                conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 20_000
                    readTimeout = 90_000
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                NetLog.request("AI", "POST", ENDPOINT, code, System.currentTimeMillis() - started)
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
                .put("mime", "audio/wav")
                .toString()
            val started = System.currentTimeMillis()
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
            NetLog.request("AI", "POST", ENDPOINT, code, System.currentTimeMillis() - started)
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

    /**
     * One question, with the conversation so far.
     *
     * [history] is the earlier turns, oldest first, as role to text. Sending it
     * is what makes a follow-up work: without it every question starts from
     * nothing, and "and the second one?" has no idea what the first one was.
     *
     * The answer comes back as an [AiResult], not as a string. A string had to
     * carry both the reply and the failure, and the caller could not tell which
     * it had — so a failure was shown, and kept, as if the assistant had said
     * it.
     */
    suspend fun ask(
        question: String,
        context: String = "",
        history: List<Pair<String, String>> = emptyList(),
    ): AiResult = withContext(Dispatchers.IO) {
        // One id for this attempt, sent to the worker and written into both
        // events. Without it a failed answer is a timestamp in one log and a
        // line in another, and matching them is guesswork. It is made before the
        // try so the failure paths — the ones somebody actually has to look up —
        // can carry it as well.
        val requestId = java.util.UUID.randomUUID().toString().take(8)
        var conn: HttpURLConnection? = null
        try {
            val payload = JSONObject().put("question", question).apply {
                if (context.isNotBlank()) put("context", context)
                if (history.isNotEmpty()) {
                    val turns = JSONArray()
                    for ((role, text) in history.takeLast(16)) {
                        turns.put(JSONObject().put("role", role).put("content", text))
                    }
                    put("history", turns)
                }
            }.toString()

            val started = System.currentTimeMillis()
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 45_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Request-Id", requestId)
            }
            count("ask")
            EventLog.beginRequest()
            // The question itself is not logged. A calculation can be a salary, a
            // dose or a debt, and a diagnostics export is a file the user may
            // hand to somebody else. What is logged is enough to diagnose a
            // failure without carrying any of that: how big the request was, and
            // whether it had attachments.
            DiagnosticsLogger.log(
                "AI",
                "Ask: ${question.length} chars" +
                    (if (context.isNotBlank()) ", context ${context.length} chars" else ""),
            )
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            NetLog.request("AI", "POST", ENDPOINT, code, System.currentTimeMillis() - started)
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            val json = runCatching { JSONObject(text) }.getOrNull()

            val elapsed = System.currentTimeMillis() - started
            if (code in 200..299) {
                val ans = json?.optString("answer")?.takeIf { it.isNotBlank() }
                DiagnosticsLogger.log("AI", "Answer received (${ans?.length ?: 0} chars)")
                val model = json?.optString("model").orEmpty()
                val searched = json?.optJSONArray("searched")?.length() ?: 0
                val verdict = Verification.check(question, ans.orEmpty())
                EventLog.ai(
                    "request.completed",
                    provider = "sarvam",
                    model = model,
                    fields = mapOf(
                        "status" to "success",
                        "request_id" to requestId,
                        "duration_ms" to elapsed.toString(),
                        "prompt_chars" to question.length.toString(),
                        "answer_chars" to (ans?.length ?: 0).toString(),
                        "has_context" to context.isNotBlank().toString(),
                        "history_turns" to history.size.toString(),
                        "search_count" to searched.toString(),
                        "verification" to verdict.state,
                        "verification_detail" to verdict.detail,
                    ),
                )
                // A 200 with nothing in it is a failed ask, not an empty reply:
                // there is no answer to show and nothing to keep.
                if (ans == null) {
                    AiResult.Failed(NO_ANSWER, requestId, code)
                } else {
                    AiResult.Ok(ans, requestId)
                }
            } else {
                DiagnosticsLogger.log("AI", "Error $code (request $requestId)")
                EventLog.ai(
                    "request.failed",
                    provider = "sarvam",
                    model = json?.optString("model").orEmpty(),
                    fields = mapOf(
                        "status" to "http_$code",
                        "request_id" to requestId,
                        "duration_ms" to elapsed.toString(),
                        "http_code" to code.toString(),
                        "rate_limited" to (code == 429).toString(),
                        "server_error" to (code >= 500).toString(),
                    ),
                )
                AiResult.Failed(
                    "Could not reach the assistant: " +
                        (json?.optString("error")?.takeIf { it.isNotBlank() } ?: "request failed ($code)"),
                    requestId,
                    code,
                )
            }
        } catch (e: Exception) {
            // Anything that came off the wire is a connection problem, and is
            // reported as one; anything else (a body that will not parse) is a
            // failure of the request itself. The two lead to different advice,
            // so they are different outcomes.
            val offline = e is java.io.IOException
            DiagnosticsLogger.log(
                "AI",
                (if (offline) "Network error" else "Ask failed") +
                    " (request $requestId): " + e.javaClass.simpleName,
            )
            EventLog.ai(
                "request.failed",
                provider = "sarvam",
                model = "",
                fields = mapOf(
                    "status" to if (offline) "network_error" else "client_error",
                    "request_id" to requestId,
                    "error_type" to (e.javaClass.simpleName),
                    "timeout" to (e is java.net.SocketTimeoutException).toString(),
                ),
            )
            if (offline) {
                AiResult.Offline(
                    "Could not reach the assistant. Check your internet connection and try again.",
                    requestId,
                )
            } else {
                AiResult.Failed("Something went wrong asking the assistant. Try again.", requestId)
            }
        } finally {
            EventLog.endRequest()
            conn?.disconnect()
        }
    }
}
