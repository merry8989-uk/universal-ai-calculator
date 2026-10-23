package com.merry8989uk.universalcalculator.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.ui.theme.Cyan
import com.merry8989uk.universalcalculator.ui.theme.Pink
import com.merry8989uk.universalcalculator.ui.theme.Violet

/** The AI accent gradient used by every "magic" affordance. */
val MagicBrush: Brush = Brush.linearGradient(listOf(Violet, Cyan, Pink))

/**
 * A round, softly pulsing gradient button — the "magic" AI entry point.
 */
@Composable
fun MagicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    glyph: String = "\u2726",
) {
    val transition = rememberInfiniteTransition(label = "magic")
    val glow by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "glow",
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MagicBrush)
            .border(1.5.dp, Color.White.copy(alpha = glow), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = Color.White)
    }
}
