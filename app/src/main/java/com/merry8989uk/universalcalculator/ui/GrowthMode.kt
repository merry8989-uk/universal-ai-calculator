package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.merry8989uk.universalcalculator.calc.Compounding
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.Growth
import com.merry8989uk.universalcalculator.calc.InterestTreatment
import com.merry8989uk.universalcalculator.calc.RateBasis
import com.merry8989uk.universalcalculator.calc.TimeUnit
import com.merry8989uk.universalcalculator.settings.Choice
import com.merry8989uk.universalcalculator.settings.ChoiceRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The interest screen, in the two shapes the question comes in.
 *
 * **Growth** is "what does my money become" — a lump sum, a rate, a tenure, and
 * whether the interest is left in or taken out.
 *
 * **Lending** is "what does a loan cost me" — a rate that has its own period, a
 * duration in its own unit, a late charge, a fee, and instalments.
 *
 * They are different questions, so they are different screens rather than one
 * screen with eleven inputs on it.
 */
@Composable
fun InterestScreen(onBack: () -> Unit) {
    var growth by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        ) {
            FilterChip(selected = growth, onClick = { growth = true }, label = { Text("Growth") })
            FilterChip(selected = !growth, onClick = { growth = false }, label = { Text("Lending") })
        }
        if (growth) GrowthMode(onBack) else LendingMode(onBack)
    }
}

/**
 * What a lump sum becomes.
 *
 * The one thing this screen insists on is the difference between a nominal and
 * an effective rate. "12% a year, monthly" is either 1% a month — which is
 * 12.68% over the year — or it is 12% over the year and each month is 0.95%.
 * Both are what people mean, and a calculator that picks one silently is wrong
 * for half the people using it.
 */
