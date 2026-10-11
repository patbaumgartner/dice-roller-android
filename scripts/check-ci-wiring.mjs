#!/usr/bin/env node
// G14: the pipeline actually runs the static invariant checks and the
// unit-test coverage check, so drift fails the build instead of the review.
import { readFileSync } from 'node:fs';

const fail = (msg) => { console.error(`CI WIRING FAIL: ${msg}`); process.exit(1); };
const ci = readFileSync('.github/workflows/ci.yml', 'utf8');

const required = [
  'check-security-invariants.mjs',
  'check-pip-patterns.mjs',
  'check-display-mode-wiring.mjs',
  'check-backhandler.mjs',
  'check-custom-range-wiring.mjs',
  'check-history-wiring.mjs',
  'check-animation-wiring.mjs',
  'check-feedback-wiring.mjs',
  'check-docs-sync.mjs',
  'check-screenshots.mjs',
  'check-ci-wiring.mjs',
  'check-test-counts.mjs',
];

for (const script of required) {
  if (!ci.includes(script)) fail(`ci.yml does not run ${script}`);
}
if (!/yamllint \./.test(ci)) fail('ci.yml does not run yamllint');
if (!/shellcheck/.test(ci)) fail('ci.yml does not run shellcheck');

console.log('CI WIRING OK');
