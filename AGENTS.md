# Dice Roller — Agent Guide

Open-source, offline dice roller for Android. Start with [README.md](README.md)
for the product, [docs/architecture.md](docs/architecture.md) for the design,
and [CONTRIBUTING.md](CONTRIBUTING.md) for the contributor workflow. This file
only covers what those don't, or what is easy to get wrong.

This guide describes conventions as they are today. If the manual DI, the
single-ViewModel design, or the SharedPreferences persistence ever change,
correct this file in the same commit — a stale guide misleads rather than
merely ages.

## Build and validate

Validation requires JDK 27 (bytecode target 17), Android SDK Platform 37.1,
Node.js, Bash, jq, yamllint, and ShellCheck. Install these prerequisites
separately; Gradle does not install the lint and workflow-test tools. Gradle
finds the SDK from `ANDROID_HOME` or `sdk.dir` in `local.properties`.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease \
  assembleDebugAndroidTest bundleRelease
yamllint .
find . -name '*.sh' -not -path './*/build/*' -exec shellcheck {} +
for script in scripts/check-*.mjs; do node "$script"; done
node --test scripts/auto-merge-preflight.test.mjs
git diff --check
```

That is the core local gate — run it before claiming a change is done. CI also
runs the static invariant checks, including font, icon, and theme design, and
verifies unit-test results and required test classes. The test check does not
measure code coverage. Matching static checks use `node scripts/check-*.mjs`.
The required `Test, lint, and package` check is the final `verify` job: it
fails unless both `Build and unit tests` and `Instrumented tests (emulator)`
succeed. Instrumented UI tests locally need a running emulator:

```bash
./gradlew connectedDebugAndroidTest
```

## Build traps

- **Do not apply `org.jetbrains.kotlin.android`.** AGP 9 has Kotlin built in
  and the build fails with "no longer required since AGP 9.0". Exactly three
  plugins are applied; there is deliberately no
  `kotlin { compilerOptions { jvmTarget } }` block either.
- `app/build.gradle.kts` generates the escaped `GIT_COMMIT` build field; the
  debug `buildType` block is intentionally empty. If you add another field,
  escape its value before interpolation — a naive `"${value}"` lets a quote or
  newline break the generated source.
- The configuration cache is on; a change that reads files at configuration
  time can break it.

## Releases

Signed builds are published by `.github/workflows/release.yml` — see
CONTRIBUTING.md "Releases" and
`docs/adr/0001-release-signing-and-distribution.md`. The release signing
config in `app/build.gradle.kts` activates only when `SR_KEYSTORE_FILE` (or a
local git-ignored `keystore.properties`) is present, so plain checkouts keep
building unsigned release APKs. Never commit keystore material.

## Keep the core JVM-testable

The JVM suite (`testDebugUnitTest`) covers the whole `domain/` package and
ViewModel state transitions with plain JUnit 4 — no Robolectric, no mocking
library. The Compose UI is covered by instrumented tests in `app/src/androidTest`
(`connectedDebugAndroidTest`, needs an emulator). Preserve the split:

- `domain/` (models, `Limits` and `RollDiceUseCase`) is pure Kotlin. Only it
  may call `kotlin.random.Random`; never scatter randomness through the UI.
- `data/settings/SettingsRepository` touches `SharedPreferences` but nothing
  else Android-specific.

Putting an `android.*` import into `domain/` silently makes the behaviour
untestable. Shared caps live in `domain/model/Limits.kt` — never restate
10/50/1–40 as literals in the UI or repository.

## Product invariants

- **No network.** The manifest must never gain `android.permission.INTERNET`
  or any SDK that could fetch code or phone home. A pull request adding
  network access is rejected by policy; see CONTRIBUTING.md.
- Rolling randomness lives only in `RollDiceUseCase`, which accepts a
  `Random` so tests can seed it.
- Locked dice keep their value: `RollDiceUseCase.roll` copies
  `lockedValues[index]` verbatim and never redraws it.
- Custom ranges go through `DieConfig.custom` / `DieConfig.normalizedRange`,
  which order the bounds and clamp them to `Limits.MIN_CUSTOM..MAX_CUSTOM`
  (0–100). Never hand a raw range to `Random.nextInt`.
- Presets are capped at 10, history at 50 entries. The caps live in
  `domain/model/Limits.kt`.
- Dice counts from storage, presets, and callers go through
  `RollSettings.normalized()` before use. Reading presets never rewrites their
  stored JSON; malformed JSON still fails without being overwritten.
- Roll history is per session and in memory; it is never written to storage.
- Sound and haptics are gated by their settings and reached through the single
  roll lambda in `RollerApp`, so the button and a shake behave identically.
- Dice faces 1–6 use the classic pip layout (`DieFace.StandardPipPositions`);
  opposite faces sum to 7. Keep the patterns in `StandardPipPositions`, not
  ad-hoc rows.

## Conventions

- User-facing text belongs in `strings.xml`; the screen bodies use no inline
  literals.
- No logging at all — no `android.util.Log`, no `println`.
- State is `MutableStateFlow` + `asStateFlow()`, backing field `_name`.
  Mutate with `update { }`, never `value = value.copy(...)`.
- `internal` for module-private API, trailing commas, 120-column soft limit,
  `PascalCase` enum entries.
- KDoc only where the rationale is not obvious; comments explain *why*.
- Tests are JUnit 4 with backtick sentence names and no mocking library;
  assertions carry a message first.
- Conventional Commit subjects.
- `scripts/check-*.mjs` are the project's static invariants and run in CI.
  When a change touches pip layout, display mode, custom ranges, history,
  animation, feedback, the manifest or the docs, update the matching check in
  the same commit — a stale check fails the pipeline.

## Security invariants

- The manifest must keep exactly one permission: `VIBRATE`. A new permission
  is a review blocker. `scripts/check-security-invariants.mjs` enforces this
  and the absence of `INTERNET`.
- Backups and device transfer are disabled via `data_extraction_rules.xml`;
  keep that exclusion list complete.
- Never commit `local.properties`, keystores, or APKs.
