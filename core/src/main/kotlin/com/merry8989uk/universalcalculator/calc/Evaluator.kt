package com.merry8989uk.universalcalculator.calc

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.PI
import kotlin.math.E
import kotlin.math.acosh
import kotlin.math.asinh
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * A small, dependency-free recursive-descent expression evaluator.
 *
 * Supports + - * / ^, parentheses, unary minus, postfix factorial (!) and a
 * proper percentage key (%).
 *
 * What `+` and `-` do with a percent is the caller's choice, see
 * [PercentageMode]:
 *   - multiplicative (the default)  200 + 10% = 220,  200 - 10% = 180
 *   - additive                      200 + 10% = 200.1, 200 - 10% = 199.9
 * `*` and `/` are the same either way: 200 * 10% = 20, and 50% = 0.5.
 * Use `mod(a, b)` for a remainder.
 */
/**
 * Rounds a half **away from zero**, which is what a calculator user means.
 *
 * `kotlin.math.round` is `Math.rint`, which rounds a half to the *even* number:
 * round(2.5) is 2, and mround(2.5, 5) collapses to 0. That is right for `rint`
 * and wrong for `round`, so the two keys had quietly become the same function.
 */
internal fun roundHalfAway(x: Double): Double =
    if (x >= 0) floor(x + 0.5) else ceil(x - 0.5)

object Evaluator {

    /**
     * Something the calculator will not work out, said in words a person can
     * read. The message is shown on screen as the answer, so it has to read like
     * a fact and not like the app falling over.
     */
    open class EvalException(message: String) : Exception(message)

    /**
     * A function that has no value at this angle: tan and sec at 90°, cot and csc
     * at 0°.
     *
     * This is a result, not a fault. Those four functions have a real hole at
     * those angles, and the honest answer is the word "undefined"; the 1.6e16 the
     * division leaves behind is an artefact of pi/2 not being exactly
     * representable as a double, not a value the function ever takes. It rides on
     * [EvalException] so every caller that already catches that keeps working,
     * and it is its own type so a screen can show the message as an answer.
     */
    class UndefinedValue : EvalException(MESSAGE) {
        companion object {
            /** The whole of what the user sees, and the same for all four. */
            const val MESSAGE = "undefined"
        }
    }

    /**
     * An answer that is exact and right, and bigger than the screen can carry.
     *
     * The counting functions work their answers out in full integers, and then
     * have nowhere to put them: an answer is shown as a double, and a double
     * runs out a little under 2^1024. Past that point there is no number to
     * show, and the honest thing is to say so — a wrapped or rounded leftover
     * would look like an answer while being a different number. It rides on
     * [EvalException] so every caller that already catches that keeps working,
     * and it is its own type so a screen can tell this apart from bad input.
     */
    class TooLargeToDisplay : EvalException(MESSAGE) {
        companion object {
            /** The whole of what the user sees: the result, refused by name. */
            const val MESSAGE = "too large to display"
        }
    }

    /**
     * Evaluates [expression].
     *
     * [degrees] switches trigonometry between degrees and radians. Pass [unit]
     * instead when gradians are wanted too; it wins over [degrees].
     */
    fun evaluate(
        expression: String,
        degrees: Boolean = false,
        vars: Map<String, Double> = emptyMap(),
        unit: AngleUnit? = null,
        percentage: PercentageMode = PercentageMode.MULTIPLICATIVE,
        implied: ImpliedMultiplication = ImpliedMultiplication.SAME_PRECEDENCE,
    ): Double {
        val parser = Parser(
            expression,
            unit ?: if (degrees) AngleUnit.DEGREE else AngleUnit.RADIAN,
            percentage,
            implied,
            vars,
        )
        val v = parser.parseExpression()
        parser.expectEnd()
        if (v.isNaN()) throw EvalException("Result is not a number")
        if (v.isInfinite()) throw EvalException("Result is infinite (overflow)")
        return v
    }

    /**
     * Every function name the evaluator resolves.
     *
     * The guard reads its allow-list from here rather than keeping one of its
     * own, so a function cannot be added to the parser and be missing from what
     * the calculator will accept. Reading it costs nothing: it is the table's
     * own set of keys.
     */
    val FUNCTION_NAMES: Set<String> get() = Parser.functionNames

