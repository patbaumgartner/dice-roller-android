# Contributing

Thanks for improving Dice Roller. Small, focused changes with behavioural
tests are easiest to review.

## Ground rules

This project exists as a clean alternative to ad-supported dice apps. Do not
introduce advertising, analytics, tracking, network access, or any other
dark pattern. The app must remain able to run with no network permission at
all; a pull request that adds network access will be rejected.

## Set up a development environment

Install JDK 17 and Android SDK 37, then clone the repository. Android Studio
can import the root directory directly; command-line builds use the checked-in
Gradle wrapper and need `ANDROID_HOME` set, or an `sdk.dir` line in
`local.properties`.

```bash
git clone https://github.com/patbaumgartner/dice-roller-android.git
cd dice-roller-android
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew :app:assembleDebug
```

A real device is not required for unit tests or compilation.

## Make a change

1. Create a branch from `main`.
2. Keep the change scoped to one concern.
3. Add or update tests for changed behaviour. Keep the rolling logic in
   `domain/` free of Android imports so it stays unit-testable on the JVM.
4. Keep user-facing text in `app/src/main/res/values/strings.xml`.
5. Never commit `local.properties`, keystores, APKs, or generated build
   output.

Use Conventional Commit subjects, for example `feat: add dice lock` or
`fix: keep locked dice between rolls`.

## Validate

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
yamllint .
find . -name '*.sh' -not -path './*/build/*' -exec shellcheck {} +
node scripts/check-ci-wiring.mjs
git diff --check
```

That is the full CI gate. CI also runs the static invariant checks in
`scripts/` (including the design checks for fonts, icons, and theme) and
verifies unit-test results and required test classes. That check does not
measure code coverage. Run the matching
`node scripts/check-*.mjs` before claiming a change is done.

## Pull requests

Explain the user-visible effect, why the change is needed, and exactly how it
was tested. Include screenshots for visual changes when they make review
materially easier.
Refresh affected documentation images using the
[screenshot capture guide](docs/screenshot-capture.md). Launcher-only changes
can regenerate the preview with `node scripts/render-launcher-icon.mjs`,
without an emulator.

Security vulnerabilities belong in a private report as described in
[SECURITY.md](SECURITY.md), not a public issue.
