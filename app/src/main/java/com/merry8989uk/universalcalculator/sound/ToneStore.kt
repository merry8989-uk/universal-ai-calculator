package com.merry8989uk.universalcalculator.sound

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Keeps the user's own key tone.
 *
 * The picked file is copied into the app's own folder rather than referenced
 * where it sits, because a document URI can be withdrawn the moment the app is
 * closed or the file is moved. A copy is small and it cannot disappear.
 */
object ToneStore {

    private const val FOLDER = "tones"
    private const val FILE_NAME = "key_tone"

    /** Why the last copy failed, for the screen to show. Empty on success. */
    var lastProblem: String = ""
        private set

    private fun folder(context: Context): File = File(context.filesDir, FOLDER)

    /** The tone in use, or null when none has been chosen. */
    fun current(context: Context): File? {
        val dir = folder(context)
        if (!dir.isDirectory) return null
        return dir.listFiles()?.firstOrNull()
    }

    /**
     * Copies the chosen file in, replacing any previous one.
     *
     * Returns the path to play, or null with [lastProblem] set to a sentence the
     * screen can show. A file that fails the rules is never copied, so a bad
     * choice leaves the previous tone working.
     */
    fun copyIn(context: Context, uri: Uri): String? {
        lastProblem = ""
        val resolver = context.contentResolver

        var displayName: String? = null
        var size = -1L
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameAt = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeAt = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameAt >= 0) displayName = cursor.getString(nameAt)
                    if (sizeAt >= 0) size = cursor.getLong(sizeAt)
                }
            }
        }

        ToneLibrary.problemWith(displayName, size, displayName?.substringAfterLast('.', ""))?.let {
            lastProblem = it
            return null
        }

        val dir = folder(context)
        if (!dir.isDirectory && !dir.mkdirs()) {
            lastProblem = "The app could not make room to keep the tone."
            return null
        }

        val extension = (displayName ?: "").substringAfterLast('.', "wav").lowercase()
        val target = File(dir, "$FILE_NAME.$extension")
        return runCatching {
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                lastProblem = "That file could not be opened."
                return null
            }
            // one tone at a time: drop anything left from a previous choice
            dir.listFiles()?.filter { it != target }?.forEach { it.delete() }
            target.absolutePath
        }.getOrElse {
            lastProblem = "That file could not be copied."
            null
        }
    }

    /** Forgets the tone and removes the copy. */
    fun clear(context: Context) {
        folder(context).listFiles()?.forEach { it.delete() }
    }
}
