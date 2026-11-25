# AkSpend 1.6.0 candidate verification — 25 November 2026

## Scope

Integrated Quick Settings entry (#101), optional budget alerts (#98), monthly PDF reports (#99), Activity swipe actions (#97), UI feedback and spacing (#107), and dependency updates (#91, #92, #93, #96, #106). Version is 1.6.0 (8). The experimental liquid controls remain reverted.

## Automated results

- `compileDebugKotlin testDebugUnitTest lintDebug assembleDebug`: passed.
- 421 host tests: zero failures, zero errors, two skipped.
- `assembleDebugAndroidTest bundleRelease assembleRelease`: passed.
- Signed release APK: Android signature verification passed.
- Release manifest: no INTERNET permission.
- Repository guard and whitespace checks: passed.
- Robolectric 4.17 requires opening `java.base/jdk.internal.access` for its file-descriptor interceptor. The test JVM configuration includes this flag.

## Android 15 emulator results

Used a separate API 35 Pixel 7 emulator with synthetic data. No personal ledger was used.

| Check | Result |
| --- | --- |
| Upgrade from 1.5.0 to 1.6.0 without clearing app data | Passed; the existing ₹42.35 expense remained present |
| Exact ledger comparison after upgrade and native tests | Passed; all ledger tables matched the baseline |
| Budget WorkManager worker, both 80% and 100% notifications | Passed; both notifications posted, reruns did not duplicate claims, disabling alerts prevented additional claims |
| Transfer deletion and Undo | Passed; both legs removed and the exact snapshot restored |
| PDF writing and pagination | Passed; normal report and large tables generated successfully across multiple pages |
| Quick Settings tile through System UI | Passed; opened the Add transaction expense form |
| Monthly report screen | Passed; showed the expected ₹42.35 spending and opened Android's share sheet with a PDF attachment |
| Activity swipe left and snackbar Undo | Passed; the synthetic expense was restored |
| Activity at 200% system text size | Passed; search hint remained on one line and date filters wrapped with spacing |

The three instrumented tests passed on the emulator. Host screenshot coverage also exercises the main tabs, More tools, account routes, onboarding, CSV import steps, themes and large fonts. Local evidence includes instrumentation output, screenshots, swipe video and exact database comparison; generated evidence is excluded from Git.

## Pending physical-phone verification

The user requested completing emulator testing and leaving physical-phone verification pending. Keep the public release as a draft until these checks are completed:

- Quick Settings entry while locked, after reboot and under the phone manufacturer's background restrictions.
- Notification permission refusal, real scheduled delivery, Doze and reboot behavior.
- Opening the shared PDF in a recipient app and checking print/rendering output on the phone.
- TalkBack traversal, announcements and swipe alternatives on a physical phone.

No physical-device results are claimed by this report.
