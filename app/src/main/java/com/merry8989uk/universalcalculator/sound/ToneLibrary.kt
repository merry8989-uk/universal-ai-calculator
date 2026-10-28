package com.merry8989uk.universalcalculator.sound

/**
 * Which tones exist, and what makes a file a tone.
 *
 * The nine built-in ones are generated as small wav files that ship inside the
 * app, so choosing one costs nothing and needs no network. The tenth choice is
 * the user's own recording, which is the only case where a file is read from
 * storage.
 */
object ToneLibrary {

    const val OFF = "Off"

    /** The name the user's own tone is shown under. */
    const val CUSTOM = "My tone"

    /** The tones that ship with the app, in the order they are offered. */
    val BUILT_IN: List<String> = listOf(
        "Click", "Bubble", "Pop",
        "Drop", "Tick", "Chime", "Wood", "Coin", "Soft",
    )

    /** Everything the picker shows. */
    val CHOICES: List<String> = listOf(OFF) + BUILT_IN + CUSTOM

    /** True when this choice needs a file the user supplied. */
    fun isCustom(choice: String): Boolean = choice == CUSTOM

    /** The audio types a phone can hand over and the player can read. */
    private val ALLOWED = setOf("wav", "mp3", "ogg", "m4a", "aac", "opus", "flac")

    /** How large a clip may be before it is refused, in bytes. */
    const val MAX_BYTES = 2_000_000L

    /**
     * Why this file cannot be used as a key tone, or null when it can.
     *
     * A button tone is tens of milliseconds. Anything approaching a song is
     * almost certainly a mistake, and a large file loaded into the sound pool
     * for every keypress is a way to make the calculator stutter.
     */
    fun problemWith(
        name: String?,
        sizeBytes: Long,
        extension: String?,
    ): String? {
        if (name.isNullOrBlank()) return "That file could not be read."
        val ext = (extension ?: name.substringAfterLast('.', "")).lowercase()
        if (ext !in ALLOWED) {
            return "An audio file is needed — wav, mp3, ogg, m4a, aac, opus or flac."
        }
        if (sizeBytes <= 0L) return "That file is empty."
        if (sizeBytes > MAX_BYTES) {
            return "That file is too large. A key tone should be a second or two."
        }
        return null
    }
}
