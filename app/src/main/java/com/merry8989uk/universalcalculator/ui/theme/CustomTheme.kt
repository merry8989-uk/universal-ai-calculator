package com.merry8989uk.universalcalculator.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A theme the user built.
 *
 * Five colours are enough to describe the whole app: the canvas, the panels on
 * it, the keys, the accent that marks the answer and the selected tab, and a
 * second accent for the operators. Everything else — disabled states, borders,
 * the glass film — is derived, so a user never has to pick a dozen shades.
 */
data class CustomTheme(
    /** Stable across renames, so a theme stays selected when its name changes. */
    val id: String,
    val name: String,
    val background: Color,
    val surface: Color,
    val primary: Color,
    val secondary: Color,
    /** A picture to sit behind everything, or blank for a flat colour. */
    val imagePath: String = "",
) {
    /** A readable line for the list. */
    fun blurb(): String =
        if (imagePath.isNotBlank()) "Built from a photo" else "Built from your own colours"

    companion object {
        /** The theme the builder opens with, so nothing starts black. */
        val START = CustomTheme(
            id = "",
            name = "My theme",
            background = Color(0xFF0E1018),
            surface = Color(0xFF1A1D2B),
            primary = Color(0xFF4EA8FF),
            secondary = Color(0xFFFF7AB6),
        )
    }
}

/**
 * Turning a photo into a theme.
 *
 * Kept as a pure function over pixels rather than a Bitmap, so the rule can be
 * tested without a device: given a pile of colours, which two become the accent
 * and the second accent, and what becomes the canvas.
 */
object ThemeFromImage {

    /**
     * Two colours from a picture: the most vivid one, and the one that goes with
     * it.
     *
     * Vividness matters more than frequency. A photo of a red flower on a grey
     * wall is mostly grey, and a theme built from "the most common colour" would
     * be a grey app with a grey accent — technically faithful, and dull.
     */
    fun pick(pixels: IntArray): Pair<Color, Color> {
        if (pixels.isEmpty()) return CustomTheme.START.primary to CustomTheme.START.secondary

        var best = 0
        var bestScore = -1f
        for (p in pixels) {
            if (alphaOf(p) < 128) continue
            val score = vividness(p)
            if (score > bestScore) {
                bestScore = score
                best = p
            }
        }

        // The second colour is the most vivid one that is clearly a different
        // hue, so the two accents do not look like a printing error.
        var second = best
        var secondScore = -1f
        val bestHue = hueOf(best)
        for (p in pixels) {
            if (alphaOf(p) < 128) continue
            if (hueGap(hueOf(p), bestHue) < 40f) continue
            val score = vividness(p)
            if (score > secondScore) {
                secondScore = score
                second = p
            }
        }
        if (secondScore < 0f) second = shiftHue(best, 60f)

        return Color(best) to Color(second)
    }

    /**
     * A canvas colour that suits the accent: the same hue, very dark and very
     * still, so the answer stays the brightest thing on the screen.
     */
    fun canvasFor(accent: Color): Color {
        val h = hueOf(accent.toArgb())
        return hslToArgb(h, 0.30f, 0.06f).let { Color(it) }
    }

    /** The panel colour that sits on that canvas. */
    fun surfaceFor(accent: Color): Color {
        val h = hueOf(accent.toArgb())
        return hslToArgb(h, 0.26f, 0.12f).let { Color(it) }
    }

    // ---- the colour arithmetic ----

    private fun alphaOf(argb: Int): Int = (argb ushr 24) and 0xFF

    /** How much a colour would draw the eye: saturation first, lightness second. */
    private fun vividness(argb: Int): Float {
        val r = ((argb ushr 16) and 0xFF) / 255f
        val g = ((argb ushr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val lightness = (hi + lo) / 2f
        val saturation = if (hi == lo) 0f else (hi - lo) / (1f - abs(2f * lightness - 1f))
        // a mid-tone vivid colour beats a near-black or near-white one
        val middle = 1f - abs(lightness - 0.55f) * 1.6f
        return saturation * 2f + middle * 0.6f
    }

    /** Hue in degrees, 0 to 360. */
    private fun hueOf(argb: Int): Float {
        val r = ((argb ushr 16) and 0xFF) / 255f
        val g = ((argb ushr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val d = hi - lo
        if (d == 0f) return 0f
        val h = when (hi) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        }
        return ((h * 60f) + 360f) % 360f
    }

    /** How far apart two hues are, the short way round the circle. */
    private fun hueGap(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }

    private fun shiftHue(argb: Int, by: Float): Int =
        hslToArgb((hueOf(argb) + by) % 360f, 0.62f, 0.55f)

    private fun hslToArgb(hue: Float, saturation: Float, lightness: Float): Int {
        val c = (1f - abs(2f * lightness - 1f)) * saturation
        val x = c * (1f - abs((hue / 60f) % 2f - 1f))
        val m = lightness - c / 2f
        val (r1, g1, b1) = when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}

private fun Color.toArgb(): Int =
    (alpha * 255).roundToInt().coerceIn(0, 255) shl 24 or
        (red * 255).roundToInt().coerceIn(0, 255) shl 16 or
        (green * 255).roundToInt().coerceIn(0, 255) shl 8 or
        (blue * 255).roundToInt().coerceIn(0, 255)
