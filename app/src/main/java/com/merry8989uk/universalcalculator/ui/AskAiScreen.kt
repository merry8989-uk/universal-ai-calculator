package com.merry8989uk.universalcalculator.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.merry8989uk.universalcalculator.R
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.ai.AiClient
import com.merry8989uk.universalcalculator.ai.AnswerFormat
import com.merry8989uk.universalcalculator.ai.Attachments
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/**
 * "Ask AI" — ask anything, or attach a photo, an image, an audio clip or a text
 * file and get it solved. The key is fixed on the server, so there is nothing
 * for the user to configure.
 */
@Composable
fun AskAiScreen(
    contextText: String,
    onClearContext: () -> Unit,
    enabled: Boolean = true,
    initialQuestion: String = "",
    onInitialQuestionUsed: () -> Unit = {},
    resetSignal: Int = 0,
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var used by remember { mutableStateOf(false) }

    var attachBusy by remember { mutableStateOf(false) }
    var attachLabel by remember { mutableStateOf("") }
    var attachText by remember { mutableStateOf("") }
    var attachError by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun send(q: String) {
        if (loading) return
        val full = Attachments.contextFor(attachLabel, attachText, q)
        if (full.isBlank()) return
        loading = true
        answer = ""
        used = true
        scope.launch {
            answer = AnswerFormat.clean(AiClient.ask(full, contextText))
            loading = false
        }
    }

    fun handleRead(r: Attachments.Read) {
        attachBusy = false
        if (r.error != null) {
            attachError = r.error
            attachText = ""
            attachLabel = ""
        } else {
            attachError = ""
            attachText = r.text
            attachLabel = r.label
            // give the solution straight away
            send(question)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        attachBusy = true
        attachError = ""
        scope.launch { handleRead(Attachments.read(context, uri, question)) }
    }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bmp: Bitmap? ->
        if (bmp == null) return@rememberLauncherForActivityResult
        attachBusy = true
        attachError = ""
        scope.launch {
            val bytes = ByteArrayOutputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.toByteArray()
            }
            // With a question typed, the photo is answered rather than mined
            // for a sum — a picture of an exam paper is a question, not an
            // expression.
            if (question.isNotBlank()) {
                val answer = AiClient.answerImage(bytes, question)
                handleRead(
                    if (answer != null) Attachments.Read("image", "photo", answer)
                    else Attachments.Read("image", "photo", "", "Could not answer that from the photo."),
                )
            } else {
                val r = AiClient.solveImage(bytes)
                handleRead(
                    if (r.expression != null) {
                        Attachments.Read(
                            "image",
                            if (r.lowConfidence) "photo (please check)" else "photo",
                            "The photo shows this calculation: ${r.expression}" +
                                if (r.lowConfidence) " (read with low confidence, please check it)" else "",
                        )
                    } else {
                        Attachments.Read("image", "photo", "", r.error ?: "Could not read the photo.")
                    },
                )
            }
        }
    }

    // Pressing the Ask AI tab again clears the screen back to the start.
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            question = ""; answer = ""; attachText = ""; attachLabel = ""
            attachError = ""; attachBusy = false; used = false
        }
    }

    // A question handed over from the calculator (voice or photo) is asked here.
    LaunchedEffect(initialQuestion, enabled) {
        if (enabled && initialQuestion.isNotBlank()) {
            question = initialQuestion
            send(initialQuestion)
            onInitialQuestionUsed()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        Text(stringResource(R.string.tab_ask_ai), style = MaterialTheme.typography.titleLarge)

        if (!enabled) {
            Text(
                stringResource(R.string.ai_off),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
            return@Column
        }

        Text(
            stringResource(R.string.ask_anything),
            style = MaterialTheme.typography.bodySmall,
        )

        if (contextText.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.context_label, contextText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClearContext) { Text(stringResource(R.string.clear)) }
            }
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(stringResource(R.string.your_question)) },
            // A tap on the cross empties the question box.
            trailingIcon = {
                if (question.isNotEmpty()) {
                    val clearSpoken = stringResource(R.string.clear_the_question_a11y)
                    IconButton(
                        onClick = { question = "" },
                        modifier = Modifier.semantics { contentDescription = clearSpoken },
                    ) {
                        Text("\u2715", style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )

        // ---- attach ----
        Text(
            stringResource(R.string.attach_something),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 14.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { camera.launch(null) },
                enabled = !attachBusy && !loading,
            ) { Text("\uD83D\uDCF7 " + stringResource(R.string.photo)) }
            OutlinedButton(
                onClick = { filePicker.launch(
                    arrayOf("image/*", "audio/*", "text/*", "application/json", "application/pdf"),
                ) },
                enabled = !attachBusy && !loading,
            ) { Text("\uD83D\uDCCE " + stringResource(R.string.file)) }
        }
        Text(
            stringResource(R.string.attach_help),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (attachBusy) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.attach_read))
            }
        }
        if (attachError.isNotBlank()) {
            Text(
                attachError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (attachLabel.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.attached, attachLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { attachText = ""; attachLabel = "" }) { Text(stringResource(R.string.remove)) }
            }
        }

        Button(
            onClick = { send(question) },
            enabled = !loading && !attachBusy && (question.isNotBlank() || attachText.isNotBlank()),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            Text(
                if (loading) stringResource(R.string.thinking) else stringResource(R.string.tab_ask_ai),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (loading) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.working_it_out))
            }
        }

        if (answer.isNotBlank()) {
            Text(stringResource(R.string.answer), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp))
            // Selectable, so it can be copied by hand as well.
            val answerSpoken = stringResource(R.string.assistant_answer_a11y, answer)
            SelectionContainer {
                Text(answer, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = answerSpoken
                        })
            }
            AnswerActions(answer, onClear = { answer = "" })
        }
    }
}