    /** Every name that stands for a value rather than doing something to one. */
    val CONSTANT_NAMES: Set<String> get() = Parser.constantNames

    private class Parser(
        private val s: String,
        private val angle: AngleUnit,
        private val percentage: PercentageMode,
        private val implied: ImpliedMultiplication,
        private val vars: Map<String, Double>,
    ) {
        private var i = 0

        /** A value plus whether it was written with a trailing percent sign. */
        private data class Operand(val value: Double, val isPercent: Boolean)

        private fun peek(): Char? = if (i < s.length) s[i] else null
        private fun skipSpaces() { while (i < s.length && s[i].isWhitespace()) i++ }

        fun expectEnd() {
            skipSpaces()
            if (i < s.length) throw EvalException("Unexpected '${s[i]}' at position $i")
        }

        fun parseExpression(): Double {
            var acc = parseTerm()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '+' -> {
                        i++
                        val r = parseTerm()
                        acc = Operand(
                            if (r.isPercent) {
                                // multiplicative takes the percent of what is there
                                // already; additive treats it as a plain number
                                if (percentage == PercentageMode.MULTIPLICATIVE) {
                                    acc.value + acc.value * r.value
                                } else {
                                    acc.value + r.value
                                }
                            } else {
                                acc.value + r.value
                            },
                            false,
                        )
                    }
                    '-' -> {
                        i++
                        val r = parseTerm()
                        acc = Operand(
                            if (r.isPercent) {
                                if (percentage == PercentageMode.MULTIPLICATIVE) {
                                    acc.value - acc.value * r.value
                                } else {
                                    acc.value - r.value
                                }
                            } else {
                                acc.value - r.value
                            },
                            false,
                        )
                    }
                    else -> return acc.value
                }
            }
        }

        private fun parseTerm(): Operand {
            // When a missing sign binds tighter, the operands of * and / are read
            // one level down, so 1/2pi becomes 1/(2pi) rather than (1/2)pi.
            var acc = if (implied == ImpliedMultiplication.HIGHER_PRECEDENCE) parseImplicit() else parseUnary()
            while (true) {
                skipSpaces()
                when (peek()) {
                    '*' -> {
                        i++
                        val r = if (implied == ImpliedMultiplication.HIGHER_PRECEDENCE) parseImplicit() else parseUnary()
                        acc = Operand(acc.value * r.value, false)
                    }
                    '/' -> {
                        i++
                        val r = if (implied == ImpliedMultiplication.HIGHER_PRECEDENCE) parseImplicit() else parseUnary()
                        if (r.value == 0.0) throw EvalException("Division by zero")
                        acc = Operand(acc.value / r.value, false)
                    }
                    // Implicit multiplication: 2pi, 3(4+5), (2)(3)
                    null -> return acc
                    else -> {
                        if (peek()!!.isLetter() || peek() == '(') {
                            if (implied == ImpliedMultiplication.HIGHER_PRECEDENCE) return acc
                            val r = parseUnary()
                            acc = Operand(acc.value * r.value, false)
                        } else return acc
                    }
                }
            }
        }

        /**
         * Multiplication that was not written down: `2pi`, `3(4+5)`, `(2)(3)`.
         *
         * Only reached when the setting says a missing sign binds tighter than an
         * explicit one, in which case this sits between * / and unary minus.
         */
        private fun parseImplicit(): Operand {
            var acc = parseUnary()
            while (true) {
                skipSpaces()
                val c = peek() ?: return acc
                if (c.isLetter() || c == '(') {
                    val r = parseUnary()
                    acc = Operand(acc.value * r.value, false)
                } else {
                    return acc
                }
            }
        }

        private fun parseUnary(): Operand {
            skipSpaces()
            when (peek()) {
                '-' -> { i++; val v = parseUnary(); return Operand(-v.value, v.isPercent) }
                '+' -> { i++; return parseUnary() }
            }
            return parsePower()
        }

        private fun parsePostfix(): Operand {
            var v = parseAtom()
            var percent = false
            skipSpaces()
            while (peek() == '%') {
                i++
                percent = true
                skipSpaces()
            }
            while (peek() == '!') {
                i++
                v = factorial(v)
                skipSpaces()
            }
            // A degree sign marks the number as an angle: in DEG mode it is left
            // alone (trig already reads degrees), in RAD mode it becomes radians.
            while (peek() == '\u00B0') {
                i++
                v = if (angle == AngleUnit.DEGREE) v * PI / 180.0 else angle.toRadians(v)
                skipSpaces()
            }
            return Operand(if (percent) v / 100.0 else v, percent)
        }

        private fun parsePower(): Operand {
            val base = parsePostfix()
            skipSpaces()
            if (peek() == '^') {
                i++
                val exponent = parseUnary()
                return Operand(base.value.pow(exponent.value), false)
            }
            return base
        }

        private fun parseAtom(): Double {
            skipSpaces()
            val c = peek() ?: throw EvalException("Unexpected end of expression")
            if (c == '(') {
                i++
                val v = parseExpression()
                skipSpaces()
                if (peek() != ')') throw EvalException("Missing ')'")
                i++
                return v
            }
            if (c.isDigit() || c == '.') return parseNumber()
            if (c.isLetter()) return parseIdentifier()
            throw EvalException("Unexpected '$c' at position $i")
        }

        private fun parseNumber(): Double {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
                val save = i
                i++
                if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
                if (i < s.length && s[i].isDigit()) {
                    while (i < s.length && s[i].isDigit()) i++
                } else {
                    i = save
                }
            }
            val text = s.substring(start, i)
            return text.toDoubleOrNull() ?: throw EvalException("Invalid number '$text'")
        }

        private fun parseIdentifier(): Double {
            val start = i
            while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_')) i++
            val name = s.substring(start, i).lowercase()
            skipSpaces()
            if (peek() == '(') {
                i++
                val args = mutableListOf<Double>()
                skipSpaces()
                if (peek() != ')') {
                    args.add(parseExpression())
                    skipSpaces()
                    while (peek() == ',') {
                        i++
                        args.add(parseExpression())
                        skipSpaces()
                    }
                }
                if (peek() != ')') throw EvalException("Missing ')' after $name")
                i++
                return applyFunction(name, args)
            }
            // The constants are read from the table, so the guard can ask the
            // same table what a bare word means instead of listing it again.
            CONSTANTS[name]?.let { return it }
            // `ans` is whatever the caller last worked out, so it is not a
            // constant of the calculator: it lives in the variables.
            if (name == "ans") return vars["ans"] ?: 0.0
            return vars[name]
                // Identifiers are folded to lower case above, so a caller
                // that stored its variables as "R" would otherwise be told
                // there is no such name. Match without regard to case.
                ?: vars.entries.firstOrNull { it.key.lowercase() == name }?.value
                ?: throw EvalException("Unknown name '$name'")
        }

        /**
         * Applies [name] to [a], or says there is no such function.
         *
         * The lookup is the whole of the dispatch: a name is a function because it
         * is a row of [FUNCTIONS], and there is nowhere else for one to be listed.
         */
        private fun applyFunction(name: String, a: List<Double>): Double =
            FUNCTIONS[name]?.invoke(angle, a) ?: throw EvalException("Unknown function '$name'")

        /**
         * The two tables the parser reads by name: what a name does, and what it
         * stands for.
         *
         * They are the single source of truth for those names. The guard that
         * reads an expression before it is evaluated asks these tables what the
         * calculator knows rather than keeping a list of its own, because a
         * second list is a list that drifts: it once refused thirty-four
         * functions the evaluator could work out, and a refused sum is a sum the
         * user cannot get.
         *
         * Adding a function is one row here and nothing else.
         */
        companion object {

            /** The functions, by name, each reading its arguments the way it wants. */
            /**
             * How many quarter turns [value] is, in the unit in force, or null
             * when it is not a whole number of them.
             *
             * Quarter turns are what the poles are counted in: cosine is zero on
             * the odd ones and sine on the even ones, and that is true in
             * degrees, gradians and radians alike, so one count serves all three.
             * Past a billion quarter turns a double no longer has the precision
             * to say whether the angle landed on one, so nothing is claimed.
             */
            private fun quarterTurns(value: Double, unit: AngleUnit): Long? {
                val turns = value / (unit.perTurn / 4.0)
                if (!turns.isFinite() || abs(turns) > 1e9) return null
                if (turns != floor(turns)) return null
                return turns.toLong()
            }

            /** True on a zero of cosine: 90°, 270°, -90°, 450°, pi/2 ... */
            private fun onCosineZero(value: Double, unit: AngleUnit): Boolean =
                quarterTurns(value, unit)?.let { it % 2L != 0L } == true

            /** True on a zero of sine: 0°, 180°, 360°, -180°, pi ... */
            private fun onSineZero(value: Double, unit: AngleUnit): Boolean =
                quarterTurns(value, unit)?.let { it % 2L == 0L } == true

            private val FUNCTIONS: Map<String, (AngleUnit, List<Double>) -> Double> = mapOf(
                // ---- trigonometric ----
                "sin" to { angle, a -> sin(angle.toRadians(one(a, "sin"))) },
                "cos" to { angle, a -> cos(angle.toRadians(one(a, "cos"))) },
                // The pole is checked on the angle as it was written, not on the
                // result: by the time 90° is a double its cosine is 6e-17 rather
                // than 0, and the division then answers 1.6e16 instead of
                // admitting there is no answer.
                "tan" to { angle, a ->
                    val x = one(a, "tan")
                    if (onCosineZero(x, angle)) throw UndefinedValue()
                    tan(angle.toRadians(x))
                },
                "csc" to { angle, a ->
                    val x = one(a, "csc")
                    if (onSineZero(x, angle)) throw UndefinedValue()
                    1.0 / sin(angle.toRadians(x))
                },
                "sec" to { angle, a ->
                    val x = one(a, "sec")
                    if (onCosineZero(x, angle)) throw UndefinedValue()
                    1.0 / cos(angle.toRadians(x))
                },
                "cot" to { angle, a ->
                    val x = one(a, "cot")
                    if (onSineZero(x, angle)) throw UndefinedValue()
                    1.0 / tan(angle.toRadians(x))
                },
                "sinc" to { _, a ->
                    val x = one(a, "sinc")
                    if (x == 0.0) 1.0 else sin(x) / x
                },

                // ---- inverse trigonometric ----
                "asin" to { angle, a -> angle.fromRadians(asin(one(a, "asin"))) },
                "acos" to { angle, a -> angle.fromRadians(acos(one(a, "acos"))) },
                "atan" to { angle, a -> angle.fromRadians(atan(one(a, "atan"))) },
                "acot" to { angle, a ->
                    val x = one(a, "acot")
                    val r = atan(1.0 / x)
                    angle.fromRadians(if (x < 0) r + PI else r)
                },
                "asec" to { angle, a -> angle.fromRadians(acos(1.0 / one(a, "asec"))) },
                "acsc" to { angle, a -> angle.fromRadians(asin(1.0 / one(a, "acsc"))) },
                "atan2" to { angle, a ->
                    val (y, x) = two(a, "atan2")
                    angle.fromRadians(atan2(y, x))
                },

                // ---- hyperbolic ----
                "sinh" to { _, a -> sinh(one(a, "sinh")) },
                "cosh" to { _, a -> cosh(one(a, "cosh")) },
                "tanh" to { _, a -> tanh(one(a, "tanh")) },
                "coth" to { _, a -> 1.0 / tanh(one(a, "coth")) },
                "sech" to { _, a -> 1.0 / cosh(one(a, "sech")) },
                "csch" to { _, a -> 1.0 / sinh(one(a, "csch")) },

                // ---- inverse hyperbolic ----
                "asinh" to { _, a -> asinh(one(a, "asinh")) },
                "acosh" to { _, a -> acosh(one(a, "acosh")) },
                "atanh" to { _, a -> atanh(one(a, "atanh")) },
                "acoth" to { _, a -> 0.5 * ln((one(a, "acoth") + 1.0) / (one(a, "acoth") - 1.0)) },
                "asech" to { _, a -> acosh(1.0 / one(a, "asech")) },
                "acsch" to { _, a -> asinh(1.0 / one(a, "acsch")) },

                // ---- exponential and logarithmic ----
                "ln" to { _, a -> ln(one(a, "ln")) },
                "log" to { _, a -> if (a.size == 2) log(a[0], a[1]) else log10(one(a, "log")) },
                "log10" to { _, a -> log10(one(a, "log10")) },
                "log2" to { _, a -> log2(one(a, "log2")) },
                "log1p" to { _, a -> ln(1.0 + one(a, "log1p")) },
                "exp" to { _, a -> exp(one(a, "exp")) },
                "exp2" to { _, a -> 2.0.pow(one(a, "exp2")) },
                "expm1" to { _, a -> exp(one(a, "expm1")) - 1.0 },

                // ---- powers and roots ----
                "sqrt" to { _, a -> sqrt(one(a, "sqrt")) },
                "cbrt" to { _, a -> cbrt(one(a, "cbrt")) },
                "square" to { _, a -> one(a, "square").pow(2.0) },
                "cube" to { _, a -> one(a, "cube").pow(3.0) },
                "nthroot" to { _, a ->
                    val (x, n) = two(a, "nthroot")
                    if (n == 0.0) throw EvalException("nthroot() needs a non-zero root")
                    if (x < 0 && abs(n % 2.0) == 1.0) -(-x).pow(1.0 / n)
                    else if (x < 0) throw EvalException("Even root of a negative number")
                    else x.pow(1.0 / n)
                },
                "hypot" to { _, a ->
                    val (x, y) = two(a, "hypot")
                    hypot(x, y)
                },
                "pow" to { _, a ->
                    val (x, y) = two(a, "pow")
                    x.pow(y)
                },

                // ---- rounding and number handling ----
                "abs" to { _, a -> abs(one(a, "abs")) },
                "floor" to { _, a -> floor(one(a, "floor")) },
                "ceil" to { _, a -> ceil(one(a, "ceil")) },
                "round" to { _, a -> roundHalfAway(one(a, "round")) },
                "trunc" to { _, a ->
                    val x = one(a, "trunc")
                    if (x >= 0) floor(x) else ceil(x)
                },
                "frac" to { _, a ->
                    val x = one(a, "frac")
                    x - (if (x >= 0) floor(x) else ceil(x))
                },
                "sign" to { _, a -> when {
                    one(a, "sign") > 0 -> 1.0
                    one(a, "sign") < 0 -> -1.0
                    else -> 0.0
                } },
                "rint" to { _, a -> Math.rint(one(a, "rint")) },
                "clamp" to { _, a ->
                    if (a.size < 3) throw EvalException("clamp() needs three arguments")
                    a[0].coerceIn(a[1], a[2])
                },
                "mround" to { _, a ->
                    val (x, m) = two(a, "mround")
                    if (m == 0.0) throw EvalException("mround() needs a non-zero multiple")
                    roundHalfAway(x / m) * m
                },
                "even" to { _, a -> if (one(a, "even") == 0.0) 0.0 else
                    (if (one(a, "even") > 0) 1.0 else -1.0) * 2.0 * ceil(abs(one(a, "even")) / 2.0)
                },
                "odd" to { _, a -> (if (one(a, "odd") < 0) -1.0 else 1.0) *
                    (2.0 * ceil((abs(one(a, "odd")) + 1.0) / 2.0) - 1.0)
                },

                // ---- number theory ----
                "fact" to { _, a -> factorial(one(a, "fact")) },
                "ncr" to { _, a ->
                    val (n, r) = two(a, "ncr")
                    Evaluator.combinations(n, r)
                },
                "npr" to { _, a ->
                    val (n, r) = two(a, "npr")
                    Evaluator.permutations(n, r)
                },
                "gcd" to { _, a ->
                    val (x, y) = two(a, "gcd")
                    gcd(x.toLong(), y.toLong()).toDouble()
                },
                "lcm" to { _, a ->
                    val (x, y) = two(a, "lcm")
                    lcm(x.toLong(), y.toLong()).toDouble()
                },
                "mod" to { _, a ->
                    val (x, y) = two(a, "mod")
                    if (y == 0.0) throw EvalException("Modulo by zero")
                    x % y
                },
                "isprime" to { _, a -> if (isPrime(one(a, "isprime").toLong())) 1.0 else 0.0 },
                "nextprime" to { _, a -> nextPrime(one(a, "nextprime").toLong()).toDouble() },
                "totient" to { _, a -> totient(one(a, "totient").toLong()).toDouble() },
                "isqrt" to { _, a -> floor(sqrt(one(a, "isqrt"))) },
                "fib" to { _, a -> Evaluator.toDisplay(Evaluator.fibonacciExact(one(a, "fib").toInt())) },
                "catalan" to { _, a -> Evaluator.toDisplay(Evaluator.catalanExact(one(a, "catalan").toInt())) },

                // ---- statistics over the arguments ----
                "sum" to { _, a -> a.sum() },
                "prod" to { _, a -> a.fold(1.0) { acc, v -> acc * v } },
                "count" to { _, a -> a.size.toDouble() },
                "mean" to { _, a -> if (a.isEmpty()) 0.0 else a.sum() / a.size },
                "min" to { _, a -> a.minOrNull() ?: 0.0 },
                "max" to { _, a -> a.maxOrNull() ?: 0.0 },
                "range" to { _, a -> if (a.isEmpty()) 0.0 else (a.maxOrNull() ?: 0.0) - (a.minOrNull() ?: 0.0) },
                "median" to { _, a -> medianOf(a) },
                "mode" to { _, a -> modeOf(a) },
                "var" to { _, a -> varianceOf(a) },
                "sd" to { _, a -> sqrt(varianceOf(a)) },
                "geomean" to { _, a ->
                    if (a.isEmpty() || a.any { it <= 0 }) {
                        throw EvalException("geomean() needs positive values")
                    }
                    exp(a.sumOf { ln(it) } / a.size)
                },
                "harmmean" to { _, a ->
                    if (a.isEmpty() || a.any { it == 0.0 }) {
                        throw EvalException("harmmean() needs non-zero values")
                    }
                    a.size / a.sumOf { 1.0 / it }
                },

                // ---- special functions ----
                "gamma" to { _, a -> SpecialFunctions.gamma(one(a, "gamma")) },
                "lgamma" to { _, a -> SpecialFunctions.logGamma(one(a, "lgamma")) },
                "beta" to { _, a ->
                    val (x, y) = two(a, "beta")
                    SpecialFunctions.beta(x, y)
                },
                "erf" to { _, a -> SpecialFunctions.erf(one(a, "erf")) },
                "erfc" to { _, a -> SpecialFunctions.erfc(one(a, "erfc")) },
                "zeta" to { _, a -> SpecialFunctions.zeta(one(a, "zeta")) },
                "lambertw" to { _, a -> SpecialFunctions.lambertW(one(a, "lambertw")) },
                "digamma" to { _, a -> SpecialFunctions.digamma(one(a, "digamma")) },

                // ---- complex, for real inputs ----
                "real" to { _, a -> one(a, "real") },
                "imag" to { _, _ -> 0.0 },
                "conj" to { _, a -> one(a, "conj") },
                "arg" to { _, a -> if (one(a, "arg") >= 0) 0.0 else PI },

                // ---- misc ----
                "rand" to { _, _ -> Math.random() },
                "heaviside" to { _, a -> when {
                    one(a, "heaviside") < 0 -> 0.0
                    one(a, "heaviside") > 0 -> 1.0
                    else -> 0.5
                } },
                "radians" to { _, a -> Math.toRadians(one(a, "radians")) },
                "degrees" to { _, a -> Math.toDegrees(one(a, "degrees")) },
                "deg2rad" to { _, a -> Math.toRadians(one(a, "deg2rad")) },
                "rad2deg" to { _, a -> Math.toDegrees(one(a, "rad2deg")) },
                "pct" to { _, a -> one(a, "pct") / 100.0 },
            )

            /**
             * The bare words that stand for a number, by name.
             *
             * `ans` is not one of them: it is whatever the caller last worked out,
             * so it is read from the variables rather than from a constant.
             */
            private val CONSTANTS: Map<String, Double> = mapOf(
                "pi" to PI,
                "e" to E,
                "tau" to 2 * PI,
                "phi" to (1 + sqrt(5.0)) / 2,
            )

            /** Every function name the evaluator resolves. */
            val functionNames: Set<String> get() = FUNCTIONS.keys

            /** Every name that stands for a value, `ans` included. */
            val constantNames: Set<String> get() = CONSTANTS.keys + "ans"

            /** The single argument of a function of one. */
            private fun one(a: List<Double>, name: String): Double =
                a.firstOrNull() ?: throw EvalException("$name() needs an argument")

            /** The first two arguments of a function of two. */
            private fun two(a: List<Double>, name: String): Pair<Double, Double> =
                if (a.size >= 2) a[0] to a[1] else throw EvalException("$name() needs two arguments")

            // ---- helpers the function table leans on ----

            private fun medianOf(values: List<Double>): Double {
                if (values.isEmpty()) return 0.0
                val sorted = values.sorted()
                val mid = sorted.size / 2
                return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
            }

            private fun modeOf(values: List<Double>): Double {
                if (values.isEmpty()) return 0.0
                val counts = HashMap<Double, Int>()
                values.forEach { counts[it] = (counts[it] ?: 0) + 1 }
                // the most frequent, and the smallest of those if there is a tie
                return counts.entries
                    .sortedWith(compareByDescending<Map.Entry<Double, Int>> { it.value }.thenBy { it.key })
                    .first()
                    .key
            }

            /** Population variance. */
            private fun varianceOf(values: List<Double>): Double {
                if (values.size < 1) return 0.0
                val mean = values.sum() / values.size
                return values.sumOf { (it - mean) * (it - mean) } / values.size
            }

            private fun isPrime(n: Long): Boolean {
                if (n < 2) return false
                if (n % 2 == 0L) return n == 2L
                if (n % 3 == 0L) return n == 3L
                var d = 5L
                while (d * d <= n) {
                    if (n % d == 0L || n % (d + 2) == 0L) return false
                    d += 6
                }
                return true
            }

            private fun nextPrime(after: Long): Long {
                var n = if (after < 2) 2 else after + 1
                while (!isPrime(n)) n++
                return n
            }

            private fun totient(n: Long): Long {
                if (n < 1) return 0
                var result = n
                var remaining = n
                var p = 2L
                while (p * p <= remaining) {
                    if (remaining % p == 0L) {
                        while (remaining % p == 0L) remaining /= p
                        result -= result / p
                    }
                    p++
                }
                if (remaining > 1) result -= result / remaining
                return result
            }
        }
    }

    /**
     * The largest number an answer can carry.
     *
     * Everything an expression produces is a double, so the answer field tops
     * out a little under 2^1024 — about 1.8e308, three hundred and nine digits.
     * The counting functions below work their answers out exactly and hand them
     * over here, and this is the ceiling they are handed over against: past it
     * there is no number the display can show.
     */
    private val DISPLAY_CEILING: BigInteger = BigInteger.ONE.shiftLeft(1024)

    /**
     * An exact count as the number the display can show.
     *
     * Below the ceiling this is the nearest double, which is as much as the
     * display was ever going to print anyway. Above it the answer is refused by
     * name: a double would come back as infinity, and anything else would be a
     * wrapped or rounded number wearing the shape of an answer.
     */
    fun toDisplay(v: BigInteger): Double {
        val d = v.toDouble()
        if (d.isInfinite()) throw TooLargeToDisplay()
        return d
    }

    /**
     * A counting argument as the whole number it is.
     *
     * A hair off a whole number is forgiven, because an argument that arrived
     * through arithmetic can be 4.999999999999999 when the user meant 5. A count
     * of two and a half of something is refused, because there is no such count.
     * The conversion is done from the double's exact value rather than through a
     * long, so an argument too big for a long stays the number it is instead of
     * saturating to the largest one.
     */
    private fun whole(v: Double, name: String): BigInteger {
        if (!v.isFinite() || abs(v - round(v)) > 1e-9) {
            throw EvalException("$name needs whole numbers")
        }
        return BigDecimal(round(v)).toBigIntegerExact()
    }

    fun factorial(x: Double): Double {
        if (!x.isFinite() || x < 0 || x != floor(x)) {
            throw EvalException("Factorial needs a non-negative whole number")
        }
        return toDisplay(factorialExact(BigDecimal(x).toBigIntegerExact()))
    }

    /**
     * n! as an exact integer.
     *
     * The product is abandoned the moment it passes the display ceiling: the
     * ceiling is passed for good at 171!, so a request for the factorial of a
     * very large number is refused in 171 multiplications rather than in as many
     * as were asked for.
     */
    fun factorialExact(n: BigInteger): BigInteger {
        if (n.signum() < 0) throw EvalException("Factorial needs a non-negative whole number")
        var result = BigInteger.ONE
        var k = BigInteger.valueOf(2L)
        while (k <= n) {
            result *= k
            if (result.bitLength() > DISPLAY_CEILING.bitLength()) throw TooLargeToDisplay()
            k += BigInteger.ONE
        }
        return result
    }

    /**
     * nCr: how many ways there are to choose [r] of [n] things, exactly.
     *
     * Worked out by multiplying and dividing as it goes instead of through
     * factorials. The factorial form needed three factorials and their quotient,
     * so nCr(171, 1) was refused as "Factorial is too large" although the answer
     * is 171 — the refusal was about the working, not about the result. Choosing
     * the smaller of r and n−r to count with also means the short answer is the
     * cheap one: nCr(1000, 999) is one multiplication.
     */
    fun combinationsExact(n: BigInteger, r: BigInteger): BigInteger {
        if (n.signum() < 0 || r.signum() < 0 || r > n) {
            throw EvalException("nCr needs 0 <= r <= n")
        }
        val k = r.min(n - r)
        var result = BigInteger.ONE
        var i = BigInteger.ONE
        while (i <= k) {
            result = result * (n - k + i) / i
            if (result.bitLength() > DISPLAY_CEILING.bitLength()) throw TooLargeToDisplay()
            i += BigInteger.ONE
        }
        return result
    }

    fun combinations(n: Double, r: Double): Double {
        if (r < 0 || n < 0 || r > n) throw EvalException("nCr needs 0 <= r <= n")
        return toDisplay(combinationsExact(whole(n, "nCr"), whole(r, "nCr")))
    }

    /**
     * nPr: how many ways there are to arrange [r] of [n] things, exactly.
     *
     * Same rule as [combinationsExact]: the running product is abandoned once it
     * is past what the display can carry, which for permutations happens within
     * a couple of dozen multiplications however large r was.
     */
    fun permutationsExact(n: BigInteger, r: BigInteger): BigInteger {
        if (n.signum() < 0 || r.signum() < 0 || r > n) {
            throw EvalException("nPr needs 0 <= r <= n")
        }
        var result = BigInteger.ONE
        var i = BigInteger.ZERO
        while (i < r) {
            result *= n - i
            if (result.bitLength() > DISPLAY_CEILING.bitLength()) throw TooLargeToDisplay()
            i += BigInteger.ONE
        }
        return result
    }

    fun permutations(n: Double, r: Double): Double {
        if (r < 0 || n < 0 || r > n) throw EvalException("nPr needs 0 <= r <= n")
        return toDisplay(permutationsExact(whole(n, "nPr"), whole(r, "nPr")))
    }

    /**
     * The n-th Fibonacci number, exactly.
     *
     * Counted in a word, this wrapped: fib(93) is 12200160415121876738, one past
     * the largest signed 64-bit number, so it came back as −6.2e18 — a negative
     * count of rabbits. The walk is abandoned past the display ceiling, which
     * fib(1477) is the first to pass, so asking for fib of a billion answers in
     * about fifteen hundred steps instead of a billion.
     */
    fun fibonacciExact(n: Int): BigInteger {
        if (n <= 0) return BigInteger.ZERO
        var a = BigInteger.ZERO
        var b = BigInteger.ONE
        repeat(n - 1) {
            val next = a + b
            if (next.bitLength() > DISPLAY_CEILING.bitLength()) throw TooLargeToDisplay()
            a = b
            b = next
        }
        return b
    }

    /**
     * The n-th Catalan number, exactly.
     *
     * It was counted in a long like the Fibonacci numbers were, and went wrong
     * earlier than the answer alone suggests: the running product reaches (n+1)
     * times the answer before the last division, so catalan(33) came back as
     * 8.07e15 when the answer is 2.12e17.
     */
    fun catalanExact(n: Int): BigInteger {
        if (n < 0) return BigInteger.ZERO
        var result = BigInteger.ONE
        for (i in 0 until n) {
            result = result * BigInteger.valueOf(2L * n - i) / BigInteger.valueOf(i + 1L)
            if (result.bitLength() > DISPLAY_CEILING.bitLength()) throw TooLargeToDisplay()
        }
        return result / BigInteger.valueOf(n + 1L)
    }

    fun gcd(a: Long, b: Long): Long {
        var x = abs(a); var y = abs(b)
        while (y != 0L) { val t = x % y; x = y; y = t }
        return x
    }

    fun lcm(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        return abs(a / gcd(a, b) * b)
    }
}
