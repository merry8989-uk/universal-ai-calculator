package com.merry8989uk.universalcalculator

import com.merry8989uk.universalcalculator.formula.CalculationRequest
import com.merry8989uk.universalcalculator.formula.FormulaCalculator
import com.merry8989uk.universalcalculator.formula.FormulaClassifier
import com.merry8989uk.universalcalculator.formula.FormulaDefinition
import com.merry8989uk.universalcalculator.formula.FormulaNormaliser
import com.merry8989uk.universalcalculator.formula.FormulaRegistry
import com.merry8989uk.universalcalculator.formula.FormulaType
import com.merry8989uk.universalcalculator.formula.RawFormula
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The universal calculation core, checked against every formula in the app.
 *
 * The point of this file is that a formula is not called a calculator because it
 * looks like one. Every entry that claims to be calculable is evaluated here with
 * real numbers, and every identity that claims to hold is checked with real
 * numbers. If the normaliser or the classifier regresses, this fails.
 */
class FormulaCoreTest {

    private val raw: List<RawFormula> by lazy { loadRaw() }
    private val definitions: List<FormulaDefinition> by lazy { FormulaRegistry.build(raw) }

    private fun file(path: String): File? = listOf(File("../$path"), File(path))
        .firstOrNull { it.exists() }

    private fun loadRaw(): List<RawFormula> {
        val f = file("app/src/main/assets/formulas.json")
            ?: file("src/main/assets/formulas.json")
            ?: return emptyList()
        return MiniJson.readFormulas(f.readText())
    }

    // ---- the data ----

    @Test fun theFileHoldsEveryEntry() {
        if (raw.isEmpty()) return
        assertEquals("the bundled formula file changed size", 1403, raw.size)
        assertEquals("the category count changed", 16, raw.map { it.category }.distinct().size)
    }

    @Test fun everyEntryGetsAnIdAndNoTwoShareOne() {
        if (definitions.isEmpty()) return
        val ids = definitions.map { it.id }
        assertEquals("some entry produced no id", definitions.size, ids.count { it.isNotBlank() })
        val duplicates = ids.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue("duplicate ids: ${duplicates.keys.take(5)}", duplicates.isEmpty())
    }

    @Test fun everyEntryIsClassifiedAndTheCountsAddUp() {
        if (definitions.isEmpty()) return
        val counts = FormulaRegistry.counts(definitions)
        assertEquals(1403, counts.values.sum())
        // Every kind of outcome the classifier can reach must be reached, or the
        // rule for it is dead. CONVERTER is deliberately absent: nothing in this
        // data set is a unit conversion — the seven entries that mention degrees
        // are trigonometry facts and a degree-of-dissociation formula — so it
        // stays in the model for the converter screen rather than being forced
        // onto a formula that is not one.
        for (type in FormulaType.entries) {
            if (type == FormulaType.CONVERTER) continue
            assertTrue(
                "${type.name} was never chosen, so the rule for it is dead",
                (counts[type] ?: 0) > 0,
            )
        }
    }

    // ---- the claim that matters: a calculator actually calculates ----

    @Test fun everyCalculatorProducesARealNumber() {
        if (definitions.isEmpty()) return
        val calculators = FormulaRegistry.calculators(definitions)
        assertTrue("no formula became a calculator at all", calculators.size >= 100)

        // Two sets of values. One formula may legitimately fail for one set —
        // (i-1)/(n-1) is 0/0 when both are 1 — so the claim being tested is that
        // the formula calculates for reasonable inputs, not for every input.
        val sets = listOf(
            { d: FormulaDefinition -> d.inputs.associate { it.id to it.sample } },
            { d: FormulaDefinition -> d.inputs.mapIndexed { i, spec -> spec.id to (3.5 + i) }.toMap() },
        )
        val broken = ArrayList<String>()
        for (d in calculators) {
            var lastError = "no input set worked"
            var worked = false
            for (set in sets) {
                val result = FormulaCalculator.calculate(d, CalculationRequest(d.id, set(d)))
                if (!result.success) {
                    lastError = result.error ?: "failed"
                    continue
                }
                val value = result.approximateValue
                if (value == null || value.isNaN() || value.isInfinite()) {
                    lastError = "gave no usable number"
                    continue
                }
                if (result.steps.isEmpty()) {
                    lastError = "no working shown"
                    continue
                }
                worked = true
                break
            }
            if (!worked) broken.add("${d.title}: $lastError")
        }
        assertTrue("these calculators do not calculate:\n" + broken.take(12).joinToString("\n"),
            broken.isEmpty())
    }

