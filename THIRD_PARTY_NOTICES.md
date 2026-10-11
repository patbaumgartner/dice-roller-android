# Third-party notices

## Manrope

The Manrope font files in `app/src/main/res/font/` are copyright 2018 The
Manrope Project Authors (<https://github.com/sharanda/manrope>).

They are distributed under the SIL Open Font License, Version 1.1. The complete
license is included in
[`app/src/main/res/raw/manrope_license.txt`](app/src/main/res/raw/manrope_license.txt)
and is packaged with the application.

## AndroidX and Kotlin libraries

The AndroidX, Kotlin, and kotlinx.coroutines libraries are distributed under
the Apache License 2.0 - the same licence as this project, whose full text is
in [LICENSE](LICENSE). They are resolved from Google Maven and Maven Central
at build time and are not bundled in this repository.

## Material Design icons

The four navigation drawables (`ic_arrow_back`, `ic_delete_outline`,
`ic_more_horiz`, and `ic_settings`) in `app/src/main/res/drawable/` are
derived from the Material Design icons, copyright Google LLC, and are
distributed under the Apache License 2.0 - the same licence as this project,
whose full text is in [LICENSE](LICENSE).

They were generated from `androidx.compose.material:material-icons-extended`
so that the app does not depend on that artifact, which is no longer
released. The launcher drawables and `ic_roll_dice.xml` are original to this
project and are not covered by this notice.
