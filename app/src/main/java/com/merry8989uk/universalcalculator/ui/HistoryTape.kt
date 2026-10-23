package com.merry8989uk.universalcalculator.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import kotlin.math.roundToInt

/**
 * The tape's entries, newest first. A blank line or trailing newline in the
 * stored history is ignored.
 */
fun tapeEntries(history: String): List<String> =
    history.split("\n").filter { it.isNotBlank() }.reversed()

/**
 * The history tape: a panel that lives off the bottom of the calculator and is
 * pulled up over the keypad, rather than a screen of its own.
 *
 * Drag the grabber to move it, let go to settle open or closed, tap an entry to
 * load it back onto the board.
 */
@Composable
fun HistoryTape(
    history: String,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = remember(history) { tapeEntries(history) }

    val density = LocalDensity.current
    val tapeHeight = 330.dp
    val heightPx = with(density) { tapeHeight.toPx() }

    // Settled position (0 = fully open, 1 = fully closed) plus whatever the
    // finger has added on top of it.
    val settled by animateFloatAsState(
        targetValue = if (open) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "tape",
    )
    var dragged by remember { mutableFloatStateOf(0f) }
    val fraction = (settled + dragged).coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {
        // Dim what is behind, and tap it to close.
        if (fraction < 0.96f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - fraction) * 0.5f))
                    .clickable { onOpenChange(false) },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(tapeHeight)
                .offset { IntOffset(0, (fraction * heightPx).roundToInt()) }
                .glassSurface(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), elevation = 22.dp),
        ) {
            // The grabber. Dragging it is how the tape is opened and closed.
            val grabberSpoken = stringResource(R.string.history_grabber_a11y)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            dragged = (dragged + delta / heightPx).coerceIn(-1f, 1f)
                        },
                        onDragStopped = { velocity ->
                            val settleOpen = when {
                                velocity < -250f -> true
                                velocity > 250f -> false
                                else -> fraction < 0.5f
                            }
                            dragged = 0f
                            onOpenChange(settleOpen)
                        },
                    )
                    .semantics { contentDescription = grabberSpoken }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f)),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.history_recent),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row {
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
                    }
                    TextButton(onClick = onSeeAll) { Text(stringResource(R.string.history_see_all)) }
                }
            }

            if (entries.isEmpty()) {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(entries) { entry ->
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPick(entry) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
