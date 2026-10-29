package com.merry8989uk.universalcalculator.data

import android.content.Context
import com.merry8989uk.universalcalculator.calc.ChatFormat
import com.merry8989uk.universalcalculator.calc.ChatTurn
import com.merry8989uk.universalcalculator.calc.SavedChat
import java.io.File
import java.time.LocalDateTime

/**
 * The conversation archive, on the device.
 *
 * It sits beside the calculation archive — `Nexus Calculator/Chats/` — and is
 * completely separate from it. A conversation is never written into a day file,
 * the day files are never read here, and clearing one never touches the other.
 * That separation is the point: the calculations archive is a record of sums,
 * and this is a record of what was asked.
 *
 * One file per conversation, so a chat can be opened, exported or deleted on its
 * own. The files are plain text, so they can be read in any editor once the
 * folder is copied off the phone.
 */
object ChatStore {

    /** The folder inside the archive folder, as it appears in a file manager. */
    const val FOLDER = "Chats"

    private const val MAX_CHATS = 500

    fun root(context: Context): File = File(ArchiveStore.root(context), FOLDER)

    /** Every saved chat, newest first. A file that will not parse is skipped. */
    fun list(context: Context): List<SavedChat> {
        val dir = root(context)
        if (!dir.isDirectory) return emptyList()
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") } ?: return emptyList()
        return files
            .sortedByDescending { it.name }
            .mapNotNull { f ->
                runCatching { f.readText() }.getOrNull()?.let { ChatFormat.parse(it) }
            }
    }

    /** The file names, paired with the chats, so one can be deleted. */
    fun listWithFiles(context: Context): List<Pair<File, SavedChat>> {
        val dir = root(context)
        if (!dir.isDirectory) return emptyList()
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") } ?: return emptyList()
        return files
            .sortedByDescending { it.name }
            .mapNotNull { f ->
                runCatching { f.readText() }.getOrNull()
                    ?.let { ChatFormat.parse(it) }
                    ?.let { chat -> f to chat }
            }
    }

    /**
     * Writes one conversation.
     *
     * Called with the same name every time a turn is added, so a chat is one file
     * that grows rather than a new file per message. Returns the file, or null
     * when the folder could not be written — the app carries on either way,
     * because an assistant that refuses to answer because its notebook is full is
     * a worse assistant.
     */
    fun save(context: Context, at: LocalDateTime, chat: SavedChat): File? = try {
        val dir = root(context)
        if (!dir.isDirectory && !dir.mkdirs()) return null
        val file = File(dir, ChatFormat.fileName(at, chat.title))
        file.writeText(ChatFormat.write(chat))
        prune(context)
        file
    } catch (e: Exception) {
        null
    }

    fun delete(file: File): Boolean = try {
        file.delete()
    } catch (e: Exception) {
        false
    }

    fun deleteAll(context: Context): Int {
        val dir = root(context)
        if (!dir.isDirectory) return 0
        var removed = 0
        dir.listFiles()?.forEach { if (it.isFile && it.delete()) removed++ }
        return removed
    }

    /**
     * Keeps the folder from growing without limit.
     *
     * The oldest go first. This is not the three-day rule the calculation history
     * follows: a conversation is kept until the user deletes it, and only the
     * five-hundredth-oldest is dropped, which nobody will reach by accident.
     */
    private fun prune(context: Context) {
        val dir = root(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") } ?: return
        if (files.size <= MAX_CHATS) return
        files.sortedBy { it.name }
            .take(files.size - MAX_CHATS)
            .forEach { runCatching { it.delete() } }
    }

    /** Turns for the store, from what the screen holds. */
    fun turns(pairs: List<Pair<String, String>>): List<ChatTurn> =
        pairs.map { (role, text) -> ChatTurn(if (role == "user") "u" else "a", text) }
}
