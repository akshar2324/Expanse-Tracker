# AkSpend release E2E — November 2026

Run date: 7 November 2026 (India). Device run stopped at the owner’s request after the phone was disconnected. Host validation is complete; phone retests remain pending.

Device: physical Honor ELI-NX9, Android 16, USB authorized.
Build: com.akshar.akspend 1.4.0 (6), R8-minified release. Installed APK SHA-256 matches the supplied release artifact: `3e7f3fbcf170954789df69bec649f0867de60fe2f7a9580722d9e688d1e94a1c`.
Only synthetic demo financial entries were entered. Existing app data was not cleared or uninstalled.

| Scenario | Result | Observations |
|---|---|---|
| 1. Onboarding and transactions | Financial workflow passed; copy fixed in code, phone retest pending | Five expenses: Food 45 (`120/3+5`), Grocery 80, Transportation 35, Fuel 20, Shopping 60. Income: Salary 500, Freelance 150. Home: income 650, spending 240, net 410. Onboarding incorrectly advertises a default PIN. |
| 2. Accounts and transfer | Passed | Added Demo Bank (Bank, 1000) and Demo Cash (Cash, 200). Transferred 125 Bank → Cash; balances 875 and 325. Existing Cash remains 410; combined 1610. |
| 3. Budgets | Passed | Monthly Food 100: spent 45, left 55. Weekly Grocery 200: spent 80, left 120. Budgets and Home show 175 left. |
| 4. Activity | Passed | Grocery search returns one 80 expense. Expense + Cash account + This month + 30–90 amount returns four entries totaling 220. Saved as Demo November, cleared, reopened with same four filters/results, cleared again. |
| 5. Insights | Passed after adding repeat history | Spending forecast 240 / 7 × 30 ≈ 1029 and daily burn 34; category and cash-flow charts sensible, transfers excluded. Home forecast requests more history. After scenario 10, detects DEMO STREAM at 99 monthly, seen 3 times. Home expands to show balance 1027, expected spending 158.13, projected 868.87 (headline rounded to about 870). |
| 6. App lock | Passed (onboarding copy tracked separately) | Fresh lock off/disabled with no PIN; set demo PIN 2468; five wrong 0000 attempts produce ~30-second keypad wait; correct PIN works after wait. Launcher quick return after 2 seconds remains unlocked; launcher return after 33 seconds locks. App lock and fingerprint left off. |
| 7. Bank SMS | Passed | Shared sample opens a 232.00 expense review with KUMAR GENERAL STORE, 14 Oct 2026 and account suffix 4821. Exact OTP sample is rejected. More → Import bank SMS also rejects manually entered OTP text. Also verified actual Paste after copying only the supplied demo message within AkSpend; the prior clipboard was not read. One demo SMS expense was saved during testing; total all-time spending then becomes 472. |
| 8. Auto-import | Setup passed; real capture untested | Disclosure appears in-app before settings. Notification access for AkSpend was already enabled in system settings; no other settings were changed. Return shows On and Nothing to review. No real notification triggered. |
| 9. Backup and restore | Passed | Selected Download/AkSpend-E2E via system folder picker. Automatic JSON backup contained 10 transactions. Changed Freelance income from 150 to 175; picked JSON, confirmed replacement, verified 150 and received total 650 restored. Automatic backups left paused. |
| 10. CSV import | Passed | Six demo rows (Date, Amount, Description) imported as expenses. Second read of the identical file shows Already imported from a statement on all six rows; all unchecked. No second insertion performed. |
| 11. Widgets | Manual | Launcher placement and interaction not exercised. ADB does not supply a launcher-neutral widget placement workflow; budget and quick-add widgets need manual launcher testing. |
| 12. Large text | Clipping found; code fix, phone retest pending | At font scale 2.0, Home, Activity and Add are navigable. Month labels, bottom navigation words, and long category names truncate. Font scale restored to 1.0. |
| 13. Rotation / process death | Rotation code fix, phone retest pending; process death passed | Portrait draft: 77, Grocery, Demo rotation. Landscape initially displays a blank amount; portrait restores 77. Confirmed am kill removed process after temporarily disabling only AkSpend notification-listener access; cold reopen restored draft. Listener access restored immediately afterward. |

## Confirmed failure: onboarding advertises default PIN

Steps: first launch → Continue → Continue → currency selection.
Observed: “The app starts locked with passcode 1234…”
Expected: explain that lock is off until an owner-selected passcode is set.
Cause: stale literal in `OnboardingScreen.kt`; `AppLock` already implements opt-in lock with no default PIN.
Screenshot: [currency step](release-e2e-2026-11/01-default-passcode-onboarding.png).
Log: [scenario 1](release-e2e-2026-11/01-onboarding-transactions.log).
Stack trace: none; this is a copy bug, not a crash.
Fix branch: `test/release-e2e-fixes`. Copy corrected in code and covered by `OnboardingLockTest`; updated-release phone retest pending.

