#!/usr/bin/env node
// G8: roll history is exposed by the ViewModel and shown on its own screen.
import { readFileSync, existsSync } from 'node:fs';

const fail = (msg) => { console.error(`HISTORY FAIL: ${msg}`); process.exit(1); };
const read = (p) => readFileSync(p, 'utf8');

const viewModel = read('app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/RollerViewModel.kt');
const app = read('app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/RollerApp.kt');
const strings = read('app/src/main/res/values/strings.xml');
const screenPath = 'app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/screens/HistoryScreen.kt';

if (!/val history: StateFlow<List<RollEntry>>/.test(viewModel)) fail('ViewModel does not expose history');
if (!/fun clearHistory\(\)/.test(viewModel)) fail('ViewModel lacks clearHistory');
if (!existsSync(screenPath)) fail('HistoryScreen.kt missing');
if (!/R\.string\.screen_history/.test(read(screenPath))) fail('HistoryScreen does not title itself');
if (!/Screen\.History/.test(app)) fail('RollerApp does not define a History screen');
if (!/HistoryScreen\(/.test(app)) fail('RollerApp does not render HistoryScreen');
if (!/<string name="screen_history"/.test(strings)) fail('screen_history string missing');

console.log('HISTORY WIRED');
