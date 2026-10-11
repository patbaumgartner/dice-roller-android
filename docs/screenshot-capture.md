# Capturing documentation screenshots

The images in `docs/screenshots/` are real captures from a running emulator.
CI (`scripts/check-screenshots.mjs`) requires these five files, each a PNG of
at least 10 KB:

| File | Content |
| - | - |
| `roller-light-rolled.png` | Roller in light theme, right after a roll |
| `roller-dark-rolled.png` | Roller in dark theme, right after a roll |
| `settings-light.png` | Settings screen, light theme |
| `settings-dark.png` | Settings screen, dark theme |
| `presets.png` | Presets screen |

## Quick start

1. Build the debug APK and start an emulator (any API 26+ image works; a
   hardware-accelerated one is much faster than a software-rendered one):

   ```bash
   export ANDROID_HOME="$HOME/Android/Sdk"
   ./gradlew :app:assembleDebug
   "$ANDROID_HOME/emulator/emulator" -avd <your-avd> -no-window -gpu swiftshader_indirect &
   ```

2. Run the capture script:

   ```bash
   scripts/capture-screenshots.sh
   ```

3. Review every image before committing. The script retries until a frame is
   larger than 50 KB (which filters out black frames during emulator start-up),
   but only a human can confirm the *content* is right: the menu may not have
   opened, a dialog may cover the screen, or the roll may not have registered.
   Re-capture individual screens as needed.

## How the script works

The script is a plain `bash` file with these phases:

1. **`wait_boot`** polls `sys.boot_completed` (up to 1 h — software-rendered
   emulators boot slowly).
2. **`ensure_app`** installs `app/build/outputs/apk/debug/app-debug.apk` if the
   package is missing. Build it first with `./gradlew :app:assembleDebug`.
3. **`launch_app`** starts `MainActivity` with `am start -W` and waits for
   Compose to render.
4. **`snap`** captures a frame with `adb exec-out screencap -p` and retries up
   to five times, discarding anything under 50 KB — a common symptom of a
   SwiftShader frame that has not yet composited.
5. Navigation between screens uses `input tap` at fixed pixel coordinates for
   a 1080×1920 screen (the menu icon sits at ~x=990, y=160; the roll button at
   ~x=540, y=1500). If your emulator has a different resolution, adjust the
   coordinates in the script.
6. Dark theme is switched with `adb shell cmd uimode night yes` and restored
   to `no` at the end.

## Tips for slow environments

- Software rendering (`-gpu swiftshader_indirect`, no KVM) is many times
  slower than hardware acceleration. Prefer a KVM-enabled host or a real
  device when you can.
- If a screencap keeps returning a tiny file (a few KB), the emulator is
  still compositing — wait and retry rather than lowering the 50 KB bar.
- If the app shows an "isn't responding" dialog, tap **Wait** rather than
  closing it; on a slow emulator this is expected, not a bug in the app.
- `adb exec-out screencap -p > file.png` is the most reliable single-frame
  capture; `adb shell screencap` can mangle binary output on some hosts.

## When to re-capture

Any change that alters what a screenshot shows — theme, typography, layout,
new screens, renamed controls — should end with refreshed images so the README
stays honest. The docs-sync check in CI verifies the files referenced by the
README exist, but not that they are current: re-run the capture script when the
UI changes.
