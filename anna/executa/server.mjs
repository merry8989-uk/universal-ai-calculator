#!/usr/bin/env node
/*
 * The Executa: an Anna plugin that speaks JSON-RPC 2.0 over stdio.
 *
 * One JSON object per line on stdin, one per line on stdout, logs to stderr.
 * That framing is the part I could not read from Anna's docs before writing this
 * (the protocol page was not reachable), so the handshake lives in `handle()`
 * alone and nothing else in the file knows what a request looks like - if the
 * real spec differs, it is one function to change.
 *
 * The tools are deliberately few and honest:
 *   calculate          - evaluate an expression, with the engine's own errors
 *   list_calculators   - the catalogue, so the assistant can pick one
 *   run_calculator     - run one by id, returning the answer *and the working*
 *   explain            - the same run, phrased for a person to read
 *
 * `working` is why this app can claim an AI-native core: every answer arrives
 * with the sum that produced it, so the assistant can check it rather than
 * repeat it.
 */

import { createRequire } from 'node:module';
import { createInterface } from 'node:readline';
import { catalogue, runCalculator } from './calculators.mjs';

const require = createRequire(import.meta.url);
const Engine = require('./evaluator.cjs');

const NAME = 'nexus-calc';
const VERSION = '0.1.0';

const TOOLS = [
  {
    name: 'calculate',
    description:
      'Evaluate a maths expression with a calculator grammar (^ for powers, sqrt, sin/cos/tan in degrees, factorial, %, constants pi and e). Use this for a sum somebody typed or read out.',
    inputSchema: {
      type: 'object',
      properties: {
        expression: { type: 'string', description: 'The expression, for example "12*(3+4)^2" or "sqrt(16)"' },
      },
      required: ['expression'],
    },
  },
  {
    name: 'list_calculators',
    description:
      'List the built-in calculators with their inputs and defaults. Call this before run_calculator when you are not certain which calculator fits.',
    inputSchema: { type: 'object', properties: {} },
  },
  {
    name: 'run_calculator',
    description:
      'Run a built-in calculator by id. Returns the answers and the working, so a result can be explained or checked step by step. Missing inputs fall back to the calculator defaults.',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'Calculator id from list_calculators, for example "gst_add"' },
        arguments: {
          type: 'object',
          description: 'Input values by key, for example {"amount": 1000, "gst": 18}',
          additionalProperties: true,
        },
      },
      required: ['id'],
    },
  },
  {
    name: 'explain',
    description:
      'Run a calculator and return the working as a numbered list a person can follow, for when somebody asks how an answer was reached.',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string' },
        arguments: { type: 'object', additionalProperties: true },
      },
      required: ['id'],
    },
  },
];

function text(value) {
  return { content: [{ type: 'text', text: typeof value === 'string' ? value : JSON.stringify(value, null, 2) }] };
}

function failure(message) {
  return { content: [{ type: 'text', text: message }], isError: true };
}

function callTool(name, args = {}) {
  if (name === 'calculate') {
    const expression = String(args.expression ?? '').trim();
    if (!expression) return failure('calculate needs an expression.');
    try {
      const value = Engine.evaluate(expression);
      return text({ expression, value });
    } catch (e) {
      // the engine's messages are written for a person, so they are passed on
      return failure(`Could not work that out: ${e.message}`);
    }
  }

  if (name === 'list_calculators') {
    return text({ calculators: catalogue() });
  }

  if (name === 'run_calculator') {
    const result = runCalculator(String(args.id ?? ''), args.arguments ?? {});
    return result.error ? failure(result.error) : text(result);
  }

  if (name === 'explain') {
    const result = runCalculator(String(args.id ?? ''), args.arguments ?? {});
    if (result.error) return failure(result.error);
    const lines = [
      `${result.title}`,
      ...Object.entries(result.inputs).map(([k, v]) => `  ${k} = ${v}`),
      '',
      ...result.working.map((step, i) => `${i + 1}. ${step}`),
      '',
      ...result.outputs.map((o) => `= ${o.label}: ${o.value}${o.unit ? ' ' + o.unit : ''}${o.note ? ` (${o.note})` : ''}`),
    ];
    return text(lines.join('\n'));
  }

  return failure(`Unknown tool "${name}". The tools are: ${TOOLS.map((t) => t.name).join(', ')}.`);
}

/** One request in, one response out - or nothing at all for a notification. */
export function handle(request) {
  const { id, method, params } = request ?? {};
  const reply = (result) => ({ jsonrpc: '2.0', id, result });
  const error = (code, message) => ({ jsonrpc: '2.0', id, error: { code, message } });

  switch (method) {
    case 'initialize':
      return reply({
        protocolVersion: '2024-11-05',
        serverInfo: { name: NAME, version: VERSION },
        capabilities: { tools: {} },
      });
    case 'notifications/initialized':
      return null;
    case 'ping':
      return reply({});
    case 'tools/list':
      return reply({ tools: TOOLS });
    case 'tools/call': {
      const name = params?.name;
      if (!name) return error(-32602, 'tools/call needs a name');
      return reply(callTool(name, params?.arguments ?? {}));
    }
    default:
      return error(-32601, `Unknown method "${method}"`);
  }
}

function serve() {
  const lines = createInterface({ input: process.stdin, crlfDelay: Infinity });
  lines.on('line', (line) => {
    const trimmed = line.trim();
    if (!trimmed) return;
    let request;
    try {
      request = JSON.parse(trimmed);
    } catch {
      process.stdout.write(JSON.stringify({ jsonrpc: '2.0', id: null, error: { code: -32700, message: 'Not JSON' } }) + '\n');
      return;
    }
    const response = handle(request);
    if (response) process.stdout.write(JSON.stringify(response) + '\n');
  });
}

// only serve when run directly, so the tests can import handle()
if (process.argv[1] && import.meta.url.endsWith(process.argv[1].split('/').pop())) {
  process.stderr.write(`${NAME} executa ${VERSION} listening on stdio\n`);
  serve();
}
