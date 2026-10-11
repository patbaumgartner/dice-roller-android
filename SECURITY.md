# Security Policy

## Supported versions

Dice Roller is pre-1.0 software. Security fixes are applied to the latest
commit on `main`; older commits and locally modified APKs are not supported.

## Report a vulnerability

Please use GitHub's private vulnerability reporting flow at:

<https://github.com/patbaumgartner/dice-roller-android/security/advisories/new>

Include the affected version or commit, Android version, impact, reproduction
steps, and any proposed mitigation.

You should receive an acknowledgement within seven days. Valid reports will be
investigated privately, fixed on supported code, and disclosed after affected
users have a reasonable opportunity to update.

## Scope notes

The app requests no network permission and holds no secrets beyond locally
saved dice presets, so the attack surface is deliberately small. Reports that
demonstrate a path to arbitrary code execution, escape from app-private
storage, or permission escalation through intent handling are in scope.
