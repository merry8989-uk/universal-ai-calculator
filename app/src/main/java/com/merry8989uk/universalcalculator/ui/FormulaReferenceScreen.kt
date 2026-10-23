package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.data.FormulaReference

/**
 * The formula reference: everything that is a rule, identity or table rather
 * than something a calculator can evaluate.
 */
@Composable
fun FormulaReferenceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val all = remember { FormulaReference.load(context) }
    val cats = remember { FormulaReference.categories(all) }

    var query by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    val shown = remember(query, cat) { FormulaReference.search(all, query, cat) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Formula reference", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("\u2190 Back") }
        }
        Text(
            "${all.size} identities, rules and tables from the maths handbooks.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search a formula, e.g. cos, integral, Bayes") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = cat == null, onClick = { cat = null }, label = { Text("All (${all.size})") })
            cats.forEach { c ->
                FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) })
            }
        }

        if (shown.isEmpty()) {
            Text("No formula matches that search.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown) { f ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(f.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                f.formula,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Text(
                                f.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
