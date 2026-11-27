# Auto-entry refinement verification — 27 November 2026

## Changes

- Explicit capture choice, with pause as the default when no preference is stored.
- Individual supported source switches and access-settings shortcut.
- Passive notification behavior explained in setup, including pause versus revocation.
- Malformed extras handled without logging financial input; cancellation preserved.
- Direction-scoped reference plus amount used for new draft matching, and reference retained through review.
- Inbox schema 1 → 2 migration preserves old drafts without destructive fallback.
- Repository guard rejects notification mutation/action APIs in capture code.

## Checks

- Passed: `./gradlew compileDebugKotlin testDebugUnitTest --tests 'com.akshar.sms.*' lintDebug assembleDebug`.
- Passed: 42 SMS and inbox tests, including 6 passive-capture checks spanning simulated Android API 34 and 36, and legacy inbox migration/reopen.
- Passed: `./scripts/repo_guard.sh` and `git diff --check`.
- The prescribed unfiltered `./gradlew compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` was attempted. Its full test task stalled in a remaining worker after other workers completed and was stopped. No assertion failure was established for that stalled run; the full suite is not marked passed. No claim is made that this stall predates the change.
- The scoped tests were rerun after final test cleanup. They cover capture, parser/privacy filters, source settings, reference matching, and migration.

## Device limitation

No physical phone/watch is attached. User reports HONOR phone on Android 16 with HONOR Health, and missing watch notifications continue after AkSpend access is turned off. This does not establish or exclude an earlier persistent interaction. Source inspection found no notification dismissal, snooze, listener-hint or DND-changing call in capture. Simulator checks cannot prove vendor watch compatibility or restore a device's permissions/connection.

The changes are local and produce a debug APK; no device installation, release publication, watch reset or companion-setting change was performed. The recovery steps and physical test matrix are in `docs/auto-entry-checklist.md`.
