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
 * Copy and Share for an answer. The answer text itself is put inside a
 * SelectionContainer where it is shown, so it can also be selected by hand.
 */
@Composable
fun AnswerActions(
    text: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    val context = LocalContext.current

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
        OutlinedButton(onClick = { copy() }) { Text(stringResource(R.string.copy)) }
        OutlinedButton(onClick = { share() }) { Text(stringResource(R.string.share)) }
        if (onClear != null) {
            OutlinedButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
        }
    }
}
