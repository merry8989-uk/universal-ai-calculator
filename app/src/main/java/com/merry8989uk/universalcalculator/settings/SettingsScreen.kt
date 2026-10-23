package com.merry8989uk.universalcalculator.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.merry8989uk.universalcalculator.BuildConfig
import com.merry8989uk.universalcalculator.diagnostics.DiagnosticsSection
import com.merry8989uk.universalcalculator.updater.UpdateUiState
import com.merry8989uk.universalcalculator.updater.UpdateViewModel

private val THEME_NAMES = com.merry8989uk.universalcalculator.ui.theme.AppTheme.labels

private val LANGUAGES = listOf("system", "en", "hi")

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
                title = { Text("Settings") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
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
            Section("Appearance")

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

            Text("Colour theme")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                THEME_NAMES.forEach { name ->
                    FilterChip(
                        selected = state.themeName == name,
                        onClick = { vm.setThemeName(name) },
                        label = { Text(name) },
                    )
                }
            }
            Text(
                com.merry8989uk.universalcalculator.ui.theme.AppTheme.byLabel(state.themeName).blurb,
                style = MaterialTheme.typography.bodySmall,
            )

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

            Section("General")

            Text("Language")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LANGUAGES.forEach { lang ->
                    FilterChip(
                        selected = state.language == lang,
                        onClick = { vm.setLanguage(lang) },
                        label = { Text(lang) },
                    )
                }
            }

            ToggleRow("Notifications", state.notificationsEnabled, vm::setNotifications)
            ToggleRow("Live result preview", state.livePreview, vm::setLivePreview)
            ToggleRow("Degrees by default", state.degreesMode, vm::setDegreesMode)
            Text("Number format", style = MaterialTheme.typography.bodyMedium)
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

            Text("Decimal places", style = MaterialTheme.typography.bodyMedium)
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

            Text("Measuring units", style = MaterialTheme.typography.bodyMedium)
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
            ToggleRow("Calculation history", state.historyEnabled, vm::setHistoryEnabled)
            ToggleRow("Haptic feedback", state.hapticsEnabled, vm::setHapticsEnabled)
            ToggleRow("Fractions in results", state.fractionsEnabled, vm::setFractionsEnabled)
            ToggleRow("Keep screen on", state.keepScreenOn, vm::setKeepScreenOn)
            ToggleRow("Save state on exit", state.saveState, vm::setSaveState)

            Text("Keypad layout", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Modern", "Classic").forEach { style ->
                    FilterChip(
                        selected = state.layoutStyle == style,
                        onClick = { vm.setLayoutStyle(style) },
                        label = { Text(style) },
                    )
                }
            }
            Text(
                if (state.layoutStyle == "Classic")
                    "Classic: flat, square keys with no colour gradient."
                else "Modern: rounded keys with a colour gradient.",
                style = MaterialTheme.typography.bodySmall,
            )

            Section("AI assistant")

            Text(
                "Ask AI is built in and ready to use. The API key is fixed on the " +
                    "server, so there is nothing to set up or paste here.",
                style = MaterialTheme.typography.bodySmall,
            )
            ToggleRow("Enable Ask AI", state.aiEnabled, vm::setAiEnabled)

            Section("Updates")

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
                UpdateUiState.Downloading -> Row(
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

            Section("Reset")
            OutlinedButton(onClick = vm::reset, modifier = Modifier.fillMaxWidth()) {
                Text("Reset to defaults")
            }

            Section("Feedback")

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