## Evidence and videos

Screenshots and per-scenario logs: `release-e2e-2026-11/`.

> The evidence folder is not committed. `docs/` is published to GitHub Pages, and the recordings and logcat come from a personal phone, so they stay on the tester's machine. Links to files in `release-e2e-2026-11/` work only in that local copy.
Videos: [17 clips, 17.8 minutes](release-e2e-2026-11/videos/README.md), portrait MP4, 720 × 1600, no audio. Evidence also includes 70 verified PNG screenshots and 18 scenario log files. Filming began with Accounts after the owner's additional request, so the original onboarding and first seven transactions were not recorded. Clips are test footage and include pauses and navigation retries; they are not a finished tutorial. Recording stops before launcher or system-screen transitions.

The component in the supplied ADB example (`com.akshar.akspend/.MainActivity`) does not exist; use `com.akshar.akspend/com.akshar.MainActivity`. For lifecycle resume tests, use launcher action/category plus flags `0x10200000` to bring the existing task forward instead of creating another activity.

## Build validation

`compileDebugKotlin testDebugUnitTest lintDebug assembleDebug`: passed. Test reports: 355 total, 353 passed, 0 failures/errors, 2 pre-existing skips. New `OnboardingLockTest` and both `AdaptiveDraftTest` cases: passed. `scripts/repo_guard.sh`: passed. `bundleRelease assembleRelease`: passed. No signing material was printed or added. Device reinstallation and onboarding retest were not performed because the phone was disconnected.

## Confirmed failure: draft disappears in landscape

Steps: Add transaction → amount 77 → Grocery → description Demo rotation → rotate to landscape.
Observed: landscape shows amount 0 and loses visible draft input. Rotating back recovers the portrait values.
Cause: `AppScaffold` composes separate navigation hosts in wide and narrow layout branches, changing the saveable-state location.
Screenshot: [landscape failure](release-e2e-2026-11/13-landscape-draft-lost.png).
Log: [rotation](release-e2e-2026-11/13-rotation.log).
Stack trace: none; this is state loss, not a crash.
Regression: `AdaptiveDraftTest.draftSurvivesSwitchingBetweenBottomBarAndNavigationRail` failed with the missing `77` node before the fix and passed after preserving one navigation host.

## Confirmed failure: clipped text at 2× font scale

Steps: set system font scale to 2.0 → Home → Activity → Add transaction.
Observed: month labels truncate, navigation labels lose trailing letters, Transfer wraps awkwardly, and long category names ellipsize.
Cause: fixed-width rows, fixed-height chips and single-line labels do not adapt to enlarged text.
Screenshots: [Home](release-e2e-2026-11/12-home-font-2.png), [Activity](release-e2e-2026-11/12-activity-font-2.png), [Add](release-e2e-2026-11/12-add-keyboard-hidden.png).
Log: [large text](release-e2e-2026-11/12-large-text.log).
Stack trace: none; visual clipping.
Code now gives enlarged labels and category choices more room and adapts navigation labels. `AdaptiveDraftTest` covers rendered text bounds and complete labels at 2× font scale. Updated-release phone retest pending.

## Skills setup

Cloned the upstream Android testing skills source, read its installer before execution, and ran it with the explicit global Codex skill directory. The first installer run skipped the 54 identical existing copies. Those copies were backed up outside the repository, replaced by 54 source-backed links through the reviewed installer, and sibling cross-references were verified. All four requested sample skills were readable and relevant skills were used. No skill files were copied into this repository and no restart was needed.

## Test-state preservation

For the planned reinstall, created a second JSON backup with 16 transactions, 3 accounts and 2 budgets, including the six CSV rows. The original 10-transaction backup used for restore verification remains separate. Widget placement remains a manual check (`cmd appwidget help` returned `No shell command implementation.`).

## Stop point and remaining checks

The owner disconnected the phone and asked to finish with the work already done. The updated release has **not** been installed on the phone and its three fixes have **not** received a real-phone retest. No app data reset was performed. Fresh onboarding and the first seven transaction entries happened before recording was requested, so they are screenshots/logs only; no replacement footage was created.

Real notification capture and launcher widget placement remain untested. No app FATAL EXCEPTION, native fatal signal or app ANR was found in the captured scenario logs. One blank content screen after rapid SMS cancellation was captured; a controlled single-back retest returned Home normally. This remains an unconfirmed navigation observation. Both images remain in the evidence folder (`07-after-cancel.png`, `07-share-back-check.png`).

Final known phone state: original release APK still installed; 16 saved demo ledger rows (including the transfer pair), 3 accounts, 2 budgets; app lock off; font scale 1.0; automatic rotation restored; AkSpend notification access restored and in-app auto-import on; automatic backups paused. Two JSON backups and the CSV fixture remain under Download/AkSpend-E2E. Clipboard contains the supplied demo SMS from the Paste test. No app was uninstalled, no phone reboot was performed, and other apps’ data/settings were not changed.

