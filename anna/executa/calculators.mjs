/*
 * The calculators, ported from the Android app.
 *
 * Every one of these is a straight port of the Kotlin in
 * app/src/main/java/.../calc/FormulaLibrary.kt: the same inputs with the same
 * defaults, the same edge cases, the same words in the notes. That matters more
 * than it looks - the Anna Edition and the phone app answering differently for
 * the same GST is the kind of drift that makes two products out of one.
 *
 * `working` is the reason this package exists in this shape. The Anna review
 * asks for an app whose primary value is AI reasoning, and the honest version of
 * that for a calculator is not "we call a model" - it is that every answer comes
 * with the sum that produced it, so the assistant can read it, check it, and say
 * where a step went wrong.
 */

const round = (v) => (Number.isFinite(v) ? Math.round(v * 1e10) / 1e10 : v);
const money = (v) => round(v);

export const CALCULATORS = {
  percent_of: {
    title: '% of a number',
    category: 'Everyday',
    description: 'What is X percent of Y?',
    inputs: [
      { key: 'x', label: 'Percent (%)', default: 15 },
      { key: 'y', label: 'Of value', default: 200 },
    ],
    run: ({ x, y }) => {
      const result = (x / 100) * y;
      return {
        outputs: [{ label: 'Result', value: round(result) }],
        working: [`${x}% of ${y} = ${x} ÷ 100 × ${y} = ${round(result)}`],
      };
    },
  },

  what_percent: {
    title: 'What percent',
    category: 'Everyday',
    description: 'A is what percent of B?',
    inputs: [
      { key: 'a', label: 'Part', default: 25 },
      { key: 'b', label: 'Whole', default: 200 },
    ],
    run: ({ a, b }) => {
      if (b === 0) {
        return {
          outputs: [{ label: 'Result', value: 0, note: 'Whole must not be zero' }],
          working: ['The whole is zero, so the question has no answer.'],
        };
      }
      const pct = (a / b) * 100;
      return {
        outputs: [{ label: 'Result', value: round(pct), unit: '%' }],
        working: [`${a} ÷ ${b} × 100 = ${round(pct)}%`],
      };
    },
  },

  discount: {
    title: 'Discount / sale price',
    category: 'Everyday',
    description: 'Final price after a discount.',
    inputs: [
      { key: 'price', label: 'Price', default: 1500 },
      { key: 'off', label: 'Discount (%)', default: 20 },
    ],
    run: ({ price, off }) => {
      const save = (price * off) / 100;
      return {
        outputs: [
          { label: 'You save', value: money(save) },
          { label: 'Final price', value: money(price - save) },
        ],
        working: [
          `discount = ${price} × ${off} ÷ 100 = ${money(save)}`,
          `final = ${price} − ${money(save)} = ${money(price - save)}`,
        ],
      };
    },
  },

  tip_split: {
    title: 'Tip & bill split',
    category: 'Everyday',
    description: 'Tip on a bill and the share per person.',
    inputs: [
      { key: 'bill', label: 'Bill', default: 1000 },
      { key: 'tip', label: 'Tip (%)', default: 10 },
      { key: 'people', label: 'People', default: 4 },
    ],
    run: ({ bill, tip, people }) => {
      const n = Math.max(1, people);
      const tipAmount = (bill * tip) / 100;
      const total = bill + tipAmount;
      return {
        outputs: [
          { label: 'Tip', value: money(tipAmount) },
          { label: 'Total', value: money(total) },
          { label: 'Per person', value: money(total / n) },
        ],
        working: [
          `tip = ${bill} × ${tip} ÷ 100 = ${money(tipAmount)}`,
          `total = ${bill} + ${money(tipAmount)} = ${money(total)}`,
          `each = ${money(total)} ÷ ${n} = ${money(total / n)}`,
        ],
      };
    },
  },

  gst_add: {
    title: 'Add GST / tax',
    category: 'Everyday',
    description: 'Add a tax percentage to an amount.',
    inputs: [
      { key: 'amount', label: 'Amount', default: 1000 },
      { key: 'gst', label: 'GST (%)', default: 18 },
    ],
    run: ({ amount, gst }) => {
      const tax = (amount * gst) / 100;
      return {
        outputs: [
          { label: 'Tax', value: money(tax) },
          { label: 'Total', value: money(amount + tax) },
        ],
        working: [
          `tax = ${amount} × ${gst} ÷ 100 = ${money(tax)}`,
          `total = ${amount} + ${money(tax)} = ${money(amount + tax)}`,
        ],
      };
    },
  },

  gst_remove: {
    title: 'Remove GST / tax',
    category: 'Everyday',
    description: 'Split a tax-inclusive amount into base and tax.',
    inputs: [
      { key: 'total', label: 'Tax-inclusive amount', default: 1180 },
      { key: 'gst', label: 'GST (%)', default: 18 },
    ],
    run: ({ total, gst }) => {
      const base = total / (1 + gst / 100);
      return {
        outputs: [
          { label: 'Base amount', value: money(base) },
          { label: 'Tax', value: money(total - base) },
        ],
        working: [
          `base = ${total} ÷ (1 + ${gst} ÷ 100) = ${money(base)}`,
          `tax = ${total} − ${money(base)} = ${money(total - base)}`,
        ],
      };
    },
  },

  simple_interest: {
    title: 'Simple interest (rate per year)',
    category: 'Everyday',
    description: 'Interest = P × R × T ÷ 100, with R per year and T in years.',
    inputs: [
      { key: 'p', label: 'Principal', default: 100000 },
      { key: 'r', label: 'Rate (% per year)', default: 8 },
      { key: 't', label: 'Time (years)', default: 5 },
    ],
    run: ({ p, r, t }) => {
      const interest = (p * r * t) / 100;
      return {
        outputs: [
          { label: 'Interest', value: money(interest) },
          { label: 'Total', value: money(p + interest) },
        ],
        working: [
          `interest = ${p} × ${r} × ${t} ÷ 100 = ${money(interest)}`,
          `total = ${p} + ${money(interest)} = ${money(p + interest)}`,
        ],
      };
    },
  },

  compound_interest: {
    title: 'Compound interest',
    category: 'Finance',
    description: 'A = P(1 + r/n)^(nt).',
    inputs: [
      { key: 'p', label: 'Principal', default: 100000 },
      { key: 'r', label: 'Rate (% per year)', default: 8 },
      { key: 't', label: 'Time (years)', default: 5 },
      { key: 'n', label: 'Compounds per year', default: 4 },
    ],
    run: ({ p, r, t, n }) => {
      const rate = r / 100;
      const per = Math.max(1, n);
      const maturity = p * Math.pow(1 + rate / per, per * t);
      return {
        outputs: [
          { label: 'Maturity', value: money(maturity) },
          { label: 'Interest', value: money(maturity - p) },
        ],
        working: [
          `maturity = ${p} × (1 + ${r} ÷ 100 ÷ ${per})^(${per} × ${t}) = ${money(maturity)}`,
          `interest = ${money(maturity)} − ${p} = ${money(maturity - p)}`,
        ],
      };
    },
  },

  cagr: {
    title: 'CAGR',
    category: 'Finance',
    description: 'Compound annual growth rate.',
    inputs: [
      { key: 'begin', label: 'Initial value', default: 100000 },
      { key: 'end', label: 'Final value', default: 200000 },
      { key: 'years', label: 'Years', default: 5 },
    ],
    run: ({ begin, end, years }) => {
      if (begin <= 0 || years <= 0) {
        return {
          outputs: [{ label: 'CAGR', value: 0, note: 'Use positive values' }],
          working: ['A growth rate needs a positive starting value and a positive number of years.'],
        };
      }
      const rate = (Math.pow(end / begin, 1 / years) - 1) * 100;
      return {
        outputs: [{ label: 'CAGR', value: round(rate), unit: '%' }],
        working: [`(${end} ÷ ${begin})^(1 ÷ ${years}) − 1 = ${round(rate)}%`],
      };
    },
  },

  bmi: {
    title: 'BMI (Body Mass Index)',
    category: 'Health',
    description: 'Weight in kg over height in metres squared.',
    inputs: [
      { key: 'w', label: 'Weight (kg)', default: 70 },
      { key: 'h', label: 'Height (cm)', default: 170 },
    ],
    run: ({ w, h }) => {
      const metres = h / 100;
      if (metres <= 0) {
        return {
          outputs: [{ label: 'BMI', value: 0, note: 'Height must be positive' }],
          working: ['Height must be positive to divide by it.'],
        };
      }
      const bmi = w / (metres * metres);
      const band =
        bmi < 18.5 ? 'Underweight' : bmi < 25 ? 'Normal' : bmi < 30 ? 'Overweight' : 'Obese';
      return {
        outputs: [{ label: 'BMI', value: round(bmi), unit: 'kg/m2', note: band }],
        working: [`BMI = ${w} ÷ (${metres})^2 = ${round(bmi)} — ${band}`],
      };
    },
  },

  electricity_bill_in: {
    title: 'Electricity bill (slab)',
    category: 'Everyday',
    description: 'Slab rates, the way a state board bills them. Set the two slab ends and the three rates.',
    inputs: [
      { key: 'units', label: 'Units consumed', default: 250 },
      { key: 's1_end', label: 'First slab ends at (units)', default: 100 },
      { key: 's1_rate', label: 'First slab rate', default: 3.5 },
      { key: 's2_end', label: 'Second slab ends at (units)', default: 300 },
      { key: 's2_rate', label: 'Second slab rate', default: 4.5 },
      { key: 's3_rate', label: 'Third slab rate', default: 6 },
      { key: 'fixed', label: 'Fixed charge', default: 50 },
    ],
    run: (v) => {
      const units = Math.max(0, v.units);
      const e1 = Math.max(0, v.s1_end);
      const e2 = Math.max(e1, v.s2_end);
      const in1 = Math.min(units, e1);
      const in2 = Math.min(units, e2) - in1;
      const in3 = units - Math.min(units, e2);
      const energy = in1 * v.s1_rate + in2 * v.s2_rate + in3 * v.s3_rate;
      const total = energy + v.fixed;
      return {
        outputs: [
          { label: 'Energy charge', value: money(energy), unit: 'Rs.' },
          { label: 'Fixed charge', value: money(v.fixed), unit: 'Rs.' },
          { label: 'Total', value: money(total), unit: 'Rs.' },
          { label: 'Average rate', value: units > 0 ? round(total / units) : 0, unit: 'Rs./unit' },
        ],
        working: [
          `first ${in1} units × ${v.s1_rate} = ${money(in1 * v.s1_rate)}`,
          `next ${in2} units × ${v.s2_rate} = ${money(in2 * v.s2_rate)}`,
          `remaining ${in3} units × ${v.s3_rate} = ${money(in3 * v.s3_rate)}`,
          `energy = ${money(energy)}, total = ${money(energy)} + ${money(v.fixed)} = ${money(total)}`,
        ],
      };
    },
  },

  hra_exemption: {
    title: 'HRA exemption',
    category: 'Finance',
    description: 'The least of three limits: HRA received, rent less 10% of salary, and 50% (metro) or 40% of salary.',
    inputs: [
      { key: 'basic', label: 'Basic salary', default: 60000 },
      { key: 'da', label: 'Dearness allowance', default: 0 },
      { key: 'hra', label: 'HRA received', default: 24000 },
      { key: 'rent', label: 'Rent paid', default: 20000 },
      { key: 'metro', label: 'Metro city (1 = yes)', default: 1 },
    ],
    run: (v) => {
      const salary = v.basic + v.da;
      const received = v.hra;
      const rentLess = Math.max(0, v.rent - 0.1 * salary);
      const share = salary * (v.metro >= 1 ? 0.5 : 0.4);
      const exempt = Math.max(0, Math.min(received, rentLess, share));
      return {
        outputs: [
          { label: 'Salary considered', value: money(salary), unit: 'Rs.' },
          { label: 'Limit: HRA received', value: money(received), unit: 'Rs.' },
          { label: 'Limit: rent less 10%', value: money(rentLess), unit: 'Rs.' },
          { label: 'Limit: share of salary', value: money(share), unit: 'Rs.' },
          { label: 'Exemption', value: money(exempt), unit: 'Rs.' },
        ],
        working: [
          `salary = ${money(v.basic)} + ${money(v.da)} = ${money(salary)}`,
          `rent less 10% = ${money(v.rent)} − ${money(0.1 * salary)} = ${money(rentLess)}`,
          `${v.metro >= 1 ? '50' : '40'}% of salary = ${money(share)}`,
          `exemption = least of ${money(received)}, ${money(rentLess)}, ${money(share)} = ${money(exempt)}`,
        ],
      };
    },
  },
};

/** The catalogue, without the functions - what a listing or a prompt can carry. */
export function catalogue() {
  return Object.entries(CALCULATORS).map(([id, c]) => ({
    id,
    title: c.title,
    category: c.category,
    description: c.description,
    inputs: c.inputs,
  }));
}

/** Runs one calculator, filling missing inputs from its declared defaults. */
export function runCalculator(id, args = {}) {
  const calc = CALCULATORS[id];
  if (!calc) {
    return { error: `No calculator with id "${id}". Call list_calculators to see them.` };
  }
  const values = {};
  const missing = [];
  for (const spec of calc.inputs) {
    const given = args[spec.key];
    if (given === undefined || given === null || given === '') {
      values[spec.key] = spec.default;
    } else {
      const n = Number(given);
      if (!Number.isFinite(n)) missing.push(`${spec.key} must be a number, got "${given}"`);
      else values[spec.key] = n;
    }
  }
  if (missing.length) return { error: missing.join('; ') };

  const out = calc.run(values);
  return {
    id,
    title: calc.title,
    inputs: values,
    outputs: out.outputs,
    working: out.working,
  };
}
