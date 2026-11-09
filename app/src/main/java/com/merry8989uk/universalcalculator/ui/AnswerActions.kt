package com.merry8989uk.universalcalculator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.merry8989uk.universalcalculator.R
import androidx.compose.ui.unit.dp

/**
 * What can be done with an answer once it is on screen.
 *
 * Copy and Share take the text out of the app; Clear drops the answer from it;
 * Insert puts the answer back where it came from, on the calculator's own
 * expression line, so a result can be carried into the next sum instead of
 * retyped. The answer text itself is put inside a SelectionContainer where it is
 * shown, so it can also be selected by hand.
 *
 * Insert is a callback rather than an action here, and nullable rather than
 * defaulted to nothing, because this row is drawn in places that have no
 * expression to put anything into: a screen without one passes nothing and gets
 * no button, instead of a button that does nothing.
 */
@Composable
fun AnswerActions(
    text: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    /** Puts [text] — or the part of it the host chooses — on the expression line. */
    onInsert: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val insert = onInsert

    fun copy() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Nexus Calc answer", text))
        Toast.makeText(context, context.getString(R.string.copy), Toast.LENGTH_SHORT).show()
    }

    fun share() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "Share answer"))
    }

    Row(
        modifier = modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (insert != null) {
            OutlinedButton(onClick = insert) { Text(stringResource(R.string.insert)) }
        }
        OutlinedButton(onClick = { copy() }) { Text(stringResource(R.string.copy)) }
        OutlinedButton(onClick = { share() }) { Text(stringResource(R.string.share)) }
        if (onClear != null) {
            OutlinedButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
        }
    }
}
