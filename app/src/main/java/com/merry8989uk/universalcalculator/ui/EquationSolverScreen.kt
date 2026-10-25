package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.EquationSolver
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.LinearSolution
import com.merry8989uk.universalcalculator.calc.PolySolution
import com.merry8989uk.universalcalculator.calc.ResultDetail

/**
 * Solving equations: a polynomial in one unknown, several equations in as many
 * unknowns, or an inequality.
 *
 * Each of the three names its answer rather than just printing numbers — "two
 * distinct real roots", "no value of x works" — because the useful part is
 * knowing what kind of answer you got. Whole-number roots also open the detail
 * panel, which is where the prime factorisation and the other bases live.
 */
@Composable
internal fun EquationSolverScreen(
    onClose: () -> Unit,
    /** How the answer to an inequality is written. */
    notation: com.merry8989uk.universalcalculator.calc.IntervalNotation =
        com.merry8989uk.universalcalculator.calc.IntervalNotation.PARENTHESES,
) {
    var kind by remember { mutableStateOf("poly") }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Solve", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(kind == "poly", { kind = "poly" }, { Text("Polynomial") })
                FilterChip(kind == "system", { kind = "system" }, { Text("Simultaneous") })
                FilterChip(kind == "ineq", { kind = "ineq" }, { Text("Inequality") })
            }
        }

        when (kind) {
            "poly" -> PolynomialSolver()
            "system" -> SimultaneousSolver()
            else -> InequalitySolver(notation)
        }

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Back to the tools") }
    }
}

// ---------------- polynomial ----------------

