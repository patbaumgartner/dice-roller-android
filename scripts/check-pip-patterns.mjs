#!/usr/bin/env node
// G1: verify standard die pip patterns in DieFace.kt cover 1-6 with the
// classic layouts, including the diagonal 3, and that opposite faces sum to 7.
import { readFileSync } from 'node:fs';

const FILE = 'app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/components/DieFace.kt';
const text = readFileSync(FILE, 'utf8');
const fail = (msg) => { console.error(`PIP PATTERNS FAIL: ${msg}`); process.exit(1); };

const mapMatch = text.match(/StandardPipPositions[\s\S]*?= mapOf\(([\s\S]*?)\n\)/);
if (!mapMatch) fail('StandardPipPositions map not found');
const body = mapMatch[1];

const faceRe = /(\d+)\s+to\s+setOf\(([^)]*)\)/g;
const faces = {};
let m;
while ((m = faceRe.exec(body)) !== null) {
  const value = Number(m[1]);
  const coords = [...m[2].matchAll(/(\d+)\s+to\s+(\d+)/g)].map((c) => `${c[1]}:${c[2]}`);
  faces[value] = new Set(coords);
}

const expected = {
  1: ['1:1'],
  2: ['0:0', '2:2'],
  3: ['0:0', '1:1', '2:2'], // diagonal — the classic three
  4: ['0:0', '2:0', '0:2', '2:2'],
  5: ['0:0', '2:0', '1:1', '0:2', '2:2'],
  6: ['0:0', '2:0', '0:1', '2:1', '0:2', '2:2'],
};

for (const v of [1, 2, 3, 4, 5, 6]) {
  if (!faces[v]) fail(`face ${v} missing from StandardPipPositions`);
  const got = [...faces[v]].sort().join(',');
  const want = [...expected[v]].sort().join(',');
  if (got !== want) fail(`face ${v}: expected [${want}] got [${got}]`);
}

// Opposite faces of a real die sum to 7, so pip counts pair up as 1+6, 2+5, 3+4.
for (const [a, b] of [[1, 6], [2, 5], [3, 4]]) {
  if (faces[a].size + faces[b].size !== 7) fail(`faces ${a}+${b} must have 7 pips together (got ${faces[a].size}+${faces[b].size})`);
}

if (Object.keys(faces).length !== 6) fail(`expected exactly 6 faces, got ${Object.keys(faces).length}`);

console.log('PIP PATTERNS OK');