@Composable
internal fun GrowthMode(onBack: () -> Unit) {
    var principalText by remember { mutableStateOf("50000") }
    var rateText by remember { mutableStateOf("8") }
    var basis by remember { mutableStateOf(RateBasis.NOMINAL) }
    var tenureText by remember { mutableStateOf("1") }
    var tenureUnit by remember { mutableStateOf(TimeUnit.YEAR) }
    var compounding by remember { mutableStateOf(Compounding.WEEKLY) }
    var treatment by remember { mutableStateOf(InterestTreatment.REINVESTED) }
    var extraText by remember { mutableStateOf("0") }
    // 0 = period, 1 = week, 2 = year
    var tableView by remember { mutableStateOf(0) }
    var csvMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val principal = principalText.trim().toDoubleOrNull()
    val rate = rateText.trim().toDoubleOrNull()
    val tenure = tenureText.trim().toDoubleOrNull()
    val extra = extraText.trim().toDoubleOrNull() ?: 0.0

    val plan = if (principal != null && principal > 0 && rate != null && tenure != null && tenure > 0) {
        Growth.plan(
            principal = principal,
            annualRatePercent = rate,
            years = Growth.years(tenure, tenureUnit),
            compounding = compounding,
            basis = basis,
            treatment = treatment,
            extraDeposit = extra,
        )
    } else {
        null
    }

    val unitChoices = ArrayList<Choice>()
    for (u in listOf(TimeUnit.DAY, TimeUnit.WEEK, TimeUnit.MONTH, TimeUnit.QUARTER, TimeUnit.YEAR)) {
        unitChoices.add(Choice(u.name, u.label))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("What your money becomes", style = MaterialTheme.typography.titleLarge)

        // ---- the deposit ----
        Text("Your deposit", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        OutlinedTextField(
            value = principalText, onValueChange = { principalText = it },
            label = { Text("Principal amount") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            for ((label, value) in listOf("25,000" to "25000", "50,000" to "50000", "1,00,000" to "100000", "5,00,000" to "500000")) {
                FilterChip(
                    selected = principalText == value,
                    onClick = { principalText = value },
                    label = { Text(label) },
                )
            }
        }

        // ---- the rate, and what the sentence means ----
        Text("Interest rate (% per year)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        OutlinedTextField(
            value = rateText, onValueChange = { rateText = it },
            label = { Text("Rate") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        ChoiceRow(
            label = "How is the annual rate defined?",
            options = RateBasis.entries.map { Choice(it.name, it.label, it.note) },
            selected = basis.name,
            moreLabel = "Both readings",
            onSelect = { name -> RateBasis.entries.firstOrNull { it.name == name }?.let { basis = it } },
        )

        // ---- the tenure ----
        Text("Tenure", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        OutlinedTextField(
            value = tenureText, onValueChange = { tenureText = it },
            label = { Text("How long") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        ChoiceRow(
            label = "Measured in",
            options = unitChoices,
            selected = tenureUnit.name,
            moreLabel = "More units",
            onSelect = { name -> unitChoices.firstOrNull { it.value == name }?.let { tenureUnit = TimeUnit.valueOf(it.value) } },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            val quick = when (tenureUnit) {
                TimeUnit.WEEK -> listOf("4 weeks" to "4", "26 weeks" to "26", "52 weeks" to "52")
                TimeUnit.MONTH -> listOf("3 months" to "3", "6 months" to "6", "1 year" to "12", "5 years" to "60")
                else -> listOf("6 months" to "0.5", "1 year" to "1", "5 years" to "5", "10 years" to "10")
            }
            for ((label, value) in quick) {
                FilterChip(
                    selected = tenureText == value,
                    onClick = { tenureText = value },
                    label = { Text(label) },
                )
            }
        }

        // ---- how often, and what happens to the interest ----
        Text("Compounding frequency", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        ChoiceRow(
            label = "Interest is worked out",
            options = Compounding.entries.map { Choice(it.name, it.label) },
            selected = compounding.name,
            moreLabel = "All frequencies",
            onSelect = { name -> Compounding.entries.firstOrNull { it.name == name }?.let { compounding = it } },
        )
        ChoiceRow(
            label = "What happens to the interest?",
            options = InterestTreatment.entries.map { Choice(it.name, it.label, it.note) },
            selected = treatment.name,
            moreLabel = "Both ways",
            onSelect = { name -> InterestTreatment.entries.firstOrNull { it.name == name }?.let { treatment = it } },
        )

        OutlinedTextField(
            value = extraText, onValueChange = { extraText = it },
            label = { Text("Extra deposit every period") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        )

        // ---- the answer ----
        Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (plan == null) {
                    Text(
                        "Fill in the deposit, the rate and how long, and the answer appears here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Copyable(
                        text = Format.number(plan.maturity),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("at maturity", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "Interest earned ${Format.number(plan.totalInterest)} on " +
                            "${Format.number(plan.invested)} invested",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (plan.totalDeposits > 0.0) {
                        Text(
                            "includes ${Format.number(plan.totalDeposits)} of extra deposits",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Text(
                        "That is ${Format.number(plan.apy * 100.0)}% a year in the end",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "${plan.periods} periods of ${Format.number(plan.ratePerPeriod * 100.0)}%",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Plain simple interest would have paid ${Format.number(plan.simpleBenchmark)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        FilterChip(selected = tableView == 0, onClick = { tableView = 0 }, label = { Text("Period-wise") })
                        FilterChip(selected = tableView == 1, onClick = { tableView = 1 }, label = { Text("Week-wise") })
                        FilterChip(selected = tableView == 2, onClick = { tableView = 2 }, label = { Text("Year-wise") })
                    }

                    val rows = when (tableView) {
                        1 -> plan.byWeek()
                        2 -> plan.byYear()
                        else -> plan.rows
                    }
                    val unitName = when (tableView) {
                        1 -> "Week"
                        2 -> "Year"
                        else -> "Period"
                    }
                    Text(
                        "The whole table \u2014 ${rows.size} rows. Export as CSV for a spreadsheet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                unitName,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text("Interest", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text("Closing", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.4f))
                        }
                        for (r in rows) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                Text(
                                    "${r.period}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    Format.number(r.interest),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    Format.number(r.closing),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1.4f),
                                )
                            }
                        }
                    }

                    CopyShareRow(
                        text = "Deposit ${Format.number(plan.principal)} at ${rateText.trim()}% a year " +
                            "(${basis.label.lowercase()}), ${compounding.label.lowercase()} compounding, " +
                            "for ${plan.years} years.\n" +
                            "Maturity ${Format.number(plan.maturity)}, " +
                            "interest ${Format.number(plan.totalInterest)}, " +
                            "which is ${Format.number(plan.apy * 100.0)}% a year.",
                        subject = "Growth plan",
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                val file = withContext(Dispatchers.IO) { CsvExport.writeGrowth(context, plan) }
                                val problem = CsvExport.share(context, file)
                                csvMessage = problem ?: "Saved ${file.name}"
                            }
                        },
                        modifier = Modifier.padding(top = 6.dp),
                    ) { Text("Export the schedule as CSV") }
                    csvMessage?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}
