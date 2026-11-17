/*
 * Turning what somebody said into the sum they meant.
 *
 * This is the half of the reading path that can be proved without a camera, a
 * microphone or a model: given the text that came back from speech or from a
 * photograph, work out which calculator it is and what the numbers are. The
 * other half - pixels to text, voice to text - belongs to Anna's host, and the
 * app's job is to accept its answer in whatever form it arrives.
 *
 * The rules are ordered and explicit rather than clever. A wrong guess here is
 * worse than no guess: the assistant will show the working, and working for the
 * wrong question reads as a confident mistake. So a phrase that matches nothing
 * says so, and the caller can ask.
 */

const ONES = {
  zero: 0, one: 1, two: 2, three: 3, four: 4, five: 5, six: 6, seven: 7, eight: 8, nine: 9,
  ten: 10, eleven: 11, twelve: 12, thirteen: 13, fourteen: 14, fifteen: 15, sixteen: 16,
  seventeen: 17, eighteen: 18, nineteen: 19,
};
const TENS = { twenty: 20, thirty: 30, forty: 40, fifty: 50, sixty: 60, seventy: 70, eighty: 80, ninety: 90 };
const SCALES = { hundred: 100, thousand: 1e3, lakh: 1e5, lac: 1e5, million: 1e6, crore: 1e7, billion: 1e9 };

/**
 * Words to digits, token by token.
 *
 * It walks the sentence and rewrites only the runs that are numbers, because a
 * replacement that swallows the words after them turns "fifteen percent of two
 * hundred" into "15" and loses the question.
 */
export function wordsToDigits(text) {
  const tokens = String(text).split(/[\s-]+/).filter(Boolean);
  const out = [];
  let total = 0;
  let current = 0;
  let inRun = false;

  const flush = () => {
    if (!inRun) return;
    out.push(String(total + current));
    total = 0;
    current = 0;
    inRun = false;
  };

  for (const raw of tokens) {
    const word = raw.toLowerCase().replace(/[^a-z0-9.]/g, '');
    if (word in ONES) {
      current += ONES[word];
      inRun = true;
      continue;
    }
    if (word in TENS) {
      current += TENS[word];
      inRun = true;
      continue;
    }
    if (word in SCALES) {
      const scale = SCALES[word];
      if (scale === 100) current = Math.max(1, current) * 100;
      else {
        total += Math.max(1, current) * scale;
        current = 0;
      }
      inRun = true;
      continue;
    }
    if (word === 'and' && inRun) continue;
    flush();
    out.push(raw);
  }
  flush();
  return out.join(' ');
}

/** "1,500" and "1 500" and "₹1,500" all become 1500. */
export function normalise(text) {
  let t = String(text ?? '')
    .replace(/[₹$€£]/g, ' ')
    .replace(/(\d),(?=\d{3}\b)/g, '$1')
    .replace(/(\d)\s+(?=\d{3}\b)/g, '$1')
    .replace(/%/g, ' percent ')
    .replace(/\bper cent\b/g, 'percent')
    .replace(/\bgst\b/gi, 'gst')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
  t = wordsToDigits(t);
  return t;
}

const num = (s) => {
  // undefined and "" are not zero. Treating them as zero is how "20 percent off"
  // becomes a discount on a price of nothing.
  if (s === undefined || s === null) return null;
  const cleaned = String(s).replace(/[^0-9.\-]/g, '');
  if (cleaned === '' || cleaned === '-' || cleaned === '.') return null;
  const n = Number(cleaned);
  return Number.isFinite(n) ? n : null;
};

/**
 * The rules, most specific first.
 *
 * Each returns `{ id, arguments }` for a built-in calculator, or `{ expression }`
 * for the plain engine. Anything that needs a number it cannot find is skipped
 * rather than defaulted, because a silent default is how "20% off" becomes a
 * discount on 1500 nobody asked for.
 */
