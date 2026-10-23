package com.merry8989uk.universalcalculator.ui

/**
 * The words a screen reader should say for a key or a control.
 *
 * The keypad shows glyphs (`⌫`, `±`, `x²`, `√`), which a screen reader either
 * skips or reads as punctuation. Every key therefore has a spoken label, and a
 * test walks the keypad to make sure a new key can never ship without one.
 */
object Spoken {

    private val WORDS = mapOf(
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

    /** What to say for a key. Digits and letters say themselves. */
    fun key(key: String): String = WORDS[key] ?: key

    /** True when a key has a proper spoken label (digits are fine as they are). */
    fun isSpoken(key: String): Boolean =
        WORDS.containsKey(key) || key.all { it.isDigit() }
}
