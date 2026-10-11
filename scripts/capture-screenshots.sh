#!/usr/bin/env bash
# Capture the documentation screenshots in docs/screenshots/ from an emulator.
# Run with:  scripts/capture-screenshots.sh
# Requires a booted emulator (or boots one), the debug APK, and adb on PATH or
# ANDROID_HOME set. Idempotent: installs the APK if missing and retries each
# capture until it gets a real (non-black) frame.
#
# Produces the five images referenced by README.md:
#   roller-light-rolled.png  roller-dark-rolled.png
#   settings-light.png       settings-dark.png       presets.png
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
OUT="$ROOT/docs/screenshots"
PKG="com.patbaumgartner.android.diceroller"
ACT=".ui.MainActivity"

mkdir -p "$OUT"

wait_boot() {
  local i=0
  echo "[screenshot] waiting for boot..."
  while true; do
    local b
    b=$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    [[ "$b" == "1" ]] && break
    i=$((i+1))
    if [[ $i -gt 360 ]]; then
      echo "[screenshot] boot timeout" >&2; return 1
    fi
    sleep 10
  done
  echo "[screenshot] booted after ~$((i*10))s"
}

ensure_app() {
  if "$ADB" shell pm path "$PKG" >/dev/null 2>&1; then
    echo "[screenshot] app already installed"
  else
    echo "[screenshot] installing APK..."
    timeout 300 "$ADB" install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk" || {
      echo "[screenshot] install failed (will retry)" >&2; return 1
    }
  fi
}

launch_app() {
  "$ADB" shell am start -W -n "$PKG/$ACT" >/dev/null 2>&1 || true
  sleep 30
}

# tap x y: input taps are flaky right after boot, so retry until the screen
# content actually changes (verified by a cheap screencap size change).
tap() {
  local x="$1" y="$2"
  local before after
  before=$("$ADB" exec-out screencap -p 2>/dev/null | wc -c)
  "$ADB" shell input tap "$x" "$y"
  sleep 8
  local after
  after=$("$ADB" exec-out screencap -p 2>/dev/null | wc -c)
  if [[ "$before" == "$after" ]]; then
    "$ADB" shell input tap "$x" "$y"
    sleep 10
  fi
}

snap() {
  local name="$1"
  local path="$OUT/${name}.png"
  local tmp="/tmp/kilo-shot-${name}.png"
  local size=0
  local attempt
  for attempt in 1 2 3 4 5; do
    rm -f "$tmp"
    timeout 90 "$ADB" exec-out screencap -p > "$tmp" 2>/dev/null
    size=$(stat -c%s "$tmp" 2>/dev/null || echo 0)
    [[ $size -gt 50000 ]] && cp "$tmp" "$path" && echo "[screenshot] $name: $size bytes -> $path" && return 0
    echo "[screenshot] $name attempt $attempt: only $size bytes (retry in 15s)..."
    sleep 15
  done
  echo "[screenshot] WARN: $name failed after 5 attempts" >&2
  return 1
}

# tap_open_menu opens the top-bar menu (icon sits at ~x=990,y=160 on a
# 1080x1920 screen) and returns after the menu is visible.
tap_open_menu() {
  "$ADB" shell input tap 990 160
  sleep 10
}

# roll: tap the roll button at the centre of the lower half, wait for the
# roll animation, so the captured frame shows a rolled total.
tap_roll() {
  "$ADB" shell input tap 540 1500
  sleep 12
}

set_night_mode() {
  "$ADB" shell cmd uimode night "$1"
  sleep 20
}

main() {
  wait_boot || { echo "[screenshot] abort: no boot"; exit 1; }
  ensure_app || { echo "[screenshot] abort: install failed"; exit 1; }

  echo "[screenshot] launching app..."
  launch_app
  snap "roller-light-raw"

  echo "[screenshot] rolling for the 'rolled' frame..."
  tap_roll
  snap "roller-light-rolled"

  echo "[screenshot] settings screen (light)..."
  tap_open_menu
  "$ADB" shell input tap 540 800   # Settings entry in the menu
  sleep 15
  snap "settings-light"
  "$ADB" shell input keyevent KEYCODE_BACK
  sleep 10

  echo "[screenshot] presets screen..."
  tap_open_menu
  "$ADB" shell input tap 540 640   # Presets entry in the menu
  sleep 15
  snap "presets"
  "$ADB" shell input keyevent KEYCODE_BACK
  sleep 10

  echo "[screenshot] switching to dark mode..."
  set_night yes
  launch_app
  sleep 10
  snap "roller-dark-raw"

  echo "[screenshot] rolling for the dark 'rolled' frame..."
  tap_roll
  snap "roller-dark-rolled"

  set_night no
  echo "[screenshot] done — inspect docs/screenshots/ and adjust any retries."
}

main
