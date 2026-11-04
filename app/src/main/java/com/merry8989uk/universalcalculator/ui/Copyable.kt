package com.merry8989uk.universalcalculator.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Text a person can take out of the app.
 *
 * A formula on a screen is something people want to put in a message, a note or
 * a homework answer, and there was no way to get it out — long-press did nothing
 * because a plain Text is not selectable. Wrapping it in a SelectionContainer
 * gives the behaviour every web page has: hold to select, handles to extend the
 * selection, and Copy / Select all in the system toolbar.
 */
@Composable
fun Copyable(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    fontFamily: FontFamily? = null,
    color: Color = Color.Unspecified,
) {
    SelectionContainer(modifier = modifier) {
        Text(
            text = text,
            style = style,
            fontFamily = fontFamily,
            color = color,
        )
    }
}

/** Puts [text] on the clipboard. */
fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

/**
 * Hands [text] to whatever app the user picks.
 *
 * Started with NEW_TASK because the context here can be the application rather
 * than an activity, and starting an activity from one without the flag throws.
 */
fun shareText(context: Context, subject: String, text: String): Boolean = try {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(send, "Share").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
    )
    true
} catch (e: Exception) {
    false
}

/**
 * Copy and Share for one piece of text, with the confirmation shown in place.
 *
 * The confirmation matters: a copy button that gives no sign it worked is
 * indistinguishable from one that is broken, and the clipboard is invisible.
 */
@Composable
fun CopyShareRow(
    text: String,
    subject: String,
    modifier: Modifier = Modifier,
    onProblem: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(onClick = {
            copyToClipboard(context, subject, text)
            copied = true
        }) {
            Text(if (copied) "Copied" else "Copy", style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = {
            if (!shareText(context, subject, text)) onProblem("No app could take the text.")
        }) {
            Text("Share", style = MaterialTheme.typography.labelMedium)
        }
    }
    // the tick goes away on its own, so the row is not permanently shouting
    if (copied) {
        androidx.compose.runtime.LaunchedEffect(copied) {
            delay(1500)
            copied = false
        }
    }
}
