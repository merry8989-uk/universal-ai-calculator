package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.SavedChat
import com.merry8989uk.universalcalculator.data.ChatStore
import java.io.File

/**
 * The conversation archive.
 *
 * Deliberately its own screen and its own folder: these are chats, not
 * calculations, and they never appear in the calculation archive. A conversation
 * is kept until the user deletes it — there is no three-day rule here, because
 * the three-day rule is about the working history on the calculator screen.
 */
@Composable
fun ChatArchiveScreen(
    onBack: () -> Unit,
    onOpen: (SavedChat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var version by remember { mutableStateOf(0) }
    var open by remember { mutableStateOf<Pair<File, SavedChat>?>(null) }

    val chats = remember(version) { ChatStore.listWithFiles(context) }

    if (open != null) {
        ChatViewer(chat = open!!.second, onBack = { open = null })
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                "Chats",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            if (chats.isNotEmpty()) {
                TextButton(onClick = {
                    ChatStore.deleteAll(context)
                    version++
                }) { Text("Delete all") }
            }
        }

        Text(
            "Kept in Nexus Calculator/Chats. Separate from your calculation archive.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        if (chats.isEmpty()) {
            Text(
                "No saved chats yet. Ask something and it is kept here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(chats) { (file, chat) ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { open = file to chat }
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        chat.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${chat.started}   ${chat.asked} question" + (if (chat.asked == 1) "" else "s"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

/** One conversation, read-only, with the option to delete it. */
@Composable
private fun ChatViewer(chat: SavedChat, onBack: () -> Unit) {
    val context = LocalContext.current
    var gone by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                chat.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            chat.started + if (chat.model.isNotBlank()) "   " + chat.model else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SelectionContainer(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                items(chat.turns) { turn ->
                    Text(
                        if (turn.isUser) "You" else "Assistant",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (turn.isUser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(turn.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        OutlinedButton(
            onClick = {
                if (!gone) {
                    ChatStore.listWithFiles(context)
                        .firstOrNull { it.second.title == chat.title && it.second.started == chat.started }
                        ?.let { ChatStore.delete(it.first) }
                    gone = true
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Delete this chat") }
    }
}
