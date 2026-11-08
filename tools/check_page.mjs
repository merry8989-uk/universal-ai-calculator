/*
 * Does the shipped formula page actually work?
 *
 * FormulaPageTest reads the page and checks its words: offline, no CDN, 1,396
 * formulas, maths present. That catches a page that has been broken on purpose.
 * It cannot catch a page that loads but does nothing - a selector renamed, a
 * handler dropped, a button that renders and does not press.
 *
 * So this runs it. The real file is loaded into a DOM, its scripts execute, and
 * it is driven the way a person drives it: search, open a formula, fill the
 * inputs, press Calculate, flip the language. Then it is loaded again with a
 * stand-in `Android` bridge, because the calculator shelf and the "open
 * calculator" button only exist inside the app.
 *
 * Run: node tools/check_page.mjs
 */
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { JSDOM } from 'jsdom';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const file = join(root, 'app', 'src', 'main', 'assets', 'formula-studio.html');
const html = readFileSync(file, 'utf8');

let failures = 0;
function ok(name, cond, extra = '') {
  if (!cond) failures++;
  console.log((cond ? 'PASS ' : 'FAIL ') + name + (extra ? ' - ' + extra : ''));
}

function boot(withBridge) {
  return new JSDOM(html, {
    runScripts: 'dangerously',
    pretendToBeVisual: true,
    beforeParse(window) {
      if (!withBridge) return;
      // the Kotlin side, shaped like FormulaBridge's JSON
      window.Android = {
        opened: [],
        openTool: (id) => { window.Android.opened.push(id); },
        index: () => JSON.stringify({
          items: [
            { k: 't', i: 'emi', t: 'EMI', c: 'Finance', w: true, g: null },
            { k: 't', i: 'bmi', t: 'BMI', c: 'Health', w: true, g: null },
            { k: 'f', i: 'f-282', t: 'Arithmetic mean', c: 'Series', w: true, g: 'mean-calc' },
            { k: 'f', i: 'f-1', t: 'Union', c: 'Sets & Numbers', w: false, g: null },
          ],
        }),
        tool: (id) => JSON.stringify({
          id,
          title: id === 'emi' ? 'EMI' : 'BMI',
          category: id === 'emi' ? 'Finance' : 'Health',
          about: 'What it works out',
          inputs: [{ k: 'p', label: 'Principal', v: 100000 }, { k: 'r', label: 'Rate', v: 10 }],
        }),
        calculateTool: () => JSON.stringify({
          ok: true,
          outputs: [{ label: 'Instalment', value: 8791.59, unit: 'Rs.' }],
        }),
      };
    },
  });
}

const loaded = (window) => new Promise((r) => window.addEventListener('load', r, { once: true }));

// ---- the page as the site serves it ----
{
  const dom = boot(false);
  const { window } = dom;
  const doc = window.document;
  await loaded(window);

  ok('the maths is in the page', typeof window.math === 'object');
  ok('the list renders every formula', doc.querySelectorAll('#list .item').length === 1396,
    doc.querySelectorAll('#list .item').length + ' rows');
  ok('the categories are filled', doc.querySelectorAll('#category option').length === 17);

  const search = doc.getElementById('search');
  search.value = 'union';
  search.dispatchEvent(new window.Event('input'));
  const hits = doc.querySelectorAll('#list .item').length;
  ok('search narrows the list', hits > 0 && hits < 1396, hits + ' rows');

  doc.querySelector('#list .item').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  ok('a formula opens', !!doc.getElementById('detail').querySelector('.formula'));

  // a formula the maths can run: "Arithmetic mean", A = (a + b) / 2
  search.value = 'Arithmetic mean';
  search.dispatchEvent(new window.Event('input'));
  ok('the mean is findable', doc.querySelectorAll('#list .item').length > 0);
  doc.querySelector('#list .item').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));

  const inputs = [...doc.querySelectorAll('#detail .varcard input')];
  ok('the variable fields are the right-hand side', inputs.length === 2,
    inputs.map((i) => i.dataset.symbol).join(','));
  inputs[0].value = '4';
  inputs[1].value = '6';
  const expr = doc.getElementById('calcExpr');
  ok('the expression is filled from the formula', expr.value === '(a + b) / 2', expr.value);

  const calc = [...doc.querySelectorAll('#detail .actions button')].find((b) => /Calculate/i.test(b.textContent));
  calc.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  ok('Calculate produces the answer', /\b5\b/.test(doc.getElementById('calcResult').textContent),
    JSON.stringify(doc.getElementById('calcResult').textContent.slice(0, 50)));

  expr.value = 'sqrt(16) + 2^3';
  calc.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  ok('the workspace runs what was typed', /12\b/.test(doc.getElementById('calcResult').textContent));

  const langBtn = doc.getElementById('langBtn');
  const before = doc.querySelector('.subtitle').textContent;
  langBtn.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  ok('the language toggles', before !== doc.querySelector('.subtitle').textContent);
  ok('the page language follows', doc.documentElement.lang === 'en');
  langBtn.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  ok('and back', doc.documentElement.lang === 'hi');

  ok('the shelf is not offered on the site', doc.getElementById('tabTools').hidden === true);
  ok('no calculator button without a bridge',
    ![...doc.querySelectorAll('#detail .actions button')].some((b) => /calculator/i.test(b.textContent)));

  dom.window.close();
}

// ---- the page inside the app ----
{
  const dom = boot(true);
  const { window } = dom;
  const doc = window.document;
  await loaded(window);

  ok('the shelf is offered in the app', doc.getElementById('tabTools').hidden === false);
  ok('the site link is hidden in the app', doc.getElementById('homeLink').hidden === true);

  doc.getElementById('tabTools').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  const rows = doc.querySelectorAll('#toolList .item');
  ok('the shelf lists the calculators', rows.length === 2, rows.length + ' rows');

  rows[0].dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  const panel = doc.getElementById('toolDetail');
  ok('a calculator opens', !!panel.querySelector('h2'), panel.querySelector('h2')?.textContent);
  ok('its inputs render', panel.querySelectorAll('.varcard input').length === 2);

  [...panel.querySelectorAll('.actions button')][0]
    .dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  ok('and it calculates', /Instalment/.test(doc.getElementById('toolResult').textContent));

  // a formula that has a calculator behind it offers a way into it
  doc.getElementById('tabReference').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  const search = doc.getElementById('search');
  search.value = 'Arithmetic mean';
  search.dispatchEvent(new window.Event('input'));
  doc.querySelector('#list .item').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));

  const open = [...doc.querySelectorAll('#detail .actions button')]
    .find((b) => /Calculator kholo|Open calculator/.test(b.textContent));
  ok('the formula offers its calculator', !!open);
  if (open) {
    open.dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
    ok('and opens it through the bridge', window.Android.opened[0] === 'mean-calc',
      JSON.stringify(window.Android.opened));
  }

  search.value = 'Union';
  search.dispatchEvent(new window.Event('input'));
  doc.querySelector('#list .item').dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
  ok('a formula with no calculator shows no button',
    ![...doc.querySelectorAll('#detail .actions button')].some((b) => /Calculator kholo|Open calculator/.test(b.textContent)));

  dom.window.close();
}

console.log(failures ? `\n${failures} check(s) failed` : '\nthe formula page works');
process.exit(failures ? 1 : 0);
