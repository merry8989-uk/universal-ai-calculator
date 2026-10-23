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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.R
import com.merry8989uk.universalcalculator.ui.theme.Amber
import kotlin.math.roundToInt

/** How many keys sit across the drawer. */
private const val COLUMNS = 4

/**
 * The scientific keypad, as a panel that slides in from the left edge.
 *
 * The old two scrolling rows with a more/less toggle are gone: everything lives
 * in here, laid out in a grid, and the drawer is dragged or tapped open.
 */
@Composable
fun ScientificDrawer(
    keys: List<String>,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
    classic: Boolean = false,
) {
    if (keys.isEmpty()) return

    val density = LocalDensity.current
    val drawerWidth = 292.dp
    val widthPx = with(density) { drawerWidth.toPx() }

    val settled by animateFloatAsState(
        targetValue = if (open) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 380f),
        label = "drawer",
    )
    var dragged by remember { mutableFloatStateOf(0f) }
    val fraction = (settled + dragged).coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {
        if (fraction < 0.96f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - fraction) * 0.45f))
                    .clickable { onOpenChange(false) },
            )
        }

        val handleSpoken = stringResource(R.string.functions_drawer_a11y)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(drawerWidth)
                .offset { IntOffset(-(fraction * widthPx).roundToInt(), 0) }
                .glassSurface(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp), elevation = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("\u0192", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.functions),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onOpenChange(false) }) {
                    Text(stringResource(R.string.close))
                }
            }

            // A grabber down the outer edge, for dragging the drawer shut.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                keys.chunked(COLUMNS).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        chunk.forEach { key ->
                            KeyButton(
                                label = label(key),
                                colors = listOf(Color(0xFFFFD54F), Amber),
                                contentColor = Color(0xFF3A2600),
                                modifier = Modifier.weight(1f).height(50.dp),
                                classic = classic,
                                spoken = Spoken.key(key),
                                onPress = { onKey(key) },
                            )
                        }
                        // keep the last row's keys the same width as the rest
                        repeat(COLUMNS - chunk.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            dragged = (dragged + delta / widthPx).coerceIn(-1f, 1f)
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
                    .semantics { contentDescription = handleSpoken },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(5.dp)
                        .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}
