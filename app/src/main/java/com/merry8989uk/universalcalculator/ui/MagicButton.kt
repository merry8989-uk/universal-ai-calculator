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

/**
 * A stable pseudo-random number in 0..1 for one spark.
 *
 * Written out rather than using [kotlin.random.Random] because this runs inside
 * a draw call, sixty times a second, and the value has to be the same on every
 * frame — a spark that re-rolled its speed each frame would jitter instead of
 * rise.
 */
private fun wobble(index: Int, salt: Int): Float {
    var h = index * 374761393 + salt * 668265263
    h = (h xor (h shr 13)) * 1274126177
    h = h xor (h shr 16)
    return (h and 0x7fffffff).toFloat() / 0x7fffffff.toFloat()
}

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
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val edge = this.size.minDimension / 2f

            for (i in 0 until SPARKS) {
                // Each spark has its own speed and its own starting point in the
                // loop. Without that they sit at evenly spaced phases, the set of
                // positions looks the same in every frame, and the button reads
                // as a still picture of sparks rather than sparks rising.
                val speed = 0.75f + 0.6f * wobble(i, 3)
                val phase = (flight * speed + wobble(i, 2)) % 1f

                // Where it starts across the width, and how high it climbs.
                val spread = (wobble(i, 1) - 0.5f) * edge * 1.5f
                val rise = this.size.height * (0.85f + 0.8f * wobble(i, 4))

                // A gentle sway as it goes, so the column is not a ruler.
                val sway = sin(phase * 2.0 * PI * (0.5 + 0.9 * wobble(i, 5)))
                val x = cx + spread + (sway * edge * 0.24f * phase).toFloat()
                // From just above the middle of the button upward: fire rises,
                // it does not spray in every direction.
                val y = cy - edge * 0.30f - rise * phase

                val fade = (1f - phase) * (1f - phase)
                val radius = edge * (0.15f - 0.10f * phase) * (0.75f + 0.6f * wobble(i, 6))
                if (fade <= 0.02f || radius <= 0.4f) continue

                // A smaller spark below it on the same path, so the eye reads a
                // rising trail rather than a dot.
                if (phase > 0.10f) {
                    val back = phase - 0.10f
                    val backY = cy - edge * 0.30f - rise * back
                    val backX = cx + spread + (sin(back * 2.0 * PI * (0.5 + 0.9 * wobble(i, 5))) *
                        edge * 0.24f * back).toFloat()
                    spark(Offset(backX, backY), radius * 0.5f, Color.White.copy(alpha = fade * 0.28f))
                }
                spark(Offset(x, y), radius, Color.White.copy(alpha = fade * 0.95f))
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
