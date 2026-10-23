package com.merry8989uk.universalcalculator.ai

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
