# Architecture

Dice Roller is a single-module Jetpack Compose app built around a deliberate
split: all rolling logic is pure Kotlin with no Android imports, and only the
UI layer touches the framework. That keeps the rules of the game testable on
the JVM with plain JUnit.

## Layers

```
app/src/main/kotlin/com/patbaumgartner/android/diceroller/
├── domain/            Pure Kotlin, no Android imports
│   ├── model/         DiceModels.kt: DieType, DieConfig, RollEntry,
│   │                  RollSettings, DicePreset, enums
│   │                  Limits.kt: the 10-preset / 50-history / 1-40 dice /
│   │                  0-100 custom-range caps
│   └── usecase/       RollDiceUseCase.kt: the only place randomness happens
├── data/
│   └── settings/      SettingsRepository.kt: SharedPreferences + kotlinx
│                      serialization for settings and presets
├── feedback/          DiceSound.kt, DiceHaptics.kt: the only files touching
│                      ToneGenerator and the vibrator
├── sensor/            ShakeDetector.kt: the only file touching SensorManager
└── ui/                Compose UI
    ├── MainActivity.kt, RollerApp.kt (screen switcher + BackHandler)
    ├── RollerViewModel.kt: state holder
    ├── components/    DieFace.kt: pips for 1-6, numerals above, NUMBERS mode
    ├── screens/       RollerScreen, PresetsScreen, HistoryScreen,
    │                  SettingsScreen, DieLabel.kt
    ├── DiceRollerIcons.kt: the four vendored vector icons, wrapped as
    │                  ImageVector
    └── theme/         Color.kt, Font.kt (Manrope family), Theme.kt
                       (light + dark schemes, typography, DiceRollerAccents)
```

## Key decisions

**Pure domain.** `RollDiceUseCase` takes a `kotlin.random.Random`, so tests can
seed it and assert exact outcomes. `domain/` must never grow an
`android.*` import; that would silently make the rolling rules untestable
without a device. Product caps (`Limits`) live here too, so both the ViewModel
and the repository enforce the same numbers and both stay JVM-testable.

**No dependency injection framework.** `DiceRollerApp` builds a
`SettingsRepository` and a factory hands it to the single `RollerViewModel`.
No Hilt, no Koin.

**Persistence.** `SharedPreferences` plus kotlinx.serialization JSON. No
Room, no DataStore. Presets are capped at 10, history at 50 entries, in
memory; only settings and presets persist across launches. The caps live in
`domain/model/Limits.kt` so the ViewModel and repository share one truth.

**Rolling.** The use case receives the current `RollSettings` and a map of
locked dice (index to value). Locked values are copied verbatim; only the
unlocked indices draw a fresh random value from the config's
`normalizedRange()`. That helper orders inverted bounds and clamps them to the
supported custom bounds, so `Random.nextInt` never sees an empty or
out-of-support range.

**Custom ranges.** A custom die is a `DieConfig` built by `DieConfig.custom`,
which orders and clamps the bounds to `Limits.MIN_CUSTOM..MAX_CUSTOM` (0–100).
The settings screen exposes every `DieType`, including `CUSTOM`, and edits the
range with a `RangeSlider` that always keeps the lower bound at or below the
upper.

**History.** The ViewModel keeps this session's rolls in a `MutableStateFlow`,
newest first and capped by `Limits.cappedHistory` (50). `HistoryScreen` renders
them; clearing only drops in-memory state. History is deliberately not
persisted, so it never reaches storage or a backup.

**Animation.** `AnimationSpeed.durationMs` flows into `RollerScreen` and drives
the `AnimatedContent` `transitionSpec` for the total and the breakdown. `OFF`
is a zero-duration tween: the same code path, no motion.

**Sound and haptics.** `ui/RollerApp` owns the `DiceSound` and `DiceHaptics`
handles and routes both the roll button and a shake through one lambda, so the
`soundEnabled` and `vibrationEnabled` settings gate every roll identically.
`DiceSound` synthesises a short tone with `ToneGenerator` (no media assets, no
network); `DiceHaptics` is the only file that touches the vibrator. Both are
released when the composition leaves.

**Dice faces.** `DieFace` renders values 1–6 as pips on a real 3×3 grid with
the classic western layout (opposite faces sum to 7; the 3 is the diagonal).
Larger values render as numerals. The settings `DisplayMode` toggles between
pips (`DOTS`) and numerals (`NUMBERS`) via a `showPips` parameter.

**Navigation.** A four-value enum (`Screen`: Roller, Presets, History,
Settings) and a `Crossfade`-style `when` in `RollerApp`. No Navigation-Compose
dependency. A `BackHandler` maps system back on any sub-screen to returning to
the roller, so back never exits the app from a sub-screen.

**Sensing.** `ShakeDetector` is the single owner of `SensorManager`; the UI
never touches it. A shake is debounced with a 500 ms cooldown.

**Theming.** `DiceRollerTheme` picks a light or dark M3 color scheme from the
system setting; both schemes are defined in `Color.kt` and verified on the
emulator in both modes.

**Typography and design.** The app bundles the Manrope typeface (SIL Open Font
License 1.1) as four TTF weights in `res/font/`. `Font.kt` maps them into
`DiceRollerFontFamily`; `Theme.kt` builds `Typography` from that family and
carries the reference values — tight tracking (`-0.8.sp`) and a trimmed
line-height on `displayLarge`, a 20 sp `headlineSmall` with 26 sp line height —
so the two sibling apps share one visual language. Beyond the Material scheme,
`DiceRollerAccents` is a small data class of semantic accent colours provided
through a `CompositionLocal` (with a read-only accessor), so screens can reach
the brand accents without hard-coding swatches.

**Icons.** Four vector drawables (`ic_arrow_back`, `ic_delete_outline`,
`ic_more_horiz`, `ic_settings`) live in `res/drawable/`, derived from Material
Design icons and wrapped in `DiceRollerIcons.kt` as `ImageVector`s. The app
does not depend on `material-icons-extended`; that keeps the APK smaller and
avoids an artifact that is no longer released.

**Docs screenshots.** The images in `docs/screenshots/` are captured from a
running emulator with `scripts/capture-screenshots.sh`; see
[docs/screenshot-capture.md](screenshot-capture.md).

## Testing

The test pyramid mirrors the layer split:

- **JVM unit tests** (`app/src/test`, run by `testDebugUnitTest`): the whole
  `domain/` package — rolling rules with a seeded `Random`, model defaults and
  serialization round trips, and the history/preset caps. No Android imports,
  no mocking library, no Robolectric.
- **Instrumented UI tests** (`app/src/androidTest`, run by
  `connectedDebugAndroidTest`): Compose test rules drive the real
  `MainActivity` on a device or emulator — initial state, rolling, menu
  navigation to Presets, History and Settings, custom-range selection, and back
  navigation.
- **Static invariant checks** (`scripts/check-*.mjs`, run by CI with Node):
  assert the pip layouts, the display-mode/custom-range/history/animation/
  feedback wiring, the manifest's single `VIBRATE` permission, the screenshots,
  the documentation sync, and that CI keeps running them. `check-test-counts.mjs`
  parses the JUnit XML and fails on any failure, error, skip, or missing class.

## What is deliberately absent

- No INTERNET permission, no network stack, no analytics
- No ads, purchases, or rewards of any kind
- No Room, DataStore, Hilt, or Navigation libraries
- No Robolectric or mocking library — domain logic stays pure, UI tests run
  for real on an emulator
- No crash reporter and no logging
