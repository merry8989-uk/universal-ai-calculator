package com.merry8989uk.universalcalculator.sync

import android.content.Context
import com.merry8989uk.universalcalculator.diagnostics.Analytics
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the sync endpoints on the app's own Worker.
 *
 * Only an opaque slot id and an already-encrypted blob cross the wire, so the
 * server cannot read anything and cannot work out the sync code.
 */
object SyncClient {

    private const val ENDPOINT = "https://nexus-calc-ai.br19shc39.workers.dev"

    sealed interface Result {
        /** [version] is what the server holds now; it must be sent back on the next write. */
        data class Ok(val blob: String?, val version: Int = 0) : Result
        data class Failed(val message: String) : Result
    }

    /** Returns the blob, or null when nothing has been stored for this code yet. */
    suspend fun download(slot: String): Result = call(JSONObject().put("sync", "get").put("slot", slot))

    /**
     * Writes the blob, naming the version it replaces.
     *
     * [version] of 0 means "I believe nothing is stored here". The server refuses
     * the write when that is wrong, so a second device cannot be overwritten in
     * silence — it has to read first.
     */
    suspend fun upload(slot: String, blob: String, version: Int): Result =
        call(
            JSONObject().put("sync", "put").put("slot", slot).put("blob", blob)
                .put("version", version),
        )

    /** Deleting is confirmed explicitly, because it is the one action with no undo. */
    suspend fun delete(slot: String): Result =
        call(JSONObject().put("sync", "delete").put("slot", slot).put("confirm", true))

    private suspend fun call(payload: JSONObject): Result =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 45_000
                    setRequestProperty("Content-Type", "application/json")
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()

                when {
                    code == 501 -> Result.Failed(
                        "Sync is not switched on for this app yet. The assistant still works.",
                    )
                    code !in 200..299 -> Result.Failed(
                        json?.optString("error")?.takeIf { it.isNotBlank() }
                            ?: "The server said no (HTTP $code).",
                    )
                    else -> Result.Ok(
                        json?.optString("blob")?.takeIf { it.isNotBlank() },
                        json?.optInt("version") ?: 0,
                    )
                }
            } catch (e: Exception) {
                DiagnosticsLogger.log("Sync", "Request failed: ${e.javaClass.simpleName}")
                Result.Failed("Could not reach the server. Check your connection.")
            } finally {
                connection?.disconnect()
            }
        }

    /** Counted locally, like every other network call. */
    fun count(context: Context, action: String) {
        Analytics.event(context, "sync:$action")
    }
}
