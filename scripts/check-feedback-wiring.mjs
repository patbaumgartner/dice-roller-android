#!/usr/bin/env node
// G10: sound and haptics play on a roll and are gated by their settings.
import { readFileSync, existsSync } from 'node:fs';

const fail = (msg) => { console.error(`FEEDBACK FAIL: ${msg}`); process.exit(1); };
const read = (p) => readFileSync(p, 'utf8');

const soundPath = 'app/src/main/kotlin/com/patbaumgartner/android/diceroller/feedback/DiceSound.kt';
const hapticsPath = 'app/src/main/kotlin/com/patbaumgartner/android/diceroller/feedback/DiceHaptics.kt';

if (!existsSync(soundPath)) fail('DiceSound.kt missing');
if (!existsSync(hapticsPath)) fail('DiceHaptics.kt missing');
if (!/ToneGenerator/.test(read(soundPath))) fail('DiceSound does not synthesise a tone');
if (!/VibrationEffect/.test(read(hapticsPath))) fail('DiceHaptics does not use VibrationEffect');

const app = read('app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/RollerApp.kt');
if (!/DiceSound\(\)/.test(app)) fail('RollerApp does not create DiceSound');
if (!/DiceHaptics\(/.test(app)) fail('RollerApp does not create DiceHaptics');
if (!/settings\.soundEnabled\) sound\.play\(\)/.test(app)) fail('sound is not gated by its setting');
if (!/settings\.vibrationEnabled\) haptics\.tick\(\)/.test(app)) fail('haptics are not gated by their setting');

console.log('FEEDBACK WIRED');
