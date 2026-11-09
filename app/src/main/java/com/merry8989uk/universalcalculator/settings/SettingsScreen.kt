package com.merry8989uk.universalcalculator.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.merry8989uk.universalcalculator.ui.ArchiveScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.merry8989uk.universalcalculator.R
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.merry8989uk.universalcalculator.BuildConfig
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsScreen
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsSection
import com.merry8989uk.universalcalculator.sound.ToneRecorder
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.merry8989uk.universalcalculator.sound.ToneLibrary
import com.merry8989uk.universalcalculator.sound.ToneStore
import com.merry8989uk.universalcalculator.ui.theme.CustomTheme
import com.merry8989uk.universalcalculator.ui.theme.ThemeBuilderScreen
import com.merry8989uk.universalcalculator.ui.theme.ThemeStore
import com.merry8989uk.universalcalculator.sound.SoundPlayer
import com.merry8989uk.universalcalculator.ui.UpdatePrompt
import com.merry8989uk.universalcalculator.updater.UpdateUiState
import com.merry8989uk.universalcalculator.updater.UpdateViewModel
import com.merry8989uk.universalcalculator.updater.updatePromptVisible

/** Display names, localised. The stored value stays the enum's own label. */
@Composable
private fun themeName(theme: com.merry8989uk.universalcalculator.ui.theme.AppTheme): String =
    when (theme) {
        com.merry8989uk.universalcalculator.ui.theme.AppTheme.CYBERPUNK -> stringResource(R.string.theme_cyberpunk)
        com.merry8989uk.universalcalculator.ui.theme.AppTheme.EMERALD -> stringResource(R.string.theme_emerald)
        com.merry8989uk.universalcalculator.ui.theme.AppTheme.SUNSET -> stringResource(R.string.theme_sunset)
        com.merry8989uk.universalcalculator.ui.theme.AppTheme.MIDNIGHT -> stringResource(R.string.theme_midnight)
        com.merry8989uk.universalcalculator.ui.theme.AppTheme.DYNAMIC -> stringResource(R.string.theme_dynamic)
    }

/** What a tone is called on its chip. */
@Composable
private fun toneName(choice: String): String = when (choice) {
    ToneLibrary.OFF -> stringResource(R.string.sound_off)
    "Click" -> stringResource(R.string.sound_click)
    "Bubble" -> stringResource(R.string.sound_bubble)
    "Pop" -> stringResource(R.string.sound_pop)
    else -> choice
}

/** Every language offered, with "follow the phone" and English at the front. */
private val LANGUAGES: List<String> =
    listOf("system", "en") + com.merry8989uk.universalcalculator.locale.Languages.ALL.map { it.tag }

/**
 * The name of a language.
 *
 * It is written in its own script, with the English name beside it, so a Tamil
 * speaker can find தமிழ் and everybody else can read Tamil.
 */
@Composable
private fun languageName(code: String): String = when (code) {
    "system" -> stringResource(R.string.lang_system)
    "en" -> "English"
    else -> com.merry8989uk.universalcalculator.locale.Languages.byTag(code)
        ?.let { it.native + "  \u00B7  " + it.english }
        ?: code
}

