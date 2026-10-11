#!/usr/bin/env node
// Preview the nominal circular launcher mask (72dp of the 108dp adaptive canvas).
import { readFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = new URL('../', import.meta.url);
const foreground = readFileSync(new URL('app/src/main/res/drawable/ic_launcher_foreground.xml', root), 'utf8');
const colors = readFileSync(new URL('app/src/main/res/values/colors.xml', root), 'utf8');
const background = colors.match(/<color name="ic_launcher_background">(#[0-9A-Fa-f]{6})<\/color>/)?.[1];
if (!background) throw new Error('Launcher background color is missing.');
const paths = [...foreground.matchAll(/<path\b[^>]*\/>/g)].map(([path]) => {
  const fill = path.match(/android:fillColor="(#[0-9A-Fa-f]{6})"/)?.[1];
  const data = path.match(/android:pathData="([^"]+)"/)?.[1];
  if (!fill || !data) throw new Error('Expected a solid-color launcher vector path.');
  return `<path fill="${fill}" d="${data}"/>`;
});
if (paths.length === 0) throw new Error('Launcher artwork is empty.');
const maskSize = 400 * 72 / 108;
const maskInset = (400 - maskSize) / 2;
const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="${maskInset} ${maskInset} ${maskSize} ${maskSize}">
<defs><clipPath id="mask"><circle cx="200" cy="200" r="${maskSize / 2}"/></clipPath></defs>
<g clip-path="url(#mask)">
<circle cx="200" cy="200" r="${maskSize / 2}" fill="${background}"/>
${paths.join('\n')}
</g>
</svg>`;
const output = new URL('docs/screenshots/', root);
mkdirSync(output, { recursive: true });
const result = spawnSync('rsvg-convert', ['-o', fileURLToPath(new URL('launcher.png', output))], {
  input: svg,
  encoding: 'utf8',
});
if (result.error) throw new Error('Launcher preview requires rsvg-convert (librsvg2-bin).', { cause: result.error });
if (result.status !== 0) throw new Error(`Launcher render failed: ${result.stderr}`);
console.log('LAUNCHER PREVIEW RENDERED');
