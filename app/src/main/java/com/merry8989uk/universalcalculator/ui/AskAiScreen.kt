package com.merry8989uk.universalcalculator.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.merry8989uk.universalcalculator.ai.AskFailure
import com.merry8989uk.universalcalculator.ai.Attachments
import com.merry8989uk.universalcalculator.ai.UploadDecision
import com.merry8989uk.universalcalculator.ai.askOutcome
import com.merry8989uk.universalcalculator.ai.uploadDecision
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
    savingEnabled: Boolean = true,
    onOpenArchive: () -> Unit = {},
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    /**
     * The conversation so far, oldest first: "user" or "assistant" to the text.
     *
     * This is what makes the screen a chat rather than a search box.
     */
    var turns by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    /**
     * When this conversation began.
     *
     * Held for the whole conversation so every save overwrites the same file —
     * a chat is one file that grows, not a new file per message.
     */
    var chatStarted by remember { mutableStateOf<java.time.LocalDateTime?>(null) }
    var chatModel by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var used by remember { mutableStateOf(false) }

    /**
     * The card to show when the last question was not answered.
     *
     * Held apart from [turns] on purpose. A failure is not something the
     * assistant said, so it is not a turn: it cannot be drawn as the latest
     * answer, sent back as history, or written to the archive.
     */
    var failure by remember { mutableStateOf<AskFailure?>(null) }

    /** The question of the last attempt, so Retry can ask exactly that again. */
    var lastAsked by remember { mutableStateOf("") }

    var attachBusy by remember { mutableStateOf(false) }
    var attachLabel by remember { mutableStateOf("") }
    var attachText by remember { mutableStateOf("") }
    var attachError by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    /**
     * Asks the assistant and puts the result where it belongs.
     *
     * A Retry comes back through here with [newTurn] false: the question is
     * already on screen, and asking it again must not say it twice. What is done
     * with the result is decided by [askOutcome] rather than here, so the rule
     * that only an answer is kept can be tested without a network.
     */
    fun send(q: String, newTurn: Boolean = true) {
        if (loading) return
        val asked = q.ifBlank { "Read the questions in this picture and solve them one by one, starting at question 1." }
        val full = Attachments.contextFor(attachLabel, attachText, asked)
        if (full.isBlank()) return
        loading = true
        answer = ""
        used = true
        failure = null
        lastAsked = asked
        // A retry leaves the question where it is: the history sent with it is
        // the conversation before that question, not one that already contains
        // it twice.
        val so_far = if (newTurn) turns else turns.dropLastWhile { it.first == "user" }
        if (newTurn) turns = so_far + ("user" to asked)
        scope.launch {
            val outcome = askOutcome(AiClient.ask(full, contextText, so_far), asked)
            loading = false
            val turn = outcome.turn
            if (turn == null) {
                // Nothing was said, so nothing is added and nothing is saved.
                // The card says what happened and offers the question again.
                failure = outcome.failure
            } else {
                answer = turn.second
                turns = turns + turn
                // Kept in the chat archive — never in the calculation archive.
                // Only an answer reaches this line; a failure has no turn, so
                // there is nothing here for it to archive.
                if (savingEnabled) {
                    if (outcome.saveToArchive) {
                        val started = chatStarted ?: java.time.LocalDateTime.now().also { chatStarted = it }
                        val saved = com.merry8989uk.universalcalculator.calc.SavedChat(
                            title = com.merry8989uk.universalcalculator.calc.ChatFormat.titleOf(
                                com.merry8989uk.universalcalculator.data.ChatStore.turns(turns),
                            ),
                            started = started.format(
                                java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"),
                            ),
                            model = chatModel,
                            turns = com.merry8989uk.universalcalculator.data.ChatStore.turns(turns),
                        )
                        com.merry8989uk.universalcalculator.data.ChatStore.save(context, started, saved)
                    }
                }
            }
        }
    }

    /** Asks the question that failed once more, without adding a second copy of it. */
    fun retry() {
        val asked = lastAsked
        if (asked.isBlank()) return
        send(asked, newTurn = false)
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
        // A picked file is the user's own picture, recording or page, and the
        // reading below posts it, so the switch is asked before the file is
        // opened. A refusal is shown where the attachment would have been
        // listed: nothing is read, and nothing is sent.
        val decision = uploadDecision(savingEnabled)
        if (decision is UploadDecision.Refused) {
            attachError = decision.message
            attachText = ""
            attachLabel = ""
            return@rememberLauncherForActivityResult
        }
        attachBusy = true
        attachError = ""
        // An attachment in the assistant is something to talk about, so a
        // question is always sent with it. Without one, a question paper used to
        // go through the expression reader and come back as the marks line in
        // its header.
        scope.launch {
            handleRead(
                Attachments.read(
                    context,
                    uri,
                    savingEnabled = savingEnabled,
                    question = question.ifBlank {
                        "Read the questions in this picture and solve them one by one, starting at question 1."
                    },
                ),
            )
        }
    }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bmp: Bitmap? ->
        if (bmp == null) return@rememberLauncherForActivityResult
        // The camera button used to post its picture without asking anything.
        // The switch is asked before the bitmap is compressed, so a refusal
        // costs no work and opens no connection.
        val decision = uploadDecision(savingEnabled)
        if (decision is UploadDecision.Refused) {
            attachError = decision.message
            attachText = ""
            attachLabel = ""
            return@rememberLauncherForActivityResult
        }
        attachBusy = true
        attachError = ""
        scope.launch {
            val bytes = ByteArrayOutputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.toByteArray()
            }
            // A photo taken here is something to talk about, never a sum to be
            // mined: a picture of an exam paper is a question, and reading the
            // first arithmetic on the page gives the marks line in its header.
            val asked = question.ifBlank { "Read the questions in this picture and solve them one by one, starting at question 1." }
            val reply = AiClient.answerImage(bytes, asked)
            handleRead(
                if (reply != null) Attachments.Read("image", "photo", reply)
                else Attachments.Read("image", "photo", "", "Could not answer that from the photo."),
            )
        }
    }

    // Pressing the Ask AI tab again clears the screen back to the start.
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            question = ""; answer = ""; attachText = ""; attachLabel = ""
            attachError = ""; attachBusy = false; used = false
            turns = emptyList(); chatStarted = null; failure = null; lastAsked = ""
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
        // Said here, under the camera, and not only on a policy page nobody
        // opens: the picture leaves the phone, so the user is told where they
        // attach it rather than afterwards.
        Text(
            stringResource(R.string.attach_sent_notice),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
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

        // ---- the conversation ----
        if (turns.isNotEmpty() || failure != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (turns.count { it.first == "user" } > 1) "Conversation" else stringResource(R.string.answer),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenArchive) { Text("Chats") }
                TextButton(onClick = {
                    turns = emptyList()
                    answer = ""
                    question = ""
                    chatStarted = null
                    failure = null
                }) { Text("New chat") }
            }

            // A failure is not a turn, so it is never drawn as the answer and
            // never becomes part of what the next question is asked against.
            val answered = failure == null

            // Earlier turns stay on screen, dimmer, so a follow-up reads in
            // context rather than out of nowhere.
            val earlier = if (answered) turns.dropLast(1) else turns
            earlier.forEach { (role, text) ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (role == "user") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            val latest = if (answered) turns.lastOrNull() else null
            if (latest != null) {
                val latestSpoken = stringResource(R.string.assistant_answer_a11y, latest.second)
                Spacer(Modifier.height(10.dp))
                SelectionContainer {
                    Text(
                        latest.second,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = latestSpoken
                            },
                    )
                }
                AnswerActions(latest.second, onClear = {
                    turns = emptyList()
                    answer = ""
                    chatStarted = null
                    failure = null
                })
            } else {
                // The question stays where the user typed it and the card sits
                // where the answer would have been.
                failure?.let { notAnswered ->
                    Spacer(Modifier.height(10.dp))
                    AiFailureCard(notAnswered, onRetry = { retry() })
                }
            }
        }
    }
}

/**
 * The card shown when a question was not answered.
 *
 * It sits in the conversation but is not part of it: there is no turn behind it,
 * so it is never saved, never read back as history, and disappears the moment
 * the question is answered. Retry re-asks the same question, because the
 * question is not what went wrong.
 */
@Composable
private fun AiFailureCard(failure: AskFailure, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                failure.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            // The id is the one the failed attempt sent as X-Request-Id, so a
            // report can be matched to the line in the log rather than guessed
            // at from a timestamp.
            Text(
                "Request " + failure.requestId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(top = 6.dp),
            )
            OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 10.dp)) {
                Text("Retry")
            }
        }
    }
}
