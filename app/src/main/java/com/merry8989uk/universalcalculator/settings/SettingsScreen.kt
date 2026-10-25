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
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsSection
import com.merry8989uk.universalcalculator.sound.SoundPlayer
import com.merry8989uk.universalcalculator.updater.UpdateUiState
import com.merry8989uk.universalcalculator.updater.UpdateViewModel

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

private val LANGUAGES = listOf("system", "en", "hi")

/** Friendly names for the language chips. */
@Composable
private fun languageName(code: String): String = when (code) {
    "en" -> stringResource(R.string.lang_en)
    "hi" -> stringResource(R.string.lang_hi)
    else -> stringResource(R.string.lang_system)
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

    // Pressing the Settings tab again returns to the top of this screen.
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) scroll.animateScrollTo(0)
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
            Section(stringResource(R.string.section_appearance))

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
                        selected = state.themeName == theme.label,
                        onClick = { vm.setThemeName(theme.label) },
                        label = { Text(themeName(theme)) },
                    )
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

            Section(stringResource(R.string.section_general))

            Text(stringResource(R.string.language))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LANGUAGES.forEach { lang ->
                    FilterChip(
                        selected = state.language == lang,
                        onClick = {
                            vm.setLanguage(lang)
                            // Reload so the new language takes effect at once.
                            (context as? android.app.Activity)?.recreate()
                        },
                        label = { Text(languageName(lang)) },
                    )
                }
            }

            ToggleRow(stringResource(R.string.notifications), state.notificationsEnabled, vm::setNotifications)
            ToggleRow(stringResource(R.string.live_preview), state.livePreview, vm::setLivePreview)
            ToggleRow(stringResource(R.string.degrees_default), state.degreesMode, vm::setDegreesMode)
            Text(stringResource(R.string.number_format), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Indian", "International", "European", "None").forEach { fmt ->
                    FilterChip(
                        selected = state.numberFormat == fmt,
                        onClick = { vm.setNumberFormat(fmt) },
                        label = { Text(fmt) },
                    )
                }
            }
            Text(
                when (state.numberFormat) {
                    "Indian" -> "30,00,000.5 \u2014 lakh and crore grouping."
                    "European" -> "3.000.000,5 \u2014 dot groups, comma decimal."
                    "None" -> "3000000.5 \u2014 no separators."
                    else -> "3,000,000.5 \u2014 thousands grouping."
                },
                style = MaterialTheme.typography.bodySmall,
            )

            Text(stringResource(R.string.decimal_places), style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(-1 to "Auto", 0 to "0", 2 to "2", 3 to "3", 4 to "4", 6 to "6").forEach { (n, name) ->
                    FilterChip(
                        selected = state.decimals == n,
                        onClick = { vm.setDecimals(n) },
                        label = { Text(name) },
                    )
                }
            }
            Text(
                if (state.decimals < 0) "Results keep as many decimals as they need."
                else "Results are rounded to ${state.decimals} decimal places.",
                style = MaterialTheme.typography.bodySmall,
            )

            Text(stringResource(R.string.measuring_units), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Metric", "Imperial").forEach { sys ->
                    FilterChip(
                        selected = state.unitSystem == sys,
                        onClick = { vm.setUnitSystem(sys) },
                        label = { Text(sys) },
                    )
                }
            }
            Text(
                if (state.unitSystem == "Imperial")
                    "Converters open on imperial units (miles, pounds, gallons)."
                else "Converters open on metric units (kilometres, kilograms, litres).",
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow(stringResource(R.string.calculation_history), state.historyEnabled, vm::setHistoryEnabled)
            ToggleRow(stringResource(R.string.haptic_feedback), state.hapticsEnabled, vm::setHapticsEnabled)

            Text(stringResource(R.string.key_sounds), style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SoundPlayer.CHOICES.forEach { choice ->
                    FilterChip(
                        selected = state.keySound == choice,
                        onClick = {
                            vm.setKeySound(choice)
                            // play it straight away so the choice is audible
                            SoundPlayer.setChoice(choice)
                            SoundPlayer.key()
                        },
                        label = {
                            Text(
                                when (choice) {
                                    SoundPlayer.OFF -> stringResource(R.string.sound_off)
                                    "Click" -> stringResource(R.string.sound_click)
                                    "Bubble" -> stringResource(R.string.sound_bubble)
                                    else -> stringResource(R.string.sound_pop)
                                },
                            )
                        },
                    )
                }
            }
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
                listOf("Modern", "Classic", "Classic Scientific").forEach { style ->
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

            // ---- Sci Classic ----
            Section("Sci Classic")
            Text(
                "These apply to the classic scientific pad. They do not change the " +
                    "Standard or the modern Scientific keypad.",
                style = MaterialTheme.typography.bodySmall,
            )

            Text(stringResource(R.string.angle_unit), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("DEGREE", "RADIAN", "GRADIAN").forEach { unit ->
                    FilterChip(
                        selected = state.angleUnit == unit,
                        onClick = { vm.setAngleUnit(unit) },
                        label = { Text(unit.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Text(
                "A full turn is 360, 2\u03C0 or 400. The DEG key cycles through all three.",
                style = MaterialTheme.typography.bodySmall,
            )

            Text(stringResource(R.string.default_output), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.fractionsEnabled,
                    onClick = { vm.setFractionsEnabled(true) },
                    label = { Text(stringResource(R.string.output_fraction)) },
                )
                FilterChip(
                    selected = !state.fractionsEnabled,
                    onClick = { vm.setFractionsEnabled(false) },
                    label = { Text(stringResource(R.string.output_decimal)) },
                )
            }

            // ---- how a percent after + or - is read ----
            val pctMode = com.merry8989uk.universalcalculator.calc.PercentageMode.of(state.percentageMode)
            Text(
                stringResource(R.string.percentage_type),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.percentage_type_help),
                style = MaterialTheme.typography.bodySmall,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                com.merry8989uk.universalcalculator.calc.PercentageMode.all.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { vm.setPercentageMode(mode.name) }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = mode == pctMode,
                            onClick = { vm.setPercentageMode(mode.name) },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                            Text(mode.example, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            var decimalDialog by remember { mutableStateOf(false) }
            val decimalMode = com.merry8989uk.universalcalculator.calc.DecimalFormat.of(state.decimalFormat)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { decimalDialog = true }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.decimal_number_format),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        decimalMode.label +
                            if (decimalMode != com.merry8989uk.universalcalculator.calc.DecimalFormat.NORMAL) {
                                "  \u00B7  ${state.decimalDigits} digits"
                            } else {
                                ""
                            },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("\u203A", style = MaterialTheme.typography.titleLarge)
            }
            if (decimalDialog) {
                com.merry8989uk.universalcalculator.ui.DecimalFormatDialog(
                    current = decimalMode,
                    digits = state.decimalDigits,
                    onPick = { vm.setDecimalFormat(it.name) },
                    onDigits = { vm.setDecimalDigits(it) },
                    onDismiss = { decimalDialog = false },
                )
            }

            ToggleRow(
                stringResource(R.string.auto_close_brackets),
                state.autoCloseBrackets,
                vm::setAutoCloseBrackets,
            )
            Text(
                stringResource(R.string.auto_close_brackets_help),
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow(
                stringResource(R.string.ignore_trailing_operator),
                state.ignoreTrailingOperator,
                vm::setIgnoreTrailingOperator,
            )
            Text(
                stringResource(R.string.ignore_trailing_operator_help),
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

            Section(stringResource(R.string.sync_title))
            com.merry8989uk.universalcalculator.sync.SyncSection()

            Section(stringResource(R.string.section_updates))

            val updateState = updater.state
            Text("Current version ${BuildConfig.VERSION_NAME}")

            when (updateState) {
                UpdateUiState.Checking -> Text("Checking for updates…")
                UpdateUiState.UpToDate -> Text("You're on the latest version.")
                is UpdateUiState.Available -> {
                    Text("Version ${updateState.info.latestVersion} is available.")
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