    @Test fun everyCheckerHoldsForTheValuesItWasGiven() {
        if (definitions.isEmpty()) return
        val checkers = FormulaRegistry.checkers(definitions)
        val broken = ArrayList<String>()
        for (d in checkers) {
            val inputs = d.inputs.associate { it.id to 2.0 }
            val result = FormulaCalculator.calculate(d, CalculationRequest(d.id, inputs))
            if (!result.success) {
                broken.add("${d.title}: ${result.error}")
                continue
            }
            if (result.exactValue != "Holds") {
                broken.add("${d.title}: does not hold")
            }
        }
        assertTrue("these identities do not hold:\n" + broken.take(10).joinToString("\n"),
            broken.isEmpty())
    }

    @Test fun aCalculatorRefusesToRunWithoutItsInputs() {
        if (definitions.isEmpty()) return
        val d = FormulaRegistry.calculators(definitions).firstOrNull { it.inputs.size >= 2 } ?: return
        val result = FormulaCalculator.calculate(d, CalculationRequest(d.id, emptyMap()))
        assertFalse("it calculated with nothing filled in", result.success)
        assertNotNull("it did not say what was missing", result.error)
    }

    @Test fun aFormulaThatIsOnlyAReferenceCannotBeCalculated() {
        if (definitions.isEmpty()) return
        val d = definitions.firstOrNull { it.type == FormulaType.REFERENCE_ONLY } ?: return
        val result = FormulaCalculator.calculate(d, CalculationRequest(d.id, emptyMap()))
        assertFalse(result.success)
        assertTrue(result.error!!.isNotBlank())
    }

    // ---- the normaliser, which everything else depends on ----

    @Test fun aRootGetsItsBrackets() {
        assertEquals("sqrt(a^2+b^2)", FormulaNormaliser.normalise("√(a² + b²)"))
        // without brackets the root must stop at the next operator
        assertEquals("sqrt(a^2)+b", FormulaNormaliser.normalise("√a² + b"))
    }

    @Test fun aProductWrittenWithoutASignStillMultiplies() {
        assertEquals("(a-b)*(a+b)", FormulaNormaliser.normalise("(a − b)(a + b)"))
        assertEquals("2*a*b", FormulaNormaliser.normalise("2ab"))
    }

    @Test fun aFunctionNameIsNotSplitIntoAProduct() {
        val out = FormulaNormaliser.normalise("sin(θ)")!!
        assertTrue("sin was treated as s*i*n: $out", out.startsWith("sin("))
        assertFalse(out.contains("s*i*n"))
    }

    @Test fun superscriptsBecomePowers() {
        assertEquals("a^2", FormulaNormaliser.normalise("a²"))
        assertEquals("a^(n-1)", FormulaNormaliser.normalise("aⁿ⁻¹"))
    }

    @Test fun greekLettersKeepTheirIdentity() {
        // θ must not become the latin t, and Δ must not collide with δ
        assertTrue(FormulaNormaliser.normalise("sinθ")!!.contains("th"))
        assertTrue(FormulaNormaliser.normalise("Δx")!!.contains("delta"))
        assertTrue(FormulaNormaliser.normalise("δx")!!.contains("d1"))
    }

