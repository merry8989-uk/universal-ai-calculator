# Nexus Calc

<img src="docs/icon.png" alt="Nexus Calc logo" width="120">

Nexus Calc is an all-in-one Android calculator: standard and scientific maths, a full unit
converter, a programmer tool, date maths, statistics, matrices, numerical
calculus, a graphing plotter, format converters and 151 built-in one-shot
calculators across Everyday, Finance, Economics, Health, Geometry, Physics,
Engineering, Construction and Math.

- **App name:** Nexus Calc
- **Package ID:** `com.merry8989uk.universalcalculator` (unchanged, so updates install over the old app)
- **Platform:** Android (Kotlin + Jetpack Compose)
- **Public app page:** https://merry8989-uk.github.io/universal-ai-calculator/
- **Downloads:** https://github.com/merry8989-uk/universal-ai-calculator/releases

## Features

- **Ask AI** — ask anything in plain language and get an answer with the working.
  The assistant runs on a Cloudflare Worker; the app
  never holds the key and the user never enters one.
- **Standard calculator** — live result preview, `ans` memory, a real percent
  key (200 + 10% = 220), sign toggle, thousands separators.
- **Persistent history** — stored on the device and kept until you clear it.
- **Scientific calculator** — trig (DEG/RAD), inverse trig, logs, powers, roots,
  factorial, nCr/nPr, gcd/lcm, constants (pi, e, tau, phi).
- **Unit converter** — 26 categories and 304 units (length, mass, temperature,
  area, volume, speed, time, data, data rate, pressure, energy, power, force,
  torque, frequency, fuel economy, illuminance, radiation, electric, density,
  viscosity, cooking, typography, sound, acceleration, volumetric flow).
- **Programmer tool** — base 2/8/10/16/36 conversion and AND/OR/XOR/NOT/shift,
  rotate-free bitwise helpers and popcount.
- **Date calculator** — difference, age, weeks/months/years, working days, week
  number, quarter, day-of-year, add/subtract days, Unix timestamps.
- **Fractions in results** — 0.75 is also shown as 3/4
- **Haptic feedback**, keep screen on, save state, Modern/Classic keypad layout
- **Formula reference** — 1663 formulas across 13 topics, searchable
- **Statistics** — mean/median/mode, sample & population variance and SD,
  quartiles, IQR, skewness, kurtosis and linear regression.
- **Handwriting pad** — write a calculation with a finger
- **Least squares** and eigenvalues for any matrix size
- **Voice input** and a **photo solver** — both send the text to the assistant to answer and explain
- **x variable key** (with an inline value field) and a **⇄ swap** key
- **Number format** (Indian / International / European), **decimal places** and **measuring units** (metric / imperial)
- **Drag-down history tape** over the keypad
- **Material You** dynamic colour (Android 12+)
- **ANR capture** alongside crash reports
- **On-device analytics and crash reports** (exportable, never uploaded)
- **PDF solve** (attach a PDF to Ask AI)
- **Glassmorphism** with an ambient glow and a light sweep on =
- **Auto-scaling results** that shrink to fit
- **Key sounds** (click / bubble / pop) and spring press feedback
- **Privacy policy** at [docs/privacy.html](docs/privacy.html)
- **Overflow menu**: Settings, diagnostics logs, app info (update / contact / privacy)
- **Hindi UI** (Settings → Language) and English
- **Screen-reader labels** on every key, tab and control
- **Home-screen widget** (solid / dimmed / transparent)
- **Copy / select / share** on every assistant answer
- **Attach a photo, image, audio clip or text file** to Ask AI and get it solved
- **3-D surface** and **parametric** graphing
- **Complex numbers on the main keypad** — the `i` key works, and 2pi / 3(4+5) are accepted
- **Black-Scholes, option Greeks, WACC, annuities, index numbers, Sharpe ratio** and more
- **Symbolic algebra** — simplify, differentiate, integrate, factor and solve polynomials
- **ANOVA** and **chi-square** tests, **QR decomposition**
- **Distributions & tests** — normal/t/binomial/Poisson, t-tests, confidence intervals
- **Complex numbers** — full arithmetic, polar form and roots
- **RPN (stack) calculator** — reverse-Polish entry
- **Matrix calculator** — add, subtract, multiply, transpose, determinant,
  trace and inverse.
- **Calculus** — numerical derivative, second derivative, definite integral
  (Simpson) and summation of f(x).
- **Graphing** — plot y = f(x) live on a canvas with an adjustable x-range.
- **Converters** — Roman numerals, number to words (Indian & international
  grouping), text ↔ binary / hex / ASCII, RGB ↔ hex, timestamps.
- **Currency converter** — live exchange rates (open.er-api.com).
- **192 formula calculators** — EMI, SIP, NPV, IRR, CAGR, ROI, break-even,
  mortgage, gratuity, PF, depreciation, BMI, BMR, TDEE, BSA, eGFR, creatinine
  clearance, Parkland, insulin, geometry solids, physics laws, concrete/bricks/
  tiles/paint/steel, staircase, solar sizing, GPA, cooking scale, run rate and
  much more.
- **Rich Settings** — theme, six accent colours, text size, density, language,
  per-feature toggles including Enable Ask AI.
- **In-app updates** — checks GitHub Releases and installs a new signed APK
  without leaving the app.
- **4 dynamic themes** — Cyberpunk Neon, Emerald Mint, Sunset Crimson and
  Midnight Gold, switchable live, with a gradient colour-coded keypad.
- **Diagnostics** — log collection on by default, with a **real-time log box** in
  Settings showing every line as it is written (all on-device), plus share /
  export (.txt, .json, .md, .zip) and clear.

## Project layout

```
app/src/main/java/com/merry8989uk/universalcalculator/
  ai/           client for the assistant backend (key lives server-side)
  calc/         evaluator, units, formulas, statistics, matrix, calculus,
                converters, programmer, dates, currency
  ui/           AppRoot + the five mode screens and every tool screen (Compose)
  settings/     DataStore-backed Settings screen + contact footer
  updater/      in-app update check and install (GitHub Releases)
  diagnostics/  rolling log file, exporter, and the Diagnostics settings block
.github/workflows/  release.yml (signed build + release), pages.yml (app page)
docs/               the public GitHub Pages site
```

## AI backend

The app never holds the Gemini API key. It calls a small proxy that does; two
ready-to-deploy versions live in `backend/`:

- `backend/cloudflare/` — Cloudflare Worker (deployed, live)
- `backend/gcloud/` — Google Cloud Function (alternative)

Both read the key from a secret named `GEMINI_API_KEY`. After deploying, put the
URL into `ai/AiClient.kt` and rebuild.

## Tests

Every calculation engine has a JVM unit test suite (114 tests) covering
arithmetic, powers, percentages, scientific functions, unit conversions,
all 192 formulas, matrices, statistics, format converters, dates, the
programmer tool and numerical calculus. It runs in CI:

```
./gradlew test
```

The `Tests` workflow runs it on every push to `main`.

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
