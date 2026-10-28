package com.merry8989uk.universalcalculator.ui.theme

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Building a theme.
 *
 * The screen is a live preview at the top and four colour slots below it, so
 * every tap is visible immediately in the thing it will change. A photo can fill
 * all four slots at once, and the two colours are read out of the picture rather
 * than averaged, so a photo of a red flower on a grey wall gives a red theme.
 */
@Composable
fun ThemeBuilderScreen(
    /** The theme being edited, or null when a new one is being made. */
    editing: CustomTheme?,
    onSave: (CustomTheme) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var theme by remember { mutableStateOf(editing ?: CustomTheme.START) }
    var problem by remember { mutableStateOf("") }
    var pickPhoto by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val built = ThemeStore.imageTheme(context, uri, theme.name, theme)
        if (built == null) {
            problem = "That picture could not be read. Try another."
        } else {
            problem = ""
            theme = built
        }
    }
    LaunchedEffect(pickPhoto) {
        if (pickPhoto) {
            pickImage.launch(arrayOf("image/*"))
            pickPhoto = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text(
            if (editing == null) "Create a theme" else "Edit theme",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Pick four colours, or build the whole thing from a photo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ---- the preview ----
        ThemePreview(theme)

        OutlinedTextField(
            value = theme.name,
            onValueChange = { theme = theme.copy(name = it) },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        ColourSlot(
            title = "Background",
            hint = "Behind everything",
            chosen = theme.background,
            onPick = { theme = theme.copy(background = it) },
        )
        ColourSlot(
            title = "Keys",
            hint = "The buttons themselves",
            chosen = theme.surface,
            onPick = { theme = theme.copy(surface = it) },
        )
        ColourSlot(
            title = "Accent",
            hint = "The answer, the selected tab, the equals key",
            chosen = theme.primary,
            onPick = { theme = theme.copy(primary = it) },
        )
        ColourSlot(
            title = "Second accent",
            hint = "The operators",
            chosen = theme.secondary,
            onPick = { theme = theme.copy(secondary = it) },
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { pickPhoto = true }) { Text("Build from a photo") }
            if (theme.imagePath.isNotBlank()) {
                TextButton(onClick = { theme = theme.copy(imagePath = "") }) {
                    Text("Remove the photo")
                }
            }
        }
        if (theme.imagePath.isNotBlank()) {
            Text(
                "The two accents and the canvas came from your photo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (problem.isNotBlank()) {
            Text(problem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (theme.name.isBlank()) {
                    problem = "Give the theme a name."
                } else {
                    onSave(theme)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp),
        ) { Text(if (editing == null) "Save this theme" else "Save changes") }
    }
}

/** A small calculator wearing the theme, so the choice is visible before it is saved. */
@Composable
private fun ThemePreview(theme: CustomTheme) {
    val onBackground = readableOn(theme.background)
    val onSurface = readableOn(theme.surface)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(theme.background)
            .border(1.dp, onBackground.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        // the display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                "250 \u00D7 12 = 3,000",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                color = theme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("7", "8", "\u00F7").forEach { label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = onSurface)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(theme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text("=", color = readableOn(theme.primary))
            }
        }
    }
}

/** One colour: what it is for, the current swatch, and a row of choices. */
@Composable
private fun ColourSlot(
    title: String,
    hint: String,
    chosen: Color,
    onPick: (Color) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(chosen)
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (colour in Palette.ALL) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colour)
                        .border(
                            if (colour == chosen) 3.dp else 1.dp,
                            if (colour == chosen) Color.White else Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { onPick(colour) },
                )
            }
        }
    }
}

/**
 * The swatches on offer.
 *
 * Two families: the deep bases a canvas or a key wants, and the vivid accents a
 * selected tab wants. Offering only vivid colours would give every theme a
 * glowing background; offering only deep ones would give none any accent.
 */
object Palette {
    val BASES: List<Color> = listOf(
        Color(0xFF0E1018), Color(0xFF12141F), Color(0xFF1A1D2B), Color(0xFF232838),
        Color(0xFF0A0A0C), Color(0xFF15140F), Color(0xFF06110D), Color(0xFF140A0C),
        Color(0xFFF3F7FA), Color(0xFFFDF4F2), Color(0xFFF1FAF6), Color(0xFFFAF8F2),
    )
    val ACCENTS: List<Color> = listOf(
        Color(0xFF4EA8FF), Color(0xFF00E5FF), Color(0xFF3B82F6), Color(0xFF6C4CF1),
        Color(0xFFFF2E97), Color(0xFFFF7AB6), Color(0xFFEF4444), Color(0xFFFF8A3D),
        Color(0xFFF5C542), Color(0xFF34D399), Color(0xFF14B8A6), Color(0xFFA7F3D0),
    )
    val ALL: List<Color> = BASES + ACCENTS
}
