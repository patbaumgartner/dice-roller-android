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

console.log('VENDORED ICONS OK');
