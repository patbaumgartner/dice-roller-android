#!/usr/bin/env node
// G8: required screenshots exist in docs/screenshots and are non-trivial PNGs.
import { readFileSync, existsSync, statSync } from 'node:fs';
import { join } from 'node:path';

const DIR = 'docs/screenshots';
const fail = (msg) => { console.error(`SCREENSHOTS FAIL: ${msg}`); process.exit(1); };

const required = [
  'roller-light-rolled.png',
  'roller-dark-rolled.png',
  'settings-light.png',
  'settings-dark.png',
  'presets.png',
  'launcher.png',
];

if (!existsSync(DIR)) fail('docs/screenshots missing');
for (const name of required) {
  const p = join(DIR, name);
  if (!existsSync(p)) fail(`missing screenshot: ${name}`);
  const size = statSync(p).size;
  if (size < 10_000) fail(`screenshot too small to be real: ${name} (${size} bytes)`);
  const magic = readFileSync(p).subarray(0, 8);
  const isPng = magic[0] === 0x89 && magic[1] === 0x50 && magic[2] === 0x4e && magic[3] === 0x47;
  if (!isPng) fail(`not a PNG: ${name}`);
}

console.log('SCREENSHOTS PRESENT');
