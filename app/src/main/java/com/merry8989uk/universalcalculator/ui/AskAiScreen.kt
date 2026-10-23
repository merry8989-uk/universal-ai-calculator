package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.ai.AiClient
import com.merry8989uk.universalcalculator.ai.AnswerFormat
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
 * for the user to configure. The suggestion list disappears once it has been
 * used, so it never nags.
 */
@Composable
fun AskAiScreen(
    contextText: String,
    onClearContext: () -> Unit,
    enabled: Boolean = true,
    initialQuestion: String = "",
    onInitialQuestionUsed: () -> Unit = {},
) {
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var used by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun send(q: String) {
        if (q.isBlank() || loading) return
        loading = true
        answer = ""
        used = true
        scope.launch {
            answer = AnswerFormat.clean(AiClient.ask(q, contextText))
            loading = false
        }
    }

    // A question handed over from the calculator (voice or photo) is asked here.
    LaunchedEffect(initialQuestion, enabled) {
        if (enabled && initialQuestion.isNotBlank()) {
            question = initialQuestion
            send(initialQuestion)
            onInitialQuestionUsed()
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
            return@Column
        }

        Text(
            "Ask anything \u2014 maths, science, money, health or everyday questions.",
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
            modifier = Modifier.fillMaxWidth().height(104.dp).padding(top = 8.dp),
        )

        // Big, magical Ask button.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(MagicBrush)
                .clickable(enabled = !loading && question.isNotBlank()) { send(question) },
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("\u2726", color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (loading) "Thinking\u2026" else "Ask AI",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (loading) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Text("Working it out\u2026")
            }
        }

        if (answer.isNotBlank()) {
            Text("Answer", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp))
            // Selectable, so it can be copied by hand as well.
            SelectionContainer {
                Text(answer, style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp))
            }
            AnswerActions(answer)
        }

        // Suggestions show only until the assistant has been used once.
        if (!used && answer.isBlank()) {
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
