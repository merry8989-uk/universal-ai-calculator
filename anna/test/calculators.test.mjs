import test from 'node:test';
import assert from 'node:assert/strict';
import { runCalculator, catalogue, CALCULATORS } from '../executa/calculators.mjs';

test('the port matches the app on the cases the app tests use', () => {
  const gst = runCalculator('gst_add', { amount: 1000, gst: 18 });
  assert.equal(gst.outputs[0].value, 180);
  assert.equal(gst.outputs[1].value, 1180);

  const tip = runCalculator('tip_split', { bill: 1000, tip: 10, people: 4 });
  assert.equal(tip.outputs[2].value, 275);

  const bill = runCalculator('electricity_bill_in', { units: 350 });
  assert.equal(bill.outputs[0].value, 1550);   // 350 + 900 + 300
  assert.equal(bill.outputs[2].value, 1600);   // plus the fixed charge

  const hra = runCalculator('hra_exemption', {});
  assert.equal(hra.outputs[4].value, 14000);   // rent 20000 - 10% of 60000
});

test('a missing input falls back to the calculator default', () => {
  const half = runCalculator('gst_add', { amount: 2000 });
  assert.equal(half.inputs.gst, 18);
  assert.equal(half.outputs[1].value, 2360);
});

test('an edge case answers with the app\u2019s note, not a wrong number', () => {
  const zeroWhole = runCalculator('what_percent', { a: 25, b: 0 });
  assert.equal(zeroWhole.outputs[0].value, 0);
  assert.match(zeroWhole.outputs[0].note, /must not be zero/);

  const badCagr = runCalculator('cagr', { begin: 0, end: 200000, years: 5 });
  assert.equal(badCagr.outputs[0].value, 0);
  assert.match(badCagr.outputs[0].note, /positive/);
});

test('every answer comes with the working that produced it', () => {
  for (const id of Object.keys(CALCULATORS)) {
    const out = runCalculator(id, {});
    assert.ok(Array.isArray(out.working) && out.working.length > 0, `${id} has no working`);
    assert.ok(out.outputs.length > 0, `${id} has no outputs`);
    assert.ok(!out.outputs.some((o) => Number.isNaN(o.value)), `${id} produced NaN`);
  }
});

test('an unknown id says so, and says what to do about it', () => {
  const out = runCalculator('no_such_thing', {});
  assert.match(out.error, /list_calculators/);
});

test('a non-numeric input is refused by name', () => {
  const out = runCalculator('gst_add', { amount: 'a thousand' });
  assert.match(out.error, /amount must be a number/);
});

test('the catalogue carries what a listing needs and nothing it cannot serialise', () => {
  const all = catalogue();
  assert.equal(all.length, Object.keys(CALCULATORS).length);
  for (const c of all) {
    assert.ok(c.id && c.title && c.category && c.description);
    assert.ok(Array.isArray(c.inputs));
    assert.equal(JSON.parse(JSON.stringify(c)).id, c.id);
  }
});
