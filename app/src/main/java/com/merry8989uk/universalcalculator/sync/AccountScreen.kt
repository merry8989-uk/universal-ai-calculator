package com.merry8989uk.universalcalculator.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The account: one id, one password, and the data that follows them.
 *
 * The shape of the screen follows the order a person actually does things:
 *
 *   signed out — pick an id, pick a password, press the button
 *   signed in  — see who you are, back up, restore, delete, sign out
 *
 * Nothing here happens by itself. There is no background upload; a backup is a
 * button the user presses.
 *
 * Signing out and deleting are two different promises, so they are two different
 * buttons. Signing out forgets the account on this phone and leaves the
 * encrypted copy where it is; deleting removes the copy itself. Until now only
 * the first existed, which meant a user who wanted their data off the server had
 * nothing to press and no way to find that out.
 */
@Composable
fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var signedInId by remember { mutableStateOf(AccountStore.id(context)) }
    var message by remember { mutableStateOf("") }
    var messageIsBad by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    // the two forms
    var mode by remember { mutableStateOf(0) } // 0 log in, 1 create
    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // the password prompt for a backup or a restore
    var askingFor by remember { mutableStateOf("") }
    var askPassword by remember { mutableStateOf("") }
    var askShow by remember { mutableStateOf(false) }
    // one switch for everything that is the user's own working; see BackupContents
    var includeCalculations by remember { mutableStateOf(true) }
    // the delete is never one tap: this holds the dialog open until it is answered
    var confirmingDelete by remember { mutableStateOf(false) }

    fun say(text: String, bad: Boolean) {
        message = text
        messageIsBad = bad
    }

    fun run(block: suspend () -> AccountService.Outcome) {
        if (busy) return
        busy = true
        scope.launch {
            val outcome = block()
            busy = false
            when (outcome) {
                is AccountService.Outcome.Ok -> {
                    say(outcome.message, false)
                    signedInId = AccountStore.id(context)
                    password = ""
                    confirm = ""
                }
                is AccountService.Outcome.Failed -> say(outcome.message, true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Your account", style = MaterialTheme.typography.titleLarge)
        Text(
            if (signedInId.isBlank()) {
                "Everything stays on this phone until you sign in. Your calculations are never uploaded on their own."
            } else {
                "Your backup is encrypted on this phone before it is sent. The server cannot read it."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (message.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (messageIsBad) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                ),
            ) {
                Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (signedInId.isBlank()) {
            // ---------------- signed out ----------------
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                SegmentedButton(
                    selected = mode == 0,
                    onClick = { mode = 0; say("", false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Log in") }
                SegmentedButton(
                    selected = mode == 1,
                    onClick = { mode = 1; say("", false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Create account") }
            }

            OutlinedTextField(
                value = id,
                onValueChange = { id = it },
                label = { Text("Your id") },
                singleLine = true,
                isError = id.isNotBlank() && Account.problemWithId(id) != null,
                supportingText = {
                    Text(
                        if (id.isBlank()) {
                            "This is what you will type to get your data back."
                        } else {
                            Account.problemWithId(id) ?: "That id is fine."
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                    }
                },
                isError = password.isNotBlank() && Account.problemWithPassword(password) != null,
                supportingText = {
                    Text(
                        if (password.isBlank()) {
                            "At least ${Account.MIN_PASSWORD} characters. You will need it to get your data back."
                        } else {
                            Account.problemWithPassword(password) ?: "Good."
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (mode == 1) ImeAction.Next else ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            if (mode == 1) {
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Password again") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = confirm.isNotBlank() && Account.problemWithConfirmation(password, confirm) != null,
                    supportingText = {
                        if (confirm.isNotBlank()) {
                            Text(Account.problemWithConfirmation(password, confirm) ?: "The two match.")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            val ready = if (mode == 0) {
                Account.problemWithId(id) == null && password.isNotEmpty()
            } else {
                Account.problemWithId(id) == null &&
                    Account.problemWithPassword(password) == null &&
                    Account.problemWithConfirmation(password, confirm) == null
            }

            Button(
                onClick = {
                    if (mode == 0) {
                        run { AccountService.logIn(context, id, password) }
                    } else {
                        run { AccountService.signUp(context, id, password) }
                    }
                },
                enabled = ready && !busy,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text(
                    when {
                        busy -> "Please wait\u2026"
                        mode == 0 -> "Log in"
                        else -> "Create my account"
                    },
                )
            }

            Text(
                if (mode == 0) {
                    "Forgot the password? It cannot be reset — it is the key to your own data, and nobody else holds a copy."
                } else {
                    "Your id and password are the only way back to your data. Write them somewhere safe."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            // ---------------- signed in ----------------
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Signed in as", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "@" + signedInId,
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
                    )
                    val last = AccountStore.lastBackup(context)
                    Text(
                        if (last == 0L) "No backup has been made yet."
                        else "Last backup: " + whenAgo(last),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(BackupContents.SWITCH_TITLE, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        BackupContents.SWITCH_DETAIL,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.Switch(
                    checked = includeCalculations,
                    onCheckedChange = { includeCalculations = it },
                )
            }

            Button(
                onClick = { askingFor = "backup"; askPassword = "" },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Back up now") }

            OutlinedButton(
                onClick = { askingFor = "restore"; askPassword = "" },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Restore from this account") }

            // The way out that was missing. It sits above sign out and is coloured
            // as a warning, because it is the only button here that destroys
            // something — and what it destroys is not on this phone.
            OutlinedButton(
                onClick = { confirmingDelete = true },
                enabled = !busy,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Delete the backup on the server") }
            Text(
                "Removes the encrypted copy from the server. Your archive on this phone is not touched, and you stay signed in.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            TextButton(
                onClick = {
                    AccountService.signOut(context)
                    signedInId = ""
                    id = ""
                    password = ""
                    confirm = ""
                    say(
                        "Signed out on this phone. Your backup is still on the server — " +
                            "\"Delete the backup on the server\" removes it.",
                        false,
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Sign out on this phone") }
        }
    }

    // The confirmation names what goes, part by part, because "delete" on its own
    // is a word a user can agree to without knowing what they agreed to. There is
    // no password field: the password is the key to the data, not to the account,
    // and the device already holds what the server checks.
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete the backup on the server?") },
            text = {
                Column {
                    Text(
                        "This removes the encrypted copy from the server for good. What goes:",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text("\u2022 the archive: every calculation you have ever worked out", modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
                    Text("\u2022 the recent history", style = MaterialTheme.typography.bodySmall)
                    Text("\u2022 your saved calculators", style = MaterialTheme.typography.bodySmall)
                    Text("\u2022 your settings", style = MaterialTheme.typography.bodySmall)
                    Text(
                        // the list above is what a backup holds with the switch on;
                        // saying so keeps the list from promising more than the
                        // blob on the server actually contains
                        "With the switch above off, only the last two were ever sent.",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "It cannot be undone. Nobody can get it back — the server holds it encrypted and cannot read it, and no other copy exists. " +
                            "Your archive on this phone stays where it is, and you stay signed in, so you can back up again later.",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        run { AccountService.deleteBackup(context) }
                    },
                    enabled = !busy,
                ) { Text("Delete it", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Keep it") }
            },
        )
    }

    if (askingFor.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { askingFor = "" },
            title = { Text(if (askingFor == "backup") "Back up" else "Restore") },
            text = {
                Column {
                    Text(
                        "Type your password. It is what encrypts the backup, and the app does not keep it.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = askPassword,
                        onValueChange = { askPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (askShow) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { askShow = !askShow }) {
                                Text(if (askShow) "Hide" else "Show", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val which = askingFor
                        val typed = askPassword
                        askingFor = ""
                        if (which == "backup") {
                            run { AccountService.backUp(context, typed, includeCalculations) }
                        } else {
                            run { AccountService.restore(context, typed, includeCalculations) }
                        }
                    },
                    enabled = askPassword.isNotEmpty() && !busy,
                ) { Text(if (busy) "Please wait\u2026" else "Continue") }
            },
            dismissButton = {
                TextButton(onClick = { askingFor = "" }) { Text("Cancel") }
            },
        )
    }
}

/** "2 hours ago", for the last-backup line. */
private fun whenAgo(millis: Long): String {
    val seconds = (System.currentTimeMillis() - millis) / 1000
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60} minutes ago"
        seconds < 86_400 -> "${seconds / 3600} hours ago"
        seconds < 172_800 -> "yesterday"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
    }
}
