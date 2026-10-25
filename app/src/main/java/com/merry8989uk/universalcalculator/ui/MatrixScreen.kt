package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.AdvancedMatrix
import com.merry8989uk.universalcalculator.calc.Complex
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Matrix
import com.merry8989uk.universalcalculator.calc.NamedMatrix
import com.merry8989uk.universalcalculator.data.MatrixStore
import androidx.compose.ui.platform.LocalContext

/**
 * The matrix calculator, in the three parts the reference uses.
 *
 *   NAMES   the matrices the user has, and their shape
 *   EDIT    one of them, cell by cell
 *   MATH    what to do with them
 *
 * The point of the names is that a result can be kept: whatever an operation
 * works out lands in MatAns, and can then be used as an input to the next one.
 */
@Composable
fun MatrixScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var matrices by remember { mutableStateOf(MatrixStore.load(context)) }
    var selected by remember { mutableStateOf(matrices.firstOrNull()?.name ?: "MatA") }
    var tab by remember { mutableStateOf("NAMES") }
    var result by remember { mutableStateOf("") }
    var power by remember { mutableStateOf("3") }
    var renameTo by remember { mutableStateOf("") }
    var newRows by remember { mutableStateOf("3") }
    var newCols by remember { mutableStateOf("3") }

    fun persist(next: List<NamedMatrix>) {
        matrices = next
        MatrixStore.save(context, next)
    }

    fun current(): NamedMatrix? = matrices.firstOrNull { it.name == selected }

    fun replace(next: NamedMatrix) {
        persist(matrices.map { if (it.name == next.name) next else it })
    }

    fun put(name: String, a: Array<DoubleArray>) {
        val next = NamedMatrix.of(name, a)
        persist(if (matrices.any { it.name == name }) matrices.map { if (it.name == name) next else it } else matrices + next)
        selected = name
    }

    fun run(block: () -> String) {
        result = try { block() } catch (e: Exception) { "Error: ${e.message}" }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 " + stringResourceSafe("Back")) }
        Text("Matrices", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (t in listOf("NAMES", "EDIT", "MATH")) {
                FilterChip(
                    selected = tab == t,
                    onClick = { tab = t; result = "" },
                    label = { Text(t) },
                )
            }
        }

        when (tab) {
            "NAMES" -> {
                Text(
                    "A named matrix can be used again. Whatever an operation works out " +
                        "lands in MatAns.",
                    style = MaterialTheme.typography.bodySmall,
                )
                matrices.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = m.name; tab = "EDIT" }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            (if (m.name == selected) "\u25CF  " else "     ") + m.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                        )
                        Text(m.shape, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text("Rename \u201C$selected\u201D", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = renameTo,
                    onValueChange = { renameTo = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val name = renameTo.trim()
                        val m = current()
                        if (name.isNotEmpty() && m != null && matrices.none { it.name == name }) {
                            persist(matrices.map { if (it.name == m.name) m.copy(name = name) else it })
                            selected = name
                            renameTo = ""
                        }
                    }) { Text("Rename") }
                    Button(onClick = {
                        val m = current()
                        if (m != null && matrices.size > 1) {
                            val next = matrices.filterNot { it.name == m.name }
                            persist(next)
                            selected = next.first().name
                        }
                    }) { Text("Delete") }
                }

                Text("A new matrix", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newRows, onValueChange = { newRows = it },
                        label = { Text("Rows") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = newCols, onValueChange = { newCols = it },
                        label = { Text("Columns") }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(onClick = {
                    val h = newRows.trim().toIntOrNull()?.coerceIn(1, 10) ?: 3
                    val w = newCols.trim().toIntOrNull()?.coerceIn(1, 10) ?: 3
                    var name = "MatE"
                    var n = 0
                    while (matrices.any { it.name == name }) { n++; name = "Mat${'A' + (4 + n) % 26}" }
                    put(name, Array(h) { DoubleArray(w) })
                    tab = "EDIT"
                }) { Text("Add") }
            }

            "EDIT" -> {
                val m = current()
                if (m == null) {
                    Text("Pick a matrix on the NAMES tab.")
                } else {
                    Text(
                        "${m.name}  \u00B7  ${m.shape}  \u00B7  one row per line, cells separated by spaces",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    var draft by remember(m.name, m.shape) { mutableStateOf(m.text()) }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text("Cells") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            run {
                                val rows = draft.lines().filter { it.isNotBlank() }.map { line ->
                                    line.trim().split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
                                        .map { it.toDoubleOrNull() ?: 0.0 }
                                }
                                if (rows.isEmpty() || rows.any { it.size != rows.first().size }) {
                                    "Every row has to be the same length."
                                } else {
                                    replace(NamedMatrix(m.name, rows))
                                    "Saved ${m.name} (${rows.size}\u00D7${rows.first().size})."
                                }
                            }
                        }) { Text("Save") }
                        Button(onClick = { draft = m.text() }) { Text("Undo") }
                        Button(onClick = { run { "The transpose is in MatAns.\n" + Matrix.format(
                            Matrix.transpose(m.toArray()).also { put("MatAns", it) },
                        ) } }) { Text("Transpose") }
                    }
                    if (result.isNotBlank()) {
                        Text(result, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }

            else -> {
                Text(
                    "Operations use the named matrices. The answer goes into MatAns.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("Working with: $selected", style = MaterialTheme.typography.bodyMedium)

                OutlinedTextField(
                    value = power, onValueChange = { power = it },
                    label = { Text("Power n") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { run { "det($selected) = " + Format.number(
                        Matrix.determinant(current()?.toArray() ?: Array(0) { DoubleArray(0) }),
                    ) } }) { Text("det") }
                    Button(onClick = { run { "trace($selected) = " + Format.number(
                        Matrix.trace(current()?.toArray() ?: Array(0) { DoubleArray(0) }),
                    ) } }) { Text("trace") }
                    Button(onClick = { run {
                        val inv = Matrix.inverse(current()?.toArray() ?: Array(0) { DoubleArray(0) })
                            ?: return@run "$selected is singular (no inverse)."
                        put("MatAns", inv)
                        "The inverse is in MatAns.\n" + Matrix.format(inv)
                    } }) { Text("Inverse") }
                    Button(onClick = { run {
                        val t = Matrix.transpose(current()?.toArray() ?: Array(0) { DoubleArray(0) })
                        put("MatAns", t)
                        "The transpose is in MatAns.\n" + Matrix.format(t)
                    } }) { Text("Transpose") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { run {
                        val e = Matrix.eigenvalues(current()?.toArray() ?: Array(0) { DoubleArray(0) })
                        if (e.isEmpty()) "No eigenvalues could be resolved." else
                            "Eigenvalues:\n" + e.joinToString("\n") { "\u03BB = " + Complex.text(it) }
                    } }) { Text("Eigenvalues") }
                    Button(onClick = { run {
                        val l = AdvancedMatrix.cholesky(current()?.toArray() ?: Array(0) { DoubleArray(0) })
                            ?: return@run "$selected has no Cholesky factor: it is not symmetric and positive definite."
                        put("MatAns", l)
                        "A = L\u00B7L\u1D40, with L =\n" + Matrix.format(l)
                    } }) { Text("Cholesky") }
                    Button(onClick = { run {
                        val n = power.trim().toIntOrNull() ?: return@run "The power needs a whole number."
                        val r = AdvancedMatrix.power(current()?.toArray() ?: Array(0) { DoubleArray(0) }, n)
                            ?: return@run "That power could not be worked out."
                        put("MatAns", r)
                        "$selected^$n is in MatAns.\n" + Matrix.format(r)
                    } }) { Text("A^n") }
                    Button(onClick = { run {
                        val r = AdvancedMatrix.expm(current()?.toArray() ?: Array(0) { DoubleArray(0) })
                            ?: return@run "e^A could not be worked out."
                        put("MatAns", r)
                        "e^A is in MatAns.\n" + Matrix.format(r)
                    } }) { Text("e^A") }
                }

                Text("Two at a time", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val a = matrices.getOrNull(0)
                    val b = matrices.getOrNull(1)
                    Button(onClick = { run {
                        if (a == null || b == null) return@run "Two matrices are needed."
                        if (a.shape != b.shape) return@run "${a.name} is ${a.shape} and ${b.name} is ${b.shape}."
                        val r = Matrix.add(a.toArray(), b.toArray())
                        put("MatAns", r)
                        "${a.name} + ${b.name} is in MatAns.\n" + Matrix.format(r)
                    } }) { Text("${a?.name ?: "A"} + ${b?.name ?: "B"}") }
                    Button(onClick = { run {
                        if (a == null || b == null) return@run "Two matrices are needed."
                        val r = Matrix.mul(a.toArray(), b.toArray())
                        put("MatAns", r)
                        "${a.name} \u00D7 ${b.name} is in MatAns.\n" + Matrix.format(r)
                    } }) { Text("${a?.name ?: "A"} \u00D7 ${b?.name ?: "B"}") }
                }

                if (result.isNotBlank()) {
                    Text(
                        result,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

/** The back label, which the app already has in every language. */
@Composable
private fun stringResourceSafe(key: String): String =
    androidx.compose.ui.res.stringResource(
        when (key) {
            "Back" -> com.merry8989uk.universalcalculator.R.string.back
            else -> com.merry8989uk.universalcalculator.R.string.back
        },
    )
