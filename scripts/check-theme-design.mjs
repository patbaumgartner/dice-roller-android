#!/usr/bin/env node
// Design-fit: typography and colour scheme mirror the watchdog reference app.
import { readFileSync } from 'node:fs';

const fail = (msg) => { console.error(`TYPOGRAPHY FAIL: ${msg}`); process.exit(1); };

const theme = readFileSync(
  'app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/theme/Theme.kt', 'utf8',
);
const checks = [
  ['typography(DiceRollerFontFamily)', 'typography is built from the Manrope family'],
  ['lineHeightStyle = Trim', 'displayLarge does not apply the watchdog line-height trim'],
  ['letterSpacing = (-0.8).sp', 'displayLarge loses the watchdog tight tracking'],
  ['fontSize = 20.sp', 'headlineSmall does not match the reference size'],
  ['data class DiceRollerAccents', 'semantic accents missing'],
  ['CompositionLocalProvider', 'accents are not provided around MaterialTheme'],
  ['@Composable @ReadOnlyComposable get()', 'accents accessor is not a read-only composable'],
];
for (const [needle, why] of checks) {
  if (!theme.includes(needle)) fail(`Theme.kt: ${needle} missing`);
}
if (!/lineHeight = 50\.sp/.test(theme)) fail('displayLarge lineHeight does not match the reference');
if (!/lineHeight = 26\.sp/.test(theme)) fail('headlineSmall lineHeight does not match the reference');

const color = readFileSync(
  'app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/theme/Color.kt', 'utf8',
);
for (const swatch of ['BrandCyan', 'OnBrandCyan', 'BrandCyanDeep', 'BrandCyanLight', 'Porcelain', 'Ink', 'Graphite', 'Bone', 'Coral']) {
  if (!color.includes(`val ${swatchName(swatch)}`)) fail(`Color.kt is missing ${swatch}`);
}
function swatchName(name) { return name; }
if (!/val Coral = Color\(0xFFC2603F\)/.test(color)) fail('Coral drifts from the reference value');
if (!/val BrandCyan = Color\(0xFF00AFE1\)/.test(color)) fail('BrandCyan drifts from the reference');

console.log('THEME DESIGN OK');