@Composable
private fun PolynomialSolver() {
    var degree by remember { mutableStateOf(2) }
    // coefficients, highest power first
    val coeffs = remember { mutableStateListOf("1", "0", "-4", "0", "0", "0") }
    var solution by remember { mutableStateOf<PolySolution?>(null) }
    var error by remember { mutableStateOf("") }

    Text(
        "Enter the coefficients, highest power first. For x\u00B2 \u2212 4 that is 1, 0, \u22124.",
        style = MaterialTheme.typography.bodySmall,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..EquationSolver.MAX_DEGREE).forEach { d ->
            FilterChip(degree == d, { degree = d }, { Text(EquationSolver.degreeName(d)) })
        }
    }

    val names = listOf("x\u2075", "x\u2074", "x\u00B3", "x\u00B2", "x", "const")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (0..degree).forEach { i ->
            val index = EquationSolver.MAX_DEGREE - degree + i
            OutlinedTextField(
                value = coeffs[index],
                onValueChange = { coeffs[index] = it },
                label = { Text(names[index]) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
    }

    Button(
        onClick = {
            error = ""
            val values = (0..degree)
                .map { coeffs[EquationSolver.MAX_DEGREE - degree + it].toDoubleOrNull() }
            if (values.any { it == null }) {
                error = "Every coefficient needs a number. Use 0 where there is no term."
                solution = null
            } else {
                solution = EquationSolver.polynomial(values.map { it!! }.toDoubleArray())
                if (solution == null) error = "That is not an equation: every coefficient is zero."
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Solve") }

    if (error.isNotEmpty()) {
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }

    solution?.let { sol ->
        HorizontalDivider()
        Text(
            "${EquationSolver.degreeName(sol.degree)} equation  \u00B7  ${sol.kind}",
            style = MaterialTheme.typography.titleSmall,
        )
        sol.roots.forEachIndexed { i, r ->
            Text(
                "x${subscript(i + 1)} = ${r.text}",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            )
        }
        sol.sumOfRoots?.let {
            Text("Sum of the roots = ${Format.number(it)}", style = MaterialTheme.typography.bodySmall)
        }
        sol.productOfRoots?.let {
            Text("Product of the roots = ${Format.number(it)}", style = MaterialTheme.typography.bodySmall)
        }

        // the extra facts, for any root that is a whole number
        val whole = sol.roots.filter { it.isWholeNumber }.map { Math.round(it.re) }
        if (whole.isNotEmpty()) {
            HorizontalDivider()
            Text("About the roots", style = MaterialTheme.typography.titleSmall)
            whole.forEach { n -> NumberFacts(n) }
        }
    }
}

/** The prime factorisation, the divisors and the other bases of a whole number. */
@Composable
private fun NumberFacts(n: Long) {
    val factors = ResultDetail.primeFactorisation(n)
    val divisors = ResultDetail.divisors(n)
    val prime = ResultDetail.isPrime(n)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("$n", style = MaterialTheme.typography.bodyLarge)
        Text(
            if (prime) "prime" else "prime factors: $factors",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
        if (divisors.size <= 24) {
            Text(
                "divisors: ${divisors.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        } else {
            Text(
                "${divisors.size} divisors",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (n >= 0) {
            Text(
                "bin ${ResultDetail.binary(n)}  \u00B7  oct ${ResultDetail.octal(n)}  \u00B7  hex ${ResultDetail.hex(n)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}

// ---------------- simultaneous ----------------

@Composable
private fun SimultaneousSolver() {
    var size by remember { mutableStateOf(2) }
    val grid = remember {
        mutableStateListOf(
            "1", "1", "10",
            "1", "-1", "2",
            "0", "0", "0",
            "0", "0", "0",
        )
    }
    var solution by remember { mutableStateOf<LinearSolution?>(null) }
    var error by remember { mutableStateOf("") }

    Text(
        "One row per equation. The last box in each row is what the row is equal to.",
        style = MaterialTheme.typography.bodySmall,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(2, 3, 4).forEach { n ->
            FilterChip(size == n, { size = n }, { Text("$n unknowns") })
        }
    }

    val letters = listOf("x", "y", "z", "w")
    (0 until size).forEach { r ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (0..size).forEach { c ->
                val index = r * 3 + c
                OutlinedTextField(
                    value = grid[index],
                    onValueChange = { grid[index] = it },
                    label = { Text(if (c == size) "=" else letters[c]) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    Button(
        onClick = {
            error = ""
            val a = Array(size) { r -> DoubleArray(size) { c -> grid[r * 3 + c].toDoubleOrNull() ?: Double.NaN } }
            val b = DoubleArray(size) { r -> grid[r * 3 + size].toDoubleOrNull() ?: Double.NaN }
            if (a.any { row -> row.any { it.isNaN() } } || b.any { it.isNaN() }) {
                error = "Every box needs a number. Use 0 where a variable is missing."
                solution = null
            } else {
                solution = EquationSolver.simultaneous(a, b)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Solve") }

    if (error.isNotEmpty()) {
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }

    when (val s = solution) {
        is LinearSolution.Unique -> {
            HorizontalDivider()
            Text("One answer each", style = MaterialTheme.typography.titleSmall)
            s.values.forEachIndexed { i, v ->
                Text(
                    "${letters[i]} = ${Format.number(v)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
        LinearSolution.None -> {
            HorizontalDivider()
            Text("No answer", style = MaterialTheme.typography.titleSmall)
            Text(
                "The equations contradict each other, so no set of values satisfies them all.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        LinearSolution.Infinite -> {
            HorizontalDivider()
            Text("Endless answers", style = MaterialTheme.typography.titleSmall)
            Text(
                "The equations repeat the same information, so there is a whole family of answers " +
                    "rather than one. Add an equation that says something new.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        null -> Unit
    }
}

// ---------------- inequality ----------------

@Composable
private fun InequalitySolver(
    notation: com.merry8989uk.universalcalculator.calc.IntervalNotation,
) {
    var quadratic by remember { mutableStateOf(true) }
    var a by remember { mutableStateOf("1") }
    var b by remember { mutableStateOf("0") }
    var c by remember { mutableStateOf("-4") }
    var lessThan by remember { mutableStateOf(true) }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(quadratic, { quadratic = true }, { Text("Quadratic") })
        FilterChip(!quadratic, { quadratic = false }, { Text("Linear") })
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = a, onValueChange = { a = it }, label = { Text(if (quadratic) "a (x\u00B2)" else "a (x)") },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = b, onValueChange = { b = it }, label = { Text("b") },
            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        if (quadratic) {
            OutlinedTextField(
                value = c, onValueChange = { c = it }, label = { Text("c") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilterChip(lessThan, { lessThan = true }, { Text("< 0") })
            FilterChip(!lessThan, { lessThan = false }, { Text("> 0") })
        }
    }

    Button(
        onClick = {
            error = ""
            val av = a.toDoubleOrNull()
            val bv = b.toDoubleOrNull()
            val cv = c.toDoubleOrNull() ?: 0.0
            if (av == null || bv == null || (quadratic && cv.isNaN())) {
                error = "Every box needs a number."
                answer = ""
            } else {
                answer = if (quadratic) {
                    EquationSolver.quadraticInequality(av, bv, cv, lessThan, notation)
                } else {
                    EquationSolver.linearInequality(av, bv, lessThan, notation)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Solve") }

    if (error.isNotEmpty()) {
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }

    if (answer.isNotEmpty()) {
        HorizontalDivider()
        Text("The answer", style = MaterialTheme.typography.titleSmall)
        answer.lines().forEach { line ->
            Text(line, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace))
        }
    }
}

/** Small subscript digits, for writing x₁ and x₂. */
private fun subscript(n: Int): String {
    val digits = "\u2080\u2081\u2082\u2083\u2084\u2085\u2086\u2087\u2088\u2089"
    return n.toString().map { digits[it - '0'] }.joinToString("")
}
