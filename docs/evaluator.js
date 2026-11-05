/*
 * The formula engine, in the browser.
 *
 * This is a port of the evaluator the app uses, not a second implementation of
 * the maths: the same grammar, the same precedence, the same function names.
 * That matters because the site and the app must give the same answer for the
 * same formula — a website that quietly computes something else is worse than
 * no website, and there is a check in CI that compares the two on every formula.
 *
 * Grammar, from loosest to tightest:
 *   + -   |   * / and implied multiplication   |   unary - +   |   ^   |   % ! °   |   atom
 */
(function (root) {
  'use strict';

  const PI = Math.PI;
  const E = Math.E;

  function roundHalfAway(x) {
    // a calculator user means 2.5 -> 3, not 2
    return x >= 0 ? Math.floor(x + 0.5) : Math.ceil(x - 0.5);
  }

  function factorial(x) {
    if (x < 0 || x !== Math.floor(x)) throw new Error('Factorial needs a non-negative whole number');
    if (x > 170) throw new Error('Factorial is too large');
    let r = 1;
    for (let k = 2; k <= x; k++) r *= k;
    return r;
  }

  function gcd(a, b) {
    let x = Math.abs(Math.trunc(a));
    let y = Math.abs(Math.trunc(b));
    while (y !== 0) { const t = x % y; x = y; y = t; }
    return x;
  }

  function lcm(a, b) {
    const x = Math.trunc(a), y = Math.trunc(b);
    if (x === 0 || y === 0) return 0;
    return Math.abs((x / gcd(x, y)) * y);
  }

  function isPrime(n) {
    if (n < 2) return false;
    if (n % 2 === 0) return n === 2;
    if (n % 3 === 0) return n === 3;
    for (let d = 5; d * d <= n; d += 6) {
      if (n % d === 0 || n % (d + 2) === 0) return false;
    }
    return true;
  }

  function nextPrime(after) {
    let n = after < 2 ? 2 : Math.trunc(after) + 1;
    while (!isPrime(n)) n++;
    return n;
  }

  function totient(n) {
    if (n < 1) return 0;
    let result = n, remaining = Math.trunc(n);
    for (let p = 2; p * p <= remaining; p++) {
      if (remaining % p === 0) {
        while (remaining % p === 0) remaining /= p;
        result -= result / p;
      }
    }
    if (remaining > 1) result -= result / remaining;
    return result;
  }

  function fibonacci(n) {
    if (n <= 0) return 0;
    let a = 0, b = 1;
    for (let i = 0; i < n - 1; i++) { const t = a + b; a = b; b = t; }
    return b;
  }

  function catalan(n) {
    if (n < 0) return 0;
    let result = 1;
    for (let i = 0; i < n; i++) result = (result * (2 * n - i)) / (i + 1);
    return Math.trunc(result / (n + 1));
  }

  // ---- special functions -------------------------------------------------

  const LANCZOS = [
    0.99999999999980993, 676.5203681218851, -1259.1392167224028,
    771.32342877765313, -176.61502916214059, 12.507343278686905,
    -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7,
  ];

  function gamma(z) {
    if (z < 0.5) return PI / (Math.sin(PI * z) * gamma(1 - z));
    z -= 1;
    let x = LANCZOS[0];
    for (let i = 1; i < 9; i++) x += LANCZOS[i] / (z + i);
    const t = z + 7.5;
    return Math.sqrt(2 * PI) * Math.pow(t, z + 0.5) * Math.exp(-t) * x;
  }

  function logGamma(z) {
    const g = gamma(z);
    if (g > 0) return Math.log(g);
    // reflection for the negative half-plane, where gamma is negative
    return Math.log(Math.abs(g));
  }

  function beta(x, y) {
    return (gamma(x) * gamma(y)) / gamma(x + y);
  }

  // Abramowitz and Stegun 7.1.26, good to about 1.5e-7
  function erf(x) {
    const sign = x < 0 ? -1 : 1;
    const ax = Math.abs(x);
    const t = 1 / (1 + 0.3275911 * ax);
    const y = 1 - ((((1.061405429 * t - 1.453152027) * t + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-ax * ax);
    return sign * y;
  }

  function erfc(x) { return 1 - erf(x); }

  function digamma(x) {
    let result = 0;
    while (x < 7) { result -= 1 / x; x += 1; }
    const inv = 1 / x, inv2 = inv * inv;
    return result + Math.log(x) - 0.5 * inv
      - inv2 * (1 / 12 - inv2 * (1 / 120 - inv2 * (1 / 252 - inv2 * (1 / 240))));
  }

  function zeta(s) {
    if (s === 1) throw new Error('zeta(1) is infinite');
    if (s < 1) return Math.pow(2, s) * Math.pow(PI, s - 1) * Math.sin((PI * s) / 2) * gamma(1 - s) * zeta(1 - s);
    // Euler-Maclaurin, plenty for the values a formula book asks for
    let sum = 0;
    const N = 20000;
    for (let n = 1; n < N; n++) sum += Math.pow(n, -s);
    sum += Math.pow(N, 1 - s) / (s - 1) + Math.pow(N, -s) / 2;
    sum += s * Math.pow(N, -s - 1) / 12 - s * (s + 1) * (s + 2) * Math.pow(N, -s - 3) / 720;
    return sum;
  }

  function lambertW(x) {
    if (x < -1 / E) throw new Error('lambertW needs x >= -1/e');
    let w = x < 10 ? x : Math.log(x) - Math.log(Math.log(x));
    if (x === 0) return 0;
    for (let i = 0; i < 100; i++) {
      const ew = Math.exp(w);
      const f = w * ew - x;
      const fp = ew * (w + 1) - ((w + 2) * f) / (2 * w + 2);
      const next = w - f / fp;
      if (Math.abs(next - w) < 1e-14) return next;
      w = next;
    }
    return w;
  }

  function medianOf(a) {
    if (!a.length) return 0;
    const s = [...a].sort((p, q) => p - q);
    const mid = Math.floor(s.length / 2);
    return s.length % 2 === 1 ? s[mid] : (s[mid - 1] + s[mid]) / 2;
  }

  function modeOf(a) {
    if (!a.length) return 0;
    const counts = new Map();
    for (const v of a) counts.set(v, (counts.get(v) || 0) + 1);
    let best = null, bestCount = -1;
    for (const [k, c] of counts) {
      if (c > bestCount || (c === bestCount && k < best)) { best = k; bestCount = c; }
    }
    return best;
  }

  function varianceOf(a) {
    if (!a.length) return 0;
    const mean = a.reduce((s, v) => s + v, 0) / a.length;
    return a.reduce((s, v) => s + (v - mean) * (v - mean), 0) / a.length;
  }

  function combinations(n, r) {
    if (n < 0 || r < 0 || r > n) throw new Error('nCr needs 0 <= r <= n');
    const nn = Math.round(n), rr = Math.round(r);
    if (Math.abs(n - nn) > 1e-9 || Math.abs(r - rr) > 1e-9) throw new Error('nCr needs whole numbers');
    let result = 1;
    for (let i = 1; i <= rr; i++) result = (result * (nn - rr + i)) / i;
    return Math.round(result);
  }

  function permutations(n, r) {
    if (n < 0 || r < 0 || r > n) throw new Error('nPr needs 0 <= r <= n');
    const nn = Math.round(n), rr = Math.round(r);
    let result = 1;
    for (let i = 0; i < rr; i++) result *= nn - i;
    return Math.round(result);
  }

  // ---- the parser --------------------------------------------------------

  function Parser(text, vars) {
    this.s = text;
    this.i = 0;
    this.vars = vars || {};
  }

  Parser.prototype.peek = function () { return this.i < this.s.length ? this.s[this.i] : null; };
  Parser.prototype.skipSpaces = function () { while (this.i < this.s.length && /\s/.test(this.s[this.i])) this.i++; };
  Parser.prototype.expectEnd = function () {
    this.skipSpaces();
    if (this.i < this.s.length) throw new Error("Unexpected '" + this.s[this.i] + "' at position " + this.i);
  };

  function op(value, isPercent) { return { value: value, isPercent: !!isPercent }; }

  Parser.prototype.parseExpression = function () {
    let acc = this.parseTerm();
    for (;;) {
      this.skipSpaces();
      const c = this.peek();
      if (c === '+') {
        this.i++;
        const r = this.parseTerm();
        // a trailing percent of what is already there: 200 + 10% is 220
        acc = op(r.isPercent ? acc.value + acc.value * r.value : acc.value + r.value, false);
      } else if (c === '-') {
        this.i++;
        const r = this.parseTerm();
        acc = op(r.isPercent ? acc.value - acc.value * r.value : acc.value - r.value, false);
      } else {
        return acc;
      }
    }
  };

  Parser.prototype.parseTerm = function () {
    let acc = this.parseUnary();
    for (;;) {
      this.skipSpaces();
      const c = this.peek();
      if (c === '*') {
        this.i++;
        acc = op(acc.value * this.parseUnary().value, false);
      } else if (c === '/') {
        this.i++;
        const r = this.parseUnary();
        if (r.value === 0) throw new Error('Division by zero');
        acc = op(acc.value / r.value, false);
      } else if (c === null) {
        return acc;
      } else if (/[a-zA-Z(]/.test(c)) {
        // multiplication that was not written down: 2pi, 3(4+5)
        acc = op(acc.value * this.parseUnary().value, false);
      } else {
        return acc;
      }
    }
  };

  Parser.prototype.parseUnary = function () {
    this.skipSpaces();
    const c = this.peek();
    if (c === '-') { this.i++; const v = this.parseUnary(); return op(-v.value, v.isPercent); }
    if (c === '+') { this.i++; return this.parseUnary(); }
    return this.parsePower();
  };

  Parser.prototype.parsePower = function () {
    const base = this.parsePostfix();
    this.skipSpaces();
    if (this.peek() === '^') {
      this.i++;
      const exponent = this.parseUnary();
      return op(Math.pow(base.value, exponent.value), false);
    }
    return base;
  };

  Parser.prototype.parsePostfix = function () {
    let v = this.parseAtom();
    let percent = false;
    this.skipSpaces();
    while (this.peek() === '%') { this.i++; percent = true; this.skipSpaces(); }
    while (this.peek() === '!') { this.i++; v = factorial(v); this.skipSpaces(); }
    while (this.peek() === '\u00B0') { this.i++; v = (v * PI) / 180; this.skipSpaces(); }
    return op(percent ? v / 100 : v, percent);
  };

  Parser.prototype.parseAtom = function () {
    this.skipSpaces();
    const c = this.peek();
    if (c === null) throw new Error('Unexpected end of expression');
    if (c === '(') {
      this.i++;
      const v = this.parseExpression().value;
      this.skipSpaces();
      if (this.peek() !== ')') throw new Error("Missing ')'");
      this.i++;
      return v;
    }
    if (/[0-9.]/.test(c)) return this.parseNumber();
    if (/[a-zA-Z]/.test(c)) return this.parseIdentifier();
    throw new Error("Unexpected '" + c + "' at position " + this.i);
  };

  Parser.prototype.parseNumber = function () {
    const start = this.i;
    while (this.i < this.s.length && /[0-9.]/.test(this.s[this.i])) this.i++;
    if (this.i < this.s.length && /[eE]/.test(this.s[this.i])) {
      const save = this.i;
      this.i++;
      if (this.i < this.s.length && /[+-]/.test(this.s[this.i])) this.i++;
      if (this.i < this.s.length && /[0-9]/.test(this.s[this.i])) {
        while (this.i < this.s.length && /[0-9]/.test(this.s[this.i])) this.i++;
      } else {
        this.i = save;
      }
    }
    const text = this.s.substring(start, this.i);
    const v = Number(text);
    if (Number.isNaN(v)) throw new Error("Invalid number '" + text + "'");
    return v;
  };

  Parser.prototype.parseIdentifier = function () {
    const start = this.i;
    while (this.i < this.s.length && /[a-zA-Z0-9_]/.test(this.s[this.i])) this.i++;
    const name = this.s.substring(start, this.i).toLowerCase();
    this.skipSpaces();
    if (this.peek() === '(') {
      this.i++;
      const args = [];
      this.skipSpaces();
      if (this.peek() !== ')') {
        args.push(this.parseExpression().value);
        this.skipSpaces();
        while (this.peek() === ',') { this.i++; args.push(this.parseExpression().value); this.skipSpaces(); }
      }
      if (this.peek() !== ')') throw new Error("Missing ')' after " + name);
      this.i++;
      return this.applyFunction(name, args);
    }
    if (name === 'pi') return PI;
    if (name === 'e') return E;
    if (name === 'tau') return 2 * PI;
    if (name === 'phi') return (1 + Math.sqrt(5)) / 2;
    if (name === 'ans') return this.vars.ans || 0;
    if (Object.prototype.hasOwnProperty.call(this.vars, name)) return this.vars[name];
    // the caller may have stored its variables with capital letters
    for (const k of Object.keys(this.vars)) {
      if (k.toLowerCase() === name) return this.vars[k];
    }
    throw new Error("Unknown name '" + name + "'");
  };

  Parser.prototype.applyFunction = function (name, a) {
    const one = () => { if (!a.length) throw new Error(name + '() needs an argument'); return a[0]; };
    const two = () => { if (a.length < 2) throw new Error(name + '() needs two arguments'); return [a[0], a[1]]; };

    switch (name) {
      // trigonometric, in radians, which is what the app uses for formulas
      case 'sin': return Math.sin(one());
      case 'cos': return Math.cos(one());
      case 'tan': return Math.tan(one());
      case 'csc': return 1 / Math.sin(one());
      case 'sec': return 1 / Math.cos(one());
      case 'cot': return 1 / Math.tan(one());
      case 'sinc': { const x = one(); return x === 0 ? 1 : Math.sin(x) / x; }
      case 'asin': return Math.asin(one());
      case 'acos': return Math.acos(one());
      case 'atan': return Math.atan(one());
      case 'acot': { const x = one(); const r = Math.atan(1 / x); return x < 0 ? r + PI : r; }
      case 'asec': return Math.acos(1 / one());
      case 'acsc': return Math.asin(1 / one());
      case 'atan2': { const [y, x] = two(); return Math.atan2(y, x); }

      case 'sinh': return Math.sinh(one());
      case 'cosh': return Math.cosh(one());
      case 'tanh': return Math.tanh(one());
      case 'coth': return 1 / Math.tanh(one());
      case 'sech': return 1 / Math.cosh(one());
      case 'csch': return 1 / Math.sinh(one());
      case 'asinh': return Math.asinh(one());
      case 'acosh': return Math.acosh(one());
      case 'atanh': return Math.atanh(one());
      case 'acoth': { const x = one(); return 0.5 * Math.log((x + 1) / (x - 1)); }
      case 'asech': return Math.acosh(1 / one());
      case 'acsch': return Math.asinh(1 / one());

      case 'ln': return Math.log(one());
      case 'log': return a.length === 2 ? Math.log(a[1]) / Math.log(a[0]) : Math.log10(one());
      case 'log10': return Math.log10(one());
      case 'log2': return Math.log2(one());
      case 'log1p': return Math.log1p(one());
      case 'exp': return Math.exp(one());
      case 'exp2': return Math.pow(2, one());
      case 'expm1': return Math.expm1(one());

      case 'sqrt': return Math.sqrt(one());
      case 'cbrt': return Math.cbrt(one());
      case 'square': { const x = one(); return x * x; }
      case 'cube': { const x = one(); return x * x * x; }
      case 'nthroot': {
        const [x, n] = two();
        if (n === 0) throw new Error('nthroot() needs a non-zero root');
        if (x < 0 && Math.abs(n % 2) === 1) return -Math.pow(-x, 1 / n);
        if (x < 0) throw new Error('Even root of a negative number');
        return Math.pow(x, 1 / n);
      }
      case 'hypot': { const [x, y] = two(); return Math.hypot(x, y); }
      case 'pow': { const [x, y] = two(); return Math.pow(x, y); }

      case 'abs': return Math.abs(one());
      case 'floor': return Math.floor(one());
      case 'ceil': return Math.ceil(one());
      case 'round': return roundHalfAway(one());
      case 'trunc': { const x = one(); return x >= 0 ? Math.floor(x) : Math.ceil(x); }
      case 'frac': { const x = one(); return x - (x >= 0 ? Math.floor(x) : Math.ceil(x)); }
      case 'sign': { const x = one(); return x > 0 ? 1 : x < 0 ? -1 : 0; }
      case 'rint': return Math.round(one()) === one() ? one() : one();
      case 'clamp': { if (a.length < 3) throw new Error('clamp() needs three arguments'); return Math.min(Math.max(a[0], a[1]), a[2]); }
      case 'mround': { const [x, m] = two(); if (m === 0) throw new Error('mround() needs a non-zero multiple'); return roundHalfAway(x / m) * m; }
      case 'even': { const x = one(); return x === 0 ? 0 : (x > 0 ? 1 : -1) * 2 * Math.ceil(Math.abs(x) / 2); }
      case 'odd': { const x = one(); return (x < 0 ? -1 : 1) * (2 * Math.ceil((Math.abs(x) + 1) / 2) - 1); }

      case 'fact': return factorial(one());
      case 'ncr': { const [n, r] = two(); return combinations(n, r); }
      case 'npr': { const [n, r] = two(); return permutations(n, r); }
      case 'gcd': { const [x, y] = two(); return gcd(x, y); }
      case 'lcm': { const [x, y] = two(); return lcm(x, y); }
      case 'mod': { const [x, y] = two(); if (y === 0) throw new Error('Modulo by zero'); return x % y; }
      case 'isprime': return isPrime(Math.trunc(one())) ? 1 : 0;
      case 'nextprime': return nextPrime(Math.trunc(one()));
      case 'totient': return totient(Math.trunc(one()));
      case 'isqrt': return Math.floor(Math.sqrt(one()));
      case 'fib': return fibonacci(Math.trunc(one()));
      case 'catalan': return catalan(Math.trunc(one()));

      case 'sum': return a.reduce((s, v) => s + v, 0);
      case 'prod': return a.reduce((s, v) => s * v, 1);
      case 'count': return a.length;
      case 'mean': return a.length ? a.reduce((s, v) => s + v, 0) / a.length : 0;
      case 'min': return a.length ? Math.min(...a) : 0;
      case 'max': return a.length ? Math.max(...a) : 0;
      case 'range': return a.length ? Math.max(...a) - Math.min(...a) : 0;
      case 'median': return medianOf(a);
      case 'mode': return modeOf(a);
      case 'var': return varianceOf(a);
      case 'sd': return Math.sqrt(varianceOf(a));
      case 'geomean': {
        if (!a.length || a.some((v) => v <= 0)) throw new Error('geomean() needs positive values');
        return Math.exp(a.reduce((s, v) => s + Math.log(v), 0) / a.length);
      }
      case 'harmmean': {
        if (!a.length || a.some((v) => v === 0)) throw new Error('harmmean() needs non-zero values');
        return a.length / a.reduce((s, v) => s + 1 / v, 0);
      }

      case 'gamma': return gamma(one());
      case 'lgamma': return logGamma(one());
      case 'beta': { const [x, y] = two(); return beta(x, y); }
      case 'erf': return erf(one());
      case 'erfc': return erfc(one());
      case 'zeta': return zeta(one());
      case 'lambertw': return lambertW(one());
      case 'digamma': return digamma(one());

      case 'real': return one();
      case 'imag': return 0;
      case 'conj': return one();
      case 'arg': return one() >= 0 ? 0 : PI;

      case 'rand': return Math.random();
      case 'heaviside': { const x = one(); return x < 0 ? 0 : x > 0 ? 1 : 0.5; }
      case 'radians': return (one() * PI) / 180;
      case 'degrees': return (one() * 180) / PI;
      case 'deg2rad': return (one() * PI) / 180;
      case 'rad2deg': return (one() * 180) / PI;
      case 'pct': return one() / 100;

      default: throw new Error("Unknown function '" + name + "'");
    }
  };

  function evaluate(expression, vars) {
    const p = new Parser(String(expression), vars);
    const v = p.parseExpression().value;
    p.expectEnd();
    if (Number.isNaN(v)) throw new Error('Result is not a number');
    if (!Number.isFinite(v)) throw new Error('Result is infinite (overflow)');
    return v;
  }

  const api = { evaluate: evaluate, factorial: factorial, gcd: gcd, lcm: lcm };

  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.NexusEvaluator = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
