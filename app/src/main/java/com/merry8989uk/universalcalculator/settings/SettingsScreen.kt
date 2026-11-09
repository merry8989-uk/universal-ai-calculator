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
import com.merry8989uk.universalcalculator.calc.Format
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

/**
 * The name of a language.
 *
 * It is written in its own script, with the English name beside it, so a Tamil
 * speaker can find தமிழ் and everybody else can read Tamil.
 */
@Composable
private fun languageName(code: String): String = when (code) {
    "system" -> stringResource(R.string.lang_system)
    "en" -> stringResource(R.string.lang_english)
    else -> com.merry8989uk.universalcalculator.locale.Languages.byTag(code)
        ?.let { named(it) }
        ?: code
}

/** A language's name, in its own script with the English beside it. */
private fun named(entry: com.merry8989uk.universalcalculator.locale.Languages.Entry): String =
    entry.native + "  \u00B7  " + entry.english

/**
 * A language whose translation is still thin, labelled as such.
 *
 * It is still offered, because a translated tab bar is better than none, but the
 * label says what the user is getting: most of the app will stay in English. A
 * picker that promises Nepali and delivers an English Settings screen is the lie
 * this label exists to stop.
 */
@Composable
private fun partialLanguageName(
    entry: com.merry8989uk.universalcalculator.locale.Languages.Entry,
): String = stringResource(R.string.lang_partial_label, named(entry))

/** What a theme mode is called on its chip. The stored value stays the enum's name. */
@Composable
private fun themeModeName(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
}

/**
 * What a keypad layout is called on its chip.
 *
 * The stored value is the English name and has to stay that way — it is already
 * in DataStore on every phone that has run this app — so only the label changes.
 */
