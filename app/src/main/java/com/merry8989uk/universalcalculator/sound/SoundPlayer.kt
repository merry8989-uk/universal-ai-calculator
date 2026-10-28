package com.merry8989uk.universalcalculator.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.merry8989uk.universalcalculator.R

/**
 * The little tones a key makes.
 *
 * Nine short clips ship inside the app and are generated from a few lines of
 * arithmetic, so there is nothing to download and no dependency. The tenth
 * choice is a file the user supplied, which is copied into the app's own folder
 * and played from there.
 */
object SoundPlayer {

    const val OFF = ToneLibrary.OFF
    val CHOICES: List<String> = ToneLibrary.CHOICES

    private var pool: SoundPool? = null
    private var sounds: Map<String, Int> = emptyMap()
    private var customSound: Int = 0
    private var customPath: String = ""
    private var choice: String = OFF
    private var volume: Float = 0.7f

    /** Call once, from the Application. */
    fun init(context: Context) {
        if (pool != null) return
        val built = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        sounds = mapOf(
            "Click" to built.load(context, R.raw.key_click, 1),
            "Bubble" to built.load(context, R.raw.key_bubble, 1),
            "Pop" to built.load(context, R.raw.key_pop, 1),
            "Drop" to built.load(context, R.raw.key_drop, 1),
            "Tick" to built.load(context, R.raw.key_tick, 1),
            "Chime" to built.load(context, R.raw.key_chime, 1),
            "Wood" to built.load(context, R.raw.key_wood, 1),
            "Coin" to built.load(context, R.raw.key_coin, 1),
            "Soft" to built.load(context, R.raw.key_soft, 1),
        )
        pool = built
    }

    /** Follows the setting; nothing plays while it is Off. */
    fun setChoice(value: String) {
        choice = value
    }

    /** How loud a key is, from 0 to 1. */
    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
    }

    /**
     * Points the player at the user's own tone.
     *
     * Loading is asynchronous inside SoundPool, so a key pressed in the first
     * instant after choosing may still play silence. That is better than
     * blocking the first keypress on disk.
     */
    fun setCustomPath(path: String) {
        if (path == customPath) return
        customPath = path
        val p = pool ?: return
        if (customSound != 0) {
            runCatching { p.unload(customSound) }
            customSound = 0
        }
        if (path.isBlank()) return
        customSound = runCatching { p.load(path, 1) }.getOrDefault(0)
    }

    /** The tone a key should make, or 0 when it should be silent. */
    fun soundId(): Int = when {
        choice == OFF -> 0
        ToneLibrary.isCustom(choice) -> customSound
        else -> sounds[choice] ?: 0
    }

    fun key() {
        val id = soundId()
        if (id == 0) return
        pool?.play(id, volume, volume, 1, 0, 1f)
    }
}
