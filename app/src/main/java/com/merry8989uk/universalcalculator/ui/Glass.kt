package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The glassmorphism look from the design blueprint: a barely-there white film,
 * a faint white edge, and a deep shadow.
 *
 * Compose cannot blur what is *behind* a view (CSS `backdrop-filter`), so the
 * frosted effect comes from the translucent film over the ambient glow behind
 * it, rather than from a real blur.
 */
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 18.dp,
): Modifier = this
    .shadow(elevation, shape, clip = false)
    .clip(shape)
    .background(Color.White.copy(alpha = 0.04f))
    .border(1.dp, Color.White.copy(alpha = 0.09f), shape)

/**
 * The soft coloured glow the glass sits on: a dark base with a few large,
 * blurred-looking colour pools.
 */
@Composable
fun AmbientGlow(modifier: Modifier = Modifier) {
    // Taken from the theme, so Material You drags the glow to the wallpaper's colours.
    val base = MaterialTheme.colorScheme.background
    val glowA = MaterialTheme.colorScheme.primary
    val glowB = MaterialTheme.colorScheme.secondary
    val glowC = MaterialTheme.colorScheme.tertiary
    Box(modifier = modifier.fillMaxSize().background(base)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            fun pool(colour: Color, at: Offset, radius: Float, strength: Float) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colour.copy(alpha = strength), Color.Transparent),
                        center = at,
                        radius = radius,
                    ),
                    radius = radius,
                    center = at,
                )
            }
            pool(glowA, Offset(w * 0.15f, h * 0.10f), w * 0.75f, 0.22f)
            pool(glowB, Offset(w * 0.95f, h * 0.28f), w * 0.70f, 0.16f)
            pool(glowC, Offset(w * 0.30f, h * 0.92f), w * 0.85f, 0.14f)
            pool(glowA, Offset(w * 0.85f, h * 0.80f), w * 0.55f, 0.12f)
        }
    }
}
