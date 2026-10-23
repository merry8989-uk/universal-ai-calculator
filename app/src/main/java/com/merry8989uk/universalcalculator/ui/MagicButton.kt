package com.merry8989uk.universalcalculator.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.ui.theme.Cyan
import com.merry8989uk.universalcalculator.ui.theme.Pink
import com.merry8989uk.universalcalculator.ui.theme.Violet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The AI accent gradient used by every "magic" affordance. */
val MagicBrush: Brush = Brush.linearGradient(listOf(Violet, Cyan, Pink))

/** How many sparks fly off the button at any time. */
private const val SPARKS = 7

/** A four-point sparkle: two crossed strokes with a bright core. */
private fun DrawScope.spark(centre: Offset, radius: Float, colour: Color) {
    val w = (radius * 0.42f).coerceAtLeast(1f)
    drawLine(colour, Offset(centre.x, centre.y - radius), Offset(centre.x, centre.y + radius), w, StrokeCap.Round)
    drawLine(colour, Offset(centre.x - radius, centre.y), Offset(centre.x + radius, centre.y), w, StrokeCap.Round)
    drawCircle(colour, radius * 0.3f, centre)
}

/**
 * A round, softly pulsing gradient button — the "magic" AI entry point.
 *
 * Sparks drift outwards from the edge on a loop, so it always looks like
 * something is coming off it.
 */
@Composable
fun MagicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    glyph: String = "\u2726",
    /** What a screen reader says. The glyph on its own means nothing. */
    spoken: String? = null,
) {
    val description = spoken ?: stringResource(R.string.explain_with_ai_a11y)
    val transition = rememberInfiniteTransition(label = "magic")

    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "glow",
    )
    // One pass per spark; each spark is offset so they are evenly spaced.
    val flight by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "sparks",
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MagicBrush)
            .border(1.5.dp, Color.White.copy(alpha = glow), CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        // Sparks are drawn behind the glyph, spilling past the button edge.
        Canvas(modifier = Modifier.matchParentSize()) {
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val edge = this.size.minDimension / 2f
            for (i in 0 until SPARKS) {
                val phase = (flight + i.toFloat() / SPARKS) % 1f
                // travel outward from the rim, then fade
                val distance = edge * (0.75f + 0.85f * phase)
                val angle = (i * 360f / SPARKS + phase * 90f) * PI / 180.0
                val at = Offset(
                    centre.x + (distance * cos(angle)).toFloat(),
                    centre.y + (distance * sin(angle)).toFloat(),
                )
                val fade = (1f - phase) * (1f - phase)
                val radius = edge * 0.13f * (1f - 0.45f * phase)
                if (fade > 0.03f) {
                    spark(at, radius, Color.White.copy(alpha = fade * 0.85f))
                }
            }
        }
        Text(glyph, color = Color.White)
    }
}
