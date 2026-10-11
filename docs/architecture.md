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
│   └── usecase/       RollDiceUseCase.kt: the only place randomness happens
├── data/
│   └── settings/      SettingsRepository.kt: SharedPreferences + kotlinx
│                      serialization for settings and presets
├── sensor/            ShakeDetector.kt: the only file touching SensorManager
└── ui/                Compose UI
    ├── MainActivity.kt, RollerApp.kt (screen switcher)
    ├── RollerViewModel.kt: state holder
    ├── screens/       RollerScreen, PresetsScreen, SettingsScreen
    └── theme/         Color.kt, Theme.kt
```

## Key decisions

**Pure domain.** `RollDiceUseCase` takes a `kotlin.random.Random`, so tests can
seed it and assert exact outcomes. `domain/` must never grow an
`android.*` import; that would silently make the rolling rules untestable
without a device.

**No dependency injection framework.** `DiceRollerApp` builds a
`SettingsRepository` and a factory hands it to the single `RollerViewModel`.
No Hilt, no Koin.

**Persistence.** `SharedPreferences` plus kotlinx.serialization JSON. No
Room, no DataStore. Presets are capped at 10, history at 50 entries, in
memory; only settings and presets persist across launches.

**Rolling.** The use case receives the current `RollSettings` and a map of
locked dice (index to value). Locked values are copied verbatim; only the
unlocked indices draw a fresh random value in `[minValue, maxValue]`. Ranges
are normalised so an inverted custom range cannot crash `Random.nextInt`.

**Navigation.** A three-value enum (`Screen`) and a `Crossfade`-style `when`
in `RollerApp`. No Navigation-Compose dependency.

**Sensing.** `ShakeDetector` is the single owner of `SensorManager`; the UI
never touches it. A shake is debounced with a 500 ms cooldown.

## What is deliberately absent

- No INTERNET permission, no network stack, no analytics
- No ads, purchases, or rewards of any kind
- No Room, DataStore, Hilt, or Navigation libraries
- No crash reporter and no logging
