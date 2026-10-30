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
private const val SPARKS = 12

/** One pass of a spark, in milliseconds. */
private const val FLIGHT_MS = 1700

/** A four-point sparkle: two crossed strokes with a bright core. */
private fun DrawScope.spark(centre: Offset, radius: Float, colour: Color) {
    val w = (radius * 0.42f).coerceAtLeast(1f)
    drawLine(colour, Offset(centre.x, centre.y - radius), Offset(centre.x, centre.y + radius), w, StrokeCap.Round)
    drawLine(colour, Offset(centre.x - radius, centre.y), Offset(centre.x + radius, centre.y), w, StrokeCap.Round)
    drawCircle(colour, radius * 0.32f, centre)
}

/**
 * A round, softly pulsing gradient button — the "magic" AI entry point.
 *
 * Sparks leave the rim and fly outwards on a loop, so something is always coming
 * off the button.
 *
 * The sparks are drawn **behind** the circle and **outside** it, which is why
 * nothing in this file clips the outer box: an earlier version put
 * `clip(CircleShape)` on the box that held the sparks, so every one of them was
 * cut off at the rim and the button looked still. Only the circle itself is
 * clipped, and the canvas is not.
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
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow",
    )
    // The whole flight loop. Each spark is offset within it so they are evenly
    // spread rather than leaving together.
    val flight by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(FLIGHT_MS, easing = LinearEasing)),
        label = "sparks",
    )
    // A slow turn of the highlight, so the button itself is not static either.
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "turn",
    )

    // The footprint stays exactly `size`: the sparks overflow it rather than
    // making the button bigger and shoving the row it sits in.
    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val centre = Offset(this.size.width / 2f, this.size.height / 2f)
            val edge = this.size.minDimension / 2f

            for (i in 0 until SPARKS) {
                val phase = (flight + i.toFloat() / SPARKS) % 1f
                // From just inside the rim to well beyond it, so the spark is
                // seen leaving the button rather than appearing in mid air.
                val distance = edge * (0.85f + 1.05f * phase)
                // Each spark also drifts round a little as it goes, which reads
                // as scattering rather than a rigid wheel.
                val angle = (i * 360f / SPARKS + phase * 55f) * PI / 180.0
                val at = Offset(
                    centre.x + (distance * cos(angle)).toFloat(),
                    centre.y + (distance * sin(angle)).toFloat(),
                )
                // Bright at the rim, gone by the end of the flight.
                val fade = (1f - phase) * (1f - phase)
                val radius = edge * (0.19f - 0.13f * phase)
                if (fade > 0.02f && radius > 0.4f) {
                    // a small trail behind it, so the eye reads motion
                    if (phase > 0.12f) {
                        val behind = edge * (0.85f + 1.05f * (phase - 0.12f))
                        val tail = Offset(
                            centre.x + (behind * cos(angle)).toFloat(),
                            centre.y + (behind * sin(angle)).toFloat(),
                        )
                        spark(tail, radius * 0.55f, Color.White.copy(alpha = fade * 0.3f))
                    }
                    spark(at, radius, Color.White.copy(alpha = fade * 0.95f))
                }
            }
        }

        // The button itself. Only this is clipped, and it is drawn after the
        // sparks so it hides the part of each flight that is behind the circle.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(MagicBrush)
                .border(1.5.dp, Color.White.copy(alpha = 0.35f + 0.5f * glow), CircleShape)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = description
                    role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            // A soft highlight that goes round the rim, so the face is alive too.
            Canvas(modifier = Modifier.matchParentSize()) {
                val centre = Offset(this.size.width / 2f, this.size.height / 2f)
                val r = this.size.minDimension / 2f
                val rad = turn * PI / 180.0
                val at = Offset(
                    centre.x + (r * 0.62f * cos(rad)).toFloat(),
                    centre.y + (r * 0.62f * sin(rad)).toFloat(),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f * glow), Color.Transparent),
                        center = at,
                        radius = r * 0.9f,
                    ),
                    radius = r,
                    center = centre,
                )
            }
            Text(glyph, color = Color.White)
        }
    }
}
