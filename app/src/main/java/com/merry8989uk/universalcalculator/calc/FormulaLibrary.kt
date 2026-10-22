package com.merry8989uk.universalcalculator.calc

import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.PI

data class InputSpec(
    val key: String,
    val label: String,
    val default: Double = 0.0,
    val hint: String = "",
)

data class OutputSpec(
    val label: String,
    val value: Double,
    val unit: String = "",
    val note: String = "",
)

class Formula(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val inputs: List<InputSpec>,
    val compute: (Map<String, Double>) -> List<OutputSpec>,
)

/**
 * The one-shot calculator library. Every entry is a pure function of its
 * inputs, so the same definition drives the UI, tests and the AI explainer.
 */
object FormulaLibrary {

    private fun v(m: Map<String, Double>, k: String) = m[k] ?: 0.0
    private fun out(label: String, value: Double, unit: String = "", note: String = "") =
        OutputSpec(label, value, unit, note)

    val categories = listOf("Everyday", "Finance", "Health", "Geometry", "Physics", "Math")

    val formulas: List<Formula> = listOf(

        // ---------------- Everyday ----------------
        Formula(
            "percent_of", "% of a number", "Everyday",
            "What is X percent of Y?",
            listOf(InputSpec("x", "Percent (%)", 15.0), InputSpec("y", "Of value", 200.0)),
        ) { m -> listOf(out("Result", v(m, "x") / 100.0 * v(m, "y"))) },

        Formula(
            "what_percent", "What percent", "Everyday",
            "A is what percent of B?",
            listOf(InputSpec("a", "Part", 25.0), InputSpec("b", "Whole", 200.0)),
        ) { m ->
            val b = v(m, "b")
            if (b == 0.0) listOf(out("Result", 0.0, note = "Whole must not be zero"))
            else listOf(out("Result", v(m, "a") / b * 100.0, "%"))
        },

        Formula(
            "percent_change", "Percentage change", "Everyday",
            "Percentage increase or decrease between two values.",
            listOf(InputSpec("from", "From", 100.0), InputSpec("to", "To", 125.0)),
        ) { m ->
            val f = v(m, "from")
            if (f == 0.0) listOf(out("Change", 0.0, note = "From must not be zero"))
            else listOf(out("Change", (v(m, "to") - f) / f * 100.0, "%"))
        },

        Formula(
            "discount", "Discount / sale price", "Everyday",
            "Final price after a discount.",
            listOf(InputSpec("price", "Price", 1500.0), InputSpec("off", "Discount (%)", 20.0)),
        ) { m ->
            val save = v(m, "price") * v(m, "off") / 100.0
            listOf(out("You save", save), out("Final price", v(m, "price") - save))
        },

        Formula(
            "tip_split", "Tip & bill split", "Everyday",
            "Tip on a bill and the share per person.",
            listOf(
                InputSpec("bill", "Bill", 1000.0),
                InputSpec("tip", "Tip (%)", 10.0),
                InputSpec("people", "People", 4.0),
            ),
        ) { m ->
            val people = v(m, "people").coerceAtLeast(1.0)
            val tip = v(m, "bill") * v(m, "tip") / 100.0
            listOf(
                out("Tip", tip),
                out("Total", v(m, "bill") + tip),
                out("Per person", (v(m, "bill") + tip) / people),
            )
        },

        Formula(
            "gst_add", "Add GST / tax", "Everyday",
            "Add a tax percentage to an amount.",
            listOf(InputSpec("amount", "Amount", 1000.0), InputSpec("gst", "GST (%)", 18.0)),
        ) { m ->
            val tax = v(m, "amount") * v(m, "gst") / 100.0
            listOf(out("Tax", tax), out("Total", v(m, "amount") + tax))
        },

        Formula(
            "gst_remove", "Remove GST / tax", "Everyday",
            "Split a tax-inclusive amount into base and tax.",
            listOf(InputSpec("total", "Tax-inclusive amount", 1180.0), InputSpec("gst", "GST (%)", 18.0)),
        ) { m ->
            val base = v(m, "total") / (1.0 + v(m, "gst") / 100.0)
            listOf(out("Base amount", base), out("Tax", v(m, "total") - base))
        },

        Formula(
            "profit", "Profit / loss", "Everyday",
            "Profit and margin from cost and selling price.",
            listOf(InputSpec("cost", "Cost price", 800.0), InputSpec("sell", "Selling price", 1000.0)),
        ) { m ->
            val p = v(m, "sell") - v(m, "cost")
            val margin = if (v(m, "cost") == 0.0) 0.0 else p / v(m, "cost") * 100.0
            listOf(out("Profit", p), out("Margin", margin, "%"))
        },

        Formula(
            "fuel_cost", "Fuel cost", "Everyday",
            "Litres and cost of a trip from distance and mileage.",
            listOf(
                InputSpec("distance", "Distance (km)", 300.0),
                InputSpec("mileage", "Mileage (km/L)", 15.0),
                InputSpec("price", "Fuel price (per L)", 100.0),
            ),
        ) { m ->
            val litres = if (v(m, "mileage") == 0.0) 0.0 else v(m, "distance") / v(m, "mileage")
            listOf(out("Fuel needed", litres, "L"), out("Trip cost", litres * v(m, "price")))
        },

        // ---------------- Finance ----------------
        Formula(
            "simple_interest", "Simple interest", "Finance",
            "Interest = P x R x T / 100.",
            listOf(
                InputSpec("p", "Principal", 100000.0),
                InputSpec("r", "Rate (% per year)", 8.0),
                InputSpec("t", "Time (years)", 5.0),
            ),
        ) { m ->
            val i = v(m, "p") * v(m, "r") * v(m, "t") / 100.0
            listOf(out("Interest", i), out("Total", v(m, "p") + i))
        },

        Formula(
            "compound_interest", "Compound interest", "Finance",
            "A = P(1 + r/n)^(nt).",
            listOf(
                InputSpec("p", "Principal", 100000.0),
                InputSpec("r", "Rate (% per year)", 8.0),
                InputSpec("t", "Time (years)", 5.0),
                InputSpec("n", "Compounds per year", 4.0),
            ),
        ) { m ->
            val rate = v(m, "r") / 100.0
            val n = v(m, "n").coerceAtLeast(1.0)
            val a = v(m, "p") * (1.0 + rate / n).pow(n * v(m, "t"))
            listOf(out("Maturity", a), out("Interest", a - v(m, "p")))
        },

        Formula(
            "emi", "Loan EMI", "Finance",
            "Equated monthly instalment for a loan.",
            listOf(
                InputSpec("p", "Loan amount", 500000.0),
                InputSpec("r", "Annual interest (%)", 9.0),
                InputSpec("t", "Tenure (years)", 5.0),
            ),
        ) { m ->
            val n = v(m, "t") * 12.0
            val i = v(m, "r") / 100.0 / 12.0
            val emi = if (i == 0.0) v(m, "p") / n
            else v(m, "p") * i * (1.0 + i).pow(n) / ((1.0 + i).pow(n) - 1.0)
            listOf(
                out("Monthly EMI", emi),
                out("Total payment", emi * n),
                out("Total interest", emi * n - v(m, "p")),
            )
        },

        Formula(
            "sip", "SIP / recurring investment", "Finance",
            "Future value of a monthly investment.",
            listOf(
                InputSpec("m", "Monthly investment", 5000.0),
                InputSpec("r", "Expected return (% per year)", 12.0),
                InputSpec("t", "Years", 10.0),
            ),
        ) { m ->
            val n = v(m, "t") * 12.0
            val i = v(m, "r") / 100.0 / 12.0
            val fv = if (i == 0.0) v(m, "m") * n
            else v(m, "m") * ((1.0 + i).pow(n) - 1.0) / i * (1.0 + i)
            listOf(
                out("Invested", v(m, "m") * n),
                out("Future value", fv),
                out("Estimated gain", fv - v(m, "m") * n),
            )
        },

        Formula(
            "cagr", "CAGR", "Finance",
            "Compound annual growth rate.",
            listOf(
                InputSpec("begin", "Initial value", 100000.0),
                InputSpec("end", "Final value", 200000.0),
                InputSpec("years", "Years", 5.0),
            ),
        ) { m ->
            val b = v(m, "begin"); val y = v(m, "years")
            if (b <= 0.0 || y <= 0.0) listOf(out("CAGR", 0.0, note = "Use positive values"))
            else listOf(out("CAGR", ((v(m, "end") / b).pow(1.0 / y) - 1.0) * 100.0, "%"))
        },

        Formula(
            "roi", "Return on investment", "Finance",
            "ROI = (gain - cost) / cost.",
            listOf(InputSpec("cost", "Cost", 100000.0), InputSpec("gain", "Current value", 150000.0)),
        ) { m ->
            val c = v(m, "cost")
            if (c == 0.0) listOf(out("ROI", 0.0, note = "Cost must not be zero"))
            else listOf(out("Profit", v(m, "gain") - c), out("ROI", (v(m, "gain") - c) / c * 100.0, "%"))
        },

        Formula(
            "break_even", "Break-even point", "Finance",
            "Units needed to cover fixed costs.",
            listOf(
                InputSpec("fixed", "Fixed cost", 100000.0),
                InputSpec("price", "Price per unit", 500.0),
                InputSpec("var", "Variable cost per unit", 300.0),
            ),
        ) { m ->
            val contrib = v(m, "price") - v(m, "var")
            if (contrib <= 0.0) listOf(out("Units", 0.0, note = "Price must exceed variable cost"))
            else {
                val units = v(m, "fixed") / contrib
                listOf(out("Units to break even", units), out("Break-even revenue", units * v(m, "price")))
            }
        },

        Formula(
            "depreciation_sl", "Depreciation (straight line)", "Finance",
            "Equal yearly depreciation over the useful life.",
            listOf(
                InputSpec("cost", "Asset cost", 500000.0),
                InputSpec("salvage", "Salvage value", 50000.0),
                InputSpec("years", "Useful life (years)", 10.0),
            ),
        ) { m ->
            val y = v(m, "years").coerceAtLeast(1.0)
            val annual = (v(m, "cost") - v(m, "salvage")) / y
            listOf(out("Annual depreciation", annual), out("Book value at end", v(m, "salvage")))
        },

        // ---------------- Health ----------------
        Formula(
            "bmi", "BMI (Body Mass Index)", "Health",
            "Weight in kg over height in metres squared.",
            listOf(InputSpec("w", "Weight (kg)", 70.0), InputSpec("h", "Height (cm)", 170.0)),
        ) { m ->
            val hm = v(m, "h") / 100.0
            if (hm <= 0.0) listOf(out("BMI", 0.0, note = "Height must be positive"))
            else {
                val bmi = v(m, "w") / (hm * hm)
                val cat = when {
                    bmi < 18.5 -> "Underweight"
                    bmi < 25.0 -> "Normal"
                    bmi < 30.0 -> "Overweight"
                    else -> "Obese"
                }
                listOf(out("BMI", bmi, "kg/m2", cat))
            }
        },

        Formula(
            "bmr", "BMR (Mifflin-St Jeor)", "Health",
            "Resting calories per day. Sex: 1 = male, 0 = female.",
            listOf(
                InputSpec("w", "Weight (kg)", 70.0),
                InputSpec("h", "Height (cm)", 170.0),
                InputSpec("age", "Age (years)", 30.0),
                InputSpec("sex", "Sex (1=M, 0=F)", 1.0),
            ),
        ) { m ->
            val base = 10.0 * v(m, "w") + 6.25 * v(m, "h") - 5.0 * v(m, "age")
            val bmr = if (v(m, "sex") >= 0.5) base + 5.0 else base - 161.0
            listOf(out("BMR", bmr, "kcal/day"))
        },

        Formula(
            "tdee", "TDEE (daily calories)", "Health",
            "BMR x activity factor (1.2 sedentary to 1.9 athlete).",
            listOf(
                InputSpec("bmr", "BMR", 1600.0),
                InputSpec("activity", "Activity factor", 1.375, "1.2 / 1.375 / 1.55 / 1.725 / 1.9"),
            ),
        ) { m -> listOf(out("Maintenance calories", v(m, "bmr") * v(m, "activity"), "kcal/day")) },

        Formula(
            "ideal_weight", "Ideal body weight (Devine)", "Health",
            "Sex: 1 = male, 0 = female.",
            listOf(InputSpec("h", "Height (cm)", 170.0), InputSpec("sex", "Sex (1=M, 0=F)", 1.0)),
        ) { m ->
            val inches = v(m, "h") / 2.54
            val extra = 2.3 * (inches - 60.0)
            val w = if (v(m, "sex") >= 0.5) 50.0 + extra else 45.5 + extra
            listOf(out("Ideal weight", w, "kg"))
        },

        Formula(
            "body_fat", "Body fat % (from BMI)", "Health",
            "Adult estimate. Sex: 1 = male, 0 = female.",
            listOf(
                InputSpec("bmi", "BMI", 24.0),
                InputSpec("age", "Age (years)", 30.0),
                InputSpec("sex", "Sex (1=M, 0=F)", 1.0),
            ),
        ) { m ->
            val bf = 1.20 * v(m, "bmi") + 0.23 * v(m, "age") - 10.8 * v(m, "sex") - 5.4
            listOf(out("Body fat", bf, "%"))
        },

        Formula(
            "water", "Daily water intake", "Health",
            "About 33 ml per kg of body weight.",
            listOf(InputSpec("w", "Weight (kg)", 70.0)),
        ) { m -> listOf(out("Water", v(m, "w") * 0.033, "L/day")) },

        Formula(
            "heart_rate", "Heart rate zones", "Health",
            "Estimated max heart rate (220 - age) and training zones.",
            listOf(InputSpec("age", "Age (years)", 30.0)),
        ) { m ->
            val maxHr = 220.0 - v(m, "age")
            listOf(
                out("Max heart rate", maxHr, "bpm"),
                out("Fat burn (60-70%)", maxHr * 0.65, "bpm"),
                out("Cardio (70-85%)", maxHr * 0.775, "bpm"),
            )
        },

        // ---------------- Geometry ----------------
        Formula(
            "circle", "Circle", "Geometry",
            "Area and circumference from the radius.",
            listOf(InputSpec("r", "Radius", 5.0)),
        ) { m ->
            listOf(
                out("Area", PI * v(m, "r").pow(2)),
                out("Circumference", 2 * PI * v(m, "r")),
            )
        },

        Formula(
            "rectangle", "Rectangle", "Geometry",
            "Area and perimeter.",
            listOf(InputSpec("w", "Width", 4.0), InputSpec("h", "Height", 6.0)),
        ) { m ->
            listOf(out("Area", v(m, "w") * v(m, "h")), out("Perimeter", 2 * (v(m, "w") + v(m, "h"))))
        },

        Formula(
            "triangle_heron", "Triangle (3 sides)", "Geometry",
            "Area by Heron's formula.",
            listOf(InputSpec("a", "Side a", 3.0), InputSpec("b", "Side b", 4.0), InputSpec("c", "Side c", 5.0)),
        ) { m ->
            val s = (v(m, "a") + v(m, "b") + v(m, "c")) / 2.0
            val area = s * (s - v(m, "a")) * (s - v(m, "b")) * (s - v(m, "c"))
            listOf(out("Area", if (area < 0) 0.0 else sqrt(area)), out("Perimeter", 2 * s))
        },

        Formula(
            "sphere", "Sphere", "Geometry",
            "Volume and surface area.",
            listOf(InputSpec("r", "Radius", 5.0)),
        ) { m ->
            val r = v(m, "r")
            listOf(
                out("Volume", 4.0 / 3.0 * PI * r.pow(3)),
                out("Surface area", 4 * PI * r.pow(2)),
            )
        },

        Formula(
            "cylinder", "Cylinder", "Geometry",
            "Volume and total surface area.",
            listOf(InputSpec("r", "Radius", 3.0), InputSpec("h", "Height", 10.0)),
        ) { m ->
            val r = v(m, "r"); val h = v(m, "h")
            listOf(
                out("Volume", PI * r.pow(2) * h),
                out("Surface area", 2 * PI * r * (r + h)),
            )
        },

        Formula(
            "cone", "Cone", "Geometry",
            "Volume, slant height and surface area.",
            listOf(InputSpec("r", "Radius", 3.0), InputSpec("h", "Height", 4.0)),
        ) { m ->
            val r = v(m, "r"); val h = v(m, "h")
            val slant = sqrt(r * r + h * h)
            listOf(
                out("Volume", PI * r.pow(2) * h / 3.0),
                out("Slant height", slant),
                out("Surface area", PI * r * (r + slant)),
            )
        },

        Formula(
            "cube", "Cube", "Geometry",
            "Volume and surface area.",
            listOf(InputSpec("a", "Side", 4.0)),
        ) { m ->
            listOf(out("Volume", v(m, "a").pow(3)), out("Surface area", 6 * v(m, "a").pow(2)))
        },

        Formula(
            "trapezoid", "Trapezium / trapezoid", "Geometry",
            "Area from two parallel sides and height.",
            listOf(
                InputSpec("a", "Parallel side a", 8.0),
                InputSpec("b", "Parallel side b", 5.0),
                InputSpec("h", "Height", 4.0),
            ),
        ) { m -> listOf(out("Area", (v(m, "a") + v(m, "b")) / 2.0 * v(m, "h"))) },

        Formula(
            "pythagoras", "Pythagoras (hypotenuse)", "Geometry",
            "Hypotenuse from two legs.",
            listOf(InputSpec("a", "Leg a", 3.0), InputSpec("b", "Leg b", 4.0)),
        ) { m -> listOf(out("Hypotenuse", sqrt(v(m, "a").pow(2) + v(m, "b").pow(2)))) },

        // ---------------- Physics ----------------
        Formula(
            "ohms_law", "Ohm's law", "Physics",
            "Current and power from voltage and resistance.",
            listOf(InputSpec("v", "Voltage (V)", 12.0), InputSpec("r", "Resistance (ohm)", 6.0)),
        ) { m ->
            val r = v(m, "r")
            if (r == 0.0) listOf(out("Current", 0.0, note = "Resistance must not be zero"))
            else listOf(
                out("Current", v(m, "v") / r, "A"),
                out("Power", v(m, "v").pow(2) / r, "W"),
            )
        },

        Formula(
            "kinetic_energy", "Kinetic energy", "Physics",
            "KE = 0.5 x m x v^2.",
            listOf(InputSpec("m", "Mass (kg)", 2.0), InputSpec("v", "Velocity (m/s)", 10.0)),
        ) { m -> listOf(out("Kinetic energy", 0.5 * v(m, "m") * v(m, "v").pow(2), "J")) },

        Formula(
            "potential_energy", "Potential energy", "Physics",
            "PE = m x g x h (g = 9.81).",
            listOf(InputSpec("m", "Mass (kg)", 2.0), InputSpec("h", "Height (m)", 10.0)),
        ) { m -> listOf(out("Potential energy", v(m, "m") * 9.81 * v(m, "h"), "J")) },

        Formula(
            "work", "Work", "Physics",
            "W = F x d.",
            listOf(InputSpec("f", "Force (N)", 10.0), InputSpec("d", "Distance (m)", 5.0)),
        ) { m -> listOf(out("Work", v(m, "f") * v(m, "d"), "J")) },

        Formula(
            "power", "Power", "Physics",
            "P = W / t.",
            listOf(InputSpec("w", "Work (J)", 500.0), InputSpec("t", "Time (s)", 10.0)),
        ) { m ->
            val t = v(m, "t")
            if (t == 0.0) listOf(out("Power", 0.0, note = "Time must not be zero"))
            else listOf(out("Power", v(m, "w") / t, "W"))
        },

        Formula(
            "density", "Density", "Physics",
            "rho = m / V.",
            listOf(InputSpec("m", "Mass (kg)", 10.0), InputSpec("vol", "Volume (m3)", 2.0)),
        ) { m ->
            val vol = v(m, "vol")
            if (vol == 0.0) listOf(out("Density", 0.0, note = "Volume must not be zero"))
            else listOf(out("Density", v(m, "m") / vol, "kg/m3"))
        },

        Formula(
            "speed", "Speed", "Physics",
            "Speed = distance / time.",
            listOf(InputSpec("d", "Distance (m)", 100.0), InputSpec("t", "Time (s)", 9.58)),
        ) { m ->
            val t = v(m, "t")
            if (t == 0.0) listOf(out("Speed", 0.0, note = "Time must not be zero"))
            else listOf(out("Speed", v(m, "d") / t, "m/s"))
        },

        Formula(
            "force", "Force (Newton)", "Physics",
            "F = m x a.",
            listOf(InputSpec("m", "Mass (kg)", 10.0), InputSpec("a", "Acceleration (m/s2)", 2.0)),
        ) { m -> listOf(out("Force", v(m, "m") * v(m, "a"), "N")) },

        Formula(
            "pressure", "Pressure", "Physics",
            "P = F / A.",
            listOf(InputSpec("f", "Force (N)", 100.0), InputSpec("a", "Area (m2)", 2.0)),
        ) { m ->
            val a = v(m, "a")
            if (a == 0.0) listOf(out("Pressure", 0.0, note = "Area must not be zero"))
            else listOf(out("Pressure", v(m, "f") / a, "Pa"))
        },

        // ---------------- Math ----------------
        Formula(
            "gcd_lcm", "GCD & LCM", "Math",
            "Greatest common divisor and least common multiple.",
            listOf(InputSpec("a", "Number a", 12.0), InputSpec("b", "Number b", 18.0)),
        ) { m ->
            listOf(
                out("GCD", Evaluator.gcd(v(m, "a").toLong(), v(m, "b").toLong()).toDouble()),
                out("LCM", Evaluator.lcm(v(m, "a").toLong(), v(m, "b").toLong()).toDouble()),
            )
        },

        Formula(
            "ncr", "Combinations nCr", "Math",
            "Number of ways to choose r from n.",
            listOf(InputSpec("n", "n", 5.0), InputSpec("r", "r", 2.0)),
        ) { m -> listOf(out("nCr", Evaluator.combinations(v(m, "n"), v(m, "r")))) },

        Formula(
            "npr", "Permutations nPr", "Math",
            "Number of ordered arrangements of r from n.",
            listOf(InputSpec("n", "n", 5.0), InputSpec("r", "r", 2.0)),
        ) { m -> listOf(out("nPr", Evaluator.permutations(v(m, "n"), v(m, "r")))) },

        Formula(
            "quadratic", "Quadratic roots", "Math",
            "Solves a x^2 + b x + c = 0.",
            listOf(InputSpec("a", "a", 1.0), InputSpec("b", "b", -3.0), InputSpec("c", "c", 2.0)),
        ) { m ->
            val a = v(m, "a"); val b = v(m, "b"); val c = v(m, "c")
            if (a == 0.0) {
                listOf(out("Root", 0.0, note = "a must not be zero (not quadratic)"))
            } else {
                val d = b * b - 4 * a * c
                if (d < 0) {
                    listOf(
                        out("Discriminant", d),
                        out("Real part", -b / (2 * a)),
                        out("Imag part", sqrt(-d) / (2 * a), note = "Complex roots"),
                    )
                } else {
                    listOf(
                        out("Discriminant", d),
                        out("Root 1", (-b + sqrt(d)) / (2 * a)),
                        out("Root 2", (-b - sqrt(d)) / (2 * a)),
                    )
                }
            }
        },

        Formula(
            "logarithms", "Logarithms", "Math",
            "Natural log, log base 10 and log base 2.",
            listOf(InputSpec("x", "Value", 100.0)),
        ) { m ->
            val x = v(m, "x")
            if (x <= 0.0) listOf(out("ln", 0.0, note = "Value must be positive"))
            else listOf(out("ln", ln(x)), out("log10", log10(x)), out("log2", ln(x) / ln(2.0)))
        },
    )

    fun byCategory(category: String): List<Formula> = formulas.filter { it.category == category }
}
