#!/usr/bin/env bash
# Capture the five app screens and render the launcher preview used in README.md.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB=("$ANDROID_HOME/platform-tools/adb")
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB+=(-s "$ANDROID_SERIAL")
fi
OUT="$ROOT/docs/screenshots"
PKG="com.patbaumgartner.android.diceroller"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
TMP="$(mktemp -d)"
ORIGINAL_NIGHT=""

cleanup() {
  local result=$?
  if [[ -n "$ORIGINAL_NIGHT" ]]; then
    if ! "${ADB[@]}" shell cmd uimode night "$ORIGINAL_NIGHT"; then
      echo "Failed to restore the emulator's original night mode." >&2
      result=1
    fi
  fi
  rm -f "$TMP/window.xml" "$TMP/frame.png"
  rmdir "$TMP"
  return "$result"
}
trap cleanup EXIT

node "$ROOT/scripts/render-launcher-icon.mjs"
[[ -f "$APK" ]] || { echo "Build the debug APK first: ./gradlew assembleDebug" >&2; exit 1; }
timeout 1800 "${ADB[@]}" wait-for-device
[[ $("${ADB[@]}" shell getprop ro.kernel.qemu | tr -d '\r') == "1" ]] || {
  echo "Use a disposable emulator, not a personal device." >&2
  exit 1
}
ready=false
for ((attempt = 0; attempt < 180; attempt++)); do
  if [[ $("${ADB[@]}" shell getprop sys.boot_completed | tr -d '\r') == "1" ]] &&
      "${ADB[@]}" shell service check package | grep -q ': found' &&
      "${ADB[@]}" shell service check uimode | grep -q ': found'; then
    ready=true
    break
  fi
  sleep 10
done
[[ "$ready" == true ]] || {
  echo "Emulator boot or Android service startup timed out." >&2
  exit 1
}
night_mode=$("${ADB[@]}" shell cmd uimode night | sed -n 's/^Night mode: //p' | tr -d '\r')
[[ "$night_mode" =~ ^(yes|no|auto|custom)$ ]] || {
  echo "Could not read the original night mode." >&2
  exit 1
}
ORIGINAL_NIGHT="$night_mode"

# Always reinstall so captures cannot silently show an older build.
timeout 300 "${ADB[@]}" install -r "$APK"
"${ADB[@]}" shell input keyevent KEYCODE_WAKEUP
"${ADB[@]}" shell wm dismiss-keyguard
"${ADB[@]}" shell am force-stop "$PKG"
"${ADB[@]}" shell cmd uimode night no
"${ADB[@]}" shell am start -W -n "$PKG/.ui.MainActivity"

tap_node() {
  local attribute="$1" label="$2" coordinates
  for ((attempt = 0; attempt < 12; attempt++)); do
    if ! timeout 90 "${ADB[@]}" shell uiautomator dump /sdcard/window.xml >/dev/null; then
      echo "UI hierarchy unavailable; retrying $label ($((attempt + 1))/12)." >&2
      sleep 3
      continue
    fi
    "${ADB[@]}" exec-out cat /sdcard/window.xml > "$TMP/window.xml"
    coordinates=$(node - "$TMP/window.xml" "$attribute" "$label" <<'JS'
const fs = require('node:fs');
const [file, attribute, label] = process.argv.slice(2);
const nodes = fs.readFileSync(file, 'utf8').match(/<node\b[^>]*>/g) ?? [];
for (const node of nodes) {
  if (!node.includes(`${attribute}="${label}"`)) continue;
  const bounds = node.match(/bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/);
  if (!bounds) continue;
  const [, left, top, right, bottom] = bounds.map(Number);
  if (right <= left || bottom <= top) continue;
  process.stdout.write(`${Math.round((left + right) / 2)} ${Math.round((top + bottom) / 2)}`);
  break;
}
JS
)
    if [[ -n "$coordinates" ]]; then
      local x y
      read -r x y <<< "$coordinates"
      "${ADB[@]}" shell input tap "$x" "$y"
      sleep 3
      return
    fi
    sleep 3
  done
  echo "Could not find $attribute=$label. Use an English-language emulator." >&2
  return 1
}

snap() {
  local name="$1"
  sleep 5
  for ((attempt = 0; attempt < 5; attempt++)); do
    timeout 90 "${ADB[@]}" exec-out screencap -p > "$TMP/frame.png"
    if [[ $(stat -c%s "$TMP/frame.png") -ge 10000 ]]; then
      cp "$TMP/frame.png" "$OUT/$name.png"
      echo "[screenshot] $name"
      return
    fi
    sleep 5
  done
  echo "Screenshot $name was empty or too small; inspect the emulator." >&2
  return 1
}

open_screen() {
  tap_node content-desc "More options"
  tap_node text "$1"
}

tap_node content-desc "Roll"
snap roller-light-rolled
open_screen Settings
snap settings-light
tap_node content-desc Back
open_screen Presets
snap presets
tap_node content-desc Back
"${ADB[@]}" shell cmd uimode night yes
tap_node content-desc "Roll"
snap roller-dark-rolled
open_screen Settings
snap settings-dark
"${ADB[@]}" shell rm -f /sdcard/window.xml
echo "[screenshot] done — inspect all images in docs/screenshots/"
