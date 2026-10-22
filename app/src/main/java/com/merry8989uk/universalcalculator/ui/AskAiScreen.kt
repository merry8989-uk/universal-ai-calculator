package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.ai.AiClient
import kotlinx.coroutines.launch

private val SUGGESTIONS = listOf(
    "Explain the Pythagorean theorem with an example",
    "A 1200 sq ft flat at 2500 per sq ft, what is the total cost?",
    "How much EMI on a 20 lakh loan at 9% for 15 years?",
    "Convert 5 miles per hour to km/h and explain",
    "Is 91 a prime number, and why?",
    "Compound interest on 50000 at 8% for 5 years?",
)

/**
 * "Ask AI" — ask anything. The key is fixed on the server, so there is nothing
 * for the user to configure.
 */
@Composable
fun AskAiScreen(contextText: String, onClearContext: () -> Unit, enabled: Boolean = true) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun send(q: String) {
        if (q.isBlank() || loading) return
        loading = true
        answer = ""
        scope.launch {
            answer = AiClient.ask(q, contextText)
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("Ask AI", style = MaterialTheme.typography.titleLarge)

        if (!enabled) {
            Text(
                "Ask AI is turned off. Turn it back on in Settings \u2192 AI assistant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Text(
                "Ask anything \u2014 maths, science, money, health or everyday questions. " +
                    "Answered by the app's built-in assistant.",
                style = MaterialTheme.typography.bodySmall,
            )

            if (contextText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Context: $contextText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearContext) { Text("Clear") }
                }
            }

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Your question") },
                modifier = Modifier.fillMaxWidth().height(110.dp).padding(top = 8.dp),
            )

            Button(
                onClick = { send(question) },
                enabled = !loading && question.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(if (loading) "Thinking\u2026" else "Ask") }

            if (loading) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Getting the answer\u2026")
                }
            }

            if (answer.isNotBlank()) {
                Text("Answer", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp))
                Text(answer, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp))
            }

            Text("Try one of these", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                SUGGESTIONS.forEach { s ->
                    AssistChip(onClick = { question = s; send(s) }, label = { Text(s) })
                }
            }
        }
    }
}
