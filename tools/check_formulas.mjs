/*
 * Does the website compute what the app computes?
 *
 * docs/formulas.json carries, for every formula that can be calculated, the
 * answer the app's own engine produced on the sample inputs. This runs the same
 * formulas through the site's JavaScript engine and compares. Any disagreement
 * fails the build, so the site cannot drift away from the app without someone
 * noticing.
 *
 * Run: node tools/check_formulas.mjs
 */
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const require = createRequire(import.meta.url);
const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..');

const Evaluator = require(join(root, 'docs', 'evaluator.js'));

let data;
try {
  data = JSON.parse(readFileSync(join(root, 'docs', 'formulas.json'), 'utf8'));
} catch (e) {
  console.error('Could not read docs/formulas.json — run the FormulaExportTest first.');
  console.error(e.message);
  process.exit(1);
}

const formulas = data.formulas || [];
if (!formulas.length) {
  console.error('docs/formulas.json has no formulas in it.');
  process.exit(1);
}

let checked = 0;
let skipped = 0;
const failures = [];

for (const f of formulas) {
  if (f.expected === null || f.expected === undefined) continue;
  if (!f.expression) { skipped++; continue; }

  const vars = {};
  for (const input of f.inputs) vars[input.id] = input.sample;

  let got;
  try {
    got = Evaluator.evaluate(f.expression, vars);
  } catch (e) {
    failures.push({ id: f.id, title: f.title, why: 'threw: ' + e.message, expr: f.expression });
    continue;
  }

  const want = f.expected;
  // relative tolerance, because these are floating point sums of different orders
  const scale = Math.max(1, Math.abs(want));
  if (!Number.isFinite(got) || Math.abs(got - want) > 1e-9 * scale) {
    failures.push({
      id: f.id,
      title: f.title,
      why: `app says ${want}, site says ${got}`,
      expr: f.expression,
    });
  }
  checked++;
}

console.log(`formulas in the file: ${formulas.length}`);
console.log(`compared against the app: ${checked}`);
if (skipped) console.log(`skipped (no expression): ${skipped}`);

if (failures.length) {
  console.error(`\n${failures.length} formula(s) disagree with the app:\n`);
  for (const f of failures.slice(0, 25)) {
    console.error(`  ${f.id} — ${f.title}`);
    console.error(`    ${f.why}`);
    console.error(`    ${f.expr}`);
  }
  if (failures.length > 25) console.error(`  ... and ${failures.length - 25} more`);
  process.exit(1);
}

console.log('the site and the app agree on every formula that can be calculated.');
