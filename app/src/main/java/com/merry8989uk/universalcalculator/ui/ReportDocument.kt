package com.merry8989uk.universalcalculator.ui

import com.merry8989uk.universalcalculator.calc.Format
import com.merry8989uk.universalcalculator.calc.GrowthPlan
import com.merry8989uk.universalcalculator.calc.InterestBreakdown

/**
 * The document a person would actually hand to someone.
 *
 * A printout is not the screen: it has no buttons, it is read on paper, and the
 * whole point of it is that somebody else can follow the arithmetic. So it
 * carries the inputs, the answer, the working and the whole schedule, in that
 * order, with nothing to press.
 */
object ReportDocument {

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun page(title: String, body: String): String = """
<!doctype html>
<html><head><meta charset="utf-8"><title>${esc(title)}</title>
<style>
  body { font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
         color: #1f2328; margin: 0; font-size: 11pt; }
  h1 { font-size: 18pt; margin: 0 0 2pt; }
  .sub { color: #656d76; font-size: 9pt; margin-bottom: 14pt; }
  h2 { font-size: 12pt; margin: 16pt 0 6pt; border-bottom: 1px solid #d0d7de;
       padding-bottom: 3pt; }
  table { border-collapse: collapse; width: 100%; font-size: 9pt; }
  th, td { text-align: left; padding: 4pt 6pt; border-bottom: 1px solid #e6e8eb; }
  th { background: #f6f8fa; font-weight: 600; }
  td.n, th.n { text-align: right; font-variant-numeric: tabular-nums; }
  .row { display: flex; justify-content: space-between; padding: 3pt 0;
         border-bottom: 1px solid #e6e8eb; }
  .row .k { color: #656d76; }
  .big { font-size: 20pt; font-weight: 600; margin: 4pt 0; }
  .note { color: #656d76; font-size: 8.5pt; margin-top: 14pt; }
  tr { page-break-inside: avoid; }
  thead { display: table-header-group; }
</style></head><body>
$body
<p class="note">Nexus Calc. The yearly and monthly figures are there so a rate can be
compared with one quoted the usual way.</p>
</body></html>
"""

    /**
     * An amount with its thousands grouped, the way it is written on paper.
     *
     * Format.number deliberately gives a bare 50000 — it is a calculator's
     * readout, where every character is typed and space is short. A printed
     * statement is read, not typed, and 1,00,000 is easier to check than 100000.
     */
    private fun money(v: Double): String =
        String.format(java.util.Locale("en", "IN"), "%,.2f", v)

    private fun row(k: String, v: String) =
        "<div class=\"row\"><span class=\"k\">${esc(k)}</span><span>${esc(v)}</span></div>"

    /** A growth plan, ready to print. */
    fun growth(plan: GrowthPlan, rateText: String, basis: String, compounding: String, years: String): String {
        val body = StringBuilder()
        body.append("<h1>What your money becomes</h1>")
        body.append("<p class=\"sub\">Nexus Calc \u00B7 growth plan</p>")

        body.append("<h2>The arrangement</h2>")
        body.append(row("Deposit", money(plan.principal)))
        body.append(row("Rate", "$rateText% a year, $basis"))
        body.append(row("Compounding", compounding))
        body.append(row("Tenure", years))
        body.append(row("Extra deposit each period", money(plan.totalDeposits / plan.periods.coerceAtLeast(1))))

        body.append("<h2>The answer</h2>")
        body.append("<div class=\"big\">${esc(money(plan.maturity))}</div>")
        body.append(row("Invested", money(plan.invested)))
        body.append(row("Interest earned", money(plan.totalInterest)))
        body.append(row("Growth a year", Format.number(plan.apy * 100.0) + "%"))
        body.append(row("Rate each period", Format.number(plan.ratePerPeriod * 100.0) + "%"))
        body.append(row("Periods", plan.periods.toString()))
        body.append(row("Simple interest would have paid", money(plan.simpleBenchmark)))

        body.append("<h2>The schedule</h2>")
        body.append("<table><thead><tr><th>Period</th><th>Year</th><th class=\"n\">Opening</th>")
        body.append("<th class=\"n\">Interest</th><th class=\"n\">Closing</th></tr></thead><tbody>")
        for (r in plan.rows) {
            body.append("<tr><td>${r.period}</td><td>${r.year}</td>")
            body.append("<td class=\"n\">${esc(money(r.opening))}</td>")
            body.append("<td class=\"n\">${esc(money(r.interest))}</td>")
            body.append("<td class=\"n\">${esc(money(r.closing))}</td></tr>")
        }
        body.append("</tbody></table>")
        return page("Growth plan", body.toString())
    }

    /** A lending calculation, ready to print. */
    fun lending(b: InterestBreakdown, rateText: String): String {
        val body = StringBuilder()
        body.append("<h1>Interest calculation</h1>")
        body.append("<p class=\"sub\">Nexus Calc \u00B7 loan and interest</p>")

        body.append("<h2>The arrangement</h2>")
        body.append(row("Principal", money(b.principal)))
        body.append(row("Rate", "$rateText% every ${money(b.daysPerRateCharge)} days"))
        body.append(row("Daily rate", Format.number(b.dailyRatePercent) + "%"))
        body.append(row("Duration", money(b.totalDays) + " days"))
        body.append(row("Fees", money(b.fees)))

        body.append("<h2>The answer</h2>")
        body.append("<div class=\"big\">${esc(money(b.totalInterest))}</div>")
        body.append(row("Interest", money(b.totalInterest)))
        body.append(row("Interest a day", money(b.dailyInterestAmount)))
        body.append(row("Total payable", money(b.totalAmount)))
        if (b.penaltyAmount > 0.0) body.append(row("Late charge", money(b.penaltyAmount)))
        if (b.tdsAmount > 0.0) body.append(row("Tax withheld", money(b.tdsAmount)))
        if (b.upfrontDeducted > 0.0) body.append(row("Received after the fee", money(b.amountReceived)))
        body.append(row("That is a year", Format.number(b.effectiveAnnualPercent) + "%"))
        body.append(row("That is a month", Format.number(b.effectiveMonthlyPercent) + "%"))

        if (b.tierInterest.isNotEmpty()) {
            body.append("<h2>The rate bands</h2><table><thead><tr><th>Band</th>")
            body.append("<th class=\"n\">Interest</th></tr></thead><tbody>")
            for ((label, amount) in b.tierInterest) {
                body.append("<tr><td>${esc(label)}</td>")
                body.append("<td class=\"n\">${esc(money(amount))}</td></tr>")
            }
            body.append("</tbody></table>")
        }

        if (b.schedule.isNotEmpty()) {
            body.append("<h2>The instalments</h2>")
            body.append("<table><thead><tr><th>Period</th><th class=\"n\">Opening</th>")
            body.append("<th class=\"n\">Interest</th><th class=\"n\">Principal</th>")
            body.append("<th class=\"n\">Instalment</th><th class=\"n\">Closing</th></tr></thead><tbody>")
            for (r in b.schedule) {
                body.append("<tr><td>${r.period}</td>")
                body.append("<td class=\"n\">${esc(money(r.opening))}</td>")
                body.append("<td class=\"n\">${esc(money(r.interest))}</td>")
                body.append("<td class=\"n\">${esc(money(r.principal))}</td>")
                body.append("<td class=\"n\">${esc(money(r.instalment))}</td>")
                body.append("<td class=\"n\">${esc(money(r.closing))}</td></tr>")
            }
            body.append("</tbody></table>")
        }
        return page("Interest calculation", body.toString())
    }
}
