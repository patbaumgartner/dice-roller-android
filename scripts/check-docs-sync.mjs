#!/usr/bin/env node
// G9: documentation must match shipped behaviour — test pyramid, screenshots,
// and the important feature claims in README, architecture docs and AGENTS.md.
import { readFileSync, existsSync } from 'node:fs';

const fail = (msg) => { console.error(`DOCS FAIL: ${msg}`); process.exit(1); };

const readme = readFileSync('README.md', 'utf8');
const arch = readFileSync('docs/architecture.md', 'utf8');
const agents = readFileSync('AGENTS.md', 'utf8');

// README: screenshots section pointing at real files.
if (!/## Screenshots/.test(readme)) fail('README lacks a Screenshots section');
for (const shot of ['roller-light-rolled.png', 'roller-dark-rolled.png', 'settings-dark.png']) {
  if (!readme.includes(shot)) fail(`README does not reference ${shot}`);
  if (!existsSync(`docs/screenshots/${shot}`)) fail(`README references missing file docs/screenshots/${shot}`);
}
if (!/instrumented/.test(readme.toLowerCase()) && !/UI tests/.test(readme)) {
  fail('README does not describe the test pyramid');
}

// Architecture doc: documents the test layers and the display mode wiring.
if (!/testDebugUnitTest/.test(arch)) fail('architecture doc does not document unit test task');
if (!/connectedDebugAndroidTest|instrumented/.test(arch)) fail('architecture doc does not document instrumented tests');
if (!/DisplayMode|displayMode/.test(arch)) fail('architecture doc does not document display modes');
if (!/BackHandler/.test(arch)) fail('architecture doc does not document back handling');
for (const needle of ['DiceSound', 'DiceHaptics', 'HistoryScreen', 'custom range', 'animation']) {
  if (!new RegExp(needle, 'i').test(arch)) fail(`architecture doc does not document ${needle}`);
}

// README: the shipped feature set is described.
for (const needle of ['custom range', 'history', 'animation', 'sound', 'haptic']) {
  if (!new RegExp(needle, 'i').test(readme)) fail(`README does not describe ${needle}`);
}

// AGENTS.md: test command reality.
if (!/connectedDebugAndroidTest/.test(agents) && !/instrumented/.test(agents)) {
  fail('AGENTS.md does not mention instrumented tests');
}

console.log('DOCS IN SYNC');
