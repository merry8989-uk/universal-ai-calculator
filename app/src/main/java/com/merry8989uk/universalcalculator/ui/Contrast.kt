package com.merry8989uk.universalcalculator.ui

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * How far apart two colours are, the way WCAG measures it.
 *
 * The number is the one accessibility rules are written in, so it is the number
 * to quote in a review: 4.5:1 for ordinary text, 3:1 for large text and for the
 * shapes of things. It is kept here, apart from the colours themselves, so the
 * same arithmetic can be pointed at the pad, at a label, or at a colour somebody
 * is proposing, and so it can be tested without a device.
 *
 * Colours are passed as 0xAARRGGBB, which is the same number the theme's
 * literals are written as. Alpha is ignored: the ratio is between two colours
 * that are actually drawn on top of each other.
 */
object Contrast {

    /** AA for text below 18pt, or below 14pt when bold. */
    const val AA_NORMAL = 4.5

    /** AA for large text: 18pt and up, or 14pt and up when bold. */
    const val AA_LARGE = 3.0

    /** AAA, quoted where a key is worth getting all the way to. */
    const val AAA_NORMAL = 7.0

    /** The ratio between two colours, always 1.0 or more. */
    fun ratio(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /**
     * Relative luminance, as WCAG defines it.
     *
     * The channels are linearised first because the eye does not read sRGB's
     * numbers as light: a mid grey is not half the brightness of white.
     */
    fun luminance(colour: Long): Double {
        val r = linear((colour shr 16) and 0xFF)
        val g = linear((colour shr 8) and 0xFF)
        val b = linear(colour and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** Whether text in [label] can be read on [face] at ordinary size. */
    fun passesNormal(label: Long, face: Long): Boolean = ratio(label, face) >= AA_NORMAL

    /** Whether it can be read at large size, or used for a shape rather than words. */
    fun passesLarge(label: Long, face: Long): Boolean = ratio(label, face) >= AA_LARGE

    /** The ratio as it should appear in a comment or a bug report: "4.52:1". */
    fun text(label: Long, face: Long): String = String.format(Locale.US, "%.2f:1", ratio(label, face))

    private fun linear(channel: Long): Double {
        val c = channel / 255.0
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
}
