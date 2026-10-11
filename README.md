# Dice Roller for Android

Dice Roller is a free, open-source, privacy-first dice roller for board games
and tabletop RPGs. Roll 1 to 40 dice at once, from a standard D4 up to a D100
or a fully custom range, with no ads, no tracking, and no network access.

It is a clean-room reimplementation of the feature set of a popular commercial
dice-roller app, rebuilt without ads, in-app purchases, analytics, or any other
dark patterns. The app has no network permission at all: it cannot call home
because it has no way to.

[![CI](https://github.com/patbaumgartner/dice-roller-android/actions/workflows/ci.yml/badge.svg)](https://github.com/patbaumgartner/dice-roller-android/actions/workflows/ci.yml)
[![Security](https://github.com/patbaumgartner/dice-roller-android/actions/workflows/security.yml/badge.svg)](https://github.com/patbaumgartner/dice-roller-android/actions/workflows/security.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

## Features

- Roll 1 to 40 dice at once
- Standard dice: D4, D6, D8, D10, D12, D20, D100
- Fully custom ranges, for example 0–100 or any span in between
- Roll by tap, or optionally by shaking the device
- Lock individual dice to keep their results between rolls
- Total of all dice shown after every roll
- Recent roll history
- Save up to 10 presets for quick access to a favourite setup
- Adjustable or disableable animation
- Sound effects and haptic feedback, both individually switchable off
- System-aware light and dark themes
- Fully accessible: every die and control carries a content description

## Privacy

- No INTERNET permission: the app cannot make a network connection
- No ads, no in-app purchases, no analytics, no tracking SDKs
- No account, no cloud, no "calling home" of any kind
- Presets and settings stay in app-private storage on the device
- Backups and device-to-device transfer of app data are disabled
- The only permission requested is `VIBRATE`, used for haptic feedback and
  fully switchable off in settings

## Requirements

- Android 8.0 (API 26) or newer
- JDK 17 to build
- Android SDK 37 with the matching build tools

The first build downloads the Gradle distribution and Android dependencies. It
can use several gigabytes of disk space in Gradle and Android SDK caches.

## Build and run

Gradle needs to know where the Android SDK is, from either an `ANDROID_HOME`
environment variable or an `sdk.dir` line in `local.properties`. Without one of
them the build stops with `SDK location not found`.

```bash
git clone https://github.com/patbaumgartner/dice-roller-android.git
cd dice-roller-android
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:assembleDebug
```

The resulting debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

The release APK produced by a source checkout is unsigned. Signing and
distribution remain the responsibility of the person building the app.

## Validation

Run the same primary gates used by CI:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
yamllint .
find . -name '*.sh' -not -path './*/build/*' -exec shellcheck {} +
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for the contributor workflow and
[docs/architecture.md](docs/architecture.md) for the design.

## License

Dice Roller is licensed under the [Apache License 2.0](LICENSE).
