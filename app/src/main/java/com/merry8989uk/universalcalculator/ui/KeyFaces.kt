package com.merry8989uk.universalcalculator.ui

/**
 * What a key shows on its face.
 *
 * The drawer used to print the key's own id, so the harmonic mean was a key
 * reading `harmmean` and a person had to know the evaluator's spelling before
 * they could find the function they wanted. The words were already written
 * down: [Spoken] says every key out loud for TalkBack, so the vocabulary
 * existed and the printed pad simply was not using it.
 *
 * A face is therefore that same word. One vocabulary read from one place means
 * the printed key and the spoken key cannot drift apart, and a key added later
 * is named once. [word] is what the drawer asks for; [face] is what a pad that
 * also has printed signs asks for.
 *
 * A key that is a sign keeps its sign. `√`, `x²` and `π` are what a calculator
 * prints and what a person looks for at a glance; "square root" belongs in the
 * screen reader, not on a 40dp key. Only the keys that were showing the
 * evaluator's name for something get a word instead.
 */
internal object KeyFaces {

    /**
     * Keys whose face is a printed sign rather than a word.
     *
     * These are the glyphs the pad has always drawn. They stay here so the
     * drawer and the number pad draw the same key the same way, and so a face
     * that is a sign is a decision written down rather than a leftover.
     */
    private val SIGNS = mapOf(
        "/" to "\u00F7",
        "*" to "\u00D7",
        "-" to "\u2212",
        "neg" to "\u00B1",
        "del" to "\u232B",
        "x^2" to "x\u00B2",
        "x^3" to "x\u00B3",
        "x^y" to "x\u02B8",
        "sqrt" to "\u221A",
        "cbrt" to "\u221B",
        "pi" to "\u03C0",
        "deg" to "\u00B0",
        "swap" to "\u21C4",
        "x" to "x",
        "AC" to "AC",
        "CE" to "CE",
        "GT" to "GT",
        "MU" to "MU",
        "M-" to "M\u2212",
    )

    /**
     * Ids that are the evaluator's name for something [Spoken] words differently.
     *
     * The classic pad spells two of its keys its own way — `harmean` for the
     * harmonic mean, `golden` for the golden ratio — and `variance` where the
     * evaluator says `var`. A face follows the word, not the spelling, so these
     * are pointed at the entry that already exists rather than given a second
     * wording of their own.
     */
    private val ALIASES = mapOf(
        "harmean" to "harmmean",
        "golden" to "phi",
        "variance" to "var",
    )

    /**
     * What [key] shows on its face.
     *
     * The divide and multiply signs are the two the user gets a say in, so they
     * are passed in rather than fixed here.
     */
    fun face(
        key: String,
        divisionSign: String = "\u00F7",
        multiplicationSign: String = "\u00D7",
    ): String = when (key) {
        "/" -> divisionSign
        "*" -> multiplicationSign
        else -> SIGNS[key] ?: word(key)
    }

    /**
     * The human word for [key], with no signs at all.
     *
     * Falls back to the key itself only when nothing has named it yet, which is
     * the one case where an identifier is still the honest thing to print.
     */
    fun word(key: String): String {
        val id = ALIASES[key] ?: key
        return Spoken.WORDS[id] ?: key
    }

    /** True when [key] is printed as a word rather than as a sign. */
    fun isWord(key: String): Boolean = key !in SIGNS
}
