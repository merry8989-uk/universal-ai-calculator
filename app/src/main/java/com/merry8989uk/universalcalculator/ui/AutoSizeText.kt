package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.isSpecified

/**
 * Text that shrinks to fit the width instead of running off the edge.
 *
 * A long result like 1,234,567,890.1234567 used to be clipped; now it steps
 * down to as little as [minScale] of the requested size.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minScale: Float = 0.42f,
    textAlign: TextAlign? = null,
    /** Where the text sits in the box. A key label is centred; a result is not. */
    contentAlignment: Alignment = Alignment.CenterEnd,
) {
    val measurer = rememberTextMeasurer()
    val resolved = if (style.fontSize.isSpecified) style else style.merge(LocalTextStyle.current)

    BoxWithConstraints(modifier = modifier, contentAlignment = contentAlignment) {
        val available = constraints.maxWidth
        val fitted = remember(text, resolved, available) {
            if (text.isEmpty() || available <= 0) resolved
            else {
                val probe = measurer.measure(AnnotatedString(text), style = resolved, maxLines = 1)
                val width = probe.size.width
                if (width <= available) {
                    resolved
                } else {
                    val ratio = (available.toFloat() / width).coerceIn(minScale, 1f)
                    resolved.copy(
                        fontSize = resolved.fontSize * ratio,
                        lineHeight = if (resolved.lineHeight.isSpecified) {
                            resolved.lineHeight * ratio
                        } else {
                            resolved.lineHeight
                        },
                    )
                }
            }
        }
        Text(
            text = text,
            style = fitted,
            color = color,
            maxLines = 1,
            textAlign = textAlign,
        )
    }
}
