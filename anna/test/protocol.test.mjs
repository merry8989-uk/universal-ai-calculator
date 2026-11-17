/*
 * The wire, end to end: spawn the plugin, speak JSON-RPC over its stdin, read the
 * answers off its stdout. This is the test that would have caught every framing
 * mistake I could have made, because it does not import the handler - it uses the
 * same door Anna will.
 */
import test from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const server = join(here, '..', 'executa', 'server.mjs');

function session() {
  const child = spawn(process.execPath, [server], { stdio: ['pipe', 'pipe', 'pipe'] });
  let buffer = '';
  const waiting = [];
  child.stdout.on('data', (chunk) => {
    buffer += chunk.toString();
    let index;
    while ((index = buffer.indexOf('\n')) >= 0) {
      const line = buffer.slice(0, index).trim();
      buffer = buffer.slice(index + 1);
      if (!line) continue;
      const resolve = waiting.shift();
      if (resolve) resolve(JSON.parse(line));
    }
  });
  return {
    send(request) {
      return new Promise((resolve) => {
        waiting.push(resolve);
        child.stdin.write(JSON.stringify(request) + '\n');
      });
    },
    close() {
      child.stdin.end();
      child.kill();
    },
  };
}

test('a session handshakes, lists the tools and runs one', async () => {
  const s = session();
  try {
    const init = await s.send({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {} });
    assert.equal(init.result.serverInfo.name, 'nexus-calc');

    const list = await s.send({ jsonrpc: '2.0', id: 2, method: 'tools/list' });
    const names = list.result.tools.map((t) => t.name).sort();
    assert.deepEqual(names, ['calculate', 'explain', 'list_calculators', 'read_calculation', 'run_calculator']);

    const calc = await s.send({
      jsonrpc: '2.0', id: 3, method: 'tools/call',
      params: { name: 'calculate', arguments: { expression: '12*(3+4)^2' } },
    });
    assert.equal(JSON.parse(calc.result.content[0].text).value, 588);

    const run = await s.send({
      jsonrpc: '2.0', id: 4, method: 'tools/call',
      params: { name: 'run_calculator', arguments: { id: 'gst_add', arguments: { amount: 1000, gst: 18 } } },
    });
    const payload = JSON.parse(run.result.content[0].text);
    assert.equal(payload.outputs[1].value, 1180);
    assert.ok(payload.working.length >= 2);
  } finally {
    s.close();
  }
});

test('an unknown method and an unknown tool are errors, not silence', async () => {
  const s = session();
  try {
    const bad = await s.send({ jsonrpc: '2.0', id: 9, method: 'tools/nope' });
    assert.equal(bad.error.code, -32601);

    const tool = await s.send({
      jsonrpc: '2.0', id: 10, method: 'tools/call',
      params: { name: 'nope', arguments: {} },
    });
    assert.equal(tool.result.isError, true);
    assert.match(tool.result.content[0].text, /Unknown tool/);
  } finally {
    s.close();
  }
});

test('a bad expression comes back as a sentence, not a stack trace', async () => {
  const s = session();
  try {
    const bad = await s.send({
      jsonrpc: '2.0', id: 11, method: 'tools/call',
      params: { name: 'calculate', arguments: { expression: '2+' } },
    });
    assert.equal(bad.result.isError, true);
    assert.match(bad.result.content[0].text, /^Could not work that out:/);
    // a V8 frame is an indented "at ..." line; the word "at" in a sentence is fine
    assert.ok(!/\n\s+at /.test(bad.result.content[0].text), 'a stack frame leaked to the user');
  } finally {
    s.close();
  }
});

test('a spoken sum is read, worked out, and explained', async () => {
  const s = session();
  try {
    const read = await s.send({
      jsonrpc: '2.0', id: 21, method: 'tools/call',
      params: { name: 'read_calculation', arguments: { text: 'split 2400 between 4 with 10 percent tip' } },
    });
    const payload = JSON.parse(read.result.content[0].text);
    assert.equal(payload.id, 'tip_split');
    assert.equal(payload.outputs[2].value, 660);
    assert.ok(payload.working.length >= 3);
    assert.equal(payload.heard, 'split 2400 between 4 with 10 percent tip');

    const sum = await s.send({
      jsonrpc: '2.0', id: 22, method: 'tools/call',
      params: { name: 'read_calculation', arguments: { text: 'twelve times four' } },
    });
    assert.equal(JSON.parse(sum.result.content[0].text).value, 48);
  } finally {
    s.close();
  }
});

test('a sentence that is not a sum asks a question instead of guessing', async () => {
  const s = session();
  try {
    const read = await s.send({
      jsonrpc: '2.0', id: 23, method: 'tools/call',
      params: { name: 'read_calculation', arguments: { text: 'make me a sandwich' } },
    });
    assert.equal(read.result.isError, true);
    assert.match(read.result.content[0].text, /Ask which calculator they mean/);
  } finally {
    s.close();
  }
});