    @Test fun aPowerWrittenOnTheFunctionBelongsToItsResult() {
        // sin²θ is sin(θ) squared, not sin squared times θ
        assertEquals("sin(th)^2", FormulaNormaliser.normalise("sin²θ"))
        assertEquals("cos(th)^3", FormulaNormaliser.normalise("cos³θ"))
    }

    @Test fun aCoefficientAndItsVariableAreOneArgument() {
        // cos 2θ is cos(2θ), not cos(2) times θ
        assertEquals("cos(2*th)", FormulaNormaliser.normalise("cos 2θ"))
    }

    @Test fun aDegreeSignIsRadianMeasure() {
        assertEquals("sin(90*pi/180)", FormulaNormaliser.normalise("sin(90°)"))
    }

    // ---- the two engines ----

    @Test fun aLogarithmBaseIsARealBase() {
        // logₐx is the log of x to base a: ln(x)/ln(a). Read as log(a) times x,
        // every logarithm identity comes out wrong rather than failing loudly.
        val out = FormulaNormaliser.normalise("logₐx")!!
        assertTrue("the base was not rewritten: $out", out.contains("ln("))
        assertFalse("the subscript was read as a variable: $out", out.startsWith("log(a)"))
        // logₐ(8) with a = 2 is 3
        val value = com.merry8989uk.universalcalculator.calc.Evaluator.evaluate(out, vars = mapOf("a" to 2.0, "x" to 8.0))
        assertEquals(3.0, value, 1e-9)
    }

    @Test fun aLineHoldingSeveralFormulasIsSplit() {
        val parts = FormulaClassifier.splitParts("A = a²,  P = 4a,  diagonal = a√2")
        assertEquals("the line was not split: $parts", 3, parts.size)
        assertEquals("A = a²", parts[0])
    }

    @Test fun aCommaInsideBracketsDoesNotSplit() {
        val parts = FormulaClassifier.splitParts("f(x, y) = x + y, g = 2")
        assertEquals("a bracket was cut in half: $parts", 2, parts.size)
        assertEquals("f(x, y) = x + y", parts[0])
    }

    @Test fun anEntryWithSeveralFormulasStillGetsOneCalculator() {
        val d = FormulaClassifier.classify("Geometry", "Square", "A = a²,  P = 4a,  diagonal = a√2")
        assertTrue("nothing was calculable in that line", d.type != FormulaType.NEEDS_REVIEW)
        assertEquals("the user must still see the whole line", "A = a²,  P = 4a,  diagonal = a√2", d.printed)
        assertNotNull("the part being computed was not kept", d.part)
    }

    @Test fun aCombinationIsNotAProductOfLetters() {
        // nCr is "n choose r", and read as n times C times r it is nonsense
        assertEquals("ncr(n,r)", FormulaNormaliser.normalise("nCr"))
        assertEquals("ncr(n,0)", FormulaNormaliser.normalise("nC0"))
        assertEquals("npr(n,r)", FormulaNormaliser.normalise("nPr"))
        // and a bracketed second argument
        assertEquals("ncr(n,r-1)", FormulaNormaliser.normalise("nC(r − 1)"))
    }

    @Test fun combinationsAreCountedCorrectly() {
        val e = com.merry8989uk.universalcalculator.calc.Evaluator
        assertEquals(10.0, e.evaluate("ncr(5,2)"), 1e-9)
        assertEquals(120.0, e.evaluate("ncr(10,3)"), 1e-9)
        assertEquals(1.0, e.evaluate("ncr(5,0)"), 1e-9)
        assertEquals(20.0, e.evaluate("npr(5,2)"), 1e-9)
        // and it refuses what is not a combination
        var refused = false
        try {
            e.evaluate("ncr(2,5)")
        } catch (ex: Exception) {
            refused = true
        }
        assertTrue("nCr(2,5) was allowed", refused)
    }

