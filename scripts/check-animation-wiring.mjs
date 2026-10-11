#!/usr/bin/env node
// G9: the animation-speed setting drives the roller transition duration.
import { readFileSync } from 'node:fs';

const fail = (msg) => { console.error(`ANIMATION FAIL: ${msg}`); process.exit(1); };
const roller = readFileSync('app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/screens/RollerScreen.kt', 'utf8');

if (!/animationDurationMs/.test(roller)) fail('RollerScreen has no animation duration');
if (!/settings\.animationSpeed\.durationMs/.test(roller)) fail('RollerScreen does not read animationSpeed');
if (!/transitionSpec\s*=\s*\{[\s\S]*?tween\(animationDurationMs\)/.test(roller)) {
  fail('AnimatedContent does not use the configured duration');
}
if (!/AnimatedContent\(/.test(roller)) fail('RollerScreen has no AnimatedContent');

console.log('ANIMATION WIRED');
