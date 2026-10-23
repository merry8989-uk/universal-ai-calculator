package com.merry8989uk.universalcalculator.ai

/**
 * Turns a model answer into clean plain text: strips Markdown emphasis,
 * heading marks, backticks and LaTeX, so the user never sees stray ** or $$.
 */
object AnswerFormat {

    private val D = "$"

    fun clean(input: String): String {
        var s = input

        // Block and inline maths delimiters
        s = s.replace(D + D, "\n")
        s = s.replace(D, "")

        // Common LaTeX wrappers -> their contents
        s = s.replace(Regex("""\\text\{([^}]*)\}""")) { it.groupValues[1] }
        s = s.replace(Regex("""\\mathrm\{([^}]*)\}""")) { it.groupValues[1] }
        s = s.replace(Regex("""\\mathbf\{([^}]*)\}""")) { it.groupValues[1] }
        s = s.replace(Regex("""\\frac\{([^}]*)\}\{([^}]*)\}""")) {
            it.groupValues[1] + "/" + it.groupValues[2]
        }
        s = s.replace("\\times", " x ").replace("\\div", " / ").replace("\\cdot", " x ")
        s = s.replace("\\approx", " ~ ").replace("\\pm", "+/-").replace("\\neq", " != ")
        s = s.replace("\\le", "<=").replace("\\ge", ">=").replace("\\infty", "infinity")
        s = s.replace("\\%", "%").replace("\\,", " ").replace("\\;", " ").replace("\\!", "")
        s = s.replace("\\left", "").replace("\\right", "")
        s = s.replace(Regex("""\\[a-zA-Z]+"""), "")
        s = s.replace("\\", "")
        s = s.replace("{", "").replace("}", "")

        // Markdown
        s = s.replace("**", "")
        s = s.replace("`", "")
        s = s.replace(Regex("(?m)^#{1,6}\\s*"), "")
        s = s.replace(Regex("(?m)^\\s*\\*\\s+"), "\u2022 ")
        s = s.replace(Regex("(?m)^\\s*-\\s+"), "\u2022 ")

        // Tidy whitespace
        s = s.replace(Regex("[ \\t]{2,}"), " ")
        s = s.replace(Regex("\\n{3,}"), "\n\n")

        return s.trim()
    }
}
