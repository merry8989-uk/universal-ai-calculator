package com.merry8989uk.universalcalculator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.merry8989uk.universalcalculator.calc.DateRange
import com.merry8989uk.universalcalculator.calc.DayCount
import com.merry8989uk.universalcalculator.calc.Duration
import com.merry8989uk.universalcalculator.calc.DurationSpan
import com.merry8989uk.universalcalculator.calc.DynamicInterest
import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.RateSpec
import com.merry8989uk.universalcalculator.calc.TimeUnit
import com.merry8989uk.universalcalculator.settings.Choice
import com.merry8989uk.universalcalculator.settings.ChoiceRow
import java.time.LocalDate

/**
 * Interest, in whatever unit the money was actually lent in.
 *
 * The textbook formula is P × R × T / 100 with R per year and T in years. That
 * describes a bond, not the money people lend each other: 0.1% a day for 18 days,
 * 2% a month for 75 days, 1.5% a fortnight compounded fortnightly. Here the rate
 * has its own period, the duration has its own unit, and the two are reconciled
 * once, in days. Nothing is annualised behind the user's back — the annual figure
 * is shown beside the answer as a comparison, not used as a constant.
 */
@Composable
fun InterestScreen(onBack: () -> Unit) {
    var principalText by remember { mutableStateOf("100000") }
    var feesText by remember { mutableStateOf("0") }
    var rateText by remember { mutableStateOf("2") }
    var rateUnit by remember { mutableStateOf(TimeUnit.MONTH) }
    var customRateOn by remember { mutableStateOf(false) }
    var customRateEveryText by remember { mutableStateOf("10") }
    var customRateUnit by remember { mutableStateOf(TimeUnit.DAY) }

    var byDates by remember { mutableStateOf(false) }
    var timeText by remember { mutableStateOf("45") }
    var timeUnit by remember { mutableStateOf(TimeUnit.DAY) }
    var fromText by remember { mutableStateOf(LocalDate.now().toString()) }
    var toText by remember { mutableStateOf(LocalDate.now().plusDays(30).toString()) }
    var includeBoth by remember { mutableStateOf(false) }

    var dayCount by remember { mutableStateOf(DayCount.ACTUAL_365) }
    var compoundOn by remember { mutableStateOf(false) }
    var compoundUnit by remember { mutableStateOf(TimeUnit.MONTH) }

    val principal = principalText.trim().toDoubleOrNull()
    val fees = feesText.trim().toDoubleOrNull() ?: 0.0
    val rateValue = rateText.trim().toDoubleOrNull()
    val every = customRateEveryText.trim().toDoubleOrNull() ?: 1.0

    val rate = if (rateValue == null || rateValue < 0) null else {
        if (customRateOn) RateSpec(rateValue, customRateUnit, every) else RateSpec(rateValue, rateUnit)
    }

    val duration: Duration? = if (byDates) {
        val from = runCatching { LocalDate.parse(fromText.trim()) }.getOrNull()
        val to = runCatching { LocalDate.parse(toText.trim()) }.getOrNull()
        if (from == null || to == null) null else DateRange(from, to, includeBoth)
    } else {
        val v = timeText.trim().toDoubleOrNull()
        if (v == null || v < 0) null else DurationSpan(v, timeUnit)
    }

    val breakdown = if (principal != null && principal > 0 && rate != null && duration != null) {
        DynamicInterest.compute(
            principal = principal,
            rate = rate,
            duration = duration,
            fees = fees,
            dayCount = dayCount,
            compoundEvery = if (compoundOn) compoundUnit else null,
        )
    } else {
        null
    }

    // only the units that make sense for a rate period are offered first; the
    // rest are one tap away rather than never reachable
    val unitChoices = ArrayList<Choice>()
    for (u in TimeUnit.entries) unitChoices.add(Choice(u.name, u.label))

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("\u2190 Back") }
        Text("Interest calculator", style = MaterialTheme.typography.titleLarge)
        Text(
            "Any rate period, any duration. 0.1% a day for 18 days, or 2% a month for 75 days — " +
                "neither is converted into a year behind your back.",
            style = MaterialTheme.typography.bodySmall,
        )

        // ---- principal ----
        Text("Principal and charges", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = principalText, onValueChange = { principalText = it },
                label = { Text("Principal") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = feesText, onValueChange = { feesText = it },
                label = { Text("Fees") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        // ---- the rate, with its own period ----
        Text("Interest rate", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        OutlinedTextField(
            value = rateText, onValueChange = { rateText = it },
            label = { Text("Rate, percent") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            FilterChip(
                selected = !customRateOn,
                onClick = { customRateOn = false },
                label = { Text("Per a unit") },
            )
            FilterChip(
                selected = customRateOn,
                onClick = { customRateOn = true },
                label = { Text("Every N units") },
            )
        }
        if (customRateOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedTextField(
                    value = customRateEveryText, onValueChange = { customRateEveryText = it },
                    label = { Text("Every N") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            ChoiceRow(
                label = "of",
                options = unitChoices,
                selected = customRateUnit.name,
                moreLabel = "More units",
                onSelect = { name -> TimeUnit.entries.firstOrNull { it.name == name }?.let { customRateUnit = it } },
            )
        } else {
            ChoiceRow(
                label = "The rate is charged",
                options = unitChoices,
                selected = rateUnit.name,
                moreLabel = "More units",
                onSelect = { name -> TimeUnit.entries.firstOrNull { it.name == name }?.let { rateUnit = it } },
            )
        }

        // ---- the duration ----
        Text("How long", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !byDates, onClick = { byDates = false }, label = { Text("A duration") })
            FilterChip(selected = byDates, onClick = { byDates = true }, label = { Text("Two dates") })
        }
        if (byDates) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedTextField(
                    value = fromText, onValueChange = { fromText = it },
                    label = { Text("From (yyyy-mm-dd)") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = toText, onValueChange = { toText = it },
                    label = { Text("To (yyyy-mm-dd)") }, singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = includeBoth,
                    onClick = { includeBoth = !includeBoth },
                    label = { Text("Count both the first and last day") },
                )
            }
        } else {
            OutlinedTextField(
                value = timeText, onValueChange = { timeText = it },
                label = { Text("Duration") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            ChoiceRow(
                label = "Measured in",
                options = unitChoices,
                selected = timeUnit.name,
                moreLabel = "More units",
                onSelect = { name -> TimeUnit.entries.firstOrNull { it.name == name }?.let { timeUnit = it } },
            )
        }

        // ---- the conventions that change the answer ----
        Text("Rules", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 14.dp))
        ChoiceRow(
            label = "Day count",
            options = DayCount.entries.map { Choice(it.name, it.label, it.note) },
            selected = dayCount.name,
            moreLabel = "More conventions",
            onSelect = { name -> DayCount.entries.firstOrNull { it.name == name }?.let { dayCount = it } },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            FilterChip(
                selected = !compoundOn,
                onClick = { compoundOn = false },
                label = { Text("Simple interest") },
            )
            FilterChip(
                selected = compoundOn,
                onClick = { compoundOn = true },
                label = { Text("Compound it") },
            )
        }
        if (compoundOn) {
            ChoiceRow(
                label = "Compounded every",
                options = unitChoices,
                selected = compoundUnit.name,
                moreLabel = "More units",
                onSelect = { name -> TimeUnit.entries.firstOrNull { it.name == name }?.let { compoundUnit = it } },
            )
        }

        // ---- the answer ----
        Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (breakdown == null) {
                    Text(
                        "Fill in the principal, the rate and how long, and the answer appears here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Copyable(
                        text = Format.number(breakdown.totalInterest),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text("interest", style = MaterialTheme.typography.labelMedium)
                    Copyable(
                        text = Format.number(breakdown.totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        "total payable, including ${Format.number(breakdown.fees)} in fees",
                        style = MaterialTheme.typography.labelMedium,
                    )

                    val lines = buildList {
                        add("Principal ${Format.number(breakdown.principal)}")
                        add("Rate ${rateText.trim()}% every ${Format.number(breakdown.daysPerRateCharge)} days")
                        add("Daily rate ${Format.number(breakdown.dailyRatePercent)}%")
                        add("Duration ${Format.number(breakdown.totalDays)} days")
                        add("Simple interest ${Format.number(breakdown.simpleInterest)}")
                        if (breakdown.compoundInterest != null) {
                            add(
                                "Compounded ${Format.number(breakdown.compoundInterest)} over " +
                                    "${Format.number(breakdown.compoundingPeriods ?: 0.0)} periods",
                            )
                        }
                        add("Interest per day ${Format.number(breakdown.dailyInterestAmount)}")
                        add("That is ${Format.number(breakdown.effectiveAnnualPercent)}% a year")
                        add("and ${Format.number(breakdown.effectiveMonthlyPercent)}% a month")
                        add("Total payable ${Format.number(breakdown.totalAmount)}")
                    }
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        for (line in lines) {
                            Text(
                                line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                    CopyShareRow(
                        text = lines.joinToString("\n"),
                        subject = "Interest calculation",
                    )
                }
            }
        }
        Text(
            "The yearly and monthly figures are there so the rate can be compared with a loan " +
                "quoted the usual way. They are not used in the calculation.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
