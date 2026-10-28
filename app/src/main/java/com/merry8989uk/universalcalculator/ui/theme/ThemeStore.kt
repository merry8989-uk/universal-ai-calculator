package com.merry8989uk.universalcalculator.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.io.File
import kotlin.math.roundToInt

/**
 * The themes the user has made, kept on the device.
 *
 * One line per theme in a small text file, the same way the rest of the app
 * stores things: readable, diff-able, and a half-written last line costs only
 * that line.
 */
object ThemeStore {

    private const val FILE = "custom_themes.txt"
    private const val IMAGE_FOLDER = "theme_images"

    /** How many themes one user may keep. Enough for taste, not for a library. */
    const val MAX_THEMES = 20

    private fun file(context: Context) = File(context.filesDir, FILE)

    private fun imageDir(context: Context) = File(context.filesDir, IMAGE_FOLDER)

    fun load(context: Context): List<CustomTheme> =
        runCatching {
            val f = file(context)
            if (!f.exists()) emptyList() else ThemeCodec.decode(f.readText())
        }.getOrDefault(emptyList())

    fun save(context: Context, themes: List<CustomTheme>): Boolean = runCatching {
        file(context).writeText(ThemeCodec.encode(themes))
        true
    }.getOrDefault(false)

    /**
     * Adds or replaces a theme and returns the list as it now stands.
     *
     * A theme with no id is new and is given one; a theme whose id already
     * exists replaces it, which is what editing a theme does.
     */
    fun put(context: Context, theme: CustomTheme): List<CustomTheme> {
        val current = load(context).toMutableList()
        val withId = if (theme.id.isBlank()) theme.copy(id = newId()) else theme
        val at = current.indexOfFirst { it.id == withId.id }
        if (at >= 0) current[at] = withId
        else if (current.size < MAX_THEMES) current.add(withId)
        save(context, current)
        return current
    }

    fun remove(context: Context, id: String): List<CustomTheme> {
        val current = load(context).toMutableList()
        val gone = current.firstOrNull { it.id == id }
        current.removeAll { it.id == id }
        // the picture belongs to the theme, so it goes with it
        gone?.imagePath?.takeIf { it.isNotBlank() }?.let { runCatching { File(it).delete() } }
        save(context, current)
        return current
    }

    fun byId(context: Context, id: String): CustomTheme? =
        load(context).firstOrNull { it.id == id }

    private fun newId(): String = "t" + System.currentTimeMillis().toString(36)

    /**
     * Copies a picture in and reads two colours out of it.
     *
     * The picture is shrunk before it is stored: a theme does not need a
     * twelve-megapixel photograph, and a smaller one keeps the app quick and the
     * theme file small.
     */
    fun imageTheme(
        context: Context,
        uri: Uri,
        name: String,
        base: CustomTheme,
    ): CustomTheme? = runCatching {
        val dir = imageDir(context)
        if (!dir.isDirectory) dir.mkdirs()

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val (accent, second) = ThemeFromImage.pick(pixels)

        val target = File(dir, "theme_" + System.currentTimeMillis().toString(36) + ".jpg")
        target.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out) }
        bitmap.recycle()

        base.copy(
            id = if (base.id.isBlank()) newId() else base.id,
            name = name.ifBlank { "My photo theme" },
            primary = accent,
            secondary = second,
            background = ThemeFromImage.canvasFor(accent),
            surface = ThemeFromImage.surfaceFor(accent),
            imagePath = target.absolutePath,
        )
    }.getOrNull()

    /** A power-of-two shrink so decoding a big photo stays cheap. */
    fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w > 720 || h > 720) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample
    }
}

/**
 * The text form of the theme list.
 *
 * One line per theme, fields separated by a bar, with bars in a name or a path
 * escaped so a theme called "red | blue" cannot break the file.
 */
object ThemeCodec {

    private const val HEADER = "NEXUSTHEME1"
    private const val FIELD = "|"

    fun encode(themes: List<CustomTheme>): String = buildString {
        appendLine(HEADER)
        for (t in themes) {
            appendLine(
                listOf(
                    escape(t.id),
                    escape(t.name),
                    argb(t.background).toString(),
                    argb(t.surface).toString(),
                    argb(t.primary).toString(),
                    argb(t.secondary).toString(),
                    escape(t.imagePath),
                ).joinToString(FIELD),
            )
        }
    }

    /** Skips anything it cannot read rather than losing the whole list. */
    fun decode(text: String): List<CustomTheme> {
        val out = ArrayList<CustomTheme>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line == HEADER) continue
            val parts = splitFields(line)
            if (parts.size < 6) continue
            val id = unescape(parts[0])
            if (id.isBlank()) continue
            out.add(
                CustomTheme(
                    id = id,
                    name = unescape(parts[1]),
                    background = Color(parts[2].toLongOrNull()?.toInt() ?: continue),
                    surface = Color(parts[3].toLongOrNull()?.toInt() ?: continue),
                    primary = Color(parts[4].toLongOrNull()?.toInt() ?: continue),
                    secondary = Color(parts[5].toLongOrNull()?.toInt() ?: continue),
                    imagePath = parts.getOrNull(6)?.let { unescape(it) } ?: "",
                ),
            )
        }
        return out
    }

    /** Splits on bars that are not escaped. */
    private fun splitFields(line: String): List<String> {
        val out = ArrayList<String>()
        val current = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\\' && i + 1 < line.length) {
                current.append(c).append(line[i + 1])
                i += 2
                continue
            }
            if (c == FIELD[0]) {
                out.add(current.toString())
                current.setLength(0)
                i++
                continue
            }
            current.append(c)
            i++
        }
        out.add(current.toString())
        return out
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace(FIELD, "\\" + FIELD)

    private fun unescape(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                out.append(value[i + 1])
                i += 2
                continue
            }
            out.append(c)
            i++
        }
        return out.toString()
    }

    /**
     * The colour as 0xAARRGGBB.
     *
     * Every shift is bracketed. `shl` and `or` are both infix and share a
     * precedence, so an unparenthesised chain groups left to right and rotates
     * the bytes instead of packing them — which turns blue into alpha and
     * quietly corrupts every saved theme.
     */
    private fun argb(color: Color): Int {
        val a = (color.alpha * 255).roundToInt().coerceIn(0, 255)
        val r = (color.red * 255).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255).roundToInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
