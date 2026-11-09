package com.merry8989uk.universalcalculator.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import java.io.File

/**
 * A calculation, as a picture worth sending.
 *
 * People here share a result by screenshotting it, which carries the whole phone
 * with it - the status bar, somebody's name in a notification, the keyboard.
 * This draws the sum and nothing else, on the app's own background, and hands it
 * to whatever the user wants to send it with.
 *
 * The words are built by [content], which is a pure function, so what the card
 * says is tested. The drawing is [render]: a canvas, a few paints and no
 * measuring of the world beyond the text it is given.
 */
data class CardContent(
    val title: String,
    val subtitle: String,
    /** label to value, in the order they were worked out. */
    val rows: List<Pair<String, String>>,
    val footer: String,
)

object ResultCard {

    /** A long value is cut rather than drawn off the edge of the card. */
    private const val MAX_CHARS = 34
    private const val MAX_ROWS = 7

    private const val WIDTH = 1080
    private const val PADDING = 72f
    private const val TITLE_SIZE = 52f
    private const val SUBTITLE_SIZE = 30f
    private const val LABEL_SIZE = 28f
    private const val VALUE_SIZE = 46f
    private const val FOOTER_SIZE = 26f

    /**
     * What the card says.
     *
     * The inputs are left off on purpose: what is being sent is an answer, and
     * the numbers that produced it are often somebody's salary or a dose. The
     * label and the result are the two things worth reading at a glance.
     */
    fun content(
        title: String,
        category: String,
        results: List<Pair<String, String>>,
        footer: String = "Nexus Calc",
    ): CardContent {
        val rows = results
            .map { (label, value) -> clip(label, MAX_CHARS) to clip(value, MAX_CHARS) }
            .take(MAX_ROWS)
        return CardContent(
            title = clip(title, MAX_CHARS),
            subtitle = clip(category, MAX_CHARS),
            rows = rows,
            footer = footer,
        )
    }

    private fun clip(text: String, max: Int): String {
        val oneLine = text.replace("\n", " ").trim()
        return if (oneLine.length <= max) oneLine else oneLine.take(max - 1).trimEnd() + "\u2026"
    }

    /** Draws the card and returns the file it was written to. */
    fun render(context: Context, content: CardContent): File {
        val rowHeight = VALUE_SIZE + LABEL_SIZE + 46f
        val height = (PADDING * 2 + TITLE_SIZE + SUBTITLE_SIZE + 40f +
            content.rows.size * rowHeight + FOOTER_SIZE + 40f).toInt()

        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#0A1020"))

        val panel = RectF(PADDING / 2, PADDING / 2, WIDTH - PADDING / 2, height - PADDING / 2)
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#152139") }
        canvas.drawRoundRect(panel, 36f, 36f, panelPaint)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }
        var y = PADDING + TITLE_SIZE

        text.color = Color.parseColor("#EFF4FF")
        text.textSize = TITLE_SIZE
        canvas.drawText(content.title, PADDING, y, text)

        y += SUBTITLE_SIZE + 16f
        text.color = Color.parseColor("#70DFBD")
        text.textSize = SUBTITLE_SIZE
        text.typeface = Typeface.DEFAULT
        canvas.drawText(content.subtitle, PADDING, y, text)

        y += 34f
        val rule = Paint().apply { color = Color.parseColor("#2A3A57") }
        canvas.drawRect(PADDING, y, WIDTH - PADDING, y + 2f, rule)
        y += 30f

        content.rows.forEach { (label, value) ->
            text.color = Color.parseColor("#A5B3CC")
            text.textSize = LABEL_SIZE
            canvas.drawText(label, PADDING, y, text)

            y += VALUE_SIZE
            text.color = Color.parseColor("#EFF4FF")
            text.textSize = VALUE_SIZE
            text.typeface = Typeface.MONOSPACE
            canvas.drawText(value, PADDING, y, text)
            text.typeface = Typeface.DEFAULT

            y += LABEL_SIZE + 46f
        }

        text.color = Color.parseColor("#79A7FF")
        text.textSize = FOOTER_SIZE
        canvas.drawText(content.footer, PADDING, height - PADDING, text)

        val dir = File(context.cacheDir, "cards").apply { mkdirs() }
        val file = File(dir, "nexus-calc.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    /** Hands the card to whatever the user picks. */
    fun share(context: Context, content: CardContent) {
        val file = render(context, content)
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Share this calculation")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
