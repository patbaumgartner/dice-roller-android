#!/usr/bin/env node
// G11: the manifest keeps exactly VIBRATE and never gains INTERNET, backups
// stay disabled, and build secrets stay out of git.
import { readFileSync } from 'node:fs';

const fail = (msg) => { console.error(`SECURITY FAIL: ${msg}`); process.exit(1); };
const read = (p) => readFileSync(p, 'utf8');

const manifest = read('app/src/main/AndroidManifest.xml');
const permissions = [...manifest.matchAll(/<uses-permission[^>]*android:name="([^"]+)"/g)].map((m) => m[1]);
if (permissions.length !== 1) fail(`expected exactly one permission, found ${permissions.length}: ${permissions.join(', ')}`);
if (permissions[0] !== 'android.permission.VIBRATE') fail(`only VIBRATE is allowed, found ${permissions[0]}`);
if (/android\.permission\.INTERNET/.test(manifest)) fail('manifest must never request INTERNET');
if (!/android:allowBackup="false"/.test(manifest)) fail('backups must stay disabled');
if (!/android:dataExtractionRules="@xml\/data_extraction_rules"/.test(manifest)) fail('data extraction rules not wired');

const rules = read('app/src/main/res/xml/data_extraction_rules.xml');
for (const domain of ['root', 'file', 'database', 'sharedpref']) {
  const cloud = new RegExp(`<cloud-backup>[\\s\\S]*?<exclude domain="${domain}"`);
  const transfer = new RegExp(`<device-transfer>[\\s\\S]*?<exclude domain="${domain}"`);
  if (!cloud.test(rules)) fail(`cloud-backup does not exclude ${domain}`);
  if (!transfer.test(rules)) fail(`device-transfer does not exclude ${domain}`);
}

const gitignore = read('.gitignore');
for (const entry of ['local.properties', '*.apk', '*.jks', '.gradle/']) {
  if (!gitignore.includes(entry)) fail(`.gitignore missing ${entry}`);
}

console.log('SECURITY INVARIANTS OK');
