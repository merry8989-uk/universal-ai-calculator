package com.merry8989uk.universalcalculator.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.merry8989uk.universalcalculator.R

/**
 * The words a screen reader should say for a key or a control, in the language
 * the user picked. Falls back to the English word for anything unlabelled.
 *
 * The keypad shows glyphs (`⌫`, `±`, `x²`, `√`), which a screen reader either
 * skips or reads as punctuation. Every key therefore has a spoken label, and a
 * test walks the keypad to make sure a new key can never ship without one.
 */
object Spoken {

    /** The English wording, and the source of truth for which keys are labelled. */
    internal val WORDS = mapOf(
        // Digits and the basic pad
        "." to "decimal point",
        "00" to "double zero",
        "C" to "clear",
        "del" to "delete",
        "neg" to "plus or minus",
        "ans" to "previous answer",
        "%" to "percent",
        "/" to "divide",
        "*" to "multiply",
        "-" to "minus",
        "+" to "plus",
        "^" to "to the power of",
        "(" to "open bracket",
        ")" to "close bracket",
        "=" to "equals",

        // Scientific
        "sin" to "sine",
        "cos" to "cosine",
        "tan" to "tangent",
        "asin" to "arc sine",
        "acos" to "arc cosine",
        "atan" to "arc tangent",
        "sinh" to "hyperbolic sine",
        "cosh" to "hyperbolic cosine",
        "tanh" to "hyperbolic tangent",
        "ln" to "natural logarithm",
        "log" to "logarithm base ten",
        "sqrt" to "square root",
        "cbrt" to "cube root",
        "x^2" to "x squared",
        "x^y" to "x to the power of y",
        "pi" to "pi",
        "e" to "e",
        "!" to "factorial",
        "1/x" to "one over x",
        "abs" to "absolute value",
        "nCr" to "combinations",
        "nPr" to "permutations",
        "round" to "round",
        "deg" to "degrees",
        "i" to "imaginary unit",
        "x" to "x",
        "swap" to "swap the two sides",

        // Memory row
        "MC" to "memory clear",
        "M+" to "memory add",
        "M\u2212" to "memory subtract",
        "MR" to "memory recall",
    )

    /** Resource ids for the spoken key labels, so TalkBack speaks Hindi too. */
    val SPOKEN_RES: Map<String, Int> = mapOf(
        "." to R.string.key_decimal_point,
        "00" to R.string.key_double_zero,
        "C" to R.string.key_clear,
        "del" to R.string.key_delete,
        "neg" to R.string.key_plus_minus,
        "ans" to R.string.key_previous_answer,
        "%" to R.string.key_percent,
        "/" to R.string.key_divide,
        "*" to R.string.key_multiply,
        "-" to R.string.key_minus,
        "+" to R.string.key_plus,
        "^" to R.string.key_power,
        "(" to R.string.key_open_bracket,
        ")" to R.string.key_close_bracket,
        "=" to R.string.key_equals,
        "sin" to R.string.key_sin,
        "cos" to R.string.key_cos,
        "tan" to R.string.key_tan,
        "asin" to R.string.key_asin,
        "acos" to R.string.key_acos,
        "atan" to R.string.key_atan,
        "sinh" to R.string.key_sinh,
        "cosh" to R.string.key_cosh,
        "tanh" to R.string.key_tanh,
        "ln" to R.string.key_ln,
        "log" to R.string.key_log,
        "sqrt" to R.string.key_sqrt,
        "cbrt" to R.string.key_cbrt,
        "x^2" to R.string.key_x_squared,
        "x^y" to R.string.key_x_pow_y,
        "pi" to R.string.key_pi,
        "e" to R.string.key_e,
        "!" to R.string.key_factorial,
        "1/x" to R.string.key_one_over_x,
        "abs" to R.string.key_abs,
        "nCr" to R.string.key_ncr,
        "nPr" to R.string.key_npr,
        "round" to R.string.key_round,
        "deg" to R.string.key_deg,
        "i" to R.string.key_i,
        "x" to R.string.key_x,
        "swap" to R.string.key_swap,
        "MC" to R.string.key_memory_clear,
        "M+" to R.string.key_memory_add,
        "M−" to R.string.key_memory_subtract,
        "MR" to R.string.key_memory_recall,
    )

    /** What to say for a key. Digits and letters say themselves. */
    fun key(key: String): String = WORDS[key] ?: key

    /** The English word for a key, used as the fallback. */
    fun english(key: String): String = key(key)

    /** True when a key has a proper spoken label (digits are fine as they are). */
    fun isSpoken(key: String): Boolean =
        WORDS.containsKey(key) || key.all { it.isDigit() }
}

/** What a screen reader says for a key, in the user's own language. */
@Composable
fun spokenKey(key: String): String {
    val id = Spoken.SPOKEN_RES[key]
    return if (id != null) stringResource(id) else Spoken.key(key)
}