## Checkpoint evidence

| Scenario | Screenshots | Log |
|---|---|---|
| 1 | [01-home-totals.png](release-e2e-2026-11/01-home-totals.png) | [log](release-e2e-2026-11/01-onboarding-transactions.log) |
| 2 | [02-transfer-balances.png](release-e2e-2026-11/02-transfer-balances.png) | [log](release-e2e-2026-11/02-accounts-transfers.log) |
| 3 | [03-budgets-progress.png](release-e2e-2026-11/03-budgets-progress.png), [03-home-progress.png](release-e2e-2026-11/03-home-progress.png) | [log](release-e2e-2026-11/03-budgets.log) |
| 4 | [04-reopened-saved-filter.png](release-e2e-2026-11/04-reopened-saved-filter.png), [04-cleared-filters.png](release-e2e-2026-11/04-cleared-filters.png) | [log](release-e2e-2026-11/04-activity-filters.log) |
| 5 | [05-subscription-detected.png](release-e2e-2026-11/05-subscription-detected.png), [05-month-end-breakdown.png](release-e2e-2026-11/05-month-end-breakdown.png) | [log](release-e2e-2026-11/05-insights-with-history.log) |
| 6 | [06-long-return-launcher.png](release-e2e-2026-11/06-long-return-launcher.png), [06-five-wrong-codes.png](release-e2e-2026-11/06-five-wrong-codes.png), [06-lock-off-final.png](release-e2e-2026-11/06-lock-off-final.png) | [log](release-e2e-2026-11/06-vault.log) |
| 7 | [07-review-form.png](release-e2e-2026-11/07-review-form.png), [07-clipboard-paste.png](release-e2e-2026-11/07-clipboard-paste.png), [07-exact-otp-share.png](release-e2e-2026-11/07-exact-otp-share.png) | [log](release-e2e-2026-11/07-bank-sms.log) |
| 8 | [08-disclosure-before-settings.png](release-e2e-2026-11/08-disclosure-before-settings.png), [08-empty-inbox.png](release-e2e-2026-11/08-empty-inbox.png) | [log](release-e2e-2026-11/08-auto-import.log) |
| 9 | [09-after-edit-175.png](release-e2e-2026-11/09-after-edit-175.png), [09-restored-income-150.png](release-e2e-2026-11/09-restored-income-150.png) | [log](release-e2e-2026-11/09-backup-restore.log) |
| 10 | [10-csv-import-summary.png](release-e2e-2026-11/10-csv-import-summary.png), [10-csv-duplicates.png](release-e2e-2026-11/10-csv-duplicates.png) | [log](release-e2e-2026-11/10-csv-import.log) |
| 11 | Not attempted; manual launcher check | [log](release-e2e-2026-11/11-widgets-manual.log) |
| 12 | [12-home-font-2.png](release-e2e-2026-11/12-home-font-2.png), [12-activity-font-2.png](release-e2e-2026-11/12-activity-font-2.png), [12-add-keyboard-hidden.png](release-e2e-2026-11/12-add-keyboard-hidden.png) | [log](release-e2e-2026-11/12-large-text.log) |
| 13 | [13-landscape-draft-lost.png](release-e2e-2026-11/13-landscape-draft-lost.png), [13-after-confirmed-process-death.png](release-e2e-2026-11/13-after-confirmed-process-death.png) | [log](release-e2e-2026-11/13-confirmed-process-death.log) |

## Local changes and rebuilt artifact

Fix commit: `6f4eb14` on `test/release-e2e-fixes` — correct onboarding lock guidance; keep one navigation host so draft state survives narrow/wide layout changes; adapt enlarged labels, navigation and transaction choices. All three fixes are covered by focused Robolectric tests. No push, PR or merge was performed.

Final required checks passed with a single Gradle worker and a temporary 2 GiB daemon heap to reduce host memory contention; repository Gradle settings were not changed.

Rebuilt signed APK: [app-release.apk](../../app/build/outputs/apk/release/app-release.apk). AAB: [app-release.aab](../../app/build/outputs/bundle/release/app-release.aab). These generated builds remain untracked. Rebuilt APK SHA-256: `35132f656d21255bd254affe0b5e80cac03964f394e78212eea772048c09bc25`. This artifact is **not** the APK installed on the phone during the recorded run.

Pre-existing skipped tests:

- `com.akshar.AuditOpenFindingsTest.csvHashMustDistinguishDifferentFieldBoundaries`
- `com.akshar.DashboardAuditTest.todayAmountMustRemainReadableAtDoubleFontScale`

Their existing ignore reasons concern large financial values at enlarged font sizes and a stored CSV-hash migration. They were not treated as passing checks.