const RULES = [
  {
    name: 'hra',
    test: (t) => /hra|house rent/.test(t) && /rent/.test(t),
    read: (t) => {
      const basic = num((t.match(/basic(?:\s+salary)?\s*(?:of|is|=|:)?\s*([\d.]+)/) || [])[1]);
      const rent = num((t.match(/rent(?:\s+paid)?\s*(?:of|is|=|:)?\s*([\d.]+)/) || [])[1]);
      const hra = num((t.match(/hra(?:\s+received)?\s*(?:of|is|=|:)?\s*([\d.]+)/) || [])[1]);
      if (basic == null || rent == null) return null;
      const args = { basic, rent };
      if (hra != null) args.hra = hra;
      if (/metro|mumbai|delhi|kolkata|chennai/.test(t)) args.metro = 1;
      else if (/non[- ]?metro|other city/.test(t)) args.metro = 0;
      return { id: 'hra_exemption', arguments: args };
    },
  },
  {
    name: 'electricity',
    test: (t) => /(electricity|power|bijli).*(bill|units)|units.*(bill|electricity)/.test(t),
    read: (t) => {
      const units = num((t.match(/([\d.]+)\s*units/) || [])[1]);
      return units == null ? null : { id: 'electricity_bill_in', arguments: { units } };
    },
  },
  {
    name: 'gst-remove',
    test: (t) => /gst|tax/.test(t) && /(remove|reverse|exclusive|before tax|without tax|excluding)/.test(t),
    read: (t) => {
      const total = num((t.match(/(?:from|on|of)\s*([\d.]+)/) || [])[1]);
      const rate = num((t.match(/([\d.]+)\s*percent/) || [])[1]);
      if (total == null) return null;
      return { id: 'gst_remove', arguments: rate == null ? { total } : { total, gst: rate } };
    },
  },
  {
    name: 'gst-add',
    test: (t) => /gst|tax/.test(t) && /(add|plus|include|inclusive|with|on|at|for)/.test(t),
    read: (t) => {
      const amount = num((t.match(/(?:on|to|for|of)\s*([\d.]+)/) || [])[1]);
      const rate = num((t.match(/([\d.]+)\s*percent/) || [])[1]);
      if (amount == null) return null;
      return { id: 'gst_add', arguments: rate == null ? { amount } : { amount, gst: rate } };
    },
  },
  {
    name: 'tip-split',
    test: (t) => /tip/.test(t) && /(split|between|each|per person|share)/.test(t),
    read: (t) => {
      const bill = num((t.match(/([\d.]+)/) || [])[1]);
      const tip = num((t.match(/([\d.]+)\s*percent/) || [])[1]);
      const people = num((t.match(/(?:between|among)\s*(\d+)/) || [])[1]) ??
        num((t.match(/(?:for)\s*(\d+)\s*(?:people|persons|friends|of us)/) || [])[1]);
      if (bill == null) return null;
      const args = { bill };
      if (tip != null) args.tip = tip;
      if (people != null) args.people = people;
      return { id: 'tip_split', arguments: args };
    },
  },
  {
    name: 'discount',
    test: (t) => /(discount|off)\b/.test(t) && !/tip/.test(t),
    read: (t) => {
      const off = num((t.match(/([\d.]+)\s*percent/) || [])[1]);
      // the price is the number that is not the percentage: "20 percent off 1500"
      const afterOff = num((t.match(/(?:off|on|for)\s*(?:rs\.?|inr)?\s*([\d.]+)/) || [])[1]);
      const all = [...t.matchAll(/([\d.]+)/g)].map((m) => num(m[1]));
      const price = afterOff ?? all.find((n) => n !== off);
      if (price == null || off == null) return null;
      return { id: 'discount', arguments: { price, off } };
    },
  },
  {
    name: 'compound-interest',
    test: (t) => /compound/.test(t) && /interest/.test(t),
    read: (t) => {
      const p = num((t.match(/(?:on|of)\s*([\d.]+)/) || [])[1]);
      const r = num((t.match(/([\d.]+)\s*percent/) || [])[1]);
      const years = num((t.match(/(?:for|in)\s*([\d.]+)\s*year/) || [])[1]);
      if (p == null) return null;
      const args = { p };
      if (r != null) args.r = r;
      if (years != null) args.t = years;
      return { id: 'compound_interest', arguments: args };
    },
  },
  {
    name: 'simple-interest',
    test: (t) => /interest/.test(t) && !/compound/.test(t),
    read: (t) => {
      const p = num((t.match(/(?:on|of)\s*([\d.]+)/) || [])[1]);
      const r = num((t.match(/([\d.]+)\s*percent/) || [])[1]);
      const years = num((t.match(/(?:for|in)\s*([\d.]+)\s*year/) || [])[1]);
      if (p == null) return null;
      const args = { p };
      if (r != null) args.r = r;
      if (years != null) args.t = years;
      return { id: 'simple_interest', arguments: args };
    },
  },
  {
    name: 'cagr',
    test: (t) => /cagr|growth rate/.test(t),
    read: (t) => {
      const pair = t.match(/([\d.]+)\s*to\s*([\d.]+)/);
      const years = num((t.match(/(?:in|over|for)\s*([\d.]+)\s*year/) || [])[1]);
      if (!pair) return null;
      const args = { begin: num(pair[1]), end: num(pair[2]) };
      if (years != null) args.years = years;
      return { id: 'cagr', arguments: args };
    },
  },
  {
    name: 'bmi',
    test: (t) => /bmi|body mass/.test(t),
    read: (t) => {
      const kg = num((t.match(/([\d.]+)\s*(?:kg|kgs|kilo|kilos|kilograms?)/) || [])[1]);
      const cm = num((t.match(/([\d.]+)\s*(?:cm|cms|centimetres?|centimeters?)/) || [])[1]);
      if (kg == null || cm == null) return null;
      return { id: 'bmi', arguments: { w: kg, h: cm } };
    },
  },
  {
    name: 'percent-of',
    // "of" needs a boundary, or "20 percent off" reads as a percent-of
    test: (t) => /\bpercent\b/.test(t) && /\bof\b/.test(t),
    read: (t) => {
      const rate = num((t.match(/([\d.]+)\s*percent/) || [])[1]);
      const of = num((t.match(/percent\s*of\s*([\d.]+)/) || [])[1]);
      if (rate == null || of == null) return null;
      return { id: 'percent_of', arguments: { x: rate, y: of } };
    },
  },
];

