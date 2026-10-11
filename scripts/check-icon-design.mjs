#!/usr/bin/env node
// Design-fit: icons are vendored drawables and no screen pulls material-icons.
import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const fail = (msg) => { console.error(`ICONS FAIL: ${msg}`); process.exit(1); };

const drawable = 'app/src/main/res/drawable';
const vendored = readdirSync(drawable).filter((name) => /^ic_.+\.xml$/.test(name) && name !== 'ic_launcher_foreground.xml');
if (vendored.length === 0) fail('no vendored icon drawables found');

const iconsKt = readFileSync(
  'app/src/main/kotlin/com/patbaumgartner/android/diceroller/ui/DiceRollerIcons.kt', 'utf8',
);
if (!/internal object DiceRollerIcons/.test(iconsKt)) fail('DiceRollerIcons object missing');
if (!iconsKt.includes('ImageVector.vectorResource')) fail('DiceRollerIcons does not load drawables');
if (/androidx\.compose\.material\.icons/.test(iconsKt)) fail('DiceRollerIcons imports the material-icons library');

const ktRoot = 'app/src/main/kotlin/com/patbaumgartner/android/diceroller';
const files = [];
const walk = (dir) => {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) walk(path);
    else if (entry.name.endsWith('.kt')) files.push(path);
  }
};
walk(ktRoot);
for (const file of files) {
  const text = readFileSync(file, 'utf8');
  if (/androidx\.compose\.material\.icons/.test(text)) fail(`${file} still imports material-icons`);
  if (/Icons\.(Filled|AutoMirrored|Outlined|Default)\./.test(text)) {
    fail(`${file} still uses the Icons object`);
  }
}

const gradle = readFileSync('app/build.gradle.kts', 'utf8');
if (/material\.icons|material-icons/.test(gradle)) fail('build.gradle.kts still references material-icons');

const launcherForeground = readFileSync('app/src/main/res/drawable/ic_launcher_foreground.xml', 'utf8');
if (!/android:viewportWidth="400"[\s\S]*android:viewportHeight="400"/.test(launcherForeground)) {
  fail('launcher foreground must use the expected 400x400 design canvas');
}
const dieOutline = 'M144,104 h112 a40,40 0 0 1 40,40 v112 a40,40 0 0 1 -40,40 h-112 a40,40 0 0 1 -40,-40 v-112 a40,40 0 0 1 40,-40 z';
if (!launcherForeground.includes(dieOutline)) {
  fail('launcher die must occupy 48% of the canvas, leaving room for circular masks');
}
const launcherMonochrome = 'app/src/main/res/drawable/ic_launcher_monochrome.xml';
if (!existsSync(launcherMonochrome)) fail('launcher monochrome drawable missing');
const monochrome = readFileSync(launcherMonochrome, 'utf8');
const foregroundPaths = [...launcherForeground.matchAll(/android:pathData="([^"]+)"/g)].map((match) => match[1]);
if (!monochrome.includes(`android:pathData="${foregroundPaths.join(' ')}"`)) {
  fail('monochrome launcher must match the foreground outline and pip positions');
}
// The farthest point of the rounded die must stay inside Android's 66dp safe circle.
const cornerCenterOffset = 200 - (104 + 40);
const farthestPoint = Math.SQRT2 * cornerCenterOffset + 40;
if (farthestPoint >= 400 * 33 / 108) fail('launcher die exceeds the adaptive-icon safe circle');

const rollIcon = readFileSync(`${drawable}/ic_roll_dice.xml`, 'utf8');
if ((rollIcon.match(/android:rotation=/g) ?? []).length !== 2 || !rollIcon.includes('android:strokeLineCap="round"')) {
  fail('roll icon must show two tilted dice and motion strokes');
}
if (!iconsKt.includes('ImageVector.vectorResource(R.drawable.ic_roll_dice)')) fail('rolling-dice icon is not exposed');
const roller = readFileSync(`${ktRoot}/ui/screens/RollerScreen.kt`, 'utf8');
const control = roller.slice(roller.indexOf('private fun RollControl'));
if (!control.includes('DiceRollerIcons.RollDice') || !control.includes('.size(120.dp)')) {
  fail('roll control must use the rolling-dice icon in a 120dp circle');
}
if (!control.includes('contentDescription = stringResource(R.string.action_roll)')) {
  fail('icon-only roll control must keep its accessible action label');
}
for (const icon of ['ic_launcher.xml', 'ic_launcher_round.xml']) {
  const adaptiveIcon = readFileSync(`app/src/main/res/mipmap-anydpi/${icon}`, 'utf8');
  if (!adaptiveIcon.includes('@drawable/ic_launcher_monochrome')) {
    fail(`${icon} does not use the dedicated monochrome launcher drawable`);
  }
}

console.log('VENDORED ICONS OK');
