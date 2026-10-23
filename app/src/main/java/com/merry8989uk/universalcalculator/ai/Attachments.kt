package com.merry8989uk.universalcalculator.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads a picked file and turns it into text the assistant can use:
 * an image is read with the vision model, audio is transcribed, and a text
 * file is read directly.
 */
object Attachments {

    data class Read(
        val kind: String,        // "image", "audio", "text"
        val label: String,       // what to show the user
        val text: String,        // what to send to the assistant
        val error: String? = null,
    )

    private const val MAX_BYTES = 12 * 1024 * 1024
    private const val MAX_TEXT_CHARS = 6000

    suspend fun read(context: Context, uri: Uri): Read = withContext(Dispatchers.IO) {
        val name = displayName(context, uri) ?: "file"
        val type = context.contentResolver.getType(uri).orEmpty()

        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } ?: return@withContext Read("file", name, "", "Could not open that file.")

        if (bytes.size > MAX_BYTES) {
            return@withContext Read("file", name, "", "That file is too large (over 12 MB).")
        }

        when {
            type.startsWith("image/") -> {
                val r = AiClient.solveImage(bytes)
                if (r.expression != null) {
                    Read("image", name, "The image shows this calculation: ${r.expression}", null)
                } else {
                    Read("image", name, "", r.error ?: "Could not read that image.")
                }
            }

            type.startsWith("audio/") -> {
                val text = AiClient.transcribe(bytes)
                if (text != null) {
                    Read("audio", name, "The audio says: \"$text\"", null)
                } else {
                    Read("audio", name, "", "Could not transcribe that audio.")
                }
            }

            type.startsWith("text/") || type == "application/json" || name.endsWith(".txt") ||
                name.endsWith(".md") || name.endsWith(".csv") -> {
                val text = String(bytes, Charsets.UTF_8).take(MAX_TEXT_CHARS)
                Read("text", name, "The attached file ($name) contains:\n$text", null)
            }

            else -> Read("file", name, "", "Unsupported file type: ${type.ifBlank { "unknown" }}")
        }
    }

    private fun displayName(context: Context, uri: Uri): String? = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
    } catch (e: Exception) {
        null
    }
}
