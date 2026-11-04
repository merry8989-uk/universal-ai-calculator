package com.merry8989uk.universalcalculator.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * The two chips that sit beside More.
 *
 * The default always takes the first slot, because a setting should show what
 * most people will want without opening anything. The second slot is the next
 * option, unless the user has picked something else — then it is what they
 * picked, so the current choice is never hidden behind More.
 */
internal fun visibleChoices(options: List<Choice>, selected: String): List<Choice> {
    if (options.isEmpty()) return emptyList()
    val first = options[0]
    if (options.size == 1) return listOf(first)
    val second = options[1]
    val picked = options.firstOrNull { it.value == selected }
    val slot = if (picked == null || picked.value == first.value || picked.value == second.value) {
        second
    } else {
        picked
    }
    return listOf(first, slot)
}

/** One option in a single-choice setting. */
data class Choice(
    val value: String,
    val label: String,
    val note: String = "",
)

/**
 * A setting where one thing is chosen, shown as three chips.
 *
 * The rule is the same everywhere: the default, one other, and More. A settings
 * screen that shows twenty-five languages as twenty-five chips is not a choice,
 * it is a wall — the two that almost everyone wants are lost in it, and the
 * screen has to be scrolled to find a switch that should have been one line.
 *
 * So: the default first, one alternative second, and More opens the rest. The
 * second chip shows what is actually selected when that is neither of the first
 * two, so the current choice is always visible without opening anything.
 */
@Composable
fun ChoiceRow(
    label: String,
    options: List<Choice>,
    selected: String,
    onSelect: (String) -> Unit,
    moreLabel: String = "More",
    /** Extra rows at the top of the More sheet — a recording, an upload, a delete. */
    extra: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    if (options.isEmpty()) return
    var open by remember { mutableStateOf(false) }

    val shown = visibleChoices(options, selected)
    val first = shown[0]
    val second = shown.getOrNull(1)
    val current = options.firstOrNull { it.value == selected }

    Text(label, style = MaterialTheme.typography.bodyMedium)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = selected == first.value,
            onClick = { onSelect(first.value) },
            label = { Text(first.label) },
        )
        if (second != null) {
            FilterChip(
                selected = selected == second.value,
                onClick = { onSelect(second.value) },
                label = { Text(second.label) },
            )
        }
        FilterChip(
            selected = false,
            onClick = { open = true },
            label = { Text("$moreLabel (${options.size})") },
        )
    }
    // when the current choice is one of the hidden ones, say so — otherwise the
    // three chips above can all look unselected and the setting looks broken
    if (current != null && current.value != first.value && current.value != second?.value) {
        Text(
            "Now: ${current.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (current != null && current.note.isNotBlank()) {
        Text(
            current.note,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    extra?.let {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            it { open = false }
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 340.dp).padding(top = 8.dp)) {
                        items(options) { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(option.value)
                                        open = false
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = option.value == selected,
                                    onClick = {
                                        onSelect(option.value)
                                        open = false
                                    },
                                )
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text(option.label, style = MaterialTheme.typography.bodyLarge)
                                    if (option.note.isNotBlank()) {
                                        Text(
                                            option.note,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { open = false },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("Close") }
                }
            }
        }
    }
}

/**
 * A settings group that folds away.
 *
 * Everything in Settings was open at once, which made it a long scroll where the
 * thing you wanted was never where you looked. A folder with a name on it is
 * easier to scan than the same twelve controls laid out flat.
 */
@Composable
fun Folder(
    title: String,
    subtitle: String = "",
    initiallyOpen: Boolean = false,
    content: @Composable () -> Unit,
) {
    var open by remember { mutableStateOf(initiallyOpen) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { open = !open },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    if (open) "\u2303" else "\u2304",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (open) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { content() }
            }
        }
    }
}
