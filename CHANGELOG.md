# Changelog

All notable changes are documented here. The project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

No public release has been published yet.

### Added

- Roll 1 to 40 dice, from standard D4 to D100 or a custom 0–100 range
- A custom-range editor in Settings with a slider that keeps the bounds ordered
- Tap to roll, with optional shake-to-roll; both honour the sound and haptics settings
- Per-die locking so individual results survive the next roll
- Roll total and a History screen with this session's recent rolls (up to 50)
- Up to 10 saved presets for quick access
- Display mode, adjustable animation speed, sound, and haptic feedback settings
- System-aware light and dark themes
- CI, CodeQL, dependency review, and Dependabot automation
- Static invariant checks under `scripts/` that run in CI and guard the pip
  layout, feature wiring, manifest permissions, screenshots, and docs
- Manrope typeface bundled in four weights (SIL Open Font License 1.1), with
  typography, accent colours, and vendored icons matching the
  esp32-room-watchdog-android companion app
- An open-source-licenses dialog in Settings
- Design invariant checks (font, icon, theme) that run in CI
- `scripts/capture-screenshots.sh` to regenerate the documentation
  screenshots from an emulator

### Fixed

- `DicePreset.createdAt` no longer regenerates on deserialization, which made
  the serialization round-trip test fail
