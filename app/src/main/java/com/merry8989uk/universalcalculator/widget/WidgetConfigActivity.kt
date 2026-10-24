package com.merry8989uk.universalcalculator.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.ui.theme.UniversalCalculatorTheme

/**
 * Shown when the calculator widget is dropped on the home screen: pick how
 * solid the background should be.
 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            UniversalCalculatorTheme {
                WidgetConfigScreen(
                    initial = CalculatorWidgetProvider.styleOf(this, widgetId),
                    initialLayout = CalculatorWidgetProvider.layoutOf(this, widgetId),
                    onConfirm = { style, layout ->
                        CalculatorWidgetProvider.setStyle(this, widgetId, style)
                        CalculatorWidgetProvider.setLayout(this, widgetId, layout)
                        val manager = AppWidgetManager.getInstance(this)
                        CalculatorWidgetProvider.render(this, manager, widgetId)
                        setResult(RESULT_OK, android.content.Intent().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId,
                        ))
                        finish()
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}

private val OPTIONS = listOf(
    CalculatorWidgetProvider.STYLE_SOLID to ("Solid" to "A dark card, easiest to read."),
    CalculatorWidgetProvider.STYLE_DIM to ("Dimmed" to "About 65% opaque, lets the wallpaper show through."),
    CalculatorWidgetProvider.STYLE_CLEAR to ("Transparent" to "Barely there \u2014 the keys and numbers float on your wallpaper."),
)

private val LAYOUT_OPTIONS = listOf(
    CalculatorWidgetProvider.LAYOUT_STANDARD to
        ("Standard" to "Numbers and the four operations."),
    CalculatorWidgetProvider.LAYOUT_SCIENTIFIC to
        ("Scientific" to "The same keypad with sin, cos, tan, ln, root and powers."),
)

@Composable
private fun WidgetConfigScreen(
    initial: Int,
    initialLayout: Int,
    onConfirm: (Int, Int) -> Unit,
    onCancel: () -> Unit,
) {
    var style by remember { mutableIntStateOf(initial) }
    var layout by remember { mutableIntStateOf(initialLayout) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Calculator widget", style = MaterialTheme.typography.titleLarge)

        Text("Keypad", style = MaterialTheme.typography.titleMedium)
        LAYOUT_OPTIONS.forEach { (value, text) ->
            val (title, blurb) = text
            val selected = layout == value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else Color.Transparent,
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = if (selected) 0.35f else 0.14f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { layout = value }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected, onClick = { layout = value })
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(blurb, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("Background", style = MaterialTheme.typography.titleMedium)
        OPTIONS.forEach { (value, text) ->
            val (title, blurb) = text
            val selected = style == value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else Color.Transparent,
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = if (selected) 0.35f else 0.14f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { style = value }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected, onClick = { style = value })
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(blurb, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = { onConfirm(style, layout) }, modifier = Modifier.weight(1f)) { Text("Add widget") }
        }
    }
}
