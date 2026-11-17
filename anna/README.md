# Nexus Calc — Anna Edition

The Android app is the product. This directory is the same product packaged the way the Anna Founding Builder Program requires: an **Executa** that computes with its working, a **manifest** that declares it, and a **UI bundle** generated from the page the app already ships.

Status: **working slice, not yet published.** The plugin runs and is tested end to end; the manifest and listing copy are ready; the UI bundle builds. What has not happened is anything that needs Anna's own tooling — see *What is not verified*.

---

## What is here

```
anna/
  manifest.json              the app manifest, schema 4
  executa/
    server.mjs               JSON-RPC 2.0 over stdio: initialize, tools/list, tools/call
    calculators.mjs          11 calculators ported from the Android app, with working
    evaluator.cjs            the expression engine, copied from docs/evaluator.js
  ui/build.mjs               writes ui/dist/index.html from the app's own asset
  ui/reader.html             the reading screen: say it, photograph it, or type it
  test/                      30 tests: the port, the phrase reader, and the wire
```

Run it:

```bash
cd anna
npm test          # 30 tests, no network, no keys
npm run serve     # speaks JSON-RPC on stdio
npm run build:ui  # regenerates ui/dist/index.html
```

## The five tools

| Tool | What it is for |
| --- | --- |
| `calculate` | An expression somebody typed or said. The engine's own sentence comes back on failure — `Could not work that out: Unexpected end of expression`. |
| `list_calculators` | The catalogue with inputs and defaults, so the assistant picks the right calculator instead of guessing. |
| `run_calculator` | Runs one by id, returning `outputs` **and `working`** — the substitution and the arithmetic, step by step. |
| `explain` | The same run rendered as a numbered list a person can follow. |
| `read_calculation` | **The reading path.** Give it the words - what somebody said, or what was written on a slip of paper - and it works out which calculator is meant, fills in the numbers, and returns the answer with the working. |

`working` is the whole point. Anna's review asks for an app whose primary value is AI reasoning, and for a calculator the honest version of that is not "we call a model" — it is that **every answer arrives with the sum that produced it**, so the assistant can read it, check it, and say where a step went wrong. The prompt addendum in the manifest tells the assistant to quote those steps rather than paraphrase the answer.

## The reading path

The pitch is "typed, spoken, photographed or attached calculations with visible working", and it splits cleanly in two:

- **Pixels and voice to text is the host's job.** The page asks the browser for the microphone or the camera, and the dashboard's reader hands back text.
- **Text to a calculation with working is this app's job.** `executa/phrase.mjs` reads the sentence: `18% GST on 1500`, `split 2400 between 4 with 10 percent tip`, `remove gst from 1180 at 18 percent`, `bmi for 70 kg and 170 cm`, `fifteen percent of two hundred`, `two lakh fifty thousand`, `twelve times four`. It handles number words, Indian scales (lakh, crore), rupee signs and thousands separators, and it decides between a named calculator and a plain expression.

The rule that shaped it: **a wrong guess is worse than no guess.** A phrase that matches nothing returns `unknown` with the reason, and the tool tells the assistant to ask which calculator is meant - because working shown for the wrong question reads as a confident mistake. `20 percent off`, with nothing to discount, is refused rather than computed against a price of zero.

`anna/ui/reader.html` is the screen: a text area, a microphone button, a camera button, and the answer with its steps underneath. It is a separate page from the studio, so the Android app's own asset is untouched.

## How this meets the program's review criteria

| Criterion (program §8) | Where it stands |
| --- | --- |
| Complete app experience: UI, backend, clear inputs/outputs, loading and error states | UI bundle builds from the shipped page; backend is the Executa; errors are sentences, tested. **Gap:** the SPA and the Executa are not yet wired to each other — the page still computes locally, which is fine for browsing but means the UI does not exercise the tools yet. |
| AI-native core | The tools exist for the assistant, the prompt makes the working mandatory, and `read_calculation` turns a spoken or photographed sum into a worked answer. **Gap:** the transcription half is the host's, and has not been exercised against a real microphone or camera. |
| Functional completeness and utility | 11 calculators plus the full expression language, ported exactly from the app, with the app's own edge cases (`what_percent` with a zero whole, `cagr` with a non-positive base) returning notes rather than wrong numbers. |
| Distinctiveness and reliability | No external dependency at run time: the engine and the formulas are in the bundle. The expression engine is already held to the Android app by `tools/check_formulas.mjs` in CI. |
| User experience | The studio page: search over 1,396 formulas, per-formula inputs, a workspace. |

## What is not verified

Written down rather than glossed over, because every one of these is a place the first real run could disagree with me:

1. **The wire framing.** Anna's Executa protocol page was not reachable when this was written. The implementation is newline-delimited JSON-RPC 2.0 with `initialize` / `tools/list` / `tools/call` — the shape MCP uses and the shape the docs' "JSON-RPC 2.0 over stdio" implies. All of it lives in `handle()` in `server.mjs`, so a different handshake is one function to change, not a rewrite.
2. **The `ui` section.** The manifest carries the minimal `ui: { schema: 2 }` shown in Anna's own app diagram. The full App UI Manifest reference was not reachable, so any per-view or host-API declaration still has to be added.
3. **`tool_id`.** `nexus_calc` is a placeholder until the Executa is published; the manifest must reference the id the platform issues.
4. **Nothing has been through `anna-app validate`, review, or a real install.** No CLI, no account, no deploy was used.
5. **The host call shape in `reader.html`.** `callTool` and the camera branch assume `window.anna.tools.call` and `window.anna.vision.readText`. Both are isolated in one function each, and both fail with a sentence rather than silently when the host is absent.
6. **The studio page still computes locally.** It is the library, not the reader; wiring it to the tools is separate work.

## What is deliberately not here yet

- **The other 339 calculators.** They are Kotlin lambdas in the Android app; each needs porting. 11 today, in the same shape, so the rest are mechanical.
- **Sync, the widget, the updater, the Android diagnostics.** Nothing to port: they are the phone app's business.
- **A full parity harness against the app.** `tools/check_formulas.mjs` does this for the expression language; the calculators have no equivalent yet, and the test file here covers the cases the app's own tests use rather than all of them.

## Publish steps (when the account and the CLI are to hand)

1. `anna-app init` in this directory, or paste `manifest.json` into the Versions tab.
2. Publish the Executa first — an app bundles tools by id, so `nexus_calc` must exist and be installable before the manifest can reference it.
3. `npm run build:ui`, then upload `ui/dist/index.html` through the bundle pipeline (`bundle/init` → per-file PUT → `bundle/finalize`).
4. Fill the Listing tab: name, slug, tagline, description, logo (256×256 WebP), screenshots, homepage, support and privacy URLs.
5. Submit for review. Expect up to five business days, content and security.
6. Only after it is published does the 200-MAU tier become reachable, and the qualification window runs to 31 March 2027.

## Honest note on scope

This is a slice, sized so the expensive question gets answered cheaply: **will an AI-first framing of a calculator app pass Anna's review?** One calculator end to end, in a complete package, is the smallest thing that can answer it. Porting the other 339 before knowing the answer would be the wrong order.
