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

Gradle needs the SDK location from `ANDROID_HOME` or `sdk.dir` in
`local.properties`, or it stops with `SDK location not found`. Builds on
JDK 17, same as CI.

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
yamllint .
find . -name '*.sh' -not -path './*/build/*' -exec shellcheck {} +
```

That is the full CI gate — run it before claiming a change is done.

## Build traps

- **Do not apply `org.jetbrains.kotlin.android`.** AGP 9 has Kotlin built in
  and the build fails with "no longer required since AGP 9.0". Exactly three
  plugins are applied; there is deliberately no
  `kotlin { compilerOptions { jvmTarget } }` block either.
- `app/build.gradle.kts` reads `local.properties` into debug-only `BuildConfig`
  fields, escaped by `asBuildConfigLiteral()`. Release re-declares them empty.
- The configuration cache is on; a change that reads files at configuration
  time can break it.

## Keep the core JVM-testable

There are no instrumentation, Robolectric or Compose tests, and that is
deliberate — it works only because the interesting logic has no Android
imports. Preserve that split:

- `domain/` (models and `RollDiceUseCase`) is pure Kotlin. Only it may call
  `kotlin.random.Random`; never scatter randomness through the UI.
- `data/settings/SettingsRepository` touches `SharedPreferences` but nothing
  else Android-specific.

Putting an `android.*` import into `domain/` silently makes the behaviour
untestable.

## Product invariants

- **No network.** The manifest must never gain `android.permission.INTERNET`
  or any SDK that could fetch code or phone home. A pull request adding
  network access is rejected by policy; see CONTRIBUTING.md.
- Rolling randomness lives only in `RollDiceUseCase`, which accepts a
  `Random` so tests can seed it.
- Locked dice keep their value: `RollDiceUseCase.roll` copies
  `lockedValues[index]` verbatim and never redraws it.
- Custom ranges are normalised (`minOf`/`maxOf`) before `nextInt`, because an
  inverted range throws `IllegalArgumentException`.
- Presets are capped at 10, history at 50 entries.

## Conventions

- User-facing text belongs in `strings.xml` (screen bodies currently use
  inline literals pending string extraction — do not add more).
- No logging at all — no `android.util.Log`, no `println`.
- State is `MutableStateFlow` + `asStateFlow()`, backing field `_name`.
  Mutate with `update { }`, never `value = value.copy(...)`.
- `internal` for module-private API, trailing commas, 120-column soft limit,
  `PascalCase` enum entries.
- KDoc only where the rationale is not obvious; comments explain *why*.
- Tests are JUnit 4 with backtick sentence names and no mocking library;
  assertions carry a message first.
- Conventional Commit subjects.

## Security invariants

- The manifest must keep exactly one permission: `VIBRATE`. A new permission
  is a review blocker.
- Backups and device transfer are disabled via `data_extraction_rules.xml`;
  keep that exclusion list complete.
- Never commit `local.properties`, keystores, or APKs.
