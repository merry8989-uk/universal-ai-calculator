package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * The studio look, in Compose.
 *
 * The formula page inside the app is an HTML studio: a deep navy backdrop, a
 * panel that is a shade lighter than the page, thin borders, one blue accent and
 * a green for the quiet metadata. The Formulas hub and the calculator page next
 * to it were still drawn in the app's glass theme, so the two halves of the same
 * feature looked like two different apps.
 *
 * These are the same colours, taken from the page's own stylesheet, so a change
 * to one is a change to both. The values are named after what they do rather
 * than what colour they are: a panel is a panel in either theme.
 */
object Studio {
    /** The page's backdrop: lighter at the top, as if lit from above. */
    val BackdropTop = Color(0xFF1A2B4A)
    val Backdrop = Color(0xFF0A1020)

    val PanelTop = Color(0xFF152139)
    val PanelBottom = Color(0xFF10182A)
    val Border = Color(0xFF2A3A57)

    val Text = Color(0xFFEFF4FF)
    val Muted = Color(0xFFA5B3CC)
    val Blue = Color(0xFF79A7FF)
    val Green = Color(0xFF70DFBD)
    val Error = Color(0xFFFF8A8A)

    /** A row in a list, and the same row while it is being pointed at. */
    val ItemBg = Color(0xFF0E1729)
    val ItemBorder = Color(0xFF283954)
    val ItemActive = Color(0xFF14223B)

    /** Anything typed into: the search box, the pills, a field. */
    val FieldBg = Color(0xFF0B1426)
    val PillBorder = Color(0xFF3C5278)
    val PillText = Color(0xFFCBDCFF)

    /** The formula itself, and the answer under it. */
    val FormulaBg = Color(0xFF0A1425)
    val FormulaBorder = Color(0xFF30415F)
    val ResultBg = Color(0xFF091323)
    val ResultBorder = Color(0xFF314563)

    val PanelShape = RoundedCornerShape(16.dp)
    val ItemShape = RoundedCornerShape(11.dp)
    val PillShape = RoundedCornerShape(99.dp)
    val FormulaShape = RoundedCornerShape(12.dp)
}

/**
 * Material's own widgets — the text field, the divider, the ripple — take their
 * colours from here, so the screen does not have to restyle each one by hand.
 */
@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Studio.Blue,
            onPrimary = Studio.Backdrop,
            primaryContainer = Studio.Blue.copy(alpha = 0.18f),
            onPrimaryContainer = Studio.Text,
            secondary = Studio.Green,
            onSecondary = Studio.Backdrop,
            secondaryContainer = Studio.Green.copy(alpha = 0.18f),
            onSecondaryContainer = Studio.Text,
            tertiary = Studio.Blue,
            onTertiary = Studio.Backdrop,
            background = Studio.Backdrop,
            onBackground = Studio.Text,
            surface = Studio.PanelTop,
            onSurface = Studio.Text,
            surfaceVariant = Studio.ItemBg,
            onSurfaceVariant = Studio.Muted,
            outline = Studio.Border,
            error = Studio.Error,
        ),
        content = content,
    )
}

/** The lit-from-above backdrop the page paints behind everything. */
@Composable
fun StudioBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Studio.BackdropTop, Studio.Backdrop))),
    ) {
        content()
    }
}

/** A panel: the surface a section of the page sits on. */
@Composable
fun StudioPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(Studio.PanelShape)
            .background(Brush.verticalGradient(listOf(Studio.PanelTop, Studio.PanelBottom)))
            .border(1.dp, Studio.Border, Studio.PanelShape),
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

/** The studio's heading: the wordmark in two tones, a line under it, a count. */
@Composable
fun StudioHeader(subtitle: String, pill: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Studio.Text, fontWeight = FontWeight.ExtraBold)) {
                        append("NEXUS ")
                    }
                    withStyle(SpanStyle(color = Studio.Blue, fontWeight = FontWeight.ExtraBold)) {
                        append("CALC STUDIO")
                    }
                },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Studio.Muted)
        }
        StudioLabel(pill)
    }
}

/** A pill that only says something, like the formula count. */
@Composable
fun StudioLabel(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(Studio.PillShape)
            .border(1.dp, Studio.PillBorder, Studio.PillShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = Studio.PillText)
    }
}

/** A pill you can press: one of the categories along the top of the list. */
@Composable
fun StudioPill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    // last, so a call site can pass the action as a trailing lambda
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(Studio.PillShape)
            .background(if (selected) Studio.ItemActive else Studio.FieldBg)
            .border(1.dp, if (selected) Studio.Blue else Studio.PillBorder, Studio.PillShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Studio.Text else Studio.Muted,
            maxLines = 1,
        )
    }
}

/**
 * A row in the list: a bar in the category's colour, a title, a line about it,
 * and a quiet label underneath.
 */
@Composable
fun StudioItem(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    meta: String = "",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Studio.ItemShape)
            .background(Studio.ItemBg)
            .border(1.dp, Studio.ItemBorder, Studio.ItemShape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = if (meta.isEmpty()) "$title. $subtitle" else "$title. $subtitle. $meta"
                role = Role.Button
            }
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Column(modifier = Modifier.padding(start = 11.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Studio.Text)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Studio.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.labelSmall, color = Studio.Green)
            }
        }
    }
}

/** The formula, set the way the page sets it: monospace, on its own surface. */
@Composable
fun StudioFormulaBox(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(Studio.FormulaShape)
            .background(Studio.FormulaBg)
            .border(1.dp, Studio.FormulaBorder, Studio.FormulaShape)
            .padding(14.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            color = Studio.Text,
        )
    }
}

/** One answer, with its label above and its note below. */
@Composable
fun StudioResult(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    note: String = "",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(Studio.ItemShape)
            .background(Studio.ResultBg)
            .border(1.dp, Studio.ResultBorder, Studio.ItemShape)
            .padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Studio.Muted)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            color = Studio.Text,
        )
        if (note.isNotEmpty()) {
            Text(note, style = MaterialTheme.typography.bodySmall, color = Studio.Blue)
        }
    }
}
