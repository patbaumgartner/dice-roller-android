#!/usr/bin/env node
// Design-fit: Manrope is bundled, wired into Font.kt, and licensed properly.
import { existsSync, readFileSync } from 'node:fs';

const fail = (msg) => { console.error(`FONT FAIL: ${msg}`); process.exit(1); };

for (const face of ['manrope_light', 'manrope_regular', 'manrope_medium', 'manrope_semibold']) {
  if (!existsSync(`app/src/main/res/font/${face}.ttf`)) fail(`missing font file ${face}.ttf`);
}
if (!existsSync('app/src/main/res/raw/manrope_license.txt')) fail('manrope_license.txt missing');

const fontKt = readFileSync(
  'app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/theme/Font.kt', 'utf8',
);
for (const weight of ['light', 'regular', 'medium', 'semibold']) {
  if (!fontKt.includes(`R.font.manrope_${weight}`)) fail(`Font.kt does not map manrope_${weight}`);
}
if (!/val DiceRollerFontFamily = FontFamily\(/.test(fontKt)) fail('DiceRollerFontFamily missing');

const notices = readFileSync('THIRD_PARTY_NOTICES.md', 'utf8');
if (!notices.includes('Manrope')) fail('THIRD_PARTY_NOTICES does not name Manrope');
if (!notices.includes('SIL Open Font License')) fail('THIRD_PARTY_NOTICES does not name the OFL');

console.log('FONT DESIGN OK');