@Composable
private fun layoutName(style: String): String = when (style) {
    "Classic" -> stringResource(R.string.layout_classic)
    "Classic Scientific" -> stringResource(R.string.tab_sci_classic)
    else -> stringResource(R.string.layout_modern)
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
            toneProblem = context.getString(R.string.tone_mic_refused)
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
            Section(stringResource(R.string.settings_section_data))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_keep_calculations),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        if (state.dataSavingEnabled) {
                            stringResource(R.string.settings_data_on)
                        } else {
                            stringResource(R.string.settings_data_off)
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
                    stringResource(R.string.settings_data_off_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Folder(
                stringResource(R.string.section_appearance),
                stringResource(R.string.folder_appearance_sub),
                initiallyOpen = false,
            ) {

            Text(stringResource(R.string.theme_label))
            SingleChoiceSegmentedButtonRow {
                ThemeMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        selected = state.themeMode == mode,
                        onClick = { vm.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                    ) {
                        Text(themeModeName(mode))
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
                    stringResource(R.string.settings_your_themes),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { editingTheme = CustomTheme.START; showThemeBuilder = true }) {
                    Text(stringResource(R.string.settings_create))
                }
            }
            if (customThemes.isEmpty()) {
                Text(
                    stringResource(R.string.settings_themes_empty),
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
                                Text(
                                    stringResource(R.string.settings_edit),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            TextButton(onClick = {
                                customThemes = ThemeStore.remove(context, made.id)
                                if (state.customThemeId == made.id) vm.setCustomThemeId("")
                            }) {
                                Text(
                                    stringResource(R.string.settings_delete),
                                    style = MaterialTheme.typography.labelSmall,
                                )
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

            Text(
                stringResource(
                    R.string.settings_text_size,
                    "%.2f".format(state.textScale),
                ),
            )
            Slider(
                value = state.textScale,
                onValueChange = vm::setTextScale,
                valueRange = 0.85f..1.5f,
            )

            Text(
                stringResource(
                    R.string.settings_density,
                    "%.2f".format(state.density),
                ),
            )
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
                stringResource(R.string.folder_general_sub),
                initiallyOpen = false,
            ) {

            // English and Hindi on the surface, the rest behind More — a row of
            // twenty-five chips is a wall, not a choice.
            //
            // The order carries the honesty: the languages that translate the
            // whole of this screen come first, and the thin ones are still
            // offered but say so on the label. Hiding them would take a tab bar
            // in Assamese away from a user who wants it; listing them plainly
            // would promise an Assamese app that does not exist.
            val langOptions = ArrayList<Choice>()
            langOptions.add(Choice("en", languageName("en"), stringResource(R.string.lang_default_note)))
            for (entry in com.merry8989uk.universalcalculator.locale.Languages.COMPLETE) {
                langOptions.add(Choice(entry.tag, languageName(entry.tag)))
            }
            langOptions.add(Choice(
                com.merry8989uk.universalcalculator.locale.LocaleHelper.SYSTEM,
                languageName(com.merry8989uk.universalcalculator.locale.LocaleHelper.SYSTEM),
            ))
            for (entry in com.merry8989uk.universalcalculator.locale.Languages.INCOMPLETE) {
                langOptions.add(
                    Choice(entry.tag, partialLanguageName(entry), stringResource(R.string.lang_partial_note)),
                )
            }
            ChoiceRow(
                label = stringResource(R.string.language),
                options = langOptions,
                selected = state.language,
                moreLabel = stringResource(R.string.more_languages),
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
                    Choice(
                        Format.FOLLOW_LOCALE,
                        stringResource(R.string.format_follow),
                        stringResource(R.string.format_follow_note),
                    ),
                    Choice(
                        "Indian",
                        stringResource(R.string.format_indian),
                        stringResource(R.string.format_indian_note),
                    ),
                    Choice(
                        "International",
                        stringResource(R.string.format_international),
                        stringResource(R.string.format_international_note),
                    ),
                    Choice(
                        "European",
                        stringResource(R.string.format_european),
                        stringResource(R.string.format_european_note),
                    ),
                    Choice(
                        "None",
                        stringResource(R.string.format_none),
                        stringResource(R.string.format_none_note),
                    ),
                ),
                selected = state.numberFormat,
                onSelect = { vm.setNumberFormat(it) },
            )
            // The example is worked out rather than written down, so it says what
            // this phone will actually do instead of what the option is called.
            Text(
                stringResource(
                    R.string.format_example,
                    Format.grouped("3000000.5", state.numberFormat),
                ),
                style = MaterialTheme.typography.bodySmall,
            )

            ChoiceRow(
                label = stringResource(R.string.decimal_places),
                options = listOf(
                    Choice(
                        "-1",
                        stringResource(R.string.decimals_auto),
                        stringResource(R.string.decimals_auto_note),
                    ),
                    Choice(
                        "2",
                        stringResource(R.string.decimals_two),
                        stringResource(R.string.decimals_two_note),
                    ),
                    Choice(
                        "0",
                        stringResource(R.string.decimals_zero),
                        stringResource(R.string.decimals_zero_note),
                    ),
                    Choice(
                        "3",
                        stringResource(R.string.decimals_three),
                        stringResource(R.string.decimals_three_note),
                    ),
                    Choice(
                        "4",
                        stringResource(R.string.decimals_four),
                        stringResource(R.string.decimals_four_note),
                    ),
                    Choice(
                        "6",
                        stringResource(R.string.decimals_six),
                        stringResource(R.string.decimals_six_note),
                    ),
                ),
                selected = state.decimals.toString(),
                onSelect = { vm.setDecimals(it.toIntOrNull() ?: -1) },
            )
            Text(
                if (state.decimals < 0) stringResource(R.string.decimals_auto_note)
                else stringResource(R.string.decimals_now, state.decimals),
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
                moreLabel = stringResource(R.string.more_tones),
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
                        stringResource(R.string.tone_yours),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { dismiss(); pickTone = true }) {
                            Text(stringResource(R.string.tone_upload))
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
                            Text(
                                if (recording) stringResource(R.string.tone_listening)
                                else stringResource(R.string.tone_record),
                            )
                        }
                    }
                    Text(
                        if (state.customTonePath.isBlank()) {
                            stringResource(R.string.tone_upload_help)
                        } else {
                            stringResource(R.string.tone_set_help)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )

            // How loud, and the user's own recording.
            Text(
                stringResource(R.string.tone_volume, (state.keySoundVolume * 100).toInt()),
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
                OutlinedButton(onClick = { pickTone = true }) {
                    Text(stringResource(R.string.tone_choose))
                }
                if (state.customTonePath.isNotBlank()) {
                    TextButton(onClick = {
                        ToneStore.clear(context)
                        vm.setCustomTonePath("")
                        if (state.keySound == ToneLibrary.CUSTOM) vm.setKeySound(ToneLibrary.OFF)
                        SoundPlayer.setCustomPath("")
                        toneProblem = ""
                    }) { Text(stringResource(R.string.tone_remove)) }
                }
            }
            Text(
                toneProblem.ifBlank {
                    if (state.customTonePath.isBlank()) {
                        stringResource(R.string.tone_formats_help)
                    } else {
                        stringResource(R.string.tone_saved_help)
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
                        label = { Text(layoutName(style)) },
                    )
                }
            }
            Text(
                when (state.layoutStyle) {
                    "Classic" -> stringResource(R.string.layout_classic_help)
                    "Classic Scientific" -> stringResource(R.string.layout_sci_classic_help)
                    else -> stringResource(R.string.layout_modern_help)
                },
                style = MaterialTheme.typography.bodySmall,
            )

            }

            // The calculator itself: tools, display, the assistant
            Folder(
                stringResource(R.string.folder_calculator),
                stringResource(R.string.folder_calculator_sub),
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

            ToggleRow(stringResource(R.string.settings_show_gt), state.showGtKey, vm::setShowGtKey)
            Text(
                if (state.showGtKey) {
                    stringResource(R.string.settings_gt_on_help)
                } else {
                    stringResource(R.string.settings_gt_off_help)
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
                stringResource(R.string.widget_help),
                style = MaterialTheme.typography.bodySmall,
            )

            Section(stringResource(R.string.section_ai))

            Text(
                stringResource(R.string.ai_built_in),
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow(stringResource(R.string.enable_ask_ai), state.aiEnabled, vm::setAiEnabled)
            Text(
                stringResource(R.string.enable_ask_ai_help),
                style = MaterialTheme.typography.bodySmall,
            )

            }

            // ---- the account ----
            Section(stringResource(R.string.settings_account))
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
                        if (signedInId.isBlank()) stringResource(R.string.account_not_signed_in)
                        else "@" + signedInId,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        if (signedInId.isBlank()) {
                            stringResource(R.string.account_signed_out_help)
                        } else {
                            stringResource(R.string.account_signed_in_help)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("\u203A", style = MaterialTheme.typography.titleLarge)
            }

            Section(stringResource(R.string.section_updates))

            Text(stringResource(R.string.settings_current_version, BuildConfig.VERSION_NAME))

            when (updateState) {
                UpdateUiState.Checking -> Text(stringResource(R.string.updates_checking))
                UpdateUiState.UpToDate -> Text(stringResource(R.string.up_to_date))
                is UpdateUiState.Available -> {
                    Text(
                        stringResource(
                            R.string.update_available,
                            updateState.info.latestVersion,
                        ),
                    )
                    // the one they put aside still shows here, because this is
                    // where they came to look; it just no longer interrupts them
                    if (updateState.info.latestVersion == state.dismissedUpdateVersion) {
                        Text(
                            stringResource(R.string.settings_update_put_aside),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(onClick = { updater.install(updateState.info) }) {
                        Text(stringResource(R.string.updates_now))
                    }
                }
                is UpdateUiState.Downloading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.updates_downloading))
                }
                is UpdateUiState.Failed ->
                    Text(stringResource(R.string.updates_failed, updateState.message))
                UpdateUiState.Idle -> Unit
            }

            OutlinedButton(
                onClick = { updater.check(BuildConfig.VERSION_NAME) },
                enabled = updateState !is UpdateUiState.Checking &&
                    updateState !is UpdateUiState.Downloading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.check_for_updates))
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
                Text(stringResource(R.string.settings_reset_defaults))
            }

            Section(stringResource(R.string.section_feedback))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { Contact.rateApp(context) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.settings_rate_app)) }
                OutlinedButton(
                    onClick = { Contact.reportIssue(context) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.settings_report_issue)) }
            }
            OutlinedButton(
                onClick = { Contact.emailSupport(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.settings_email_support)) }

            HorizontalDivider()

            // --- Footer: version + contact (Rule 4) ---
            Text(
                stringResource(R.string.settings_footer_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "@${Contact.INSTAGRAM_HANDLE}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { Contact.openInstagram(context) },
            )
            Text(
                stringResource(R.string.settings_footer_ask),
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
