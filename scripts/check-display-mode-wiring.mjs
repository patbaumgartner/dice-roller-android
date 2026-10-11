#!/usr/bin/env node
// G2: NUMBERS display mode must be wired from settings into DieFace.
import { readFileSync } from 'node:fs';

const roller = readFileSync('app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/screens/RollerScreen.kt', 'utf8');
const die = readFileSync('app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/components/DieFace.kt', 'utf8');
const fail = (msg) => { console.error(`DISPLAY MODE FAIL: ${msg}`); process.exit(1); };

if (!roller.includes('settings.displayMode == DisplayMode.DOTS')) fail('RollerScreen does not derive showPips from displayMode');
if (!roller.includes('showPips = showPips')) fail('DiceGrid does not forward showPips to DieFace');
if (!/showPips:\s*Boolean\s*=\s*true/.test(die)) fail('DieFace lacks showPips parameter');
if (!/PipsOrNumber\(value = value, ink = faceInk\(accent\), showPips = showPips\)/.test(die)) fail('PipsOrNumber call does not pass showPips');
if (!/val positions = if \(showPips\) pipPositions\(value\) else emptySet\(\)/.test(die)) fail('PipsOrNumber does not honour showPips');

console.log('DISPLAY MODE WIRED');
