package com.merry8989uk.universalcalculator.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.merry8989uk.universalcalculator.R
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
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

    /**
     * Where the picture the user picked has got to.
     *
     * This used to be two loose strings and a busy flag, which is why a picked
     * picture had no state in which it was *visible but not yet sent*: the
     * launcher called the reader, the reader called `send`, and the user's only
     * sight of their own photo was the answer that came back about it. One
     * stage at a time, decided by [attachAfterPick] and [attachAfterRead], is
     * what puts a preview and an accept between the pick and the wire.
     */
    var stage by remember { mutableStateOf<AttachStage>(AttachStage.Idle) }

    /**
     * What is being held, so the same picture can be sent, looked at, and sent
     * again after a failed read without the user picking it a second time.
     *
     * A camera shot arrives as a bitmap and is compressed at the moment of
     * sending; a picked file arrives as a Uri and is read from the resolver at
     * the same moment. Neither is touched until the user presses Send, and
     * neither is cleared by a failed read — that is what makes Try again a tap
     * rather than a walk back to the camera.
     */
    var heldBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var heldUri by remember { mutableStateOf<Uri?>(null) }

    /** The text and label of an attachment the user has accepted, sent with the question. */
    var attachText by remember { mutableStateOf("") }
    var attachLabel by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sending = stage is AttachStage.Sending

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

    /** Drops the held picture without sending it. The only thing that removes it. */
    fun dropHeld() {
        heldBitmap = null
        heldUri = null
        stage = AttachStage.Idle
    }

    /**
     * Sends the picture that is on screen, and nothing else.
     *
     * Reached from Send in the preview and from Try again after a failed read —
     * never from a launcher, which is what "no auto-send" means here. The switch
     * is asked again rather than trusted from the moment of the pick, because a
     * preview can sit on screen while the switch is turned off, and the answer
     * that counts is the one given at the moment of sending.
     */
    fun sendHeld() {
        if (!attachCanSend(stage)) return
        val kind = stage.kind()
        val label = stage.labelOf()
        val decision = uploadDecision(savingEnabled)
        if (decision is UploadDecision.Refused) {
            // The picture is still here, so this is a failure the user can come
            // back from: turn the switch on and press Try again.
            stage = AttachStage.Failed(kind, label, decision.message, keepsPicture = true)
            return
        }
        stage = AttachStage.Sending(kind, label)
        val bitmap = heldBitmap
        val uri = heldUri
        val asked = question.ifBlank {
            "Read the questions in this picture and solve them one by one, starting at question 1."
        }
        scope.launch {
            val read = when {
                // A photo taken here is something to talk about, never a sum to
                // be mined: a picture of an exam paper is a question, and
                // reading the first arithmetic on the page gives the marks line
                // in its header.
                bitmap != null -> {
                    // The shot is compressed here, not when it was taken, so a
                    // picture the user cancels never becomes a JPEG at all.
                    val bytes = ByteArrayOutputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        out.toByteArray()
                    }
                    val reply = AiClient.answerImage(bytes, asked)
                    if (reply != null) {
                        Attachments.Read("image", label, reply)
                    } else {
                        Attachments.Read("image", label, "", "Could not answer that from the photo.")
                    }
                }
                uri != null -> Attachments.read(context, uri, savingEnabled = savingEnabled, question = asked)
                else -> Attachments.Read(kind, label, "", "The picture is no longer here. Pick it again.")
            }
            // What the read did to the screen is decided by attachAfterRead, not
            // here: a good read becomes text the user can accept, and a bad one
            // becomes a failure that still holds the picture.
            stage = attachAfterRead(
                picked = AttachStage.Sending(kind, label),
                text = read.text,
                error = read.error,
                label = read.label,
            )
        }
    }

    /**
     * Reads a picked file that is not a picture.
     *
     * There is nothing to look at before it is read — an audio clip or a PDF has
     * no preview — so these go straight to the reader. What changed is what
     * happens after: the text comes back to the screen for the user to accept,
     * where it used to be posted as a question on their behalf.
     */
    fun readFile(uri: Uri, kind: String, label: String) {
        // Held, like a picture, so a failed read can be tried again without
        // opening the picker a second time.
        heldBitmap = null
        heldUri = uri
        val decision = uploadDecision(savingEnabled)
        if (decision is UploadDecision.Refused) {
            stage = AttachStage.Failed(kind, label, decision.message, keepsPicture = true)
            return
        }
        stage = AttachStage.Sending(kind, label)
        val asked = question.ifBlank {
            "Read the questions in this picture and solve them one by one, starting at question 1."
        }
        scope.launch {
            val read = Attachments.read(context, uri, savingEnabled = savingEnabled, question = asked)
            stage = attachAfterRead(
                picked = AttachStage.Sending(kind, label),
                text = read.text,
                error = read.error,
                label = read.label,
            )
        }
    }

    /**
     * Takes what the model read and puts it where the user would have typed it.
     *
     * Nothing is sent by this: an accepted picture's text lands in the question
     * box for the user to read, correct and ask with, and an accepted file
     * becomes the context the next question is asked against.
     */
    fun acceptRead(s: AttachStage.Showing) {
        if (readTarget(s.kind) == ReadTarget.INPUT) {
            question = s.text
        } else {
            attachText = s.text
            attachLabel = s.label
        }
        dropHeld()
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // A picked file is the user's own picture, recording or page, and the
        // reading below posts it, so the switch is asked before the file is
        // opened. A refusal is shown where the picture would have been listed:
        // nothing is read, and nothing is sent.
        val decision = uploadDecision(savingEnabled)
        if (decision is UploadDecision.Refused) {
            stage = AttachStage.Failed("file", "file", decision.message, keepsPicture = false)
            return@rememberLauncherForActivityResult
        }
        // One pick replaces whatever was held before, so a cancelled preview
        // cannot be the thing a later Send sends.
        heldBitmap = null
        heldUri = null
        val type = context.contentResolver.getType(uri).orEmpty()
        if (type.startsWith("image/")) {
            // A picture is held, not read: what the model would be sent is shown
            // first, and the user decides whether it goes.
            heldBitmap = previewOf(context, uri)
            heldUri = uri
            stage = attachAfterPick("image", "photo")
        } else {
            readFile(uri, "file", uri.lastPathSegment ?: "file")
        }
    }

    val camera = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bmp: Bitmap? ->
        if (bmp == null) return@rememberLauncherForActivityResult
        // The camera button used to post its picture without asking anything.
        // The switch is asked before the bitmap is even held, so a refusal costs
        // no work and opens no connection.
        val decision = uploadDecision(savingEnabled)
        if (decision is UploadDecision.Refused) {
            stage = AttachStage.Failed("image", "photo", decision.message, keepsPicture = false)
            return@rememberLauncherForActivityResult
        }
        heldBitmap = bmp
        heldUri = null
        // Held for the preview; the bytes are made when the user presses Send.
        stage = attachAfterPick("image", "photo")
    }

    // Pressing the Ask AI tab again clears the screen back to the start.
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            question = ""; answer = ""; attachText = ""; attachLabel = ""
            heldBitmap = null; heldUri = null; stage = AttachStage.Idle
            used = false
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
                enabled = !sending && !loading,
            ) { Text("\uD83D\uDCF7 " + stringResource(R.string.photo)) }
            OutlinedButton(
                onClick = { filePicker.launch(
                    arrayOf("image/*", "audio/*", "text/*", "application/json", "application/pdf"),
                ) },
                enabled = !sending && !loading,
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

        when (val s = stage) {
            // Nothing picked: no card, and nothing to send.
            AttachStage.Idle -> {}

            // The picture, on screen, before a byte of it leaves the phone.
            is AttachStage.Previewing -> AttachCard {
                Text(
                    stringResource(R.string.attach_preview_not_sent),
                    style = MaterialTheme.typography.bodyMedium,
                )
                heldBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = stringResource(R.string.attach_preview_a11y),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { sendHeld() }) { Text(stringResource(R.string.attach_send)) }
                    OutlinedButton(onClick = { dropHeld() }) {
                        Text(stringResource(R.string.attach_cancel))
                    }
                }
            }

            is AttachStage.Sending -> Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.attach_read))
            }

            // What the model read, offered rather than acted on. The user can
            // see whether it is right before it becomes anything.
            is AttachStage.Showing -> AttachCard {
                Text(
                    stringResource(R.string.attach_read_result, s.label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                SelectionContainer {
                    Text(
                        s.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { acceptRead(s) }) {
                        Text(
                            stringResource(
                                if (readTarget(s.kind) == ReadTarget.INPUT) {
                                    R.string.attach_use_text
                                } else {
                                    R.string.attach_use_file
                                },
                            ),
                        )
                    }
                    OutlinedButton(onClick = { dropHeld() }) {
                        Text(stringResource(R.string.attach_discard))
                    }
                }
            }

            // A failed read says what failed and keeps what it was reading, so
            // the drawing or the photo is not lost to one bad attempt.
            is AttachStage.Failed -> AttachCard(error = true) {
                Text(
                    s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (s.keepsPicture) {
                    Text(
                        stringResource(R.string.attach_failed_kept),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (attachCanSend(s)) {
                        OutlinedButton(onClick = { sendHeld() }) {
                            Text(stringResource(R.string.attach_try_again))
                        }
                    }
                    OutlinedButton(onClick = { dropHeld() }) {
                        Text(stringResource(R.string.attach_discard))
                    }
                }
            }
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
            // A picture that is on screen but not yet read is not something to
            // ask about: Send in the card above is what puts it to the model.
            enabled = !loading && !sending && !attachCanSend(stage) &&
                (question.isNotBlank() || attachText.isNotBlank()),
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
 * The card that holds whatever the attach row is currently doing.
 *
 * One shape for the preview, the read text and the failure, so the row under
 * the camera buttons reads as one place where a picture is in progress rather
 * than three unrelated pieces of the screen.
 */
@Composable
private fun AttachCard(
    error: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        colors = if (error) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        },
    ) {
        Column(Modifier.padding(14.dp), content = content)
    }
}

/**
 * A downscaled copy of a picked picture, for the preview only.
 *
 * The preview is shown to the user, not sent, so it is decoded small on purpose:
 * a 12-megapixel photo would cost a lot of memory to put on a 200 dp card, and
 * the picture that is sent is read again at full size from the same Uri.
 * Null means the file would not decode, in which case the card still explains
 * itself and Send is still offered — the reader may well cope with a format the
 * preview could not.
 */
private fun previewOf(context: android.content.Context, uri: Uri): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 1080 || bounds.outHeight / sample > 1080) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
} catch (e: Exception) {
    null
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

/**
 * Where a picked picture is on its way to the model.
 *
 * The four stages that matter are the ones the user can see: a picture held and
 * shown ([Previewing]), a request in flight ([Sending]), text the model read and
 * is offering ([Showing]), and a read that failed ([Failed]). They are a sealed
 * type so a screen cannot be in two of them at once — the bug this replaces was
 * a screen that went from "picked" to "asked" with no stage in between, which is
 * exactly what made the picture and the reading of it invisible.
 *
 * [Failed] carries [keepsPicture] because two very different failures look the
 * same from the outside: the switch refusing to let the picture go (nothing was
 * held, so there is nothing to try again) and the model failing to read a page
 * that is still in hand (everything is still held, so Try again is one tap).
 */
internal sealed interface AttachStage {

    /** Nothing is picked. Nothing will leave the phone. */
    data object Idle : AttachStage

    /** A picture is held and shown. It has not been sent. */
    data class Previewing(val kind: String, val label: String) : AttachStage

    /** The switch allowed it and the picture is with the model. */
    data class Sending(val kind: String, val label: String) : AttachStage

    /** The model's text is held for the user to accept. Nothing was sent by it. */
    data class Showing(val kind: String, val label: String, val text: String) : AttachStage

    /** The read failed. [keepsPicture] says whether Try again has anything to send. */
    data class Failed(
        val kind: String,
        val label: String,
        val message: String,
        val keepsPicture: Boolean,
    ) : AttachStage
}

/** Where the text a read produced belongs when the user accepts it. */
internal enum class ReadTarget {
    /** Into the question box, because a picture's reading is what the user would have typed. */
    INPUT,

    /** Kept as the context the next question is asked against, as a file always was. */
    ATTACHMENT,
}

/** The kind of thing a stage is holding, or "" when it is holding nothing. */
internal fun AttachStage.kind(): String = when (this) {
    AttachStage.Idle -> ""
    is AttachStage.Previewing -> kind
    is AttachStage.Sending -> kind
    is AttachStage.Showing -> kind
    is AttachStage.Failed -> kind
}

/** What the stage should call the thing it is holding. */
internal fun AttachStage.labelOf(): String = when (this) {
    AttachStage.Idle -> ""
    is AttachStage.Previewing -> label
    is AttachStage.Sending -> label
    is AttachStage.Showing -> label
    is AttachStage.Failed -> label
}

/**
 * A picture was picked: hold it, and show it.
 *
 * The one thing this must never return is [AttachStage.Sending]. Picking a photo
 * is not a decision to send it, and treating it as one is what put a thumbnail
 * on the wire before the user had seen it.
 */
internal fun attachAfterPick(kind: String, label: String): AttachStage =
    AttachStage.Previewing(kind, label)

/**
 * What a read did to the screen.
 *
 * A read that came back with text leaves the screen showing it, where the user
 * can accept or discard it; nothing is asked on their behalf. A read that failed
 * keeps whatever [picked] was holding, so the same picture can go again without
 * being picked a second time — the drawing is not lost to one bad attempt.
 */
internal fun attachAfterRead(
    picked: AttachStage,
    text: String,
    error: String?,
    label: String = "",
): AttachStage {
    val kind = picked.kind()
    val name = label.ifBlank { picked.labelOf() }
    val holdsSomething = picked is AttachStage.Previewing ||
        picked is AttachStage.Sending ||
        (picked is AttachStage.Failed && picked.keepsPicture)
    return if (error == null && text.isNotBlank()) {
        AttachStage.Showing(kind, name, text)
    } else {
        AttachStage.Failed(
            kind = kind,
            label = name,
            message = error?.takeIf { it.isNotBlank() } ?: "Could not read that.",
            keepsPicture = holdsSomething,
        )
    }
}

/** Whether there is a picture in hand to send or send again. */
internal fun attachCanSend(stage: AttachStage): Boolean =
    stage is AttachStage.Previewing || (stage is AttachStage.Failed && stage.keepsPicture)

/**
 * Where an accepted read goes.
 *
 * A photo or a sketch is read to text the user would otherwise retype, so it
 * belongs in the question box. An audio clip, a PDF or a text file is already
 * text and is context for the question rather than the question itself, which is
 * how the screen treated it before.
 */
internal fun readTarget(kind: String): ReadTarget =
    if (kind == "image") ReadTarget.INPUT else ReadTarget.ATTACHMENT