/**
 * Rich, fully-wired Settings screen. Every control writes through the view
 * model to DataStore and reads back from the same Flow (Rule 4). Ends with the
 * version + @osintgram_io contact, and carries the in-app update control
 * (Rule 6).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
    updater: UpdateViewModel = viewModel(),
    resetSignal: Int = 0,
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scroll = rememberScrollState()
    // The archive is opened from here, so it sits with the other settings
    // rather than among the formulas.
    var showArchive by remember { mutableStateOf(false) }
    // Diagnostics is its own screen now: the log, the counters, the ping and the
    // export were one long column in here, which made Settings a debugging screen.
    var showDiagnostics by remember { mutableStateOf(false) }
    // The account screen, and who is signed in — re-read whenever it closes.
    var showAccount by remember { mutableStateOf(false) }
    // Themes the user built, and the builder for one of them.
    var customThemes by remember { mutableStateOf(ThemeStore.load(context)) }
    var showThemeBuilder by remember { mutableStateOf(false) }
    var editingTheme by remember { mutableStateOf<CustomTheme?>(null) }
    if (showThemeBuilder) {
        ThemeBuilderScreen(
            editing = editingTheme,
            onSave = { made ->
                customThemes = ThemeStore.put(context, made)
                vm.setCustomThemeId(ThemeStore.load(context).firstOrNull { it.name == made.name }?.id ?: made.id)
                showThemeBuilder = false
                editingTheme = null
            },
            onBack = { showThemeBuilder = false; editingTheme = null },
        )
        return
    }
    // The file picker for a custom key tone.
    var pickTone by remember { mutableStateOf(false) }
    var toneProblem by remember { mutableStateOf("") }
    val pickToneFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val saved = ToneStore.copyIn(context, uri)
        if (saved == null) {
            toneProblem = ToneStore.lastProblem
        } else {
            toneProblem = ""
            vm.setCustomTonePath(saved)
            vm.setKeySound(ToneLibrary.CUSTOM)
            SoundPlayer.setCustomPath(saved)
            SoundPlayer.setChoice(ToneLibrary.CUSTOM)
            SoundPlayer.key()
        }
    }
    LaunchedEffect(pickTone) {
        if (pickTone) {
            pickToneFile.launch(arrayOf("audio/*"))
            pickTone = false
        }
    }
    // Recording a tone: one press listens for about a second and keeps it.
    val toneScope = rememberCoroutineScope()
    var recording by remember { mutableStateOf(false) }
    val askForMic = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (!granted) {
            toneProblem = "Microphone permission was refused, so nothing can be recorded."
        }
    }
    var signedInId by remember { mutableStateOf("") }
    LaunchedEffect(showAccount) {
        signedInId = com.merry8989uk.universalcalculator.sync.AccountStore.id(context)
    }
    if (showAccount) {
        com.merry8989uk.universalcalculator.sync.AccountScreen(
            onBack = { showAccount = false },
        )
        return
    }

    if (showDiagnostics) {
        DiagnosticsScreen(
            enabled = state.diagnosticsEnabled,
            onEnabledChange = vm::setDiagnosticsEnabled,
            onBack = { showDiagnostics = false },
        )
        return
    }

    if (showArchive) {
        ArchiveScreen(onBack = { showArchive = false })
        return
    }

    // Pressing the Settings tab again returns to the top of this screen.
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) scroll.animateScrollTo(0)
    }

    // The update prompt lives here, and only here: the user came to Settings to
    // look, so this is the one place a version may be put in front of them.
    // Nothing asks on the way in — the app opening is not a moment to interrupt.
    // A download that is already running keeps its progress on screen, since
    // the user started it from this very screen.
    val updateState = updater.state
    val offered = (updateState as? UpdateUiState.Available)?.info
    val promptState: UpdateUiState? = when {
        updateState is UpdateUiState.Downloading -> updateState
        offered != null && updatePromptVisible(
            dismissedVersion = state.dismissedUpdateVersion,
            latestVersion = offered.latestVersion,
            checking = false,
        ) -> updateState
        else -> null
    }
    if (promptState != null) {
        UpdatePrompt(
            state = promptState,
            onInstall = { offered?.let(updater::install) },
            onLater = { offered?.let(updater::later) },
            onDismissFailure = updater::dismissFailure,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- the master switch ----
            Section("Your data")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Keep my calculations", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (state.dataSavingEnabled) {
                            "Answers are kept in a folder on this phone, the history keeps three days, and a log is kept for troubleshooting."
                        } else {
                            "Nothing is kept. No archive, no history, no log — on this phone or anywhere else."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.Switch(
                    checked = state.dataSavingEnabled,
                    onCheckedChange = { vm.setDataSavingEnabled(it) },
                )
            }
            if (!state.dataSavingEnabled) {
                Text(
                    "The switches below still show what they were set to, but nothing is written while this is off.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Folder(
                stringResource(R.string.section_appearance),
                "Theme, text size, density",
                initiallyOpen = false,
            ) {

            Text("Theme")
            SingleChoiceSegmentedButtonRow {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = state.themeMode == mode,
                        onClick = { vm.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) {
                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Text(stringResource(R.string.theme_label))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                com.merry8989uk.universalcalculator.ui.theme.AppTheme.entries.forEach { theme ->
                    FilterChip(
                        // a custom theme being in use means none of these is
                        selected = state.customThemeId.isBlank() && state.themeName == theme.label,
                        onClick = {
                            vm.setCustomThemeId("")
                            vm.setThemeName(theme.label)
                        },
                        label = { Text(themeName(theme)) },
                    )
                }
            }

            // ---- themes the user built ----
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Your themes",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { editingTheme = CustomTheme.START; showThemeBuilder = true }) {
                    Text("Create")
                }
            }
            if (customThemes.isEmpty()) {
                Text(
                    "Make a theme from four colours, or from a photo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    customThemes.forEach { made ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = state.customThemeId == made.id,
                                onClick = { vm.setCustomThemeId(made.id) },
                                label = { Text(made.name) },
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { editingTheme = made; showThemeBuilder = true }) {
                                Text("Edit", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = {
                                customThemes = ThemeStore.remove(context, made.id)
                                if (state.customThemeId == made.id) vm.setCustomThemeId("")
                            }) {
                                Text("Delete", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Text(
                com.merry8989uk.universalcalculator.ui.theme.AppTheme.byLabel(state.themeName).blurb,
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.themeName == com.merry8989uk.universalcalculator.ui.theme.AppTheme.DYNAMIC.label) {
                Text(
                    stringResource(R.string.theme_dynamic_note),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text("Text size (${"%.2f".format(state.textScale)}x)")
            Slider(
                value = state.textScale,
                onValueChange = vm::setTextScale,
                valueRange = 0.85f..1.5f,
            )

            Text("Density (${"%.2f".format(state.density)}x)")
            Slider(
                value = state.density,
                onValueChange = vm::setDensity,
                valueRange = 0.80f..1.20f,
            )

            // ---- the calculation archive ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showArchive = true }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.archive_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.archive_kept),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("\u203A", style = MaterialTheme.typography.titleLarge)
            }

            }

            // General: language, number format, sounds
            Folder(
                stringResource(R.string.section_general),
                "Language, numbers, sounds",
                initiallyOpen = false,
            ) {

            // English and Hindi on the surface, the other twenty-three behind More.
            // A row of twenty-five chips is not a choice; it is a wall.
            val langOptions = ArrayList<Choice>()
            langOptions.add(Choice("en", languageName("en"), "Default"))
            if ("hi" in LANGUAGES) langOptions.add(Choice("hi", languageName("hi")))
            for (tag in LANGUAGES) {
                if (tag == "en" || tag == "hi") continue
                langOptions.add(Choice(tag, languageName(tag)))
            }
            ChoiceRow(
                label = stringResource(R.string.language),
                options = langOptions,
                selected = state.language,
                moreLabel = "More languages",
                onSelect = { lang ->
                    vm.setLanguage(lang)
                    // Reload so the new language takes effect at once.
                    (context as? android.app.Activity)?.recreate()
                },
            )

            ToggleRow(stringResource(R.string.notifications), state.notificationsEnabled, vm::setNotifications)
            ToggleRow(stringResource(R.string.live_preview), state.livePreview, vm::setLivePreview)
            ToggleRow(stringResource(R.string.degrees_default), state.degreesMode, vm::setDegreesMode)
            ChoiceRow(
                label = stringResource(R.string.number_format),
                options = listOf(
                    Choice("Indian", "Indian", "30,00,000.5 — lakh and crore grouping."),
                    Choice("International", "International", "3,000,000.5 — thousands grouping."),
                    Choice("European", "European", "3.000.000,5 — dot groups, comma decimal."),
                    Choice("None", "None", "3000000.5 — no separators."),
                ),
                selected = state.numberFormat,
                onSelect = { vm.setNumberFormat(it) },
            )
            Text(
                when (state.numberFormat) {
                    "Indian" -> "30,00,000.5 \u2014 lakh and crore grouping."
                    "European" -> "3.000.000,5 \u2014 dot groups, comma decimal."
                    "None" -> "3000000.5 \u2014 no separators."
                    else -> "3,000,000.5 \u2014 thousands grouping."
                },
                style = MaterialTheme.typography.bodySmall,
            )

            ChoiceRow(
                label = stringResource(R.string.decimal_places),
                options = listOf(
                    Choice("-1", "Auto", "Results keep as many decimals as they need."),
                    Choice("2", "2 decimals", "Rounded to two places."),
                    Choice("0", "0 decimals", "Whole numbers only."),
                    Choice("3", "3 decimals", "Rounded to three places."),
                    Choice("4", "4 decimals", "Rounded to four places."),
                    Choice("6", "6 decimals", "Rounded to six places."),
                ),
                selected = state.decimals.toString(),
                onSelect = { vm.setDecimals(it.toIntOrNull() ?: -1) },
            )
            Text(
                if (state.decimals < 0) "Results keep as many decimals as they need."
                else "Results are rounded to ${state.decimals} decimal places.",
                style = MaterialTheme.typography.bodySmall,
            )

            ToggleRow(stringResource(R.string.calculation_history), state.historyEnabled, vm::setHistoryEnabled)
            ToggleRow(stringResource(R.string.haptic_feedback), state.hapticsEnabled, vm::setHapticsEnabled)

            val toneOptions = ArrayList<Choice>()
            for (choice in SoundPlayer.CHOICES) {
                toneOptions.add(Choice(choice, toneName(choice)))
            }
            ChoiceRow(
                label = stringResource(R.string.key_sounds),
                options = toneOptions,
                selected = state.keySound,
                moreLabel = "More tones",
                onSelect = { choice ->
                    vm.setKeySound(choice)
                    // play it straight away so the choice is audible
                    SoundPlayer.setChoice(choice)
                    SoundPlayer.key()
                },
                extra = { dismiss ->
                    // Both ways of getting your own tone live in here, because
                    // they are the same idea: a sound that is yours.
                    Text(
                        "Your own tone",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { dismiss(); pickTone = true }) {
                            Text("Upload a file")
                        }
                        OutlinedButton(
                            onClick = {
                                if (!ToneRecorder.hasPermission(context)) {
                                    askForMic.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    recording = true
                                    toneScope.launch {
                                        val result = ToneRecorder.record(context)
                                        recording = false
                                        if (result.path == null) {
                                            toneProblem = result.problem
                                        } else {
                                            toneProblem = ""
                                            ToneRecorder.prune(context, result.path)
                                            vm.setCustomTonePath(result.path)
                                            vm.setKeySound(ToneLibrary.CUSTOM)
                                            SoundPlayer.setCustomPath(result.path)
                                            SoundPlayer.setChoice(ToneLibrary.CUSTOM)
                                            SoundPlayer.key()
                                            dismiss()
                                        }
                                    }
                                }
                            },
                            enabled = !recording,
                        ) {
                            Text(if (recording) "Listening…" else "Record 1 second")
                        }
                    }
                    Text(
                        if (state.customTonePath.isBlank()) {
                            "Upload a short sound, or record one. Either becomes a key tone."
                        } else {
                            "You have your own tone set. Pick it from the list, or record another."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )

            // How loud, and the user's own recording.
            Text(
                "Volume  \u00B7  ${(state.keySoundVolume * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = state.keySoundVolume,
                onValueChange = {
                    vm.setKeySoundVolume(it)
                    SoundPlayer.setVolume(it)
                },
                valueRange = 0f..1f,
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { pickTone = true }) { Text("Choose my own tone") }
                if (state.customTonePath.isNotBlank()) {
                    TextButton(onClick = {
                        ToneStore.clear(context)
                        vm.setCustomTonePath("")
                        if (state.keySound == ToneLibrary.CUSTOM) vm.setKeySound(ToneLibrary.OFF)
                        SoundPlayer.setCustomPath("")
                        toneProblem = ""
                    }) { Text("Remove it") }
                }
            }
            Text(
                toneProblem.ifBlank {
                    if (state.customTonePath.isBlank()) {
                        "A key tone is a fraction of a second. Pick a short sound — wav, mp3, ogg, m4a, aac, opus or flac."
                    } else {
                        "Your own tone is saved in the app. Choosing it plays it on every key."
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (toneProblem.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error,
            )
            ToggleRow(stringResource(R.string.fractions_in_results), state.fractionsEnabled, vm::setFractionsEnabled)
            ToggleRow(stringResource(R.string.keep_screen_on), state.keepScreenOn, vm::setKeepScreenOn)
            ToggleRow(stringResource(R.string.save_state), state.saveState, vm::setSaveState)

            ToggleRow(
                stringResource(R.string.minimal_mode),
                state.minimalMode,
                vm::setMinimalMode,
            )
            Text(
                stringResource(R.string.minimal_mode_help),
                style = MaterialTheme.typography.bodySmall,
            )

            Text(stringResource(R.string.keypad_layout), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // the classic pad is switched off for now, so it is not offered
                listOf("Modern", "Classic")
                    .let { if (com.merry8989uk.universalcalculator.ui.Features.SCI_CLASSIC) it + "Classic Scientific" else it }
                    .forEach { style ->
                    FilterChip(
                        selected = state.layoutStyle == style,
                        onClick = { vm.setLayoutStyle(style) },
                        label = {
                            Text(
                                when (style) {
                                    "Classic Scientific" -> "Sci Classic"
                                    else -> style
                                },
                            )
                        },
                    )
                }
            }
            Text(
                when (state.layoutStyle) {
                    "Classic" -> "Classic: flat, square keys with no colour gradient."
                    "Classic Scientific" ->
                        "Sci Classic: the full scientific pad, six keys across with a SHIFT " +
                            "key for the second functions. It replaces the Scientific tab " +
                            "while this is on. Standard is not affected."
                    else -> "Modern: rounded keys with a colour gradient."
                },
                style = MaterialTheme.typography.bodySmall,
            )

            }

            // The calculator itself: tools, display, the assistant
            Folder(
                "The calculator",
                "Tools, keypad, display, Ask AI",
                initiallyOpen = false,
            ) {
            // ---- the tools ----
            Section(stringResource(R.string.section_tools))

            ToggleRow(stringResource(R.string.cas_mode), state.casMode, vm::setCasMode)
            Text(
                stringResource(R.string.cas_mode_help),
                style = MaterialTheme.typography.bodySmall,
            )


            // ---- the display ----
            Section(stringResource(R.string.section_display))

            ToggleRow("Show the GT key", state.showGtKey, vm::setShowGtKey)
            Text(
                if (state.showGtKey) {
                    "The grand total key sits on the keypad. Turning it off gives the space to the = key."
                } else {
                    "The GT key is off and = is twice as wide. The grand total is still worked out and still shown in the display."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ToggleRow(stringResource(R.string.blink_cursor), state.blinkCursor, vm::setBlinkCursor)
            ToggleRow(stringResource(R.string.clear_on_exit), state.clearOnExit, vm::setClearOnExit)
            Text(
                stringResource(R.string.clear_on_exit_help),
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow(stringResource(R.string.show_calc_info), state.showCalcInfo, vm::setShowCalcInfo)
            Text(
                stringResource(R.string.show_calc_info_help),
                style = MaterialTheme.typography.bodySmall,
            )

            Text(stringResource(R.string.home_screen_widget), style = MaterialTheme.typography.bodyMedium)
            Text(
                "Long-press an empty spot on your home screen, choose Widgets, then Nexus Calc. " +
                    "While adding it you can pick a solid, dimmed or fully transparent background.",
                style = MaterialTheme.typography.bodySmall,
            )

            Section(stringResource(R.string.section_ai))

            Text(
                "Ask AI is built in and ready to use. The API key is fixed on the " +
                    "server, so there is nothing to set up or paste here.",
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow(stringResource(R.string.enable_ask_ai), state.aiEnabled, vm::setAiEnabled)
            Text(
                stringResource(R.string.enable_ask_ai_help),
                style = MaterialTheme.typography.bodySmall,
            )

            }

            // ---- the account ----
            Section("Your account")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showAccount = true }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (signedInId.isBlank()) "Not signed in" else "@" + signedInId,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        if (signedInId.isBlank()) {
                            "Your calculations stay on this phone. Sign in to keep a copy you can get back."
                        } else {
                            "Back up, restore, or sign out on this phone."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("\u203A", style = MaterialTheme.typography.titleLarge)
            }

            Section(stringResource(R.string.section_updates))

            Text("Current version ${BuildConfig.VERSION_NAME}")

            when (updateState) {
                UpdateUiState.Checking -> Text("Checking for updates…")
                UpdateUiState.UpToDate -> Text("You're on the latest version.")
                is UpdateUiState.Available -> {
                    Text("Version ${updateState.info.latestVersion} is available.")
                    // the one they put aside still shows here, because this is
                    // where they came to look; it just no longer interrupts them
                    if (updateState.info.latestVersion == state.dismissedUpdateVersion) {
                        Text(
                            "You put this one aside earlier.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(onClick = { updater.install(updateState.info) }) {
                        Text("Update now")
                    }
                }
                is UpdateUiState.Downloading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Downloading update…")
                }
                is UpdateUiState.Failed -> Text("Update failed: ${updateState.message}")
                UpdateUiState.Idle -> Unit
            }

            OutlinedButton(
                onClick = { updater.check(BuildConfig.VERSION_NAME) },
                enabled = updateState !is UpdateUiState.Checking &&
                    updateState !is UpdateUiState.Downloading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Check for updates")
            }

            DiagnosticsSection(
                enabled = state.diagnosticsEnabled,
                onEnabledChange = vm::setDiagnosticsEnabled,
                onOpen = { showDiagnostics = true },
                shareEnabled = state.shareDiagnostics,
                onShareChange = vm::setShareDiagnostics,
            )

            Section(stringResource(R.string.section_reset))
            OutlinedButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset to defaults")
            }

            Section(stringResource(R.string.section_feedback))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { Contact.rateApp(context) },
                    modifier = Modifier.weight(1f),
                ) { Text("Rate app") }
                OutlinedButton(
                    onClick = { Contact.reportIssue(context) },
                    modifier = Modifier.weight(1f),
                ) { Text("Report issue") }
            }
            OutlinedButton(
                onClick = { Contact.emailSupport(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Email support") }

            HorizontalDivider()

            // --- Footer: version + contact (Rule 4) ---
            Text("Nexus Calc  ·  version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall)
            Text(
                text = "@${Contact.INSTAGRAM_HANDLE}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { Contact.openInstagram(context) },
            )
            Text(
                "Suggestions, feedback or a new feature request? Tap the handle above.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
