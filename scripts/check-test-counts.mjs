#!/usr/bin/env node
// G5: whole JVM test suite passes and includes the required test classes.
// Parses Gradle JUnit XML results; this verifies execution, not code coverage.
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const resultsDir = 'app/build/test-results/testDebugUnitTest';
const fail = (msg) => {
  console.error(`TESTS FAIL: ${msg}`);
  process.exit(1);
};

if (!existsSync(resultsDir)) fail('unit test results missing — run ./gradlew testDebugUnitTest first');

let total = 0;
let failures = 0;
let errors = 0;
let skipped = 0;
const classes = new Set();

for (const f of readdirSync(resultsDir).filter((name) => name.endsWith('.xml'))) {
  const xml = readFileSync(join(resultsDir, f), 'utf8');
  const count = (re) => (xml.match(re) ?? []).length;
  total += count(/<testcase[\s>]/g);
  failures += count(/<failure[\s>]/g);
  errors += count(/<error[\s>]/g);
  skipped += count(/<skipped[\s/>]/g);
  for (const cls of xml.matchAll(/<testsuite[^>]*\sname="([^"]+)"/g)) {
    classes.add(cls[1]);
  }
}

if (total === 0) fail('no testcases found in results');
if (failures > 0) fail(`${failures} failing test(s)`);
if (errors > 0) fail(`${errors} errored test(s)`);
if (skipped > 0) fail(`${skipped} skipped test(s)`);

const classList = [...classes].join(' ');
for (const required of [
  'RollDiceUseCaseTest',
  'DieConfigTest',
  'DiceModelsTest',
  'LimitsTest',
  'RollerViewModelTest',
]) {
  if (!classList.includes(required)) fail(`expected test class not found: ${required}`);
}

console.log(`TESTS ALL PASS (${total} tests, ${classes.size} classes, 0 failures)`);
