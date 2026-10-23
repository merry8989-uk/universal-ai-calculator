package com.merry8989uk.universalcalculator.calc

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
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

    val categories = listOf(
        "Everyday", "Finance", "Economics", "Health",
        "Geometry", "Trigonometry", "Analytic Geometry", "Vectors",
        "Physics", "Engineering", "Construction",
        "Probability", "Series", "Number Theory", "Math",
    )

    private fun d2r(x: Double) = x * PI / 180.0
    private fun r2d(x: Double) = x * 180.0 / PI

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

        // ================= FINANCE (extended) =================
        Formula("npv", "NPV (net present value)", "Finance",
            "Discounted value of up to six yearly cash flows.",
            listOf(
                InputSpec("rate", "Discount rate (% per year)", 10.0),
                InputSpec("cf0", "CF0 (today)", -100000.0), InputSpec("cf1", "CF1", 30000.0),
                InputSpec("cf2", "CF2", 30000.0), InputSpec("cf3", "CF3", 30000.0),
                InputSpec("cf4", "CF4", 30000.0), InputSpec("cf5", "CF5", 0.0),
            ),
        ) { m ->
            val r = v(m, "rate") / 100.0
            val cfs = listOf(v(m, "cf0"), v(m, "cf1"), v(m, "cf2"), v(m, "cf3"), v(m, "cf4"), v(m, "cf5"))
            var s = 0.0
            for (i in cfs.indices) s += cfs[i] / (1.0 + r).pow(i)
            listOf(out("NPV", s))
        },

        Formula("irr", "IRR (internal rate of return)", "Finance",
            "Rate at which the cash flows break even.",
            listOf(
                InputSpec("cf0", "CF0 (today)", -100000.0), InputSpec("cf1", "CF1", 30000.0),
                InputSpec("cf2", "CF2", 30000.0), InputSpec("cf3", "CF3", 30000.0),
                InputSpec("cf4", "CF4", 30000.0), InputSpec("cf5", "CF5", 0.0),
            ),
        ) { m ->
            val cfs = listOf(v(m, "cf0"), v(m, "cf1"), v(m, "cf2"), v(m, "cf3"), v(m, "cf4"), v(m, "cf5"))
            fun npv(r: Double): Double {
                var s = 0.0
                for (i in cfs.indices) s += cfs[i] / (1.0 + r).pow(i)
                return s
            }
            var lo = -0.9; var hi = 10.0; var mid = 0.0
            val bracketed = npv(lo) * npv(hi) < 0
            if (bracketed) {
                for (k in 0 until 200) {
                    mid = (lo + hi) / 2
                    val f = npv(mid)
                    if (abs(f) < 1e-7) break
                    if (npv(lo) * f < 0) hi = mid else lo = mid
                }
                listOf(out("IRR", mid * 100.0, "%"))
            } else {
                listOf(out("IRR", 0.0, note = "No sign change in the cash flows"))
            }
        },

        Formula("pv_fv", "Present / future value", "Finance",
            "Time value of money: PV and FV of a lump sum.",
            listOf(
                InputSpec("amount", "Amount", 100000.0),
                InputSpec("rate", "Rate (% per year)", 8.0),
                InputSpec("years", "Years", 5.0),
            ),
        ) { m ->
            val r = v(m, "rate") / 100.0; val n = v(m, "years")
            listOf(
                out("Future value", v(m, "amount") * (1.0 + r).pow(n)),
                out("Present value", v(m, "amount") / (1.0 + r).pow(n)),
            )
        },

        Formula("inflation", "Inflation / purchasing power", "Finance",
            "Future cost and the value of today's money later.",
            listOf(
                InputSpec("amount", "Amount today", 100000.0),
                InputSpec("rate", "Inflation (% per year)", 6.0),
                InputSpec("years", "Years", 10.0),
            ),
        ) { m ->
            val f = (1.0 + v(m, "rate") / 100.0).pow(v(m, "years"))
            listOf(
                out("Future cost of the same basket", v(m, "amount") * f),
                out("Future value of today's money", v(m, "amount") / f),
            )
        },

        Formula("gratuity", "Gratuity (India)", "Finance",
            "Gratuity = 15/26 x last drawn salary x years of service.",
            listOf(
                InputSpec("salary", "Last drawn monthly salary", 50000.0),
                InputSpec("years", "Years of service", 10.0),
            ),
        ) { m -> listOf(out("Gratuity", 15.0 / 26.0 * v(m, "salary") * v(m, "years"))) },

        Formula("pf_maturity", "Provident fund maturity", "Finance",
            "EPF-style monthly contribution growth.",
            listOf(
                InputSpec("monthly", "Monthly contribution", 5000.0),
                InputSpec("rate", "Interest (% per year)", 8.25),
                InputSpec("years", "Years", 20.0),
            ),
        ) { m ->
            val n = v(m, "years") * 12.0; val i = v(m, "rate") / 100.0 / 12.0
            val fv = if (i == 0.0) v(m, "monthly") * n
            else v(m, "monthly") * ((1.0 + i).pow(n) - 1.0) / i * (1.0 + i)
            listOf(out("Invested", v(m, "monthly") * n), out("Maturity", fv))
        },

        Formula("salary_takehome", "CTC to monthly take-home", "Finance",
            "Rough in-hand estimate after tax. Use your own effective tax rate.",
            listOf(
                InputSpec("ctc", "Annual CTC", 1200000.0),
                InputSpec("tax", "Effective tax + deductions (%)", 20.0),
            ),
        ) { m ->
            val net = v(m, "ctc") * (1.0 - v(m, "tax") / 100.0)
            listOf(out("Annual take-home", net), out("Monthly take-home", net / 12.0))
        },

        Formula("depreciation_wdv", "Depreciation (declining balance)", "Finance",
            "Written-down-value depreciation at a fixed rate.",
            listOf(
                InputSpec("cost", "Asset cost", 500000.0),
                InputSpec("rate", "Rate (% per year)", 15.0),
                InputSpec("years", "Years", 5.0),
            ),
        ) { m ->
            val book = v(m, "cost") * (1.0 - v(m, "rate") / 100.0).pow(v(m, "years"))
            listOf(out("Book value after", book), out("Total depreciation", v(m, "cost") - book))
        },

        Formula("rule72", "Rule of 72 (doubling time)", "Finance",
            "Years to double money at a given return.",
            listOf(InputSpec("rate", "Annual return (%)", 12.0)),
        ) { m ->
            val r = v(m, "rate")
            if (r <= 0.0) listOf(out("Years to double", 0.0, note = "Rate must be positive"))
            else listOf(out("Years to double (rule of 72)", 72.0 / r))
        },

        Formula("markup", "Markup to selling price", "Finance",
            "Selling price from cost and desired markup.",
            listOf(InputSpec("cost", "Cost price", 800.0), InputSpec("markup", "Markup (%)", 25.0)),
        ) { m ->
            val price = v(m, "cost") * (1.0 + v(m, "markup") / 100.0)
            listOf(out("Selling price", price), out("Profit", price - v(m, "cost")))
        },

        Formula("roas", "ROAS (ad return)", "Finance",
            "Return on ad spend.",
            listOf(InputSpec("revenue", "Revenue", 50000.0), InputSpec("spend", "Ad spend", 10000.0)),
        ) { m ->
            val sp = v(m, "spend")
            if (sp == 0.0) listOf(out("ROAS", 0.0, note = "Spend must not be zero"))
            else listOf(out("ROAS", v(m, "revenue") / sp, "x"))
        },

        Formula("mortgage", "Home affordability", "Finance",
            "Loan needed and monthly payment for a home purchase.",
            listOf(
                InputSpec("price", "Home price", 5000000.0),
                InputSpec("down", "Down payment (%)", 20.0),
                InputSpec("rate", "Interest (% per year)", 8.5),
                InputSpec("years", "Tenure (years)", 20.0),
            ),
        ) { m ->
            val loan = v(m, "price") * (1.0 - v(m, "down") / 100.0)
            val n = v(m, "years") * 12.0; val i = v(m, "rate") / 100.0 / 12.0
            val emi = if (i == 0.0) loan / n else loan * i * (1.0 + i).pow(n) / ((1.0 + i).pow(n) - 1.0)
            listOf(out("Loan amount", loan), out("Monthly payment", emi), out("Total interest", emi * n - loan))
        },

        // ================= HEALTH (extended) =================
        Formula("bsa", "Body surface area (Mosteller)", "Health",
            "BSA = sqrt(height x weight / 3600).",
            listOf(InputSpec("w", "Weight (kg)", 70.0), InputSpec("h", "Height (cm)", 170.0)),
        ) { m -> listOf(out("BSA", sqrt(v(m, "h") * v(m, "w") / 3600.0), "m2")) },

        Formula("creatinine_clearance", "Creatinine clearance (Cockcroft-Gault)", "Health",
            "Sex: 1 = male, 0 = female.",
            listOf(
                InputSpec("age", "Age (years)", 40.0),
                InputSpec("w", "Weight (kg)", 70.0),
                InputSpec("cr", "Serum creatinine (mg/dL)", 1.0),
                InputSpec("sex", "Sex (1=M, 0=F)", 1.0),
            ),
        ) { m ->
            val cr = v(m, "cr")
            if (cr <= 0.0) listOf(out("CrCl", 0.0, note = "Creatinine must be positive"))
            else {
                val f = if (v(m, "sex") >= 0.5) 1.0 else 0.85
                listOf(out("Creatinine clearance", (140.0 - v(m, "age")) * v(m, "w") * f / (72.0 * cr), "mL/min"))
            }
        },

        Formula("egfr", "eGFR (MDRD)", "Health",
            "Estimated glomerular filtration rate. Sex: 1 = male, 0 = female.",
            listOf(
                InputSpec("cr", "Serum creatinine (mg/dL)", 1.0),
                InputSpec("age", "Age (years)", 40.0),
                InputSpec("sex", "Sex (1=M, 0=F)", 1.0),
            ),
        ) { m ->
            val cr = v(m, "cr")
            if (cr <= 0.0) listOf(out("eGFR", 0.0, note = "Creatinine must be positive"))
            else {
                val f = if (v(m, "sex") >= 0.5) 1.0 else 0.742
                listOf(out("eGFR", 175.0 * cr.pow(-1.154) * v(m, "age").pow(-0.203) * f, "mL/min/1.73m2"))
            }
        },

        Formula("parkland", "Parkland burn formula", "Health",
            "Fluid resuscitation: 4 mL x kg x %TBSA in 24 h (half in the first 8 h).",
            listOf(
                InputSpec("w", "Weight (kg)", 70.0),
                InputSpec("tbsa", "Burn area (% TBSA)", 20.0),
            ),
        ) { m ->
            val total = 4.0 * v(m, "w") * v(m, "tbsa")
            listOf(out("Total in 24 h", total, "mL"), out("First 8 h", total / 2.0, "mL"), out("Next 16 h", total / 2.0, "mL"))
        },

        Formula("aa_gradient", "A-a gradient", "Health",
            "Alveolar-arterial oxygen gradient at sea level.",
            listOf(
                InputSpec("pao2", "PaO2 (mmHg)", 90.0),
                InputSpec("paco2", "PaCO2 (mmHg)", 40.0),
                InputSpec("fio2", "FiO2 (%)", 21.0),
            ),
        ) { m ->
            val pao2Alv = v(m, "fio2") / 100.0 * (760.0 - 47.0) - v(m, "paco2") / 0.8
            listOf(out("A-a gradient", pao2Alv - v(m, "pao2"), "mmHg"))
        },

        Formula("anion_gap", "Anion gap", "Health",
            "Na - (Cl + HCO3).",
            listOf(
                InputSpec("na", "Sodium (mEq/L)", 140.0),
                InputSpec("cl", "Chloride (mEq/L)", 102.0),
                InputSpec("hco3", "Bicarbonate (mEq/L)", 24.0),
            ),
        ) { m -> listOf(out("Anion gap", v(m, "na") - (v(m, "cl") + v(m, "hco3")), "mEq/L")) },

        Formula("corrected_calcium", "Corrected calcium", "Health",
            "Calcium + 0.8 x (4 - albumin).",
            listOf(InputSpec("ca", "Calcium (mg/dL)", 9.0), InputSpec("alb", "Albumin (g/dL)", 3.0)),
        ) { m -> listOf(out("Corrected calcium", v(m, "ca") + 0.8 * (4.0 - v(m, "alb")), "mg/dL")) },

        Formula("insulin_dose", "Insulin dose", "Health",
            "Weight-based insulin estimate.",
            listOf(InputSpec("w", "Weight (kg)", 70.0), InputSpec("perKg", "Units per kg per day", 0.5)),
        ) { m -> listOf(out("Total daily insulin", v(m, "w") * v(m, "perKg"), "units/day")) },

        Formula("iv_drip", "IV drip rate", "Health",
            "Infusion rate and drops per minute.",
            listOf(
                InputSpec("volume", "Volume (mL)", 500.0),
                InputSpec("hours", "Over hours", 8.0),
                InputSpec("drop", "Drop factor (drops/mL)", 20.0),
            ),
        ) { m ->
            val h = v(m, "hours")
            if (h <= 0.0) listOf(out("Rate", 0.0, note = "Hours must be positive"))
            else {
                val perHr = v(m, "volume") / h
                listOf(out("Rate", perHr, "mL/h"), out("Drops", perHr * v(m, "drop") / 60.0, "drops/min"))
            }
        },

        Formula("blood_pressure", "Blood pressure check", "Health",
            "Mean arterial pressure and the category.",
            listOf(InputSpec("sys", "Systolic", 120.0), InputSpec("dia", "Diastolic", 80.0)),
        ) { m ->
            val s = v(m, "sys"); val d = v(m, "dia")
            val cat = when {
                s < 90 || d < 60 -> "Low"
                s < 120 && d < 80 -> "Normal"
                s < 130 && d < 80 -> "Elevated"
                s < 140 || d < 90 -> "Stage 1"
                s < 180 || d < 120 -> "Stage 2"
                else -> "Crisis"
            }
            listOf(out("Mean arterial pressure", (s + 2.0 * d) / 3.0, "mmHg", cat))
        },

        Formula("cholesterol_ratio", "Cholesterol ratio", "Health",
            "Total cholesterol over HDL.",
            listOf(InputSpec("total", "Total cholesterol", 180.0), InputSpec("hdl", "HDL", 50.0)),
        ) { m ->
            val hdl = v(m, "hdl")
            if (hdl == 0.0) listOf(out("Ratio", 0.0, note = "HDL must not be zero"))
            else listOf(out("Total / HDL ratio", v(m, "total") / hdl))
        },

        Formula("one_rep_max", "One-rep max (Epley)", "Health",
            "Estimated 1RM from a set.",
            listOf(InputSpec("w", "Weight lifted", 80.0), InputSpec("reps", "Reps", 5.0)),
        ) { m -> listOf(out("Estimated 1RM", v(m, "w") * (1.0 + v(m, "reps") / 30.0))) },

        Formula("pace", "Running pace", "Health",
            "Pace per km and speed.",
            listOf(InputSpec("distance", "Distance (km)", 10.0), InputSpec("minutes", "Time (minutes)", 50.0)),
        ) { m ->
            val d = v(m, "distance")
            if (d <= 0.0) listOf(out("Pace", 0.0, note = "Distance must be positive"))
            else {
                val minPerKm = v(m, "minutes") / d
                listOf(out("Pace", minPerKm, "min/km"), out("Speed", 60.0 / minPerKm, "km/h"))
            }
        },

        Formula("macros", "Macro split", "Health",
            "Grams of protein, carbs and fat from calories and percentages.",
            listOf(
                InputSpec("cal", "Daily calories", 2000.0),
                InputSpec("protein", "Protein (%)", 30.0),
                InputSpec("carb", "Carbs (%)", 40.0),
                InputSpec("fat", "Fat (%)", 30.0),
            ),
        ) { m ->
            listOf(
                out("Protein", v(m, "cal") * v(m, "protein") / 100.0 / 4.0, "g"),
                out("Carbs", v(m, "cal") * v(m, "carb") / 100.0 / 4.0, "g"),
                out("Fat", v(m, "cal") * v(m, "fat") / 100.0 / 9.0, "g"),
            )
        },

        Formula("waist_hip", "Waist-to-hip ratio", "Health",
            "Cardiovascular risk indicator.",
            listOf(InputSpec("waist", "Waist (cm)", 85.0), InputSpec("hip", "Hip (cm)", 100.0)),
        ) { m ->
            val hip = v(m, "hip")
            if (hip == 0.0) listOf(out("Ratio", 0.0, note = "Hip must not be zero"))
            else listOf(out("Waist-to-hip ratio", v(m, "waist") / hip))
        },

        // ================= GEOMETRY (extended) =================
        Formula("parallelogram", "Parallelogram", "Geometry", "Area from base and height.",
            listOf(InputSpec("base", "Base", 8.0), InputSpec("h", "Height", 5.0)),
        ) { m -> listOf(out("Area", v(m, "base") * v(m, "h"))) },

        Formula("rhombus", "Rhombus", "Geometry", "Area from the two diagonals.",
            listOf(InputSpec("d1", "Diagonal 1", 8.0), InputSpec("d2", "Diagonal 2", 6.0)),
        ) { m -> listOf(out("Area", v(m, "d1") * v(m, "d2") / 2.0)) },

        Formula("sector", "Circle sector", "Geometry", "Area and arc length of a sector.",
            listOf(InputSpec("r", "Radius", 5.0), InputSpec("angle", "Angle (degrees)", 60.0)),
        ) { m ->
            val r = v(m, "r"); val a = v(m, "angle")
            listOf(
                out("Area", a / 360.0 * PI * r.pow(2)),
                out("Arc length", a / 360.0 * 2.0 * PI * r),
            )
        },

        Formula("ellipse", "Ellipse", "Geometry", "Area and approximate perimeter (Ramanujan).",
            listOf(InputSpec("a", "Semi-major axis a", 6.0), InputSpec("b", "Semi-minor axis b", 4.0)),
        ) { m ->
            val a = v(m, "a"); val b = v(m, "b")
            val h = ((a - b).pow(2)) / ((a + b).pow(2))
            listOf(
                out("Area", PI * a * b),
                out("Perimeter (approx)", PI * (a + b) * (1.0 + 3.0 * h / (10.0 + sqrt(4.0 - 3.0 * h)))),
            )
        },

        Formula("polygon", "Regular polygon", "Geometry", "Area, perimeter and interior angle.",
            listOf(InputSpec("n", "Number of sides", 6.0), InputSpec("side", "Side length", 4.0)),
        ) { m ->
            val n = v(m, "n").coerceAtLeast(3.0); val s = v(m, "side")
            listOf(
                out("Area", n * s * s / (4.0 * kotlin.math.tan(PI / n))),
                out("Perimeter", n * s),
                out("Interior angle", (n - 2.0) * 180.0 / n, "deg"),
            )
        },

        Formula("hemisphere", "Hemisphere", "Geometry", "Volume and surface area.",
            listOf(InputSpec("r", "Radius", 5.0)),
        ) { m ->
            val r = v(m, "r")
            listOf(out("Volume", 2.0 / 3.0 * PI * r.pow(3)), out("Surface area", 3.0 * PI * r.pow(2)))
        },

        Formula("frustum", "Cone frustum", "Geometry", "Volume and surface of a truncated cone.",
            listOf(
                InputSpec("r1", "Top radius", 2.0),
                InputSpec("r2", "Bottom radius", 5.0),
                InputSpec("h", "Height", 8.0),
            ),
        ) { m ->
            val r1 = v(m, "r1"); val r2 = v(m, "r2"); val h = v(m, "h")
            val slant = sqrt((r2 - r1).pow(2) + h * h)
            listOf(
                out("Volume", PI * h / 3.0 * (r1 * r1 + r1 * r2 + r2 * r2)),
                out("Slant height", slant),
                out("Surface area", PI * (r1 + r2) * slant + PI * r1 * r1 + PI * r2 * r2),
            )
        },

        Formula("pyramid", "Square pyramid", "Geometry", "Volume and surface area.",
            listOf(InputSpec("base", "Base side", 6.0), InputSpec("h", "Height", 8.0)),
        ) { m ->
            val b = v(m, "base"); val h = v(m, "h")
            val slant = sqrt((b / 2.0).pow(2) + h * h)
            listOf(
                out("Volume", b * b * h / 3.0),
                out("Slant height", slant),
                out("Surface area", b * b + 2.0 * b * slant),
            )
        },

        Formula("prism", "Prism", "Geometry", "Volume from base area and height.",
            listOf(InputSpec("baseArea", "Base area", 20.0), InputSpec("h", "Height", 10.0)),
        ) { m -> listOf(out("Volume", v(m, "baseArea") * v(m, "h"))) },

        Formula("torus", "Torus", "Geometry", "Volume and surface area.",
            listOf(InputSpec("R", "Major radius", 10.0), InputSpec("r", "Minor radius", 3.0)),
        ) { m ->
            val R = v(m, "R"); val r = v(m, "r")
            listOf(
                out("Volume", 2.0 * PI * PI * R * r * r),
                out("Surface area", 4.0 * PI * PI * R * r),
            )
        },

        Formula("coordinate", "Coordinate geometry", "Geometry", "Distance, midpoint and slope of two points.",
            listOf(
                InputSpec("x1", "x1", 1.0), InputSpec("y1", "y1", 2.0),
                InputSpec("x2", "x2", 4.0), InputSpec("y2", "y2", 6.0),
            ),
        ) { m ->
            val dx = v(m, "x2") - v(m, "x1"); val dy = v(m, "y2") - v(m, "y1")
            listOf(
                out("Distance", sqrt(dx * dx + dy * dy)),
                out("Midpoint x", (v(m, "x1") + v(m, "x2")) / 2.0),
                out("Midpoint y", (v(m, "y1") + v(m, "y2")) / 2.0),
                out("Slope", if (dx == 0.0) 0.0 else dy / dx),
            )
        },

        // ================= PHYSICS (extended) =================
        Formula("kinematics", "Kinematics (v = u + at)", "Physics", "Final velocity and distance travelled.",
            listOf(
                InputSpec("u", "Initial velocity (m/s)", 0.0),
                InputSpec("a", "Acceleration (m/s2)", 9.81),
                InputSpec("t", "Time (s)", 3.0),
            ),
        ) { m ->
            val u = v(m, "u"); val a = v(m, "a"); val t = v(m, "t")
            listOf(out("Final velocity", u + a * t, "m/s"), out("Distance", u * t + 0.5 * a * t * t, "m"))
        },

        Formula("projectile", "Projectile range", "Physics", "Range, height and flight time (g = 9.81).",
            listOf(InputSpec("v", "Speed (m/s)", 20.0), InputSpec("angle", "Angle (degrees)", 45.0)),
        ) { m ->
            val v = v(m, "v"); val th = Math.toRadians(v(m, "angle"))
            listOf(
                out("Range", v * v * sin(2.0 * th) / 9.81, "m"),
                out("Max height", v * v * sin(th).pow(2) / (2.0 * 9.81), "m"),
                out("Flight time", 2.0 * v * sin(th) / 9.81, "s"),
            )
        },

        Formula("momentum", "Momentum & impulse", "Physics", "p = m v and impulse F t.",
            listOf(
                InputSpec("m", "Mass (kg)", 2.0), InputSpec("v", "Velocity (m/s)", 10.0),
                InputSpec("f", "Force (N)", 5.0), InputSpec("t", "Time (s)", 2.0),
            ),
        ) { m ->
            listOf(out("Momentum", v(m, "m") * v(m, "v"), "kg m/s"), out("Impulse", v(m, "f") * v(m, "t"), "N s"))
        },

        Formula("circular_motion", "Circular motion", "Physics", "Centripetal acceleration and force.",
            listOf(
                InputSpec("m", "Mass (kg)", 1.0),
                InputSpec("v", "Speed (m/s)", 10.0),
                InputSpec("r", "Radius (m)", 5.0),
            ),
        ) { m ->
            val r = v(m, "r")
            if (r == 0.0) listOf(out("Centripetal accel", 0.0, note = "Radius must not be zero"))
            else listOf(
                out("Centripetal acceleration", v(m, "v").pow(2) / r, "m/s2"),
                out("Centripetal force", v(m, "m") * v(m, "v").pow(2) / r, "N"),
            )
        },

        Formula("gravitation", "Gravitational force", "Physics", "F = G m1 m2 / r^2.",
            listOf(
                InputSpec("m1", "Mass 1 (kg)", 5.972e24),
                InputSpec("m2", "Mass 2 (kg)", 1000.0),
                InputSpec("r", "Distance (m)", 6.371e6),
            ),
        ) { m ->
            val r = v(m, "r")
            if (r == 0.0) listOf(out("Force", 0.0, note = "Distance must not be zero"))
            else listOf(out("Force", 6.674e-11 * v(m, "m1") * v(m, "m2") / (r * r), "N"))
        },

        Formula("escape_velocity", "Orbital & escape velocity", "Physics", "For a planet of mass M at radius r.",
            listOf(
                InputSpec("M", "Planet mass (kg)", 5.972e24),
                InputSpec("r", "Orbit radius (m)", 6.771e6),
            ),
        ) { m ->
            val r = v(m, "r")
            if (r <= 0.0) listOf(out("Orbital velocity", 0.0, note = "Radius must be positive"))
            else listOf(
                out("Orbital velocity", sqrt(6.674e-11 * v(m, "M") / r), "m/s"),
                out("Escape velocity", sqrt(2.0 * 6.674e-11 * v(m, "M") / r), "m/s"),
            )
        },

        Formula("reynolds", "Reynolds number", "Physics", "Flow regime indicator.",
            listOf(
                InputSpec("rho", "Density (kg/m3)", 1000.0),
                InputSpec("v", "Velocity (m/s)", 1.0),
                InputSpec("d", "Diameter (m)", 0.05),
                InputSpec("mu", "Viscosity (Pa s)", 0.001),
            ),
        ) { m ->
            val mu = v(m, "mu")
            if (mu == 0.0) listOf(out("Re", 0.0, note = "Viscosity must not be zero"))
            else listOf(out("Reynolds number", v(m, "rho") * v(m, "v") * v(m, "d") / mu))
        },

        Formula("buoyancy", "Buoyant force", "Physics", "F = rho V g.",
            listOf(InputSpec("rho", "Fluid density (kg/m3)", 1000.0), InputSpec("vol", "Volume (m3)", 0.01)),
        ) { m -> listOf(out("Buoyant force", v(m, "rho") * v(m, "vol") * 9.81, "N")) },

        Formula("heat", "Heat energy", "Physics", "Q = m c dT.",
            listOf(
                InputSpec("m", "Mass (kg)", 1.0),
                InputSpec("c", "Specific heat (J/kgK)", 4186.0),
                InputSpec("dt", "Temperature change (K)", 20.0),
            ),
        ) { m -> listOf(out("Heat", v(m, "m") * v(m, "c") * v(m, "dt"), "J")) },

        Formula("boyle", "Boyle's law", "Physics", "P1 V1 = P2 V2.",
            listOf(
                InputSpec("p1", "P1", 100.0), InputSpec("v1", "V1", 2.0), InputSpec("p2", "P2", 150.0),
            ),
        ) { m ->
            val p2 = v(m, "p2")
            if (p2 == 0.0) listOf(out("V2", 0.0, note = "P2 must not be zero"))
            else listOf(out("V2", v(m, "p1") * v(m, "v1") / p2))
        },

        Formula("wave", "Wave properties", "Physics", "Speed and period from frequency and wavelength.",
            listOf(InputSpec("f", "Frequency (Hz)", 50.0), InputSpec("lambda", "Wavelength (m)", 2.0)),
        ) { m ->
            val f = v(m, "f")
            listOf(
                out("Speed", f * v(m, "lambda"), "m/s"),
                out("Period", if (f == 0.0) 0.0 else 1.0 / f, "s"),
            )
        },

        Formula("lens", "Thin lens equation", "Physics", "Image distance and magnification.",
            listOf(InputSpec("f", "Focal length (cm)", 10.0), InputSpec("u", "Object distance (cm)", 30.0)),
        ) { m ->
            val f = v(m, "f"); val u = v(m, "u")
            val denom = 1.0 / f - 1.0 / u
            if (denom == 0.0) listOf(out("Image distance", 0.0, note = "Image at infinity"))
            else {
                val vImg = 1.0 / denom
                listOf(out("Image distance", vImg, "cm"), out("Magnification", -vImg / u))
            }
        },

        Formula("snell", "Snell's law", "Physics", "Refraction angle between two media.",
            listOf(
                InputSpec("n1", "n1", 1.0),
                InputSpec("theta1", "Angle of incidence (deg)", 30.0),
                InputSpec("n2", "n2", 1.5),
            ),
        ) { m ->
            val n2 = v(m, "n2")
            if (n2 == 0.0) listOf(out("Angle 2", 0.0, note = "n2 must not be zero"))
            else {
                val s = v(m, "n1") * sin(Math.toRadians(v(m, "theta1"))) / n2
                if (s > 1.0) listOf(out("Angle 2", 0.0, note = "Total internal reflection"))
                else listOf(out("Angle 2", Math.toDegrees(kotlin.math.asin(s)), "deg"))
            }
        },

        Formula("resistors", "Resistors series & parallel", "Physics", "Combined resistance of three resistors.",
            listOf(
                InputSpec("r1", "R1", 10.0), InputSpec("r2", "R2", 20.0), InputSpec("r3", "R3", 30.0),
            ),
        ) { m ->
            val r1 = v(m, "r1"); val r2 = v(m, "r2"); val r3 = v(m, "r3")
            val par = 1.0 / (1.0 / r1 + 1.0 / r2 + 1.0 / r3)
            listOf(out("Series", r1 + r2 + r3, "ohm"), out("Parallel", par, "ohm"))
        },

        Formula("rc_time", "RC time constant", "Physics", "tau = R C.",
            listOf(InputSpec("r", "Resistance (ohm)", 1000.0), InputSpec("c", "Capacitance (uF)", 100.0)),
        ) { m -> listOf(out("Time constant", v(m, "r") * v(m, "c") / 1e6, "s")) },

        Formula("emc2", "Mass-energy (E = mc^2)", "Physics", "Rest energy of a mass.",
            listOf(InputSpec("m", "Mass (kg)", 1.0)),
        ) { m -> listOf(out("Energy", v(m, "m") * (3e8).pow(2), "J")) },

        Formula("photon", "Photon energy", "Physics", "Energy of a photon from its wavelength.",
            listOf(InputSpec("lambda", "Wavelength (nm)", 550.0)),
        ) { m ->
            val l = v(m, "lambda")
            if (l == 0.0) listOf(out("Energy", 0.0, note = "Wavelength must not be zero"))
            else listOf(out("Energy", 1240.0 / l, "eV"))
        },

        Formula("torque", "Torque", "Physics", "tau = F x r.",
            listOf(InputSpec("f", "Force (N)", 50.0), InputSpec("r", "Lever arm (m)", 0.3)),
        ) { m -> listOf(out("Torque", v(m, "f") * v(m, "r"), "N m")) },

        Formula("efficiency", "Efficiency & mech. advantage", "Physics", "Output over input, and MA.",
            listOf(
                InputSpec("out", "Output", 80.0), InputSpec("in", "Input", 100.0),
                InputSpec("load", "Load", 200.0), InputSpec("effort", "Effort", 50.0),
            ),
        ) { m ->
            val inp = v(m, "in"); val eff = v(m, "effort")
            listOf(
                out("Efficiency", if (inp == 0.0) 0.0 else v(m, "out") / inp * 100.0, "%"),
                out("Mechanical advantage", if (eff == 0.0) 0.0 else v(m, "load") / eff),
            )
        },

        // ================= ENGINEERING =================
        Formula("voltage_drop", "Cable voltage drop", "Engineering", "Two-wire voltage drop.",
            listOf(
                InputSpec("i", "Current (A)", 10.0),
                InputSpec("len", "Cable length (m)", 30.0),
                InputSpec("rpm", "Resistance (ohm/m)", 0.007),
            ),
        ) { m -> listOf(out("Voltage drop", 2.0 * v(m, "i") * v(m, "len") * v(m, "rpm"), "V")) },

        Formula("led_resistor", "LED series resistor", "Engineering", "R = (Vs - Vled) / I.",
            listOf(
                InputSpec("vs", "Supply voltage", 5.0),
                InputSpec("vl", "LED forward voltage", 2.0),
                InputSpec("ma", "LED current (mA)", 20.0),
            ),
        ) { m ->
            val ma = v(m, "ma")
            if (ma == 0.0) listOf(out("Resistor", 0.0, note = "Current must not be zero"))
            else listOf(out("Resistor", (v(m, "vs") - v(m, "vl")) / (ma / 1000.0), "ohm"))
        },

        Formula("voltage_divider", "Voltage divider", "Engineering", "Vout = Vin x R2 / (R1 + R2).",
            listOf(
                InputSpec("vin", "Vin", 12.0), InputSpec("r1", "R1", 1000.0), InputSpec("r2", "R2", 2000.0),
            ),
        ) { m ->
            val d = v(m, "r1") + v(m, "r2")
            if (d == 0.0) listOf(out("Vout", 0.0, note = "R1 + R2 must not be zero"))
            else listOf(out("Vout", v(m, "vin") * v(m, "r2") / d, "V"))
        },

        Formula("three_phase", "Three-phase power", "Engineering", "P = sqrt(3) V I pf.",
            listOf(
                InputSpec("v", "Line voltage", 415.0),
                InputSpec("i", "Current (A)", 10.0),
                InputSpec("pf", "Power factor", 0.9),
            ),
        ) { m -> listOf(out("Power", sqrt(3.0) * v(m, "v") * v(m, "i") * v(m, "pf") / 1000.0, "kW")) },

        Formula("battery_life", "Battery life", "Engineering", "Hours from capacity and load.",
            listOf(InputSpec("mah", "Capacity (mAh)", 3000.0), InputSpec("load", "Load (mA)", 250.0)),
        ) { m ->
            val load = v(m, "load")
            if (load == 0.0) listOf(out("Hours", 0.0, note = "Load must not be zero"))
            else listOf(out("Hours", v(m, "mah") / load))
        },

        Formula("gear_ratio", "Gear ratio", "Engineering", "Driven over driver teeth.",
            listOf(InputSpec("driver", "Driver teeth", 12.0), InputSpec("driven", "Driven teeth", 36.0)),
        ) { m ->
            val d = v(m, "driver")
            if (d == 0.0) listOf(out("Ratio", 0.0, note = "Driver must not be zero"))
            else listOf(out("Ratio", v(m, "driven") / d))
        },

        Formula("stress_strain", "Stress, strain & Young's modulus", "Engineering", "Basic solid mechanics.",
            listOf(
                InputSpec("f", "Force (N)", 1000.0),
                InputSpec("area", "Area (mm2)", 50.0),
                InputSpec("dl", "Change in length (mm)", 0.5),
                InputSpec("l", "Original length (mm)", 100.0),
            ),
        ) { m ->
            val area = v(m, "area"); val l = v(m, "l")
            val stress = if (area == 0.0) 0.0 else v(m, "f") / area
            val strain = if (l == 0.0) 0.0 else v(m, "dl") / l
            listOf(
                out("Stress", stress, "N/mm2"),
                out("Strain", strain),
                out("Young's modulus", if (strain == 0.0) 0.0 else stress / strain, "N/mm2"),
            )
        },

        Formula("molarity", "Molarity", "Engineering", "Moles per litre.",
            listOf(InputSpec("moles", "Moles", 0.5), InputSpec("vol", "Volume (L)", 2.0)),
        ) { m ->
            val vol = v(m, "vol")
            if (vol == 0.0) listOf(out("Molarity", 0.0, note = "Volume must not be zero"))
            else listOf(out("Molarity", v(m, "moles") / vol, "mol/L"))
        },

        Formula("dilution", "Dilution (C1 V1 = C2 V2)", "Engineering", "Volume of C1 needed to reach C2.",
            listOf(
                InputSpec("c1", "Stock concentration", 10.0),
                InputSpec("v1", "Stock volume (mL)", 50.0),
                InputSpec("c2", "Target concentration", 2.0),
            ),
        ) { m ->
            val c2 = v(m, "c2")
            if (c2 == 0.0) listOf(out("Final volume", 0.0, note = "Target must not be zero"))
            else listOf(out("Final volume", v(m, "c1") * v(m, "v1") / c2, "mL"))
        },

        Formula("ph", "pH from H+ concentration", "Engineering", "pH = -log10([H+] mol/L).",
            listOf(InputSpec("h", "H+ concentration (mol/L)", 0.001)),
        ) { m ->
            val h = v(m, "h")
            if (h <= 0.0) listOf(out("pH", 0.0, note = "Concentration must be positive"))
            else listOf(out("pH", -log10(h)))
        },

        Formula("ip_subnet", "IP subnet hosts", "Engineering", "Usable hosts for a CIDR prefix.",
            listOf(InputSpec("prefix", "CIDR prefix (/n)", 24.0)),
        ) { m ->
            val p = v(m, "prefix").toInt().coerceIn(0, 32)
            val hosts = if (p >= 31) 0.0 else 2.0.pow(32 - p) - 2.0
            listOf(out("Usable hosts", hosts), out("Total addresses", 2.0.pow(32 - p)))
        },

        Formula("transfer_time", "Data transfer time", "Engineering", "Time to move a file at a given speed.",
            listOf(InputSpec("size", "Size (GB)", 5.0), InputSpec("speed", "Speed (Mbps)", 100.0)),
        ) { m ->
            val sp = v(m, "speed")
            if (sp == 0.0) listOf(out("Time", 0.0, note = "Speed must not be zero"))
            else {
                val sec = v(m, "size") * 8000.0 / sp
                listOf(out("Seconds", sec), out("Minutes", sec / 60.0))
            }
        },

        Formula("aspect_ratio", "Aspect ratio & megapixels", "Engineering", "Simplified ratio and pixel count.",
            listOf(InputSpec("w", "Width (px)", 1920.0), InputSpec("h", "Height (px)", 1080.0)),
        ) { m ->
            val w = v(m, "w").toInt(); val h = v(m, "h").toInt()
            val g = Evaluator.gcd(w.toLong(), h.toLong()).toInt().coerceAtLeast(1)
            listOf(
                out("Megapixels", v(m, "w") * v(m, "h") / 1e6, "MP"),
                out("Ratio width part", (w / g).toDouble()),
                out("Ratio height part", (h / g).toDouble()),
            )
        },

        Formula("ppi", "Screen PPI", "Engineering", "Pixels per inch.",
            listOf(
                InputSpec("w", "Width (px)", 1080.0),
                InputSpec("h", "Height (px)", 2400.0),
                InputSpec("diag", "Diagonal (inches)", 6.4),
            ),
        ) { m ->
            val d = v(m, "diag")
            if (d == 0.0) listOf(out("PPI", 0.0, note = "Diagonal must not be zero"))
            else listOf(out("PPI", sqrt(v(m, "w").pow(2) + v(m, "h").pow(2)) / d))
        },

        // ================= CONSTRUCTION =================
        Formula("concrete", "Concrete volume & mix", "Construction", "M20 (1:1.5:3) cement, sand and aggregate.",
            listOf(
                InputSpec("len", "Length (m)", 5.0),
                InputSpec("wid", "Width (m)", 3.0),
                InputSpec("thk", "Thickness (m)", 0.15),
            ),
        ) { m ->
            val wet = v(m, "len") * v(m, "wid") * v(m, "thk")
            val dry = wet * 1.54
            listOf(
                out("Wet volume", wet, "m3"),
                out("Cement bags (50 kg)", dry * 1.0 / 5.5 / 0.0347, "bags"),
                out("Sand", dry * 1.5 / 5.5, "m3"),
                out("Aggregate", dry * 3.0 / 5.5, "m3"),
            )
        },

        Formula("bricks", "Brick count", "Construction", "Standard 190x90x90 mm brick with mortar.",
            listOf(
                InputSpec("len", "Wall length (m)", 5.0),
                InputSpec("hgt", "Wall height (m)", 3.0),
                InputSpec("thk", "Wall thickness (mm)", 230.0),
            ),
        ) { m ->
            val vol = v(m, "len") * v(m, "hgt") * v(m, "thk") / 1000.0
            val bricks = vol / 0.002
            listOf(out("Bricks", Math.ceil(bricks)), out("Mortar", vol - bricks * 0.001539, "m3"))
        },

        Formula("tiles", "Tiles needed", "Construction", "Tiles for a floor, including wastage.",
            listOf(
                InputSpec("area", "Floor area (m2)", 20.0),
                InputSpec("tl", "Tile length (cm)", 60.0),
                InputSpec("tw", "Tile width (cm)", 60.0),
                InputSpec("waste", "Wastage (%)", 10.0),
            ),
        ) { m ->
            val tile = (v(m, "tl") / 100.0) * (v(m, "tw") / 100.0)
            if (tile == 0.0) listOf(out("Tiles", 0.0, note = "Tile size must be positive"))
            else {
                val base = v(m, "area") / tile
                listOf(out("Tiles", Math.ceil(base)), out("With wastage", Math.ceil(base * (1.0 + v(m, "waste") / 100.0))))
            }
        },

        Formula("paint", "Paint quantity", "Construction", "Litres from area, coats and coverage.",
            listOf(
                InputSpec("area", "Wall area (m2)", 100.0),
                InputSpec("coats", "Coats", 2.0),
                InputSpec("coverage", "Coverage (m2/L)", 10.0),
            ),
        ) { m ->
            val c = v(m, "coverage")
            if (c == 0.0) listOf(out("Paint", 0.0, note = "Coverage must not be zero"))
            else listOf(out("Paint", v(m, "area") * v(m, "coats") / c, "L"))
        },

        Formula("plaster", "Plaster quantity", "Construction", "1:4 cement plaster.",
            listOf(
                InputSpec("area", "Area (m2)", 100.0),
                InputSpec("thk", "Thickness (mm)", 12.0),
            ),
        ) { m ->
            val dry = v(m, "area") * v(m, "thk") / 1000.0 * 1.27
            listOf(
                out("Cement bags (50 kg)", dry / 5.0 / 0.0347, "bags"),
                out("Sand", dry * 4.0 / 5.0, "m3"),
            )
        },

        Formula("steel_weight", "Steel bar weight", "Construction", "TMT bar weight (d^2/162 per metre).",
            listOf(InputSpec("dia", "Bar diameter (mm)", 12.0), InputSpec("len", "Total length (m)", 100.0)),
        ) { m ->
            val perM = v(m, "dia").pow(2) / 162.0
            listOf(out("Weight", perM * v(m, "len"), "kg"), out("Per metre", perM, "kg/m"))
        },

        Formula("staircase", "Staircase design", "Construction", "Number of risers and total run.",
            listOf(
                InputSpec("height", "Floor height (m)", 3.0),
                InputSpec("riser", "Riser height (mm)", 170.0),
                InputSpec("tread", "Tread depth (mm)", 280.0),
            ),
        ) { m ->
            val r = v(m, "riser") / 1000.0
            if (r == 0.0) listOf(out("Risers", 0.0, note = "Riser must be positive"))
            else {
                val steps = Math.ceil(v(m, "height") / r)
                listOf(out("Risers", steps), out("Total run", (steps - 1) * v(m, "tread") / 1000.0, "m"))
            }
        },

        Formula("far", "Floor area ratio (FAR/FSI)", "Construction", "Built-up area over plot area.",
            listOf(InputSpec("plot", "Plot area (m2)", 200.0), InputSpec("built", "Built-up area (m2)", 300.0)),
        ) { m ->
            val plot = v(m, "plot")
            if (plot == 0.0) listOf(out("FAR", 0.0, note = "Plot area must not be zero"))
            else listOf(out("FAR / FSI", v(m, "built") / plot))
        },

        Formula("cost_estimate", "Construction cost estimate", "Construction", "Area x rate.",
            listOf(InputSpec("area", "Area (sq ft)", 1000.0), InputSpec("rate", "Rate (per sq ft)", 2000.0)),
        ) { m -> listOf(out("Estimated cost", v(m, "area") * v(m, "rate"))) },

        Formula("solar_panels", "Solar panel sizing", "Construction", "Panels for a daily energy need.",
            listOf(
                InputSpec("kwh", "Daily need (kWh)", 10.0),
                InputSpec("sun", "Peak sun hours", 4.5),
                InputSpec("panel", "Panel rating (W)", 540.0),
            ),
        ) { m ->
            val denom = v(m, "sun") * v(m, "panel") / 1000.0
            if (denom == 0.0) listOf(out("Panels", 0.0, note = "Check sun hours and panel rating"))
            else listOf(out("Panels needed", Math.ceil(v(m, "kwh") / denom)))
        },

        // ================= EVERYDAY (extended) =================
        Formula("gpa", "GPA / CGPA", "Everyday", "Total grade points over total credits.",
            listOf(InputSpec("points", "Total grade points", 42.0), InputSpec("credits", "Total credits", 12.0)),
        ) { m ->
            val c = v(m, "credits")
            if (c == 0.0) listOf(out("GPA", 0.0, note = "Credits must not be zero"))
            else listOf(out("GPA", v(m, "points") / c))
        },

        Formula("grade_percent", "Marks percentage", "Everyday", "Score as a percentage and a letter grade.",
            listOf(InputSpec("obtained", "Marks obtained", 450.0), InputSpec("total", "Total marks", 500.0)),
        ) { m ->
            val t = v(m, "total")
            if (t == 0.0) listOf(out("Percentage", 0.0, note = "Total must not be zero"))
            else {
                val p = v(m, "obtained") / t * 100.0
                val g = when {
                    p >= 90 -> "A+"
                    p >= 80 -> "A"
                    p >= 70 -> "B"
                    p >= 60 -> "C"
                    p >= 50 -> "D"
                    p >= 40 -> "E"
                    else -> "F"
                }
                listOf(out("Percentage", p, "%", "Grade $g"))
            }
        },

        Formula("cooking_scale", "Recipe scaling", "Everyday", "Scale a quantity to a new serving count.",
            listOf(
                InputSpec("orig", "Original servings", 4.0),
                InputSpec("want", "Desired servings", 6.0),
                InputSpec("qty", "Quantity", 250.0),
            ),
        ) { m ->
            val o = v(m, "orig")
            if (o == 0.0) listOf(out("Quantity", 0.0, note = "Original servings must not be zero"))
            else listOf(out("Quantity", v(m, "qty") * v(m, "want") / o))
        },

        Formula("rent_split", "Rent / bill split", "Everyday", "Equal share per person.",
            listOf(InputSpec("rent", "Total amount", 30000.0), InputSpec("people", "People", 3.0)),
        ) { m ->
            val p = v(m, "people").coerceAtLeast(1.0)
            listOf(out("Per person", v(m, "rent") / p))
        },

        Formula("hourly_wage", "Salary to hourly", "Everyday", "Hourly rate from a monthly salary.",
            listOf(
                InputSpec("salary", "Monthly salary", 50000.0),
                InputSpec("hours", "Hours per week", 45.0),
            ),
        ) { m ->
            val h = v(m, "hours") * 52.0
            if (h == 0.0) listOf(out("Hourly", 0.0, note = "Hours must not be zero"))
            else listOf(out("Hourly rate", v(m, "salary") * 12.0 / h))
        },

        Formula("overtime", "Overtime pay", "Everyday", "Base rate x hours x multiplier.",
            listOf(
                InputSpec("rate", "Base hourly rate", 300.0),
                InputSpec("hours", "Overtime hours", 10.0),
                InputSpec("mult", "Multiplier", 1.5),
            ),
        ) { m -> listOf(out("Overtime pay", v(m, "rate") * v(m, "hours") * v(m, "mult"))) },

        Formula("cricket", "Cricket run rate", "Everyday", "Current run rate and required rate.",
            listOf(
                InputSpec("runs", "Runs scored", 120.0),
                InputSpec("overs", "Overs faced", 15.0),
                InputSpec("target", "Target runs", 180.0),
                InputSpec("totalOvers", "Total overs", 20.0),
            ),
        ) { m ->
            val ov = v(m, "overs")
            val rr = if (ov == 0.0) 0.0 else v(m, "runs") / ov
            val remain = v(m, "totalOvers") - ov
            val need = v(m, "target") - v(m, "runs")
            listOf(
                out("Current run rate", rr),
                out("Required run rate", if (remain <= 0.0) 0.0 else need / remain),
            )
        },

        Formula("appliance_cost", "Appliance running cost", "Everyday", "Energy cost from watts and hours.",
            listOf(
                InputSpec("watts", "Power (W)", 1500.0),
                InputSpec("hours", "Hours per day", 2.0),
                InputSpec("price", "Electricity (per kWh)", 8.0),
            ),
        ) { m ->
            val kwhDay = v(m, "watts") / 1000.0 * v(m, "hours")
            listOf(
                out("kWh per day", kwhDay, "kWh"),
                out("Cost per day", kwhDay * v(m, "price")),
                out("Cost per month", kwhDay * v(m, "price") * 30.0),
            )
        },

        Formula("commission", "Commission", "Everyday", "Sales x rate.",
            listOf(InputSpec("sales", "Sales amount", 200000.0), InputSpec("rate", "Commission (%)", 5.0)),
        ) { m -> listOf(out("Commission", v(m, "sales") * v(m, "rate") / 100.0)) },

        // ================= MATH (extended) =================
        Formula("linear1", "Linear equation (ax + b = 0)", "Math", "Solves for x.",
            listOf(InputSpec("a", "a", 2.0), InputSpec("b", "b", -6.0)),
        ) { m ->
            val a = v(m, "a")
            if (a == 0.0) listOf(out("x", 0.0, note = "a must not be zero"))
            else listOf(out("x", -v(m, "b") / a))
        },

        Formula("system2", "2 equations, 2 unknowns", "Math",
            "a1 x + b1 y = c1 and a2 x + b2 y = c2.",
            listOf(
                InputSpec("a1", "a1", 2.0), InputSpec("b1", "b1", 1.0), InputSpec("c1", "c1", 5.0),
                InputSpec("a2", "a2", 1.0), InputSpec("b2", "b2", -1.0), InputSpec("c2", "c2", 1.0),
            ),
        ) { m ->
            val d = v(m, "a1") * v(m, "b2") - v(m, "a2") * v(m, "b1")
            if (abs(d) < 1e-12) listOf(out("x", 0.0, note = "No unique solution (parallel lines)"))
            else listOf(
                out("x", (v(m, "c1") * v(m, "b2") - v(m, "c2") * v(m, "b1")) / d),
                out("y", (v(m, "a1") * v(m, "c2") - v(m, "a2") * v(m, "c1")) / d),
            )
        },

        Formula("system3", "3 equations, 3 unknowns", "Math",
            "a x + b y + c z = d for three equations (Cramer's rule).",
            listOf(
                InputSpec("a1", "a1", 1.0), InputSpec("b1", "b1", 1.0), InputSpec("c1", "c1", 1.0), InputSpec("d1", "d1", 6.0),
                InputSpec("a2", "a2", 2.0), InputSpec("b2", "b2", -1.0), InputSpec("c2", "c2", 1.0), InputSpec("d2", "d2", 3.0),
                InputSpec("a3", "a3", 1.0), InputSpec("b3", "b3", 2.0), InputSpec("c3", "c3", -1.0), InputSpec("d3", "d3", 2.0),
            ),
        ) { m ->
            val a1 = v(m, "a1"); val b1 = v(m, "b1"); val c1 = v(m, "c1"); val d1 = v(m, "d1")
            val a2 = v(m, "a2"); val b2 = v(m, "b2"); val c2 = v(m, "c2"); val d2 = v(m, "d2")
            val a3 = v(m, "a3"); val b3 = v(m, "b3"); val c3 = v(m, "c3"); val d3 = v(m, "d3")
            val det = a1 * (b2 * c3 - b3 * c2) - b1 * (a2 * c3 - a3 * c2) + c1 * (a2 * b3 - a3 * b2)
            if (abs(det) < 1e-12) listOf(out("x", 0.0, note = "No unique solution"))
            else listOf(
                out("x", (d1 * (b2 * c3 - b3 * c2) - b1 * (d2 * c3 - d3 * c2) + c1 * (d2 * b3 - d3 * b2)) / det),
                out("y", (a1 * (d2 * c3 - d3 * c2) - d1 * (a2 * c3 - a3 * c2) + c1 * (a2 * d3 - a3 * d2)) / det),
                out("z", (a1 * (b2 * d3 - b3 * d2) - b1 * (a2 * d3 - a3 * d2) + d1 * (a2 * b3 - a3 * b2)) / det),
            )
        },

        Formula("ap", "Arithmetic progression", "Math", "nth term and sum of n terms.",
            listOf(
                InputSpec("a", "First term a", 2.0),
                InputSpec("d", "Common difference d", 3.0),
                InputSpec("n", "Number of terms n", 10.0),
            ),
        ) { m ->
            val a = v(m, "a"); val d = v(m, "d"); val n = v(m, "n")
            listOf(out("nth term", a + (n - 1) * d), out("Sum of n terms", n / 2.0 * (2 * a + (n - 1) * d)))
        },

        Formula("gp", "Geometric progression", "Math", "nth term and sum of n terms.",
            listOf(
                InputSpec("a", "First term a", 2.0),
                InputSpec("r", "Common ratio r", 3.0),
                InputSpec("n", "Number of terms n", 5.0),
            ),
        ) { m ->
            val a = v(m, "a"); val r = v(m, "r"); val n = v(m, "n")
            val sum = if (r == 1.0) a * n else a * (r.pow(n) - 1.0) / (r - 1.0)
            listOf(out("nth term", a * r.pow(n - 1)), out("Sum of n terms", sum))
        },

        Formula("proportion", "Proportion (a:b = c:x)", "Math", "Solve for x.",
            listOf(InputSpec("a", "a", 2.0), InputSpec("b", "b", 4.0), InputSpec("c", "c", 6.0)),
        ) { m ->
            val a = v(m, "a")
            if (a == 0.0) listOf(out("x", 0.0, note = "a must not be zero"))
            else listOf(out("x", v(m, "b") * v(m, "c") / a))
        },

        Formula("fraction", "Fraction to decimal & percent", "Math", "Convert a fraction.",
            listOf(InputSpec("num", "Numerator", 3.0), InputSpec("den", "Denominator", 8.0)),
        ) { m ->
            val d = v(m, "den")
            if (d == 0.0) listOf(out("Decimal", 0.0, note = "Denominator must not be zero"))
            else listOf(out("Decimal", v(m, "num") / d), out("Percent", v(m, "num") / d * 100.0, "%"))
        },

        Formula("log_base", "Logarithm to any base", "Math", "log base b of x.",
            listOf(InputSpec("x", "Value x", 1000.0), InputSpec("b", "Base b", 10.0)),
        ) { m ->
            val x = v(m, "x"); val b = v(m, "b")
            if (x <= 0.0 || b <= 0.0 || b == 1.0) listOf(out("Result", 0.0, note = "Need x > 0 and base > 0, not 1"))
            else listOf(out("log_b(x)", ln(x) / ln(b)))
        },

        Formula("fibonacci", "Fibonacci", "Math", "nth Fibonacci number (iterative).",
            listOf(InputSpec("n", "n", 10.0)),
        ) { m ->
            val n = v(m, "n").toInt().coerceIn(0, 90)
            var a = 0L; var b = 1L
            for (i in 0 until n) { val t = a + b; a = b; b = t }
            listOf(out("F(n)", a.toDouble()))
        },

        Formula("prime_checker", "Prime checker", "Math",
            "Is it prime? Also shows the smallest factor and the divisor count.",
            listOf(InputSpec("n", "Number", 91.0)),
        ) { m ->
            val n = v(m, "n").toLong()
            if (n < 2) listOf(out("Is prime", 0.0, note = "Numbers below 2 are not prime"))
            else {
                var prime = true
                var smallest = n
                var i = 2L
                while (i * i <= n) {
                    if (n % i == 0L) { prime = false; smallest = i; break }
                    i++
                }
                var count = 0L
                var d = 1L
                while (d * d <= n) {
                    if (n % d == 0L) count += if (d * d == n) 1 else 2
                    d++
                }
                listOf(
                    out("Is prime", if (prime) 1.0 else 0.0, "", if (prime) "Prime" else "Composite"),
                    out("Smallest factor", if (prime) n.toDouble() else smallest.toDouble()),
                    out("Number of divisors", count.toDouble()),
                )
            }
        },

        Formula("number_generator", "Number generator", "Everyday",
            "Random numbers in a range (re-rolls when the range changes).",
            listOf(InputSpec("min", "Minimum", 1.0), InputSpec("max", "Maximum", 100.0)),
        ) { m ->
            val lo = v(m, "min").toLong()
            val hi = v(m, "max").toLong()
            if (hi < lo) listOf(out("Random", 0.0, note = "Maximum must be at least the minimum"))
            else listOf(
                out("Random integer", kotlin.random.Random.nextLong(lo, hi + 1).toDouble()),
                out("Random decimal", lo + kotlin.random.Random.nextDouble() * (hi - lo)),
            )
        },

        Formula("average", "Average (mean)", "Math",
            "Mean and sum of up to five numbers.",
            listOf(
                InputSpec("v1", "Value 1", 10.0), InputSpec("v2", "Value 2", 20.0),
                InputSpec("v3", "Value 3", 30.0), InputSpec("v4", "Value 4", 40.0),
                InputSpec("v5", "Value 5", 50.0),
            ),
        ) { m ->
            val xs = listOf("v1", "v2", "v3", "v4", "v5").map { v(m, it) }
            listOf(out("Average", xs.sum() / xs.size), out("Sum", xs.sum()))
        },

        Formula("ratio", "Ratio", "Math",
            "Simplify a ratio and read its decimal form.",
            listOf(InputSpec("a", "First", 12.0), InputSpec("b", "Second", 18.0)),
        ) { m ->
            val a = v(m, "a").toLong()
            val b = v(m, "b").toLong()
            if (a == 0L && b == 0L) listOf(out("Simplified first", 0.0, note = "Enter a non-zero value"))
            else {
                val g = Evaluator.gcd(a, b).coerceAtLeast(1L)
                listOf(
                    out("Simplified first", (a / g).toDouble()),
                    out("Simplified second", (b / g).toDouble()),
                    out("Decimal (a / b)", if (b == 0L) 0.0 else v(m, "a") / v(m, "b")),
                )
            }
        },

        Formula("unit_price", "Unit price", "Everyday",
            "Price per unit and the cost for a wanted quantity.",
            listOf(
                InputSpec("price", "Total price", 240.0),
                InputSpec("qty", "Quantity", 12.0),
                InputSpec("want", "Wanted quantity", 20.0),
            ),
        ) { m ->
            val q = v(m, "qty")
            if (q == 0.0) listOf(out("Unit price", 0.0, note = "Quantity must not be zero"))
            else {
                val unit = v(m, "price") / q
                listOf(out("Unit price", unit), out("Cost for wanted quantity", unit * v(m, "want")))
            }
        },

        // ================= ECONOMICS =================
        Formula("gdp_expenditure", "GDP (expenditure method)", "Economics",
            "GDP = C + I + G + (X - M).",
            listOf(
                InputSpec("c", "Consumption (C)", 1000.0),
                InputSpec("i", "Investment (I)", 400.0),
                InputSpec("g", "Government spending (G)", 300.0),
                InputSpec("x", "Exports (X)", 200.0),
                InputSpec("m", "Imports (M)", 250.0),
            ),
        ) { m -> listOf(out("GDP", v(m, "c") + v(m, "i") + v(m, "g") + (v(m, "x") - v(m, "m")))) },

        Formula("gdp_growth", "GDP growth rate", "Economics",
            "Percentage change in GDP between two periods.",
            listOf(InputSpec("gdp1", "GDP period 1", 1000.0), InputSpec("gdp2", "GDP period 2", 1080.0)),
        ) { m ->
            val a = v(m, "gdp1")
            if (a == 0.0) listOf(out("Growth", 0.0, note = "Period 1 must not be zero"))
            else listOf(out("Growth", (v(m, "gdp2") - a) / a * 100.0, "%"))
        },

        Formula("gdp_per_capita", "GDP per capita", "Economics",
            "Output per person.",
            listOf(InputSpec("gdp", "GDP", 3000000.0), InputSpec("pop", "Population", 1400000000.0)),
        ) { m ->
            val p = v(m, "pop")
            if (p == 0.0) listOf(out("GDP per capita", 0.0, note = "Population must not be zero"))
            else listOf(out("GDP per capita", v(m, "gdp") / p))
        },

        Formula("gdp_deflator", "GDP deflator", "Economics",
            "Nominal GDP over real GDP, times 100.",
            listOf(InputSpec("nominal", "Nominal GDP", 1100.0), InputSpec("real", "Real GDP", 1000.0)),
        ) { m ->
            val r = v(m, "real")
            if (r == 0.0) listOf(out("Deflator", 0.0, note = "Real GDP must not be zero"))
            else listOf(out("GDP deflator", v(m, "nominal") / r * 100.0))
        },

        Formula("real_gdp", "Real GDP", "Economics",
            "Real GDP = nominal GDP / deflator x 100.",
            listOf(InputSpec("nominal", "Nominal GDP", 1100.0), InputSpec("deflator", "Deflator", 110.0)),
        ) { m ->
            val d = v(m, "deflator")
            if (d == 0.0) listOf(out("Real GDP", 0.0, note = "Deflator must not be zero"))
            else listOf(out("Real GDP", v(m, "nominal") / d * 100.0))
        },

        Formula("inflation_cpi", "Inflation rate (CPI)", "Economics",
            "Percentage change in the consumer price index.",
            listOf(InputSpec("cpi1", "CPI earlier", 120.0), InputSpec("cpi2", "CPI later", 132.0)),
        ) { m ->
            val a = v(m, "cpi1")
            if (a == 0.0) listOf(out("Inflation", 0.0, note = "Earlier CPI must not be zero"))
            else listOf(out("Inflation", (v(m, "cpi2") - a) / a * 100.0, "%"))
        },

        Formula("unemployment_rate", "Unemployment rate", "Economics",
            "Unemployed over the labour force.",
            listOf(
                InputSpec("unemployed", "Unemployed", 5.0),
                InputSpec("labour", "Labour force", 100.0),
            ),
        ) { m ->
            val l = v(m, "labour")
            if (l == 0.0) listOf(out("Unemployment rate", 0.0, note = "Labour force must not be zero"))
            else listOf(out("Unemployment rate", v(m, "unemployed") / l * 100.0, "%"))
        },

        Formula("participation_rate", "Labour participation rate", "Economics",
            "Labour force over the working-age population.",
            listOf(
                InputSpec("labour", "Labour force", 100.0),
                InputSpec("pop", "Working-age population", 160.0),
            ),
        ) { m ->
            val p = v(m, "pop")
            if (p == 0.0) listOf(out("Participation rate", 0.0, note = "Population must not be zero"))
            else listOf(out("Participation rate", v(m, "labour") / p * 100.0, "%"))
        },

        Formula("fisher", "Fisher equation (nominal & real rate)", "Economics",
            "Links nominal interest, real interest and inflation.",
            listOf(
                InputSpec("nominal", "Nominal rate (%)", 9.0),
                InputSpec("inflation", "Inflation (%)", 5.0),
            ),
        ) { m ->
            val i = v(m, "nominal") / 100.0; val pi = v(m, "inflation") / 100.0
            listOf(
                out("Real rate (exact)", ((1.0 + i) / (1.0 + pi) - 1.0) * 100.0, "%"),
                out("Real rate (approx)", v(m, "nominal") - v(m, "inflation"), "%"),
            )
        },

        Formula("money_multiplier", "Money multiplier", "Economics",
            "1 / reserve ratio.",
            listOf(InputSpec("reserve", "Reserve ratio (%)", 10.0)),
        ) { m ->
            val r = v(m, "reserve")
            if (r <= 0.0) listOf(out("Money multiplier", 0.0, note = "Reserve ratio must be positive"))
            else listOf(out("Money multiplier", 100.0 / r))
        },

        Formula("required_reserves", "Required reserves", "Economics",
            "Deposits x reserve ratio.",
            listOf(InputSpec("deposits", "Deposits", 100000.0), InputSpec("reserve", "Reserve ratio (%)", 10.0)),
        ) { m -> listOf(out("Required reserves", v(m, "deposits") * v(m, "reserve") / 100.0)) },

        Formula("money_creation", "Money creation by banks", "Economics",
            "Total deposits created from an initial deposit.",
            listOf(
                InputSpec("initial", "Initial deposit", 10000.0),
                InputSpec("reserve", "Reserve ratio (%)", 10.0),
            ),
        ) { m ->
            val r = v(m, "reserve")
            if (r <= 0.0) listOf(out("Total money", 0.0, note = "Reserve ratio must be positive"))
            else {
                val mult = 100.0 / r
                listOf(out("Money multiplier", mult), out("Total money created", v(m, "initial") * mult))
            }
        },

        Formula("velocity_money", "Velocity of money", "Economics",
            "V = (P x Y) / M.",
            listOf(
                InputSpec("p", "Price level", 110.0),
                InputSpec("y", "Real output (Y)", 1000.0),
                InputSpec("m", "Money supply (M)", 500.0),
            ),
        ) { m ->
            val ms = v(m, "m")
            if (ms == 0.0) listOf(out("Velocity", 0.0, note = "Money supply must not be zero"))
            else listOf(out("Velocity of money", v(m, "p") * v(m, "y") / ms))
        },

        Formula("quantity_equation", "Quantity equation (MV = PY)", "Economics",
            "Solves for real output Y.",
            listOf(
                InputSpec("m", "Money supply (M)", 500.0),
                InputSpec("vel", "Velocity (V)", 2.0),
                InputSpec("p", "Price level (P)", 110.0),
            ),
        ) { m ->
            val p = v(m, "p")
            if (p == 0.0) listOf(out("Real output", 0.0, note = "Price level must not be zero"))
            else listOf(out("Real output (Y)", v(m, "m") * v(m, "vel") / p))
        },

        Formula("elasticity_demand", "Price elasticity of demand", "Economics",
            "Arc elasticity between two points.",
            listOf(
                InputSpec("q1", "Quantity 1", 100.0), InputSpec("q2", "Quantity 2", 80.0),
                InputSpec("p1", "Price 1", 10.0), InputSpec("p2", "Price 2", 12.0),
            ),
        ) { m ->
            val dq = v(m, "q2") - v(m, "q1"); val dp = v(m, "p2") - v(m, "p1")
            val qAvg = (v(m, "q1") + v(m, "q2")) / 2.0; val pAvg = (v(m, "p1") + v(m, "p2")) / 2.0
            if (dp == 0.0 || pAvg == 0.0 || qAvg == 0.0) listOf(out("Elasticity", 0.0, note = "Prices must differ"))
            else {
                val e = (dq / qAvg) / (dp / pAvg)
                val kind = when {
                    abs(e) > 1.0 -> "Elastic"
                    abs(e) < 1.0 -> "Inelastic"
                    else -> "Unit elastic"
                }
                listOf(out("Price elasticity of demand", e, "", kind))
            }
        },

        Formula("elasticity_income", "Income elasticity of demand", "Economics",
            "How demand responds to income.",
            listOf(
                InputSpec("q1", "Quantity 1", 100.0), InputSpec("q2", "Quantity 2", 130.0),
                InputSpec("y1", "Income 1", 50000.0), InputSpec("y2", "Income 2", 60000.0),
            ),
        ) { m ->
            val dq = v(m, "q2") - v(m, "q1"); val dy = v(m, "y2") - v(m, "y1")
            val qAvg = (v(m, "q1") + v(m, "q2")) / 2.0; val yAvg = (v(m, "y1") + v(m, "y2")) / 2.0
            if (dy == 0.0 || qAvg == 0.0 || yAvg == 0.0) listOf(out("Elasticity", 0.0, note = "Incomes must differ"))
            else {
                val e = (dq / qAvg) / (dy / yAvg)
                val kind = if (e < 0) "Inferior good" else "Normal good"
                listOf(out("Income elasticity", e, "", kind))
            }
        },

        Formula("elasticity_cross", "Cross-price elasticity", "Economics",
            "How demand for A responds to the price of B.",
            listOf(
                InputSpec("qa1", "Qty A 1", 100.0), InputSpec("qa2", "Qty A 2", 120.0),
                InputSpec("pb1", "Price B 1", 50.0), InputSpec("pb2", "Price B 2", 60.0),
            ),
        ) { m ->
            val dq = v(m, "qa2") - v(m, "qa1"); val dp = v(m, "pb2") - v(m, "pb1")
            val qAvg = (v(m, "qa1") + v(m, "qa2")) / 2.0; val pAvg = (v(m, "pb1") + v(m, "pb2")) / 2.0
            if (dp == 0.0 || qAvg == 0.0 || pAvg == 0.0) listOf(out("Elasticity", 0.0, note = "Prices must differ"))
            else {
                val e = (dq / qAvg) / (dp / pAvg)
                val kind = if (e > 0) "Substitutes" else "Complements"
                listOf(out("Cross-price elasticity", e, "", kind))
            }
        },

        Formula("mpc_mps", "MPC & MPS", "Economics",
            "Marginal propensity to consume and to save.",
            listOf(
                InputSpec("dc", "Change in consumption", 80.0),
                InputSpec("ds", "Change in saving", 20.0),
                InputSpec("dy", "Change in income", 100.0),
            ),
        ) { m ->
            val dy = v(m, "dy")
            if (dy == 0.0) listOf(out("MPC", 0.0, note = "Change in income must not be zero"))
            else listOf(
                out("MPC", v(m, "dc") / dy),
                out("MPS", v(m, "ds") / dy),
            )
        },

        Formula("spending_multiplier", "Spending multiplier", "Economics",
            "k = 1 / (1 - MPC) = 1 / MPS.",
            listOf(InputSpec("mpc", "MPC", 0.8)),
        ) { m ->
            val mpc = v(m, "mpc")
            if (mpc >= 1.0) listOf(out("Multiplier", 0.0, note = "MPC must be below 1"))
            else listOf(out("Spending multiplier", 1.0 / (1.0 - mpc)))
        },

        Formula("tax_multiplier", "Tax multiplier", "Economics",
            "-MPC / (1 - MPC).",
            listOf(InputSpec("mpc", "MPC", 0.8)),
        ) { m ->
            val mpc = v(m, "mpc")
            if (mpc >= 1.0) listOf(out("Multiplier", 0.0, note = "MPC must be below 1"))
            else listOf(out("Tax multiplier", -mpc / (1.0 - mpc)))
        },

        Formula("multiplier_effect", "Multiplier effect on GDP", "Economics",
            "Total change in GDP from an initial spending change.",
            listOf(
                InputSpec("change", "Initial spending change", 100.0),
                InputSpec("mpc", "MPC", 0.8),
            ),
        ) { m ->
            val mpc = v(m, "mpc")
            if (mpc >= 1.0) listOf(out("Total change", 0.0, note = "MPC must be below 1"))
            else listOf(out("Total change in GDP", v(m, "change") * 1.0 / (1.0 - mpc)))
        },

        Formula("consumer_surplus", "Consumer surplus", "Economics",
            "0.5 x (max price - equilibrium price) x quantity.",
            listOf(
                InputSpec("max", "Maximum willingness to pay", 50.0),
                InputSpec("eq", "Equilibrium price", 30.0),
                InputSpec("qty", "Quantity", 100.0),
            ),
        ) { m -> listOf(out("Consumer surplus", 0.5 * (v(m, "max") - v(m, "eq")) * v(m, "qty"))) },

        Formula("producer_surplus", "Producer surplus", "Economics",
            "0.5 x (equilibrium price - minimum price) x quantity.",
            listOf(
                InputSpec("eq", "Equilibrium price", 30.0),
                InputSpec("min", "Minimum acceptable price", 15.0),
                InputSpec("qty", "Quantity", 100.0),
            ),
        ) { m -> listOf(out("Producer surplus", 0.5 * (v(m, "eq") - v(m, "min")) * v(m, "qty"))) },

        Formula("deadweight_loss", "Deadweight loss", "Economics",
            "0.5 x tax x fall in quantity.",
            listOf(
                InputSpec("tax", "Tax per unit", 5.0),
                InputSpec("dq", "Fall in quantity", 40.0),
            ),
        ) { m -> listOf(out("Deadweight loss", 0.5 * v(m, "tax") * v(m, "dq"))) },

        Formula("terms_of_trade", "Terms of trade", "Economics",
            "Export prices over import prices, times 100.",
            listOf(
                InputSpec("px", "Export price index", 120.0),
                InputSpec("pm", "Import price index", 100.0),
            ),
        ) { m ->
            val pm = v(m, "pm")
            if (pm == 0.0) listOf(out("Terms of trade", 0.0, note = "Import index must not be zero"))
            else listOf(out("Terms of trade", v(m, "px") / pm * 100.0))
        },

        Formula("balance_of_trade", "Balance of trade", "Economics",
            "Exports minus imports.",
            listOf(InputSpec("exports", "Exports", 500.0), InputSpec("imports", "Imports", 600.0)),
        ) { m ->
            val b = v(m, "exports") - v(m, "imports")
            listOf(out("Balance of trade", b, "", if (b >= 0) "Surplus" else "Deficit"))
        },

        Formula("real_exchange_rate", "Real exchange rate", "Economics",
            "Nominal rate x domestic price / foreign price.",
            listOf(
                InputSpec("nominal", "Nominal exchange rate", 83.0),
                InputSpec("pd", "Domestic price level", 110.0),
                InputSpec("pf", "Foreign price level", 100.0),
            ),
        ) { m ->
            val pf = v(m, "pf")
            if (pf == 0.0) listOf(out("Real exchange rate", 0.0, note = "Foreign price must not be zero"))
            else listOf(out("Real exchange rate", v(m, "nominal") * v(m, "pd") / pf))
        },

        Formula("ppp_rate", "Purchasing power parity rate", "Economics",
            "PPP exchange rate = price of the basket at home / abroad.",
            listOf(
                InputSpec("pd", "Price of basket at home", 2400.0),
                InputSpec("pf", "Price of same basket abroad", 30.0),
            ),
        ) { m ->
            val pf = v(m, "pf")
            if (pf == 0.0) listOf(out("PPP rate", 0.0, note = "Foreign price must not be zero"))
            else listOf(out("PPP exchange rate", v(m, "pd") / pf))
        },

        Formula("cobb_douglas", "Cobb-Douglas production", "Economics",
            "Y = A x K^alpha x L^beta.",
            listOf(
                InputSpec("a", "Total factor productivity (A)", 1.0),
                InputSpec("k", "Capital (K)", 100.0),
                InputSpec("l", "Labour (L)", 200.0),
                InputSpec("alpha", "alpha", 0.3),
                InputSpec("beta", "beta", 0.7),
            ),
        ) { m ->
            listOf(out("Output (Y)", v(m, "a") * v(m, "k").pow(v(m, "alpha")) * v(m, "l").pow(v(m, "beta"))))
        },

        Formula("labour_productivity", "Labour productivity", "Economics",
            "Output per labour hour.",
            listOf(InputSpec("output", "Output", 10000.0), InputSpec("hours", "Labour hours", 500.0)),
        ) { m ->
            val h = v(m, "hours")
            if (h == 0.0) listOf(out("Productivity", 0.0, note = "Hours must not be zero"))
            else listOf(out("Output per hour", v(m, "output") / h))
        },

        Formula("hdi", "Human Development Index", "Economics",
            "Average of the health, education and income indices (each 0-1).",
            listOf(
                InputSpec("health", "Life expectancy index", 0.8),
                InputSpec("education", "Education index", 0.7),
                InputSpec("income", "Income index", 0.75),
            ),
        ) { m ->
            val hdi = (v(m, "health") + v(m, "education") + v(m, "income")) / 3.0
            val band = when {
                hdi >= 0.8 -> "Very high"
                hdi >= 0.7 -> "High"
                hdi >= 0.55 -> "Medium"
                else -> "Low"
            }
            listOf(out("HDI", hdi, "", band))
        },

        Formula("real_wage", "Real wage", "Economics",
            "Real wage = nominal wage / CPI x 100.",
            listOf(InputSpec("wage", "Nominal wage", 30000.0), InputSpec("cpi", "CPI", 120.0)),
        ) { m ->
            val cpi = v(m, "cpi")
            if (cpi == 0.0) listOf(out("Real wage", 0.0, note = "CPI must not be zero"))
            else listOf(out("Real wage", v(m, "wage") / cpi * 100.0))
        },

        Formula("eoq", "Economic order quantity", "Economics",
            "EOQ = sqrt(2 x demand x order cost / holding cost).",
            listOf(
                InputSpec("d", "Annual demand", 10000.0),
                InputSpec("s", "Cost per order", 500.0),
                InputSpec("h", "Holding cost per unit", 20.0),
            ),
        ) { m ->
            val h = v(m, "h")
            if (h <= 0.0) listOf(out("EOQ", 0.0, note = "Holding cost must be positive"))
            else listOf(out("Economic order quantity", sqrt(2.0 * v(m, "d") * v(m, "s") / h)))
        },

        Formula("marginal_cost", "Marginal cost & revenue", "Economics",
            "Change in cost / revenue per extra unit.",
            listOf(
                InputSpec("dtc", "Change in total cost", 200.0),
                InputSpec("dtr", "Change in total revenue", 300.0),
                InputSpec("dq", "Change in quantity", 10.0),
            ),
        ) { m ->
            val dq = v(m, "dq")
            if (dq == 0.0) listOf(out("Marginal cost", 0.0, note = "Quantity change must not be zero"))
            else {
                val mc = v(m, "dtc") / dq
                val mr = v(m, "dtr") / dq
                listOf(
                    out("Marginal cost", mc),
                    out("Marginal revenue", mr),
                    out("Marginal profit", mr - mc, "", if (mr > mc) "Expand output" else "Reduce output"),
                )
            }
        },

        Formula("savings_rate", "Savings rate", "Economics",
            "Savings as a share of income.",
            listOf(InputSpec("savings", "Savings", 20000.0), InputSpec("income", "Income", 100000.0)),
        ) { m ->
            val i = v(m, "income")
            if (i == 0.0) listOf(out("Savings rate", 0.0, note = "Income must not be zero"))
            else listOf(out("Savings rate", v(m, "savings") / i * 100.0, "%"))
        },

        Formula("debt_to_gdp", "Debt to GDP ratio", "Economics",
            "Government debt over GDP.",
            listOf(InputSpec("debt", "Government debt", 8000000.0), InputSpec("gdp", "GDP", 3000000.0)),
        ) { m ->
            val g = v(m, "gdp")
            if (g == 0.0) listOf(out("Debt to GDP", 0.0, note = "GDP must not be zero"))
            else listOf(out("Debt to GDP", v(m, "debt") / g * 100.0, "%"))
        },

        Formula("fiscal_deficit", "Fiscal deficit", "Economics",
            "Expenditure minus revenue, and its share of GDP.",
            listOf(
                InputSpec("exp", "Government expenditure", 500.0),
                InputSpec("rev", "Government revenue", 450.0),
                InputSpec("gdp", "GDP", 3000.0),
            ),
        ) { m ->
            val deficit = v(m, "exp") - v(m, "rev")
            val g = v(m, "gdp")
            listOf(
                out("Fiscal deficit", deficit),
                out("Deficit as % of GDP", if (g == 0.0) 0.0 else deficit / g * 100.0, "%"),
            )
        },

        Formula("cola", "Cost of living adjustment", "Economics",
            "Salary adjusted for inflation.",
            listOf(InputSpec("salary", "Current salary", 60000.0), InputSpec("inflation", "Inflation (%)", 6.0)),
        ) { m ->
            val newSalary = v(m, "salary") * (1.0 + v(m, "inflation") / 100.0)
            listOf(out("Adjusted salary", newSalary), out("Increase needed", newSalary - v(m, "salary")))
        },

        Formula("okun", "Okun's law", "Economics",
            "Unemployment change from the GDP growth gap.",
            listOf(
                InputSpec("actual", "Actual GDP growth (%)", 4.0),
                InputSpec("potential", "Potential GDP growth (%)", 6.0),
            ),
        ) { m ->
            val gap = v(m, "actual") - v(m, "potential")
            listOf(out("Output gap", gap, "%"), out("Change in unemployment", -0.5 * gap, "%"))
        },

        Formula("rule70", "Rule of 70 (doubling time)", "Economics",
            "Years for an economy to double at a growth rate.",
            listOf(InputSpec("growth", "Growth rate (% per year)", 7.0)),
        ) { m ->
            val g = v(m, "growth")
            if (g <= 0.0) listOf(out("Years to double", 0.0, note = "Growth rate must be positive"))
            else listOf(out("Years to double", 70.0 / g))
        },

        Formula("inflation_adjusted_return", "Inflation-adjusted return", "Economics",
            "Real return after inflation.",
            listOf(InputSpec("nominal", "Nominal return (%)", 12.0), InputSpec("inflation", "Inflation (%)", 6.0)),
        ) { m ->
            val r = (1.0 + v(m, "nominal") / 100.0) / (1.0 + v(m, "inflation") / 100.0) - 1.0
            listOf(out("Real return", r * 100.0, "%"))
        },
        // ================= TRIGONOMETRY =================
        Formula("law_of_cosines", "Law of cosines", "Trigonometry",
            "Find the third side and the other angles from two sides and the included angle.",
            listOf(InputSpec("a", "Side a", 8.0), InputSpec("b", "Side b", 6.0), InputSpec("cAngle", "Included angle C (deg)", 60.0)),
        ) { m ->
            val a = v(m, "a"); val b = v(m, "b"); val c = d2r(v(m, "cAngle"))
            val side = sqrt(a * a + b * b - 2 * a * b * cos(c))
            val angA = r2d(kotlin.math.acos(((b * b + side * side - a * a) / (2 * b * side)).coerceIn(-1.0, 1.0)))
            val angB = 180.0 - v(m, "cAngle") - angA
            listOf(out("Side c", side), out("Angle A", angA, "\u00B0"), out("Angle B", angB, "\u00B0"),
                out("Area", 0.5 * a * b * sin(c)))
        },

        Formula("law_of_sines", "Law of sines", "Trigonometry",
            "Find another side from one side and two angles.",
            listOf(InputSpec("a", "Side a", 10.0), InputSpec("aAngle", "Angle A (deg)", 40.0), InputSpec("bAngle", "Angle B (deg)", 60.0)),
        ) { m ->
            val a = v(m, "a"); val A = d2r(v(m, "aAngle")); val B = d2r(v(m, "bAngle"))
            val b = a * sin(B) / sin(A)
            val C = 180.0 - v(m, "aAngle") - v(m, "bAngle")
            val c = a * sin(d2r(C)) / sin(A)
            listOf(out("Side b", b), out("Angle C", C, "\u00B0"), out("Side c", c),
                out("Area", 0.5 * a * b * sin(d2r(C))))
        },

        Formula("triangle_sas", "Triangle (two sides + angle)", "Trigonometry",
            "Area and third side from two sides and the included angle.",
            listOf(InputSpec("a", "Side a", 7.0), InputSpec("b", "Side b", 9.0), InputSpec("angle", "Included angle (deg)", 45.0)),
        ) { m ->
            val a = v(m, "a"); val b = v(m, "b"); val t = d2r(v(m, "angle"))
            listOf(out("Area", 0.5 * a * b * sin(t)),
                out("Third side", sqrt(a * a + b * b - 2 * a * b * cos(t))),
                out("Perimeter", a + b + sqrt(a * a + b * b - 2 * a * b * cos(t))))
        },

        Formula("triangle_inscribed_circumscribed", "Inradius and circumradius", "Trigonometry",
            "Circle inside and circle around a triangle.",
            listOf(InputSpec("a", "Side a", 3.0), InputSpec("b", "Side b", 4.0), InputSpec("c", "Side c", 5.0)),
        ) { m ->
            val a = v(m, "a"); val b = v(m, "b"); val c = v(m, "c")
            val sp = (a + b + c) / 2.0
            val area = sqrt((sp * (sp - a) * (sp - b) * (sp - c)).coerceAtLeast(0.0))
            listOf(out("Area", area), out("Inradius", if (sp == 0.0) 0.0 else area / sp),
                out("Circumradius", if (area == 0.0) 0.0 else a * b * c / (4 * area)))
        },

        Formula("right_triangle_solver", "Right triangle", "Trigonometry",
            "From the two legs get the hypotenuse and both angles.",
            listOf(InputSpec("opposite", "Opposite leg", 3.0), InputSpec("adjacent", "Adjacent leg", 4.0)),
        ) { m ->
            val o = v(m, "opposite"); val adj = v(m, "adjacent")
            val hyp = kotlin.math.hypot(o, adj)
            listOf(out("Hypotenuse", hyp), out("Angle A", r2d(kotlin.math.atan(o / adj)), "\u00B0"),
                out("Angle B", r2d(kotlin.math.atan(adj / o)), "\u00B0"), out("Area", 0.5 * o * adj))
        },

        Formula("arc_and_chord", "Arc, chord and sector", "Trigonometry",
            "From the radius and the central angle.",
            listOf(InputSpec("r", "Radius", 10.0), InputSpec("angle", "Central angle (deg)", 60.0)),
        ) { m ->
            val r = v(m, "r"); val t = d2r(v(m, "angle"))
            listOf(out("Arc length", r * t), out("Chord", 2 * r * sin(t / 2)),
                out("Sector area", 0.5 * r * r * t), out("Segment area", 0.5 * r * r * (t - sin(t))))
        },

        Formula("angle_converter", "Degrees and radians", "Trigonometry",
            "Convert both ways, and see the angle in turns and gradians.",
            listOf(InputSpec("angle", "Angle (deg)", 45.0)),
        ) { m ->
            val a = v(m, "angle")
            listOf(out("Radians", d2r(a)), out("Turns", a / 360.0),
                out("Gradians", a / 0.9), out("Arcminutes", a * 60.0))
        },

        Formula("inverse_trig_values", "Inverse trig (principal values)", "Trigonometry",
            "asin, acos and atan of a value, in degrees.",
            listOf(InputSpec("x", "Value", 0.5)),
        ) { m ->
            val x = v(m, "x").coerceIn(-1.0, 1.0)
            listOf(out("asin", r2d(kotlin.math.asin(x)), "\u00B0"),
                out("acos", r2d(kotlin.math.acos(x)), "\u00B0"),
                out("atan", r2d(kotlin.math.atan(v(m, "x"))), "\u00B0"))
        },

        Formula("hyperbolic_values", "Hyperbolic functions", "Trigonometry",
            "sinh, cosh and tanh of a value.",
            listOf(InputSpec("x", "Value", 1.0)),
        ) { m ->
            val x = v(m, "x")
            listOf(out("sinh", kotlin.math.sinh(x)), out("cosh", kotlin.math.cosh(x)),
                out("tanh", kotlin.math.tanh(x)))
        },

        Formula("height_by_elevation", "Height from an elevation angle", "Trigonometry",
            "Distance and the angle of elevation to the top.",
            listOf(InputSpec("distance", "Distance", 50.0), InputSpec("angle", "Angle of elevation (deg)", 30.0)),
        ) { m ->
            val d = v(m, "distance"); val t = d2r(v(m, "angle"))
            listOf(out("Height", d * tan(t)), out("Slant distance", d / cos(t)))
        },

        Formula("angle_between_lines", "Angle between two lines", "Trigonometry",
            "From the two slopes.",
            listOf(InputSpec("m1", "Slope 1", 1.0), InputSpec("m2", "Slope 2", -1.0)),
        ) { m ->
            val m1 = v(m, "m1"); val m2 = v(m, "m2")
            val t = kotlin.math.atan(abs((m1 - m2) / (1 + m1 * m2)))
            listOf(out("Angle", r2d(t), "\u00B0"), out("Angle (rad)", t))
        },

        // ================= ANALYTIC GEOMETRY =================
        Formula("line_from_two_points", "Line through two points", "Analytic Geometry",
            "Slope, intercept and the line equation.",
            listOf(InputSpec("x1", "x1", 1.0), InputSpec("y1", "y1", 2.0), InputSpec("x2", "x2", 4.0), InputSpec("y2", "y2", 8.0)),
        ) { m ->
            val x1 = v(m, "x1"); val y1 = v(m, "y1"); val x2 = v(m, "x2"); val y2 = v(m, "y2")
            if (x2 == x1) listOf(out("Slope", 0.0, note = "Vertical line"))
            else {
                val sl = (y2 - y1) / (x2 - x1)
                listOf(out("Slope", sl), out("y-intercept", y1 - sl * x1),
                    out("Distance", kotlin.math.hypot(x2 - x1, y2 - y1)))
            }
        },

        Formula("point_line_distance", "Distance from a point to a line", "Analytic Geometry",
            "For the line Ax + By + C = 0.",
            listOf(InputSpec("A", "A", 3.0), InputSpec("B", "B", 4.0), InputSpec("C", "C", -10.0),
                InputSpec("x0", "x0", 0.0), InputSpec("y0", "y0", 0.0)),
        ) { m ->
            val A = v(m, "A"); val B = v(m, "B"); val C = v(m, "C")
            val den = kotlin.math.hypot(A, B)
            listOf(out("Distance", if (den == 0.0) 0.0 else abs(A * v(m, "x0") + B * v(m, "y0") + C) / den))
        },

        Formula("ellipse_properties", "Ellipse", "Analytic Geometry",
            "Focal distance, eccentricity, latus rectum and area from the semi-axes.",
            listOf(InputSpec("a", "Semi-major a", 5.0), InputSpec("b", "Semi-minor b", 3.0)),
        ) { m ->
            val a = v(m, "a"); val b = v(m, "b")
            val c = sqrt(abs(a * a - b * b))
            listOf(out("Focal distance c", c), out("Eccentricity", if (a == 0.0) 0.0 else c / a),
                out("Latus rectum", if (a == 0.0) 0.0 else 2 * b * b / a), out("Area", PI * a * b))
        },

        Formula("hyperbola_properties", "Hyperbola", "Analytic Geometry",
            "Focal distance, eccentricity, asymptote slope and latus rectum.",
            listOf(InputSpec("a", "Transverse semi-axis a", 3.0), InputSpec("b", "Conjugate semi-axis b", 4.0)),
        ) { m ->
            val a = v(m, "a"); val b = v(m, "b")
            val c = sqrt(a * a + b * b)
            listOf(out("Focal distance c", c), out("Eccentricity", if (a == 0.0) 0.0 else c / a),
                out("Asymptote slope", if (a == 0.0) 0.0 else b / a),
                out("Latus rectum", if (a == 0.0) 0.0 else 2 * b * b / a))
        },

        Formula("parabola_properties", "Parabola", "Analytic Geometry",
            "For y\u00B2 = 4px: focus, directrix and latus rectum.",
            listOf(InputSpec("p", "p", 2.0)),
        ) { m ->
            val p = v(m, "p")
            listOf(out("Focus (p)", p), out("Directrix (x)", -p), out("Latus rectum", 4 * abs(p)))
        },

        Formula("circle_from_centre", "Circle from centre and radius", "Analytic Geometry",
            "Area, circumference and the general equation constants.",
            listOf(InputSpec("h", "Centre x", 2.0), InputSpec("k", "Centre y", -3.0), InputSpec("r", "Radius", 5.0)),
        ) { m ->
            val h = v(m, "h"); val k = v(m, "k"); val r = v(m, "r")
            listOf(out("Area", PI * r * r), out("Circumference", 2 * PI * r),
                out("Equation D", -2 * h), out("Equation E", -2 * k),
                out("Equation F", h * h + k * k - r * r))
        },

        Formula("distance_3d", "Distance in 3-D", "Analytic Geometry",
            "Between two points in space.",
            listOf(InputSpec("x1", "x1", 1.0), InputSpec("y1", "y1", 2.0), InputSpec("z1", "z1", 3.0),
                InputSpec("x2", "x2", 4.0), InputSpec("y2", "y2", 6.0), InputSpec("z2", "z2", 8.0)),
        ) { m ->
            val dx = v(m, "x2") - v(m, "x1"); val dy = v(m, "y2") - v(m, "y1"); val dz = v(m, "z2") - v(m, "z1")
            listOf(out("Distance", sqrt(dx * dx + dy * dy + dz * dz)),
                out("Midpoint x", (v(m, "x1") + v(m, "x2")) / 2),
                out("Midpoint y", (v(m, "y1") + v(m, "y2")) / 2),
                out("Midpoint z", (v(m, "z1") + v(m, "z2")) / 2))
        },

        Formula("direction_cosines", "Direction cosines", "Analytic Geometry",
            "Magnitude and the three direction cosines of a vector.",
            listOf(InputSpec("x", "x", 2.0), InputSpec("y", "y", 3.0), InputSpec("z", "z", 6.0)),
        ) { m ->
            val x = v(m, "x"); val y = v(m, "y"); val z = v(m, "z")
            val r = sqrt(x * x + y * y + z * z)
            listOf(out("Magnitude", r), out("cos \u03B1", if (r == 0.0) 0.0 else x / r),
                out("cos \u03B2", if (r == 0.0) 0.0 else y / r), out("cos \u03B3", if (r == 0.0) 0.0 else z / r))
        },

        Formula("point_plane_distance", "Distance from a point to a plane", "Analytic Geometry",
            "For the plane Ax + By + Cz + D = 0.",
            listOf(InputSpec("A", "A", 1.0), InputSpec("B", "B", 2.0), InputSpec("C", "C", 2.0),
                InputSpec("D", "D", -3.0), InputSpec("x0", "x0", 0.0), InputSpec("y0", "y0", 0.0),
                InputSpec("z0", "z0", 0.0)),
        ) { m ->
            val A = v(m, "A"); val B = v(m, "B"); val C = v(m, "C"); val D = v(m, "D")
            val den = sqrt(A * A + B * B + C * C)
            listOf(out("Distance", if (den == 0.0) 0.0 else abs(A * v(m, "x0") + B * v(m, "y0") + C * v(m, "z0") + D) / den))
        },

        // ================= VECTORS =================
        Formula("vector_dot", "Dot product", "Vectors",
            "Two 3-D vectors: dot product, magnitudes and the angle between them.",
            listOf(InputSpec("ax", "a\u2093", 1.0), InputSpec("ay", "a\u1D67", 2.0), InputSpec("az", "a\u1D69", 3.0),
                InputSpec("bx", "b\u2093", 4.0), InputSpec("by", "b\u1D67", 5.0), InputSpec("bz", "b\u1D69", 6.0)),
        ) { m ->
            val ax = v(m, "ax"); val ay = v(m, "ay"); val az = v(m, "az")
            val bx = v(m, "bx"); val by = v(m, "by"); val bz = v(m, "bz")
            val dot = ax * bx + ay * by + az * bz
            val ma = sqrt(ax * ax + ay * ay + az * az)
            val mb = sqrt(bx * bx + by * by + bz * bz)
            val cosT = if (ma * mb == 0.0) 0.0 else (dot / (ma * mb)).coerceIn(-1.0, 1.0)
            listOf(out("Dot product", dot), out("|a|", ma), out("|b|", mb),
                out("Angle", r2d(kotlin.math.acos(cosT)), "\u00B0"))
        },

        Formula("vector_cross", "Cross product", "Vectors",
            "The cross product and its magnitude.",
            listOf(InputSpec("ax", "a\u2093", 1.0), InputSpec("ay", "a\u1D67", 2.0), InputSpec("az", "a\u1D69", 3.0),
                InputSpec("bx", "b\u2093", 4.0), InputSpec("by", "b\u1D67", 5.0), InputSpec("bz", "b\u1D69", 6.0)),
        ) { m ->
            val ax = v(m, "ax"); val ay = v(m, "ay"); val az = v(m, "az")
            val bx = v(m, "bx"); val by = v(m, "by"); val bz = v(m, "bz")
            val cx = ay * bz - az * by
            val cy = az * bx - ax * bz
            val cz = ax * by - ay * bx
            listOf(out("c\u2093", cx), out("c\u1D67", cy), out("c\u1D69", cz),
                out("|c|", sqrt(cx * cx + cy * cy + cz * cz)))
        },

        Formula("vector_triple_product", "Scalar triple product", "Vectors",
            "a \u00B7 (b \u00D7 c) — the volume of the parallelepiped.",
            listOf(InputSpec("ax", "a\u2093", 1.0), InputSpec("ay", "a\u1D67", 2.0), InputSpec("az", "a\u1D69", 3.0),
                InputSpec("bx", "b\u2093", 4.0), InputSpec("by", "b\u1D67", 5.0), InputSpec("bz", "b\u1D69", 6.0),
                InputSpec("cx", "c\u2093", 7.0), InputSpec("cy", "c\u1D67", 8.0), InputSpec("cz", "c\u1D69", 9.0)),
        ) { m ->
            val ax = v(m, "ax"); val ay = v(m, "ay"); val az = v(m, "az")
            val bx = v(m, "bx"); val by = v(m, "by"); val bz = v(m, "bz")
            val cx = v(m, "cx"); val cy = v(m, "cy"); val cz = v(m, "cz")
            val t = ax * (by * cz - bz * cy) - ay * (bx * cz - bz * cx) + az * (bx * cy - by * cx)
            listOf(out("a \u00B7 (b \u00D7 c)", t), out("Volume", abs(t)))
        },

        Formula("vector_projection", "Vector projection", "Vectors",
            "Scalar and vector projection of a onto b (2-D).",
            listOf(InputSpec("ax", "a\u2093", 3.0), InputSpec("ay", "a\u1D67", 4.0),
                InputSpec("bx", "b\u2093", 1.0), InputSpec("by", "b\u1D67", 0.0)),
        ) { m ->
            val ax = v(m, "ax"); val ay = v(m, "ay")
            val bx = v(m, "bx"); val by = v(m, "by")
            val bb = bx * bx + by * by
            val scalar = if (bb == 0.0) 0.0 else (ax * bx + ay * by) / sqrt(bb)
            listOf(out("Scalar projection", scalar),
                out("Projection x", if (bb == 0.0) 0.0 else scalar * bx / sqrt(bb)),
                out("Projection y", if (bb == 0.0) 0.0 else scalar * by / sqrt(bb)))
        },

        Formula("unit_vector", "Unit vector", "Vectors",
            "Normalise a 3-D vector.",
            listOf(InputSpec("x", "x", 3.0), InputSpec("y", "y", 4.0), InputSpec("z", "z", 12.0)),
        ) { m ->
            val x = v(m, "x"); val y = v(m, "y"); val z = v(m, "z")
            val r = sqrt(x * x + y * y + z * z)
            listOf(out("Magnitude", r), out("x\u0302", if (r == 0.0) 0.0 else x / r),
                out("y\u0302", if (r == 0.0) 0.0 else y / r), out("z\u0302", if (r == 0.0) 0.0 else z / r))
        },

        // ================= NUMBER THEORY =================
        Formula("divisors_sum", "Sum of divisors", "Number Theory",
            "The divisor count and their sum.",
            listOf(InputSpec("n", "Number", 28.0)),
        ) { m ->
            val n = v(m, "n").toLong()
            if (n < 1) listOf(out("Sum of divisors", 0.0, note = "Use a positive whole number"))
            else {
                var count = 0L; var sum = 0L; var d = 1L
                while (d * d <= n) {
                    if (n % d == 0L) {
                        sum += d; count += 1
                        val other = n / d
                        if (other != d) { sum += other; count += 1 }
                    }
                    d++
                }
                listOf(out("Number of divisors", count.toDouble()), out("Sum of divisors", sum.toDouble()),
                    out("Is perfect", if (sum - n == n) 1.0 else 0.0, "", if (sum - n == n) "Yes" else "No"))
            }
        },

        Formula("euler_phi", "Euler's totient", "Number Theory",
            "How many numbers below n share no factor with n.",
            listOf(InputSpec("n", "Number", 36.0)),
        ) { m ->
            var n = v(m, "n").toLong()
            if (n < 1) listOf(out("Totient", 0.0, note = "Use a positive whole number"))
            else {
                var result = n; var p = 2L
                while (p * p <= n) {
                    if (n % p == 0L) { while (n % p == 0L) n /= p; result -= result / p }
                    p++
                }
                if (n > 1) result -= result / n
                listOf(out("Totient", result.toDouble()))
            }
        },

        Formula("modular_power", "Modular exponentiation", "Number Theory",
            "base^exp mod m, without overflowing.",
            listOf(InputSpec("base", "Base", 7.0), InputSpec("exp", "Exponent", 128.0), InputSpec("m", "Modulus", 13.0)),
        ) { m ->
            var result = 1L
            var b = v(m, "base").toLong() % v(m, "m").toLong()
            var e = v(m, "exp").toLong()
            val mod = v(m, "m").toLong()
            if (mod == 0L) listOf(out("Result", 0.0, note = "Modulus must not be zero"))
            else {
                while (e > 0) {
                    if (e and 1L == 1L) result = (result * b) % mod
                    b = (b * b) % mod
                    e = e shr 1
                }
                listOf(out("Result", result.toDouble()))
            }
        },

        Formula("digit_properties", "Digits of a number", "Number Theory",
            "Digit sum, digit count and the reversed number.",
            listOf(InputSpec("n", "Number", 12345.0)),
        ) { m ->
            val n = v(m, "n").toLong()
            val s = abs(n).toString()
            listOf(out("Digits", s.length.toDouble()),
                out("Digit sum", s.sumOf { it - '0' }.toDouble()),
                out("Reversed", s.reversed().toLongOrNull()?.toDouble() ?: 0.0),
                out("Is palindrome", if (s == s.reversed()) 1.0 else 0.0, "", if (s == s.reversed()) "Yes" else "No"))
        },

        Formula("perfect_square_cube", "Square and cube check", "Number Theory",
            "Is it a perfect square or cube, and what are the roots?",
            listOf(InputSpec("n", "Number", 144.0)),
        ) { m ->
            val n = v(m, "n")
            val r = sqrt(abs(n))
            val c = kotlin.math.cbrt(abs(n))
            listOf(out("Square root", if (n < 0) 0.0 else r),
                out("Cube root", if (n < 0) -c else c),
                out("Is perfect square", if (n >= 0 && abs(r - kotlin.math.round(r)) < 1e-9) 1.0 else 0.0,
                    "", if (n >= 0 && abs(r - kotlin.math.round(r)) < 1e-9) "Yes" else "No"))
        },

        Formula("triangular_number", "Triangular number", "Number Theory",
            "The n-th triangular number and the sum 1..n.",
            listOf(InputSpec("n", "n", 10.0)),
        ) { m ->
            val n = v(m, "n")
            listOf(out("Triangular number", n * (n + 1) / 2), out("Sum 1..n", n * (n + 1) / 2))
        },

        Formula("collatz_steps", "Collatz steps", "Number Theory",
            "How many steps to reach 1.",
            listOf(InputSpec("n", "Number", 27.0)),
        ) { m ->
            var n = v(m, "n").toLong()
            if (n < 1) listOf(out("Steps", 0.0, note = "Use a positive whole number"))
            else {
                var steps = 0L
                while (n != 1L && steps < 100000) {
                    n = if (n % 2L == 0L) n / 2 else 3 * n + 1
                    steps++
                }
                listOf(out("Steps to reach 1", steps.toDouble()))
            }
        },

        // ================= PROBABILITY =================
        Formula("probability_union", "Probability of A or B", "Probability",
            "P(A \u222A B) = P(A) + P(B) \u2212 P(A \u2229 B).",
            listOf(InputSpec("pA", "P(A)", 0.5), InputSpec("pB", "P(B)", 0.4), InputSpec("pAB", "P(A \u2229 B)", 0.2)),
        ) { m ->
            val r = (v(m, "pA") + v(m, "pB") - v(m, "pAB")).coerceIn(0.0, 1.0)
            listOf(out("P(A \u222A B)", r), out("As a percentage", r * 100.0, "%"))
        },

        Formula("probability_independent", "Both of two independent events", "Probability",
            "P(A \u2229 B) = P(A) \u00D7 P(B).",
            listOf(InputSpec("pA", "P(A)", 0.5), InputSpec("pB", "P(B)", 0.3)),
        ) { m ->
            val r = v(m, "pA") * v(m, "pB")
            listOf(out("P(A and B)", r), out("P(at least one)", (1 - (1 - v(m, "pA")) * (1 - v(m, "pB"))).coerceIn(0.0, 1.0)))
        },

        Formula("probability_conditional", "Conditional probability", "Probability",
            "P(A | B) = P(A \u2229 B) / P(B).",
            listOf(InputSpec("pAB", "P(A \u2229 B)", 0.2), InputSpec("pB", "P(B)", 0.5)),
        ) { m ->
            val pB = v(m, "pB")
            if (pB == 0.0) listOf(out("P(A | B)", 0.0, note = "P(B) must not be zero"))
            else listOf(out("P(A | B)", (v(m, "pAB") / pB).coerceIn(0.0, 1.0)))
        },

        Formula("bayes_theorem", "Bayes' theorem", "Probability",
            "P(A | B) from the prior, the likelihood and the evidence.",
            listOf(InputSpec("pA", "Prior P(A)", 0.01), InputSpec("pBGivenA", "P(B | A)", 0.9),
                InputSpec("pB", "Evidence P(B)", 0.05)),
        ) { m ->
            val pB = v(m, "pB")
            if (pB == 0.0) listOf(out("Posterior", 0.0, note = "P(B) must not be zero"))
            else listOf(out("Posterior P(A | B)", (v(m, "pA") * v(m, "pBGivenA") / pB).coerceIn(0.0, 1.0)))
        },

        Formula("probability_at_least_one", "At least one in n tries", "Probability",
            "1 \u2212 (1 \u2212 p)\u207F.",
            listOf(InputSpec("p", "Chance per try", 0.1), InputSpec("n", "Number of tries", 10.0)),
        ) { m ->
            val r = (1 - (1 - v(m, "p")).pow(v(m, "n"))).coerceIn(0.0, 1.0)
            listOf(out("P(at least one)", r), out("As a percentage", r * 100.0, "%"))
        },

        Formula("probability_complement", "Complement", "Probability",
            "1 \u2212 P(A).",
            listOf(InputSpec("p", "P(A)", 0.35)),
        ) { m ->
            val r = (1 - v(m, "p")).coerceIn(0.0, 1.0)
            listOf(out("P(not A)", r), out("Odds against", if (v(m, "p") == 0.0) 0.0 else (1 - v(m, "p")) / v(m, "p")))
        },

        Formula("odds_converter", "Odds and probability", "Probability",
            "Convert between odds and probability.",
            listOf(InputSpec("favourable", "Favourable outcomes", 3.0), InputSpec("unfavourable", "Unfavourable outcomes", 7.0)),
        ) { m ->
            val f = v(m, "favourable"); val u = v(m, "unfavourable")
            val total = f + u
            listOf(out("Odds in favour", if (u == 0.0) 0.0 else f / u),
                out("Probability", if (total == 0.0) 0.0 else f / total),
                out("Percentage", if (total == 0.0) 0.0 else f / total * 100.0, "%"))
        },

        Formula("expected_value", "Expected value (3 outcomes)", "Probability",
            "E[X] and the variance of a three-outcome distribution.",
            listOf(InputSpec("x1", "Value 1", 10.0), InputSpec("p1", "P(1)", 0.2),
                InputSpec("x2", "Value 2", 20.0), InputSpec("p2", "P(2)", 0.5),
                InputSpec("x3", "Value 3", 30.0), InputSpec("p3", "P(3)", 0.3)),
        ) { m ->
            val xs = listOf(v(m, "x1"), v(m, "x2"), v(m, "x3"))
            val ps = listOf(v(m, "p1"), v(m, "p2"), v(m, "p3"))
            val mean = xs.indices.sumOf { xs[it] * ps[it] }
            val variance = xs.indices.sumOf { ps[it] * (xs[it] - mean).pow(2.0) }
            val total = ps.sum()
            listOf(out("E[X]", mean), out("Variance", variance),
                out("Std deviation", sqrt(variance)),
                out("Probabilities sum", total, "", if (abs(total - 1.0) < 1e-9) "Correct" else "Should be 1"))
        },

        Formula("binomial_probability", "Binomial probability", "Probability",
            "P(X = k) and P(X \u2264 k) for n trials with probability p.",
            listOf(InputSpec("n", "Trials n", 10.0), InputSpec("k", "Successes k", 3.0), InputSpec("p", "Probability p", 0.5)),
        ) { m ->
            val n = v(m, "n").toInt(); val k = v(m, "k").toInt(); val p = v(m, "p")
            listOf(out("P(X = k)", Distributions.binomialPmf(k, n, p)),
                out("P(X \u2264 k)", Distributions.binomialCdf(k, n, p)),
                out("Mean np", n * p), out("Variance np(1-p)", n * p * (1 - p)))
        },

        // ================= SERIES =================
        Formula("sum_of_squares", "Sum of squares 1\u00B2+2\u00B2+\u2026+n\u00B2", "Series",
            "n(n+1)(2n+1)/6.",
            listOf(InputSpec("n", "n", 10.0)),
        ) { m ->
            val n = v(m, "n")
            listOf(out("Sum", n * (n + 1) * (2 * n + 1) / 6))
        },

        Formula("sum_of_cubes", "Sum of cubes 1\u00B3+2\u00B3+\u2026+n\u00B3", "Series",
            "(n(n+1)/2)\u00B2.",
            listOf(InputSpec("n", "n", 10.0)),
        ) { m ->
            val n = v(m, "n")
            listOf(out("Sum", (n * (n + 1) / 2).pow(2.0)))
        },

        Formula("sum_of_odds", "Sum of odd numbers", "Series",
            "1 + 3 + 5 + \u2026 + (2n\u22121) = n\u00B2.",
            listOf(InputSpec("n", "n", 10.0)),
        ) { m -> listOf(out("Sum", v(m, "n").pow(2.0))) },

        Formula("infinite_gp_sum", "Infinite geometric series", "Series",
            "a/(1\u2212r), valid when |r| < 1.",
            listOf(InputSpec("a", "First term a", 1.0), InputSpec("r", "Ratio r", 0.5)),
        ) { m ->
            val r = v(m, "r")
            if (abs(r) >= 1.0) listOf(out("Sum", 0.0, note = "The series diverges when |r| \u2265 1"))
            else listOf(out("Sum", v(m, "a") / (1 - r)))
        },

        Formula("harmonic_number", "Harmonic sum", "Series",
            "1 + 1/2 + 1/3 + \u2026 + 1/n.",
            listOf(InputSpec("n", "n", 10.0)),
        ) { m ->
            val n = v(m, "n").toInt()
            var s = 0.0
            for (i in 1..n) s += 1.0 / i
            listOf(out("Sum", s), out("ln(n) + \u03B3", if (n > 0) ln(n.toDouble()) + 0.5772156649 else 0.0))
        },

        Formula("binomial_series_term", "Binomial series term", "Series",
            "The k-th term C(n,k)\u00B7x\u1D4F of the binomial expansion.",
            listOf(InputSpec("n", "n", 5.0), InputSpec("k", "k", 2.0), InputSpec("x", "x", 1.0)),
        ) { m ->
            val n = v(m, "n"); val k = v(m, "k"); val x = v(m, "x")
            listOf(out("Term", Evaluator.combinations(n, k) * x.pow(k)),
                out("Coefficient C(n,k)", Evaluator.combinations(n, k)))
        },

        Formula("exponential_series", "e\u02E3 by series", "Series",
            "Sum of x\u1D4F/k! — check it against the built-in exp.",
            listOf(InputSpec("x", "x", 1.0), InputSpec("terms", "Terms", 12.0)),
        ) { m ->
            val x = v(m, "x"); val n = v(m, "terms").toInt()
            var s = 0.0
            for (k in 0..n) s += x.pow(k.toDouble()) / Evaluator.factorial(k.toDouble())
            listOf(out("Series sum", s), out("exp(x)", exp(x)))
        },

        Formula("sine_series", "sin x by series", "Series",
            "Sum of (\u22121)\u1D4Fx^(2k+1)/(2k+1)!.",
            listOf(InputSpec("x", "x (radians)", 0.5), InputSpec("terms", "Terms", 8.0)),
        ) { m ->
            val x = v(m, "x"); val n = v(m, "terms").toInt()
            var s = 0.0
            for (k in 0..n) s += (if (k % 2 == 0) 1.0 else -1.0) * x.pow(2.0 * k + 1) / Evaluator.factorial(2.0 * k + 1)
            listOf(out("Series sum", s), out("sin(x)", sin(x)))
        },

        Formula("cosine_series", "cos x by series", "Series",
            "Sum of (\u22121)\u1D4Fx^(2k)/(2k)!.",
            listOf(InputSpec("x", "x (radians)", 0.5), InputSpec("terms", "Terms", 8.0)),
        ) { m ->
            val x = v(m, "x"); val n = v(m, "terms").toInt()
            var s = 0.0
            for (k in 0..n) s += (if (k % 2 == 0) 1.0 else -1.0) * x.pow(2.0 * k) / Evaluator.factorial(2.0 * k)
            listOf(out("Series sum", s), out("cos(x)", cos(x)))
        },

        Formula("alternating_harmonic", "Alternating harmonic sum", "Series",
            "1 \u2212 1/2 + 1/3 \u2212 \u2026 \u00B1 1/n (tends to ln 2).",
            listOf(InputSpec("n", "n", 100.0)),
        ) { m ->
            val n = v(m, "n").toInt()
            var s = 0.0
            for (i in 1..n) s += (if (i % 2 == 1) 1.0 else -1.0) / i
            listOf(out("Sum", s), out("ln 2", ln(2.0)))
        },

        // ================= GEOMETRY (from the handbooks) =================
        Formula("equilateral_triangle", "Equilateral triangle", "Geometry",
            "Area, height, inradius and circumradius from the side.",
            listOf(InputSpec("side", "Side", 6.0)),
        ) { m ->
            val a = v(m, "side")
            listOf(out("Area", sqrt(3.0) / 4.0 * a * a), out("Height", sqrt(3.0) / 2.0 * a),
                out("Perimeter", 3 * a), out("Inradius", a * sqrt(3.0) / 6.0),
                out("Circumradius", a * sqrt(3.0) / 3.0))
        },

        Formula("isosceles_triangle", "Isosceles triangle", "Geometry",
            "Height, area and perimeter from the base and one leg.",
            listOf(InputSpec("base", "Base", 8.0), InputSpec("leg", "Equal side", 5.0)),
        ) { m ->
            val b = v(m, "base"); val a = v(m, "leg")
            val h = sqrt((a * a - b * b / 4.0).coerceAtLeast(0.0))
            listOf(out("Height", h), out("Area", b * h / 2.0), out("Perimeter", b + 2 * a),
                out("Apex angle", r2d(2 * kotlin.math.asin((b / (2 * a)).coerceIn(-1.0, 1.0))), "\u00B0"))
        },

        Formula("kite", "Kite", "Geometry",
            "Area from the two diagonals.",
            listOf(InputSpec("d1", "Diagonal 1", 10.0), InputSpec("d2", "Diagonal 2", 6.0)),
        ) { m -> listOf(out("Area", v(m, "d1") * v(m, "d2") / 2.0)) },

        Formula("cyclic_quadrilateral", "Cyclic quadrilateral", "Geometry",
            "Brahmagupta's area formula.",
            listOf(InputSpec("a", "Side a", 5.0), InputSpec("b", "Side b", 6.0),
                InputSpec("c", "Side c", 7.0), InputSpec("d", "Side d", 8.0)),
        ) { m ->
            val a = v(m, "a"); val b = v(m, "b"); val c = v(m, "c"); val d = v(m, "d")
            val sp = (a + b + c + d) / 2.0
            listOf(out("Area", sqrt(((sp - a) * (sp - b) * (sp - c) * (sp - d)).coerceAtLeast(0.0))),
                out("Perimeter", a + b + c + d), out("Semiperimeter", sp))
        },

        Formula("regular_hexagon", "Regular hexagon", "Geometry",
            "Area, perimeter and apothem from the side.",
            listOf(InputSpec("side", "Side", 5.0)),
        ) { m ->
            val a = v(m, "side")
            listOf(out("Area", 3.0 * sqrt(3.0) / 2.0 * a * a), out("Perimeter", 6 * a),
                out("Apothem", sqrt(3.0) / 2.0 * a))
        },

        Formula("rectangular_parallelepiped", "Rectangular box", "Geometry",
            "Volume, surface area and space diagonal.",
            listOf(InputSpec("l", "Length", 4.0), InputSpec("w", "Width", 3.0), InputSpec("h", "Height", 2.0)),
        ) { m ->
            val l = v(m, "l"); val w = v(m, "w"); val h = v(m, "h")
            listOf(out("Volume", l * w * h), out("Surface area", 2 * (l * w + l * h + w * h)),
                out("Space diagonal", sqrt(l * l + w * w + h * h)))
        },

        Formula("general_prism", "General prism", "Geometry",
            "Volume and surface from the base area, perimeter and height.",
            listOf(InputSpec("baseArea", "Base area", 12.0), InputSpec("perimeter", "Base perimeter", 14.0),
                InputSpec("height", "Height", 9.0)),
        ) { m ->
            val b = v(m, "baseArea"); val p = v(m, "perimeter"); val h = v(m, "height")
            listOf(out("Volume", b * h), out("Lateral surface", p * h), out("Total surface", p * h + 2 * b))
        },

        Formula("regular_tetrahedron", "Regular tetrahedron", "Geometry",
            "Volume, surface and height from the edge.",
            listOf(InputSpec("edge", "Edge", 4.0)),
        ) { m ->
            val a = v(m, "edge")
            listOf(out("Volume", a * a * a / (6.0 * sqrt(2.0))), out("Surface area", sqrt(3.0) * a * a),
                out("Height", a * sqrt(2.0 / 3.0)))
        },

        Formula("octahedron", "Regular octahedron", "Geometry",
            "Volume and surface from the edge.",
            listOf(InputSpec("edge", "Edge", 4.0)),
        ) { m ->
            val a = v(m, "edge")
            listOf(out("Volume", sqrt(2.0) / 3.0 * a * a * a), out("Surface area", 2 * sqrt(3.0) * a * a))
        },

        Formula("dodecahedron", "Regular dodecahedron", "Geometry",
            "Volume and surface from the edge.",
            listOf(InputSpec("edge", "Edge", 4.0)),
        ) { m ->
            val a = v(m, "edge")
            listOf(out("Volume", (15.0 + 7.0 * sqrt(5.0)) / 4.0 * a * a * a),
                out("Surface area", 3.0 * sqrt(25.0 + 10.0 * sqrt(5.0)) * a * a))
        },

        Formula("icosahedron", "Regular icosahedron", "Geometry",
            "Volume and surface from the edge.",
            listOf(InputSpec("edge", "Edge", 4.0)),
        ) { m ->
            val a = v(m, "edge")
            listOf(out("Volume", 5.0 * (3.0 + sqrt(5.0)) / 12.0 * a * a * a),
                out("Surface area", 5.0 * sqrt(3.0) * a * a))
        },

        Formula("spherical_cap", "Spherical cap", "Geometry",
            "Volume and surfaces of a cap of height h on a sphere of radius R.",
            listOf(InputSpec("R", "Sphere radius", 10.0), InputSpec("h", "Cap height", 4.0)),
        ) { m ->
            val r = v(m, "R"); val h = v(m, "h")
            val baseR = sqrt((2 * r * h - h * h).coerceAtLeast(0.0))
            listOf(out("Volume", PI * h * h * (3 * r - h) / 3.0), out("Curved surface", 2 * PI * r * h),
                out("Base radius", baseR), out("Total surface", 2 * PI * r * h + PI * baseR * baseR))
        },

        Formula("spherical_wedge", "Spherical wedge", "Geometry",
            "Volume and surface of a wedge of angle \u03B8.",
            listOf(InputSpec("R", "Sphere radius", 10.0), InputSpec("angle", "Angle (deg)", 60.0)),
        ) { m ->
            val r = v(m, "R"); val t = d2r(v(m, "angle"))
            listOf(out("Volume", 2.0 / 3.0 * r * r * r * t), out("Curved surface", 2 * r * r * t),
                out("Total surface", 2 * r * r * t + PI * r * r))
        },

        Formula("ellipsoid", "Ellipsoid", "Geometry",
            "Volume and approximate surface from the three semi-axes.",
            listOf(InputSpec("a", "Semi-axis a", 3.0), InputSpec("b", "Semi-axis b", 4.0), InputSpec("c", "Semi-axis c", 5.0)),
        ) { m ->
            val a = v(m, "a"); val b = v(m, "b"); val c = v(m, "c")
            val p = 1.6075
            val surf = 4 * PI * ((a.pow(p) * b.pow(p) + a.pow(p) * c.pow(p) + b.pow(p) * c.pow(p)) / 3.0).pow(1.0 / p)
            listOf(out("Volume", 4.0 / 3.0 * PI * a * b * c), out("Surface (approx)", surf))
        },

        Formula("hollow_cylinder", "Hollow cylinder", "Geometry",
            "Volume and surface of a pipe with outer radius R and inner radius r.",
            listOf(InputSpec("R", "Outer radius", 5.0), InputSpec("r", "Inner radius", 4.0), InputSpec("h", "Height", 20.0)),
        ) { m ->
            val R = v(m, "R"); val r = v(m, "r"); val h = v(m, "h")
            listOf(out("Volume", PI * (R * R - r * r) * h),
                out("Curved surface (outer+inner)", 2 * PI * (R + r) * h),
                out("Total surface", 2 * PI * (R + r) * h + 2 * PI * (R * R - r * r)))
        },

    )

    fun byCategory(category: String): List<Formula> = formulas.filter { it.category == category }
}
