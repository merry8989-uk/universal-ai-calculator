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
import com.merry8989uk.universalcalculator.calc.FeeMode
import com.merry8989uk.universalcalculator.calc.Penalty
import com.merry8989uk.universalcalculator.calc.Tier
import androidx.compose.material3.Button
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.merry8989uk.universalcalculator.calc.DayCountBasis
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

    var dayCount by remember { mutableStateOf(DayCountBasis.ACTUAL_365) }
    var compoundOn by remember { mutableStateOf(false) }
    var compoundUnit by remember { mutableStateOf(TimeUnit.MONTH) }

    // the five from the PDF's expansion matrix
    var feeMode by remember { mutableStateOf(FeeMode.ADDED_TO_TOTAL) }
    var tdsText by remember { mutableStateOf("0") }
    var tierOn by remember { mutableStateOf(false) }
    var tierDaysText by remember { mutableStateOf("10") }
    var tierRateText by remember { mutableStateOf("3") }
    var penaltyOn by remember { mutableStateOf(false) }
    var penaltyRateText by remember { mutableStateOf("0.1") }
    var penaltyDaysText by remember { mutableStateOf("5") }
    var instalmentsOn by remember { mutableStateOf(false) }
    var instalmentUnit by remember { mutableStateOf(TimeUnit.MONTH) }
    var csvMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            feeMode = feeMode,
            tdsPercent = tdsText.trim().toDoubleOrNull() ?: 0.0,
            tiers = if (tierOn && rate != null) {
                listOf(
                    Tier(tierDaysText.trim().toDoubleOrNull() ?: 10.0, rate),
                    Tier(
                        Double.MAX_VALUE,
                        RateSpec(
                            tierRateText.trim().toDoubleOrNull() ?: rate.value,
                            rate.per,
                            rate.customEvery,
                        ),
                    ),
                )
            } else {
                emptyList()
            },
            penalty = if (penaltyOn) {
                Penalty(
                    RateSpec(penaltyRateText.trim().toDoubleOrNull() ?: 0.0, TimeUnit.DAY),
                    penaltyDaysText.trim().toDoubleOrNull() ?: 0.0,
                )
            } else {
                null
            },
            instalmentEvery = if (instalmentsOn) instalmentUnit else null,
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
            options = DayCountBasis.entries.map { Choice(it.name, it.label, it.note) },
            selected = dayCount.name,
            moreLabel = "More conventions",
            onSelect = { name -> DayCountBasis.entries.firstOrNull { it.name == name }?.let { dayCount = it } },
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

        // ---- a rate that changes part way through ----
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            FilterChip(
                selected = tierOn,
                onClick = { tierOn = !tierOn },
                label = { Text("Rate changes part way") },
            )
        }
        if (tierOn) {
            Text(
                "For the first N days the rate above applies; after that, the rate below.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tierDaysText, onValueChange = { tierDaysText = it },
                    label = { Text("For the first (days)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = tierRateText, onValueChange = { tierRateText = it },
                    label = { Text("Then % per ${rateUnit.label.lowercase()}") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ---- a late charge ----
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            FilterChip(
                selected = penaltyOn,
                onClick = { penaltyOn = !penaltyOn },
                label = { Text("Late payment charge") },
            )
        }
        if (penaltyOn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = penaltyRateText, onValueChange = { penaltyRateText = it },
                    label = { Text("% per day") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = penaltyDaysText, onValueChange = { penaltyDaysText = it },
                    label = { Text("Days overdue") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ---- where the fee goes, and tax ----
        ChoiceRow(
            label = "The fee is",
            options = FeeMode.entries.map { Choice(it.name, it.label, it.note) },
            selected = feeMode.name,
            moreLabel = "Both ways",
            onSelect = { name -> FeeMode.entries.firstOrNull { it.name == name }?.let { feeMode = it } },
        )
        OutlinedTextField(
            value = tdsText, onValueChange = { tdsText = it },
            label = { Text("Tax withheld from the interest (%)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        // ---- how it is repaid ----
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            FilterChip(
                selected = !instalmentsOn,
                onClick = { instalmentsOn = false },
                label = { Text("One payment at the end") },
            )
            FilterChip(
                selected = instalmentsOn,
                onClick = { instalmentsOn = true },
                label = { Text("Equal instalments") },
            )
        }
        if (instalmentsOn) {
            ChoiceRow(
                label = "Instalments",
                options = unitChoices,
                selected = instalmentUnit.name,
                moreLabel = "More periods",
                onSelect = { name -> TimeUnit.entries.firstOrNull { it.name == name }?.let { instalmentUnit = it } },
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

                    if (breakdown.penaltyAmount > 0.0) {
                        Text(
                            "plus ${Format.number(breakdown.penaltyAmount)} late charge",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (breakdown.upfrontDeducted > 0.0) {
                        Text(
                            "You receive ${Format.number(breakdown.amountReceived)} after the fee",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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
                        if (breakdown.penaltyAmount > 0.0) {
                            add("Late charge ${Format.number(breakdown.penaltyAmount)}")
                        }
                        if (breakdown.tdsAmount > 0.0) {
                            add("Tax withheld ${Format.number(breakdown.tdsAmount)}")
                            add("Interest kept ${Format.number(breakdown.netInterest)}")
                        }
                        if (breakdown.upfrontDeducted > 0.0) {
                            add("Fee taken up front ${Format.number(breakdown.upfrontDeducted)}")
                            add("Received ${Format.number(breakdown.amountReceived)}")
                        }
                        add("Total payable ${Format.number(breakdown.totalAmount)}")
                        for ((label, amount) in breakdown.tierInterest) {
                            add("$label = ${Format.number(amount)}")
                        }
                        if (breakdown.schedule.isNotEmpty()) {
                            add(
                                "${breakdown.schedule.size} instalments of about " +
                                    Format.number(breakdown.instalmentAmount),
                            )
                        }
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
                    Button(
                        onClick = {
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    CsvExport.write(context, breakdown)
                                }
                                val problem = CsvExport.share(context, file)
                                csvMessage = problem ?: "Saved ${file.name}"
                            }
                        },
                        modifier = Modifier.padding(top = 6.dp),
                    ) { Text("Export the schedule as CSV") }
                    csvMessage?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall)
                    }
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