/**
 * Reads one phrase.
 *
 * Returns `{ kind, id, arguments }`, `{ kind: 'expression', expression }`, or
 * `{ kind: 'unknown' }` with the text it could not use, so the caller can ask a
 * question rather than guess.
 */
export function readCalculation(text) {
  const normalised = normalise(text);
  if (!normalised) return { kind: 'unknown', text: '', reason: 'There was nothing to read.' };

  for (const rule of RULES) {
    if (!rule.test(normalised)) continue;
    const hit = rule.read(normalised);
    if (hit) {
      return { kind: 'calculator', id: hit.id, arguments: hit.arguments, normalised, rule: rule.name };
    }
  }

  // Nothing named a calculator: if what is left is arithmetic, hand it to the engine.
  const expression = normalised
    .replace(/\b(what is|whats|what's|calculate|compute|how much is|equals|equal to)\b/g, ' ')
    .replace(/\b(times|multiplied by)\b/g, '*')
    .replace(/\b(divided by|over)\b/g, '/')
    .replace(/\b(plus|and)\b/g, '+')
    .replace(/\b(minus|less)\b/g, '-')
    .replace(/\bx\b/g, '*')
    .replace(/[^0-9+\-*/().^ ]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

  if (/\d/.test(expression) && /[+\-*/^]/.test(expression)) {
    return { kind: 'expression', expression, normalised };
  }

  return {
    kind: 'unknown',
    text: normalised,
    reason: 'That did not name a calculator I know, and it is not a sum on its own.',
  };
}
