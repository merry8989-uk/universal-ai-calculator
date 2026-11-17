/*
 * What somebody says, and the sum they meant.
 *
 * Every case here is a sentence a person could say out loud or write on a slip of
 * paper. A wrong guess is worse than no guess, so the table also holds the cases
 * that must NOT be guessed at.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { readCalculation, normalise, wordsToDigits } from '../executa/phrase.mjs';

const cases = [
  // phrase, expected id, expected arguments (subset)
  ['15% of 200', 'percent_of', { x: 15, y: 200 }],
  ['fifteen percent of two hundred', 'percent_of', { x: 15, y: 200 }],
  ['what is 18% GST on 1,500', 'gst_add', { amount: 1500, gst: 18 }],
  ['add 18 percent gst to 1500', 'gst_add', { amount: 1500, gst: 18 }],
  ['remove gst from 1180 at 18 percent', 'gst_remove', { total: 1180, gst: 18 }],
  ['20 percent off 1500', 'discount', { price: 1500, off: 20 }],
  ['split 2400 between 4 with 10 percent tip', 'tip_split', { bill: 2400, tip: 10, people: 4 }],
  ['simple interest on 100000 at 8 percent for 5 years', 'simple_interest', { p: 100000, r: 8, t: 5 }],
  ['compound interest on 100000 at 8 percent for 5 years', 'compound_interest', { p: 100000, r: 8, t: 5 }],
  ['bmi for 70 kg and 170 cm', 'bmi', { w: 70, h: 170 }],
  ['cagr from 100000 to 200000 in 5 years', 'cagr', { begin: 100000, end: 200000, years: 5 }],
  ['electricity bill for 350 units', 'electricity_bill_in', { units: 350 }],
  ['hra on basic 60000 rent 20000 hra 24000', 'hra_exemption', { basic: 60000, rent: 20000, hra: 24000 }],
];

for (const [phrase, id, args] of cases) {
  test(`reads "${phrase}" as ${id}`, () => {
    const read = readCalculation(phrase);
    assert.equal(read.kind, 'calculator', `got ${JSON.stringify(read)}`);
    assert.equal(read.id, id);
    for (const [key, value] of Object.entries(args)) {
      assert.equal(read.arguments[key], value, `${key} of ${id}`);
    }
  });
}

test('plain arithmetic becomes an expression, words and all', () => {
  assert.deepEqual(readCalculation('twelve times four'), {
    kind: 'expression', expression: '12 * 4', normalised: '12 times 4',
  });
  assert.equal(readCalculation('what is 2 plus 3 times 4').expression, '2 + 3 * 4');
});

test('a sentence that is not a calculation is not guessed at', () => {
  const read = readCalculation('make me a sandwich');
  assert.equal(read.kind, 'unknown');
  assert.match(read.reason, /did not name a calculator/);

  assert.equal(readCalculation('').kind, 'unknown');
});

test('indian money words and separators are read the way people write them', () => {
  assert.equal(normalise('₹1,500'), '1500');
  assert.equal(normalise('1 500'), '1500');
  assert.equal(wordsToDigits('two lakh fifty thousand'), '250000');
  assert.equal(wordsToDigits('one hundred and fifty'), '150');
});

test('the words after a number are not eaten by it', () => {
  // the bug this test exists for: a rewrite that swallowed the rest of the sentence
  assert.equal(wordsToDigits('fifteen percent of two hundred'), '15 percent of 200');
});

test('a percentage with no amount is not invented', () => {
  // "20 percent off" with nothing to discount has no price, so nothing is returned
  const read = readCalculation('20 percent off');
  assert.notEqual(read.kind, 'calculator');
});
