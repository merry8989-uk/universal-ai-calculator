package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The assistant, as a small disc you can put where you want it.
 *
 * The screens that are not the keypad have no room for a row of buttons, and a
 * fixed one lands on top of whatever the reader is trying to see. This can be
 * dragged out of the way and it remembers where it was put.
 *
 * It is drawn inside the screen, not in a system overlay, so it cannot outlive
 * the app: when the app closes, this goes with it. A floating window that can
 * outlive its app is a different thing entirely and not what is wanted here.
 *
 * Position is kept as a fraction of the space rather than in pixels, so it lands
 * in the same place after a rotation instead of somewhere off the edge. It is
 * clamped to the space it is drawn in, which is what stops it from ever being
 * dragged out of reach.
 */
@Composable
fun ExplainBubble(
    onExplain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // where it sits, as a fraction of the room it has
    var fx by rememberSaveable { mutableStateOf(0.82f) }
    var fy by rememberSaveable { mutableStateOf(0.72f) }

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val sizeDp = 54.dp
        val sizePx = with(density) { sizeDp.toPx() }
        val maxX = with(density) { (maxWidth - sizeDp).toPx() }.coerceAtLeast(0f)
        val maxY = with(density) { (maxHeight - sizeDp).toPx() }.coerceAtLeast(0f)

        var moved by rememberSaveable { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { (fx * maxX).toDp() },
                    y = with(density) { (fy * maxY).toDp() },
                )
                .size(sizeDp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF7C4DFF), Color(0xFF2196F3), Color(0xFF00D4C8)),
                    ),
                )
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragStart = { moved = false },
                        onDrag = { change, drag ->
                            change.consume()
                            moved = true
                            if (maxX > 0f) {
                                fx = ((fx * maxX + drag.x) / maxX).coerceIn(0f, 1f)
                            }
                            if (maxY > 0f) {
                                fy = ((fy * maxY + drag.y) / maxY).coerceIn(0f, 1f)
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onExplain() })
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("\u2728", fontSize = 22.sp, color = Color.White)
        }
    }
}

/**
 * The question to ask the assistant about whatever is on the screen.
 *
 * Written out rather than left to the screen so that the same words are used
 * wherever the disc is put — the assistant answers better when it is told what it
 * is looking at instead of guessing from a bare number.
 */
fun explainQuestion(label: String, text: String): String =
    if (text.isBlank()) {
        "Explain the $label screen and what I can do with it, in a few lines."
    } else {
        "Explain this $label in a few lines: $text. Say what it means and how it " +
            "was worked out, and give the answer first."
    }
