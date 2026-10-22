# Universal AI Calculator

An all-in-one Android calculator: standard and scientific maths, a full unit
converter, a programmer (base + bitwise) tool, a date calculator and 47
built-in one-shot calculators across Everyday, Finance, Health, Geometry,
Physics and Math.

- **Package ID:** `com.merry8989uk.universalcalculator`
- **Platform:** Android (Kotlin + Jetpack Compose)
- **Public app page:** https://merry8989-uk.github.io/universal-ai-calculator/
- **Downloads:** https://github.com/merry8989-uk/universal-ai-calculator/releases

## Features

- **Standard calculator** — live result preview, memory of the last answer
  (`ans`), percentage, sign toggle, thousands separators.
- **Scientific calculator** — trig (with DEG/RAD), inverse trig, hyperbolic,
  logs, powers, roots, factorial, nCr/nPr, gcd/lcm, constants (pi, e, tau, phi).
- **Unit converter** — 12 categories and 120+ units (length, mass, temperature,
  area, volume, speed, time, data, pressure, energy, power, angle).
- **Programmer tool** — binary/octal/decimal/hex conversion and bitwise
  AND/OR/XOR/NOT/shift.
- **Date calculator** — difference between dates, age, weeks, months, working
  days, leap years.
- **47 formula calculators** — BMI, BMR, TDEE, ideal weight, EMI, SIP, compound
  interest, CAGR, ROI, break-even, depreciation, GST, tip, discount, profit,
  fuel cost, geometry solids, physics laws, quadratic roots and more.
- **Rich Settings** — theme (light/dark/system), six accent colours, text size,
  density, language, per-feature toggles, optional AI backend URL.
- **In-app updates** — checks GitHub Releases and installs a new signed APK
  without leaving the app.
- **Diagnostics** — log collection on by default, with share / export
  (.txt, .json, .md, .zip) and clear.

## Project layout

```
app/src/main/java/com/merry8989uk/universalcalculator/
  calc/         expression evaluator, units, formula library, programmer, dates
  ui/           AppRoot + the five mode screens (Compose)
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
