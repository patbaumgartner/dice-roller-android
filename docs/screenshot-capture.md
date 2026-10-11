# Capturing documentation screenshots

The five app-screen images in `docs/screenshots/` are real emulator captures.
`launcher.png` is a rendered preview of the shipped launcher vector, not a
device screenshot. CI checks that all six files are PNGs of at least 10 KB;
it cannot establish whether a screen is current or correctly composed.

| File | Content |
| - | - |
| `roller-light-rolled.png` | Roller in light theme, after a roll |
| `roller-dark-rolled.png` | Roller in dark theme, after a roll |
| `settings-light.png` | Settings screen, light theme |
| `settings-dark.png` | Settings screen, dark theme |
| `presets.png` | Presets screen |
| `launcher.png` | Launcher artwork inside a cyan circular mask, 432 × 432 |

## Capture app screens

Use an English-language, disposable emulator running Android 8.0/API 26 or
newer. The script does not boot an emulator or clear app data. Start with
default settings (two D6 dice, dots) and no saved presets for the standard
documentation images. Do not use a personal device.

Build the latest APK, start your emulator from Android Studio, and run:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:assembleDebug
export ANDROID_SERIAL=emulator-5554  # use the serial shown by adb devices
scripts/capture-screenshots.sh
```

The script requires Bash, Node.js, Android platform tools, GNU `timeout`,
and `rsvg-convert` (the `librsvg2-bin` package on Debian/Ubuntu). It:

1. Renders the launcher preview from the current vector and background color.
2. Waits for the selected emulator to finish booting and its package and
   night-mode services to become available.
3. Always installs the current debug APK, even if the app is already installed.
4. Restarts the app, preserving settings and presets.
5. Finds controls by their English text or accessibility description using
   UI Automator, rather than fixed screen coordinates.
6. Captures the roller, settings, and presets in light mode, followed by the
   roller and settings in dark mode.
7. Restores the emulator's original night-mode setting, including on failure.

Review every image before committing: verify the correct screen, theme,
readable text, completed animations, visible Roll control, and absence of
system dialogs or keyboard overlays. Roll values are intentionally random.
A PNG size check alone does not prove any of these.

Software emulation without KVM can be very slow. The script allows up to
30 minutes for the device connection and another 30 minutes for boot, and
uses bounded retries for UI controls and captures. Prefer a hardware-
accelerated emulator when available.

## Refresh only the launcher preview

No emulator or APK is needed:

```bash
node scripts/render-launcher-icon.mjs
node scripts/check-screenshots.mjs
```

The renderer reads `ic_launcher_foreground.xml` and `ic_launcher_background`
from the Android resources. It previews the nominal 72 dp circular mask of
the 108 dp adaptive canvas as a 432 × 432 image. The die occupies 48% of the
design canvas with 26% margins, keeping it inside the 66 dp safe circle.
This is a documentation
preview; real launchers apply their own adaptive-icon masks and scaling.

## When to refresh

Refresh the affected images after changes to layout, typography, theme,
labels, or launcher artwork. The README, screenshot checks, and rendered
assets should change together. Validate the result with:

```bash
node scripts/check-screenshots.mjs
node scripts/check-docs-sync.mjs
shellcheck scripts/capture-screenshots.sh
git diff --check
```
