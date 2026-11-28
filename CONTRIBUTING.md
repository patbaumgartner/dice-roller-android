# Contributing

Thanks for improving Dice Roller. Small, focused changes with behavioural
tests are easiest to review.

## Ground rules

This project exists as a clean alternative to ad-supported dice apps. Do not
introduce advertising, analytics, tracking, network access, or any other
dark pattern. The app must remain able to run with no network permission at
all; a pull request that adds network access will be rejected.

## Set up a development environment

Install JDK 27 and Android SDK 37.1, then clone the repository. Android Studio
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

## Releases

Downloadable builds are published from `.github/workflows/release.yml` (see
[ADR-0001](docs/adr/0001-release-signing-and-distribution.md)):

- Every push to `main` rebuilds the **Snapshot** prerelease with a signed APK
  and `SHA256SUMS`.
- Pushing a signed tag `vX.Y.Z` publishes a **release**: signed APK + AAB +
  `SHA256SUMS`. The tag sets the version (`v1.0.0` → versionName `1.0.0`,
  versionCode `10000`):

  ```bash
  git tag -s v1.0.0 -m "v1.0.0"
  git push origin main v1.0.0
  ```

Snapshots upgrade stable releases and older snapshots. Direct Snapshot → Stable
installation is a blocked downgrade; myApps offers only a manual switch with a
data-loss warning, never an automatic uninstall.

Both jobs sign with the release key held in the repository secrets
`SR_KEYSTORE_BASE64`, `SR_KEYSTORE_PASSWORD`, `SR_KEY_ALIAS`, and
`SR_KEY_PASSWORD`; the workflow fails if they are missing. To set or rotate
the key, generate a PKCS12 keystore with `keytool` and upload it with
`gh secret set` — never commit keystore material. Keep one backup of the
keystore outside the repository (password manager / HSM); losing it means
updates can no longer be signed with the same key.

For local signed builds, either export the four `SR_*` environment variables
or create a git-ignored `keystore.properties` with `storeFile`,
`storePassword`, `keyAlias`, `keyPassword`, then run
`./gradlew :app:assembleRelease`.

## Validate

Full validation requires JDK 27, Android SDK Platform 37.1, Node.js, Bash, jq,
yamllint, and ShellCheck. Install these separately; Gradle does not install the
lint and workflow-test tools.

```bash
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease \
  assembleDebugAndroidTest bundleRelease
yamllint .
find . -name '*.sh' -not -path './*/build/*' -exec shellcheck {} +
for script in scripts/check-*.mjs; do node "$script"; done
node --test scripts/auto-merge-preflight.test.mjs
git diff --check
```

That is the full local gate. CI also runs the static invariant checks in
`scripts/` (including the design checks for fonts, icons and theme) and verifies
unit-test results and required test classes. That check does not measure code
coverage. Run the matching `node scripts/check-*.mjs` before claiming a change
is done.

## Pull requests

Explain the user-visible effect, why the change is needed, and exactly how it
was tested. Include screenshots for visual changes when they make review
materially easier.
Refresh affected documentation images using the
[screenshot capture guide](docs/screenshot-capture.md). Launcher-only changes
can regenerate the preview with `node scripts/render-launcher-icon.mjs`,
without an emulator.

The required hosted check is the final `verify` job, named
`Test, lint, and package`. It succeeds only when both `Build and unit tests` and
`Instrumented tests (emulator)` succeed; a green build job alone is not enough.
Before enabling Dependabot auto-merge, the workflow uses the metadata-readable,
paginated `rules/branches/main` API to require an active `main` ruleset with that
exact context. The endpoint accounts for enforcement and exclusions; unreadable
or missing rules fail closed. Classic branch protection alone is not visible to
this preflight and does not qualify. The `pull_request_target` workflow never
checks out or executes pull-request code.

Only the merge step uses `AUTO_MERGE_TOKEN`, scoped to this repository with
fine-grained **Contents write**, **Pull requests write**, and **Workflows write**
permissions. The step fails before merging if that secret is empty. Fetch-metadata
and the metadata-read preflight keep `GITHUB_TOKEN`. The automation token allows
post-merge pushes to trigger CI and snapshot release workflows instead of being
suppressed by `GITHUB_TOKEN` event-recursion protection.

Security vulnerabilities belong in a private report as described in
[SECURITY.md](SECURITY.md), not a public issue.
