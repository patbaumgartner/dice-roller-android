#!/usr/bin/env node
// G7: a custom die range can be chosen and edited, and rolls inside it.
import { readFileSync } from 'node:fs';

const fail = (msg) => { console.error(`CUSTOM RANGE FAIL: ${msg}`); process.exit(1); };
const read = (p) => readFileSync(p, 'utf8');

const models = read('app/src/main/kotlin/com/patbaumgartner/android/diceroller/domain/model/DiceModels.kt');
const useCase = read('app/src/main/kotlin/com/patbaumgartner/android/diceroller/domain/usecase/RollDiceUseCase.kt');
const settings = read('app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/screens/SettingsScreen.kt');

if (!/fun normalizedRange\(\): IntRange/.test(models)) fail('DieConfig.normalizedRange missing');
if (!/fun custom\(minValue: Int, maxValue: Int\): DieConfig/.test(models)) fail('DieConfig.custom factory missing');
if (!/config\.normalizedRange\(\)/.test(useCase)) fail('RollDiceUseCase does not roll the normalized range');
if (!/DieType\.entries\.forEach/.test(settings)) fail('Settings does not offer every die type, including custom');
if (!/DieType\.CUSTOM/.test(settings)) fail('Settings does not branch on the custom type');
if (!/RangeSlider\(/.test(settings)) fail('Settings lacks the custom range slider');
if (!/DieConfig\.custom\(/.test(settings)) fail('Settings does not build a custom config from the slider');

console.log('CUSTOM RANGE WIRED');