    @Test fun aNameIsNotSplitIntoAProduct() {
        // det, rank and Var read as products are not smaller answers, they are
        // confident wrong ones: det(AB) is not d times e times t times AB.
        assertEquals("det(AB)", FormulaNormaliser.normalise("det(AB)"))
        assertEquals("rank(A)", FormulaNormaliser.normalise("rank(A)"))
        assertEquals("Var(X)", FormulaNormaliser.normalise("Var(X)"))
        // and a two-letter capital name is a name too
        assertEquals("PV", FormulaNormaliser.normalise("PV"))
    }

    @Test fun aShortLowercaseRunIsAProduct() {
        // ab is a times b.
        assertEquals("a*b", FormulaNormaliser.normalise("ab"))
        assertEquals("2*a*b", FormulaNormaliser.normalise("2ab"))
    }

    @Test fun aLongLowercaseRunIsAName() {
        // abc is a name rather than a·b·c. This is a trade: the data has far more
        // three-letter names — det, adj, rank, nullity, trace — than it has a
        // product of three single letters, and reading a name as a product is a
        // wrong answer while reading a product as a name at least fails loudly.
        assertEquals("abc", FormulaNormaliser.normalise("abc"))
        assertEquals("det(AB)", FormulaNormaliser.normalise("det(AB)"))
    }

    @Test fun aFunctionOnlyCountsInLowerCase() {
        // "sin A cos B" is sin(A) times cos(B). If a function name may start at
        // a capital, the reader takes "Acos" out of the middle of it.
        assertEquals("sin(A)*cos(B)", FormulaNormaliser.normalise("sin A cos B"))
        assertEquals("cos(A)*cos(B)", FormulaNormaliser.normalise("cos A cos B"))
    }

    @Test fun aKnownLimitOfStrippingSpaces() {
        // A known limit, written down rather than hidden: the reader strips the
        // spaces before it tokenises, so "x ln a" arrives as "xlna" and there is
        // nothing left to tell a product from a name. It is read as one name.
        // Four letters with a function buried inside cannot be recovered without
        // knowing where the words were, so this stays a limitation.
        assertEquals("xlna", FormulaNormaliser.normalise("x ln a"))
    }

    @Test fun aSubscriptedLetterStaysOneVariable() {
        // lₐ is one variable. Read as l times a it is a different formula.
        val out = FormulaNormaliser.normalise("lₐ")!!
        assertEquals("l_a", out)
        assertEquals("lₐ", FormulaNormaliser.friendlyName(out))
    }

    @Test fun aTransposeIsNotAPower() {
        // (Aᵀ)ᵀ = A is true of matrices and false of numbers
        val d = FormulaClassifier.classify("Matrices", "Double transpose", "(Aᵀ)ᵀ = A")
        assertTrue(
            "a transpose was treated as a power: ${d.type}",
            d.type == FormulaType.SOLVER || d.type == FormulaType.REFERENCE_ONLY,
        )
    }

    @Test fun aReciprocalIsStillAPower() {
        // x⁻¹ is an ordinary reciprocal and must stay calculable
        assertTrue(FormulaNormaliser.containsStructure("x⁻¹ = 1/x") == null)
    }

    @Test fun anEqualsInsideABracketDoesNotDivideTheSides() {
        // P(X = k) = ... has two equals signs and only the second one divides the
        // sides. Splitting on the first makes the left side "P(X ", which is why
        // the whole probability section would not run.
        val at = FormulaClassifier.topLevelEquals("P(X = k) = λ^k e^(-λ) / k!")
        assertEquals("P(X = k) ", "P(X = k) = λ^k e^(-λ) / k!".substring(0, at))
    }

    @Test fun aProbabilityFormulaIsReadable() {
        val d = FormulaClassifier.classify("Probability", "Poisson", "P(X = k) = λ^k e^(−λ) / k!")
        assertTrue(
            "the equals sign inside the bracket is still dividing the sides: ${d.reason}",
            d.type != FormulaType.NEEDS_REVIEW || !d.reason.contains("could not be read"),
        )
    }

    // ---- the hand-corrected entries ----

