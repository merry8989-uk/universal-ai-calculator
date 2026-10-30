# Nexus Calc — engine audit

Every number here is printed by the test run against the real `formulas.json`, not
estimated. A formula counts as a calculator only after its expression has been
evaluated with real numbers and produced a real answer; an identity counts only
after both sides have been shown to agree.

## The pipeline

| stage | what it does | where |
| --- | --- | --- |
| `FormulaNormaliser` | turns what a book prints into what the evaluator runs | `core/formula/FormulaNormaliser.kt` |
| `FormulaClassifier` | decides what each entry can do, then proves it | `core/formula/FormulaClassifier.kt` |
| `FormulaCalculator` | the only place a mathematical answer is produced | `core/formula/FormulaCalculator.kt` |
| `FormulaRegistry` | ids, counts, the audit | `core/formula/FormulaRegistry.kt` |
| `Evaluator` | arithmetic, functions, nCr, nPr, factorial | `core/calc/Evaluator.kt` |
| `FormulaReferenceScreen` | boxes to fill, a Calculate button, the working | `app/ui/FormulaReferenceScreen.kt` |

The assistant is not in this pipeline on purpose. It may find a formula and explain
it; it never produces the number.

## Every formula, by engine

| engine | total | calculator | checker | solver | reference | waiting |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Integral Calculus | 154 | 8 | 0 | 78 | 32 | 36 |
| Physics | 146 | 92 | 6 | 2 | 2 | 44 |
| Trigonometry | 145 | 15 | 47 | 16 | 23 | 44 |
| Differential Calculus | 116 | 15 | 4 | 63 | 8 | 26 |
| Algebra | 112 | 7 | 24 | 10 | 27 | 44 |
| Chemistry | 111 | 34 | 3 | 5 | 13 | 56 |
| Sets & Numbers | 88 | 7 | 12 | 26 | 26 | 17 |
| Analytic Geometry | 80 | 20 | 2 | 2 | 24 | 32 |
| Geometry | 78 | 33 | 7 | 1 | 15 | 22 |
| Series | 68 | 2 | 0 | 24 | 19 | 23 |
| Financial Derivatives | 59 | 11 | 0 | 2 | 17 | 29 |
| Vectors | 58 | 8 | 5 | 0 | 11 | 34 |
| Differential Equations | 56 | 5 | 0 | 17 | 7 | 27 |
| Probability & Combinatorics | 53 | 3 | 2 | 13 | 11 | 24 |
| Finance & Economics | 42 | 9 | 0 | 11 | 6 | 16 |
| Matrices & Determinants | 39 | 3 | 3 | 13 | 2 | 18 |
| **all engines** | **1405** | **272** | **115** | **283** | **243** | **492** |

`calculator` and `checker` are the ones a user can act on. `solver` needs an equation
solved, `reference` is a definition or a table, and `waiting` is where the reading is
still wrong or still missing.

## Where each engine stands

### Physics — 98 of 146 working (67%)

92 calculators, 6 checkers, 2 needing a solver, 2 reference only, 44 waiting.

Partly working; the rest needs either a solver or a better reading.

### Trigonometry — 62 of 145 working (43%)

15 calculators, 47 checkers, 16 needing a solver, 23 reference only, 44 waiting.

Partly working; the rest needs either a solver or a better reading.

### Geometry — 40 of 78 working (51%)

33 calculators, 7 checkers, 1 needing a solver, 15 reference only, 22 waiting.

Partly working; the rest needs either a solver or a better reading.

### Chemistry — 37 of 111 working (33%)

34 calculators, 3 checkers, 5 needing a solver, 13 reference only, 56 waiting.

Mostly waiting: the reading is the blocker here, not the engine.

### Algebra — 31 of 112 working (28%)

7 calculators, 24 checkers, 10 needing a solver, 27 reference only, 44 waiting.

Partly working; the rest needs either a solver or a better reading.

### Analytic Geometry — 22 of 80 working (28%)

20 calculators, 2 checkers, 2 needing a solver, 24 reference only, 32 waiting.

Partly working; the rest needs either a solver or a better reading.

### Differential Calculus — 19 of 116 working (16%)

15 calculators, 4 checkers, 63 needing a solver, 8 reference only, 26 waiting.

Partly working; the rest needs either a solver or a better reading.

### Sets & Numbers — 19 of 88 working (22%)

7 calculators, 12 checkers, 26 needing a solver, 26 reference only, 17 waiting.

Partly working; the rest needs either a solver or a better reading.

### Vectors — 13 of 58 working (22%)

8 calculators, 5 checkers, 0 needing a solver, 11 reference only, 34 waiting.

Mostly waiting: the reading is the blocker here, not the engine.

### Financial Derivatives — 11 of 59 working (19%)

11 calculators, 0 checkers, 2 needing a solver, 17 reference only, 29 waiting.

Mostly waiting: the reading is the blocker here, not the engine.

### Finance & Economics — 9 of 42 working (21%)

9 calculators, 0 checkers, 11 needing a solver, 6 reference only, 16 waiting.

Partly working; the rest needs either a solver or a better reading.

### Integral Calculus — 8 of 154 working (5%)

8 calculators, 0 checkers, 78 needing a solver, 32 reference only, 36 waiting.

Partly working; the rest needs either a solver or a better reading.

### Matrices & Determinants — 6 of 39 working (15%)

3 calculators, 3 checkers, 13 needing a solver, 2 reference only, 18 waiting.

Mostly waiting: the reading is the blocker here, not the engine.

### Differential Equations — 5 of 56 working (9%)

5 calculators, 0 checkers, 17 needing a solver, 7 reference only, 27 waiting.

Mostly waiting: the reading is the blocker here, not the engine.

### Probability & Combinatorics — 5 of 53 working (9%)

3 calculators, 2 checkers, 13 needing a solver, 11 reference only, 24 waiting.

Mostly waiting: the reading is the blocker here, not the engine.

### Series — 2 of 68 working (3%)

2 calculators, 0 checkers, 24 needing a solver, 19 reference only, 23 waiting.

Partly working; the rest needs either a solver or a better reading.

## What the waiting ones need

| count | what it is | what would fix it |
| ---: | --- | --- |
| 172 | read wrongly — the expression runs and gives a different number | more notation rules, or hand repair |
| 145 | the left side would not run | notation the reader cannot handle |
| 127 | the right side would not run | notation the reader cannot handle |
| 48 | looks like a calculator but will not run | notation the reader cannot handle |

A wrong reading is the dangerous one: it gives a confident wrong answer rather than
an error. That is why they are listed by name in the test log.

## Test coverage

| what is checked | how |
| --- | --- |
| every entry gets a unique id | `FormulaCoreTest.everyEntryGetsAnIdAndNoTwoShareOne` |
| every calculator produces a real number and shows its working | `everyCalculatorProducesARealNumber` |
| every checker holds for the values it was given | `everyCheckerHoldsForTheValuesItWasGiven` |
| a calculator refuses to run with empty inputs | `aCalculatorRefusesToRunWithoutItsInputs` |
| no formula appears twice | `noFormulaAppearsTwiceAfterTheRepair` |
| the hand repairs are still in place | `theCorrectedEntriesSayWhatTheyShould` |
| the README counts match the data | `theReadmeDoesNotLieAboutTheCounts` |
