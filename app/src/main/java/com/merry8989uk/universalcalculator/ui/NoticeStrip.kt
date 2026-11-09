package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.ai.NoticeClient

/**
 * A message from the developer, at the top of the screen.
 *
 * An ordinary notice is a strip that can be dismissed and then stays gone. An
 * important one is the same strip in a stronger colour: it is not a modal,
 * because a modal over a calculator stops somebody mid-sum and a message that
 * arrives unasked has not earned that.
 *
 * Either way it is shown once. Dismissing it records the id, so it does not come
 * back on the next launch.
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) { Text("Close") }
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