    @Test fun theCorrectedEntriesSayWhatTheyShould() {
        if (raw.isEmpty()) return
        // Found by title, not by position: removing a duplicate moves every entry
        // after it, and a test that counts positions would pass for the wrong
        // reason. Checked so a later edit cannot quietly put the damage back.
        val expected = mapOf(
            "Whole Numbers" to "N₀ = {0, 1, 2, 3, ...}",
            "Product Formulas: quartic of a sum" to "(a + b)⁴ = a⁴ + 4a³b + 6a²b² + 4ab³ + b⁴",
            "Second Order Determinant" to "det(A) = a₁b₂ - a₂b₁",
            "Scalar Product in Coordinate Form" to "u · v = X₁X₂ + Y₁Y₂ + Z₁Z₂",
            "Probability of an Event" to "P(A) = m / n",
            "Expected Value of Discrete Random Variables" to "μ = E(X) = Σ xᵢ Pᵢ",
        )
        for ((title, text) in expected) {
            val entry = raw.firstOrNull { it.title == title }
            assertNotNull("the entry '$title' is gone", entry)
            assertEquals("'$title' was changed again", text, entry!!.printed)
        }
    }

    @Test fun noCorrectedEntryStillHasTheDamage() {
        if (raw.isEmpty()) return
        // every entry that was repaired by hand
        val repaired = setOf(
            "Whole Numbers", "Right Circular Cylinder with: base and total surface",
            "Scaling Vectors: scalar multiple", "Triple Product: vector rule",
            "Converting Rectangular Coordinates to Polar Coordinates",
            "Equation of a Line Given a Point and the Gradient", "Tangent Line",
            "RLC Circuit", "Inverse of a Complex Number in Polar Representation",
            "Rectangular Parallelepiped: volume", "Right Circular Cylinder: volume",
            "Cosine Function", "Additionand Subtraction Formulas: cosine of a difference",
            "Second Order Determinant", "Distance Between Two Points in Polar Coordinates",
            "Indefinite Integral: scaled argument", "Fourier Series: Fourier cosine coefficient",
            "Product Formulas: quartic of a sum", "Power Series", "Determinants",
            "Scalar Product in Coordinate Form", "Integration by Parts: integration by parts",
            "Separable Equations", "Probability of an Event",
            "Expected Value of Discrete Random Variables",
        )
        for (title in repaired) {
            val entry = raw.firstOrNull { it.title == title } ?: continue
            val text = entry.printed
            assertFalse("'$title' still holds a Chinese character: $text",
                text.any { it in "。、中入" })
            assertFalse("'$title' still holds a page number: $text",
                Regex("(^|\\s)\\d{2,3}(\\s|$)").containsMatchIn(text))
        }
    }

    @Test fun theSecondBatchOfRepairsIsIn() {
        if (raw.isEmpty()) return
        val expected = mapOf(
            "Powers of Trigonometric Functions: cosine squared" to "cos² α = (1 + cos 2α) / 2",
            "Second Order Determinant" to "det(A) = a₁b₂ - a₂b₁",
            "Minor" to "Mᵢⱼ = det(Aᵢⱼ)",
            "Correlation" to "ρ(X, Y) = cov(X, Y) / √[V(X)V(Y)]",
            "The Mole and Stoichiometry: Empirical formula from percent" to "% / atomic mass",
        )
        for ((title, text) in expected) {
            val entry = raw.firstOrNull { it.title == title }
            assertNotNull("'$title' is gone", entry)
            assertEquals("'$title' was changed again", text, entry!!.printed)
        }
    }

    @Test fun aMinorIsNotACofactor() {
        if (raw.isEmpty()) return
        // (-1)^(i+j) det(...) is the cofactor. The minor is the determinant alone,
        // and one of the batches confused the two.
        val minor = raw.firstOrNull { it.title == "Minor" }
        assertNotNull(minor)
        assertFalse("the minor was given the cofactor formula", minor!!.printed.contains("(-1)"))
    }

