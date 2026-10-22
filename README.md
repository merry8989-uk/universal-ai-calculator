# Universal AI Calculator

An all-in-one Android calculator: standard and scientific maths, a full unit
converter, a programmer tool, date maths, statistics, matrices, numerical
calculus, a graphing plotter, format converters and 146 built-in one-shot
calculators across Everyday, Finance, Health, Geometry, Physics, Engineering,
Construction and Math.

- **Package ID:** `com.merry8989uk.universalcalculator`
- **Platform:** Android (Kotlin + Jetpack Compose)
- **Public app page:** https://merry8989-uk.github.io/universal-ai-calculator/
- **Downloads:** https://github.com/merry8989-uk/universal-ai-calculator/releases

## Features

- **Standard calculator** — live result preview, `ans` memory, percentage, sign
  toggle, thousands separators.
- **Scientific calculator** — trig (DEG/RAD), inverse trig, logs, powers, roots,
  factorial, nCr/nPr, gcd/lcm, constants (pi, e, tau, phi).
- **Unit converter** — 24 categories and 293 units (length, mass, temperature,
  area, volume, speed, time, data, data rate, pressure, energy, power, force,
  torque, frequency, fuel economy, illuminance, radiation, electric, density,
  viscosity, cooking, typography, sound).
- **Programmer tool** — base 2/8/10/16/36 conversion and AND/OR/XOR/NOT/shift,
  rotate-free bitwise helpers and popcount.
- **Date calculator** — difference, age, weeks/months/years, working days, week
  number, quarter, day-of-year, add/subtract days, Unix timestamps.
- **Statistics** — mean/median/mode, sample & population variance and SD,
  quartiles, IQR, skewness, kurtosis and linear regression.
- **Matrix calculator** — add, subtract, multiply, transpose, determinant,
  trace and inverse.
- **Calculus** — numerical derivative, second derivative, definite integral
  (Simpson) and summation of f(x).
- **Graphing** — plot y = f(x) live on a canvas with an adjustable x-range.
- **Converters** — Roman numerals, number to words (Indian & international
  grouping), text ↔ binary / hex / ASCII, RGB ↔ hex, timestamps.
- **Currency converter** — live exchange rates (open.er-api.com).
- **146 formula calculators** — EMI, SIP, NPV, IRR, CAGR, ROI, break-even,
  mortgage, gratuity, PF, depreciation, BMI, BMR, TDEE, BSA, eGFR, creatinine
  clearance, Parkland, insulin, geometry solids, physics laws, concrete/bricks/
  tiles/paint/steel, staircase, solar sizing, GPA, cooking scale, run rate and
  much more.
- **Rich Settings** — theme, six accent colours, text size, density, language,
  per-feature toggles, optional AI backend URL.
- **In-app updates** — checks GitHub Releases and installs a new signed APK
  without leaving the app.
- **Diagnostics** — log collection on by default, with share / export
  (.txt, .json, .md, .zip) and clear.

## Project layout

```
app/src/main/java/com/merry8989uk/universalcalculator/
  calc/         evaluator, units, formulas, statistics, matrix, calculus,
                converters, programmer, dates, currency
  ui/           AppRoot + the five mode screens and every tool screen (Compose)
  settings/     DataStore-backed Settings screen + contact footer
  updater/      in-app update check and install (GitHub Releases)
  diagnostics/  rolling log file, exporter, and the Diagnostics settings block
.github/workflows/  release.yml (signed build + release), pages.yml (app page)
docs/               the public GitHub Pages site
```

## Building

CI builds every tagged release. Locally:

```bash
./gradlew assembleRelease
```

Release signing reads `KEYSTORE_PATH`, `KEYSTORE_TYPE`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS` and `KEY_PASSWORD` from the environment — the keystore itself is a
repository secret and is never committed.

## Releasing

Tag `vX.Y.Z` and push; the Release workflow builds the signed APK + AAB and
publishes them. The in-app updater compares that tag to the installed version.

## Contact

Ideas, feedback or a feature request? DM **[@osintgram_io](https://instagram.com/osintgram_io)** on Instagram.
