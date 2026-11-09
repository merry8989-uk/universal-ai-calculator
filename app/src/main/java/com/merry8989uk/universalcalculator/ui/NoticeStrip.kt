package com.merry8989uk.universalcalculator.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.ai.NoticeClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** How long a message is allowed to stay on screen. */
const val AUTO_HIDE_MS = 30_000L

/**
 * A message from the developer, at the top of the screen, swiped away.
 *
 * A swipe is the gesture people already make on a message they are done with, and
 * it needs no explanation the way a small close button does. It works both ways —
 * left or right — because there is no list here to imply a direction.
 *
 * Let go of it before it has moved far enough and it springs back rather than
 * disappearing, so a message cannot be lost by a finger that only meant to scroll.
 *
 * An ordinary notice is a quiet strip; an important one is the same strip in a
 * stronger colour rather than a dialog, because a message that arrives unasked has
 * not earned the right to stop somebody mid-sum.
 */
@Composable
fun NoticeStrip(
    notice: NoticeClient.Notice,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (notice.isImportant) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    // A third of the screen, or 90dp, whichever is less: far enough that a swipe
    // meant it, near enough that a thumb can reach it.
    val threshold = minOf(screenWidthPx / 3f, with(density) { 90.dp.toPx() })

    // A message that stays for ever becomes part of the furniture and stops being
    // read. Half a minute is long enough to read a sentence and short enough that
    // nobody has to get rid of it.
    LaunchedEffect(notice.id) {
        delay(AUTO_HIDE_MS)
        // it leaves the same way a swipe sends it, so the two do not feel like
        // different things happening to the same message
        offsetX.animateTo(screenWidthPx, animationSpec = spring(stiffness = Spring.StiffnessMedium))
        onDismiss()
    }

    fun settle() {
        val gone = abs(offsetX.value) > threshold
        scope.launch {
            if (gone) {
                // carry it off the edge it was pushed towards, then report it
                val edge = if (offsetX.value < 0) -screenWidthPx else screenWidthPx
                offsetX.animateTo(edge, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                onDismiss()
            } else {
                offsetX.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .graphicsLayer {
                translationX = offsetX.value
                // fades as it is pushed away, so it is obvious it is leaving
                alpha = (1f - (abs(offsetX.value) / screenWidthPx).coerceIn(0f, 1f) * 1.6f)
                    .coerceIn(0f, 1f)
            }
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { settle() },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    },
                    onHorizontalDrag = { change, drag ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + drag)
                        }
                    },
                )
            }
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                // Kept as a small cross for anybody who would rather tap than
                // swipe, and because a gesture with no visible alternative is a
                // gesture some people never find.
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("\u00d7", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(text = notice.body, style = MaterialTheme.typography.bodySmall)
            if (notice.link.isNotBlank()) {
                Text(
                    text = notice.link,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