    @Test fun noFormulaAppearsTwiceAfterTheRepair() {
        // the repair made two damaged entries identical to two that were already
        // right; the duplicates were removed rather than kept
        if (raw.isEmpty()) return
        val keyed = raw.groupBy {
            it.printed.replace("−", "-").replace("–", "-").replace(Regex("\\s+"), "")
        }
        val duplicates = keyed.filter { it.value.size > 1 }
        assertTrue("the same formula is in the file twice: ${duplicates.keys.take(3)}",
            duplicates.isEmpty())
    }

    @Test fun aRootIndexIsAnIndex() {
        // ⁿ√x is the nth root of x. Read as a power it became ^(n) times sqrt(x),
        // which is not a formula at all.
        // the argument still goes through the reader, so ab inside it is a times b
        assertEquals("((a*b))^(1/(n))", FormulaNormaliser.normalise("ⁿ√(ab)"))
        // but a power in front of a root is a product: a squared times root 3
        assertEquals("2*a^2*sqrt(3)", FormulaNormaliser.normalise("2a²√3"))
    }

    @Test fun anArcNameIsTheSameFunction() {
        // a book writes arcsin where the evaluator writes asin. Without the
        // mapping the name is a product and the formula quietly changes.
        assertEquals("asin(x)", FormulaNormaliser.normalise("arcsin(x)"))
        assertEquals("asinh(x)", FormulaNormaliser.normalise("arcsinh(x)"))
    }

    @Test fun theThirdBatchOfRepairsIsIn() {
        if (raw.isEmpty()) return
        val expected = mapOf(
            "Multiple Angle Formulas: cosine of a multiple angle (3)" to
                "cos 5α = 16cos⁵α − 20cos³α + 5cos α",
            "Transforming of Trigonometric: product of cosines" to
                "cos α cos β = (1/2)(cos(α − β) + cos(α + β))",
            "Cofactor" to "Cᵢⱼ = (−1)^(i+j) Mᵢⱼ",
            "Right Circular Cone: volume" to "V = (1/3)πR²H",
            "Factoring Formulas: sum of 5th powers" to
                "a⁵ + b⁵ = (a + b)(a⁴ − a³b + a²b² − ab³ + b⁴)",
        )
        for ((title, text) in expected) {
            val entry = raw.firstOrNull { it.title == title }
            assertNotNull("'$title' is gone", entry)
            assertEquals("'$title' was changed again", text, entry!!.printed)
        }
    }

    @Test fun aStructuralFormulaIsNotPretendedToBeACalculator() {
        for (printed in listOf(
            "dy/dx = 2x",
            "∫ f(x) dx = F(x) + C",
            "A ∪ B = { x : x ∈ A or x ∈ B }",
            "a ≤ b",
        )) {
            val d = FormulaClassifier.classify("Test", "T", printed)
            assertTrue(
                "$printed was called ${d.type}, but it needs a solver",
                d.type == FormulaType.SOLVER || d.type == FormulaType.REFERENCE_ONLY,
            )
        }
    }

    @Test fun theReadmeDoesNotLieAboutTheCounts() {
        // The README claimed 1,663 formulas across 13 topics and 192 calculators
        // while the files held 1,413 across 16 and 343. A number in a README that
        // nobody checks is a number that is wrong.
        val readme = file("README.md") ?: return
        val text = readme.readText()
        assertTrue("the README does not give the reference count", text.contains("1,403 formulas"))
        assertTrue("the README does not give the topic count", text.contains("16 topics"))
        assertTrue("the README does not give the calculator count", text.contains("343 formula calculators"))
        assertFalse("the old wrong count is still in the README", text.contains("1663"))
        assertFalse("the old wrong count is still in the README", text.contains("192 formula"))
    }

    @Test fun theAuditCanBePrinted() {
        // not an assertion so much as a record: the CI log holds the numbers
        if (definitions.isEmpty()) return
        println(FormulaRegistry.audit(definitions))
    }
}
