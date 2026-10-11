#!/usr/bin/env node
// G3: BackHandler returns sub-screens to the Roller instead of exiting the app.
import { readFileSync } from 'node:fs';

const FILE = 'app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/RollerApp.kt';
const text = readFileSync(FILE, 'utf8');
const fail = (msg) => { console.error(`BACK HANDLING FAIL: ${msg}`); process.exit(1); };

if (!text.includes('androidx.activity.compose.BackHandler')) fail('BackHandler import missing');
if (!/BackHandler\(enabled = screen != Screen\.Roller\)\s*\{\s*screen = Screen\.Roller\s*\}/.test(text.replace(/\r/g, ''))) {
  fail('BackHandler for non-Roller screens not found');
}

console.log('BACK HANDLING PRESENT');
