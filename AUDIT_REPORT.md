# AkSpend audit

Date: 6 November 2026. Base: `183965b` (`origin/feat/notification-import`). Work was isolated on `audit/codex` in the separate `akspend-audit` worktree. No push or PR was made. No dependencies, permissions, network features, package names, or application IDs were added or changed.

## Baseline

Run before modifications, using Android Studio's bundled JBR:

```sh
./gradlew compileDebugKotlin testDebugUnitTest lintDebug assembleDebug
./scripts/repo_guard.sh
```

Both passed. Gradle completed in 6 seconds; the baseline unit-test and lint tasks were restored from cache. This is a build/test baseline, not evidence of a fresh real-device run. Baseline lint reported 86 warnings and 7 hints, with no errors. Existing Gradle deprecation warnings were present.

## Findings

Paths below are relative to `app/src/main/java/com/akshar/` unless they start with `app/`. Lines refer to the final audit source. Severity describes the original defect. “Open” findings were not implemented because they require a broader validation, representation, security, or layout decision.

| ID | Severity | Area | File:line | Reproduction / impact | Status |
|---|---|---|---|---|---|
| F01 | high | Notification privacy | `sms/inbox/AutoImportCapture.kt:65` | Send a copied, valid HDFC payment template from a contact such as Mom in Messages. High parser confidence previously bypassed the sender check and queued personal chat content. Messaging sources now require a bank-style sender before parsing. | Fixed in `41d1b44` |
| F02 | medium | Manual money input | `utils/MoneyUtils.kt:19` | Enter `1,23,456` or `1,23,45,678`. Indian integer grouping was rejected although decimal variants were accepted. | Fixed in `4e2e928` |
| F03 | medium | CSV money input | `utils/CsvParser.kt:53` | Import `-1,23,456.78` with an `en_IN` profile. Proper Indian grouping was rejected. Western grouping remains supported; Indian grouping is enabled for Indian profiles. | Fixed in `4e2e928` |
| F04 | medium | CSV signs / ambiguity | `utils/CsvParser.kt:164` | In separate debit/credit columns, import a negative debit or a row with both debit and credit positive. A minus was stripped, or the credit was silently discarded. Such rows now produce errors. | Fixed in `4e2e928` |
| F05 | low | CSV dates | `utils/CsvParser.kt:133` | Import `2026-01-05garbage` using `yyyy-MM-dd`. Parsing previously accepted the valid prefix. The entire date must now be consumed. | Fixed in `4e2e928` |
| F06 | high | JSON compatibility | `data/backup/BackupCodec.kt:167` | Restore `{"version":999,"transactions":[]}`. The future format was accepted and could replace all existing data while discarding unknown sections. Unsupported explicit versions are now rejected. Legacy unversioned backups still decode. | Fixed in `8efe8ca` |
| F07 | high | SQLite restore safety | `ui/viewmodel/FinanceViewModel.kt:539` | Choose a header-only corrupt file, an unrelated SQLite database with `user_version=6`, or a Room database with a missing table and a stale identity hash. Header/version checks accepted these before replacing the ledger. Validation now checks integrity and forces full Room schema validation/migration on a disposable copy before touching the live database. | Fixed in `f5c6a2f`, `ba7a89c` |
| F08 | high | Backup export | `app/src/main/AndroidManifest.xml:42` | Export a physical backup. The manifest registered `com.akshar.fileprovider`, while the exporter requested the application ID's authority. FileProvider threw instead of sharing the backup. The authority now uses `${applicationId}.fileprovider`. | Fixed in `f5c6a2f` |
| F09 | medium | CSV deduplication | `ui/viewmodel/CsvImportViewModel.kt:170` | Preview a file containing the same row twice before either row exists in Room. Both were selected. The second row is now marked duplicate and skipped by default; users retain the existing override. | Fixed in `d81e30b` |
| F10 | medium | Manual money parsing | `utils/MoneyUtils.kt:54` | Enter `1,2.34` or German `1.2,34`. Group separators were silently stripped, changing the numeric meaning. Invalid mixed grouping is now rejected. | Fixed in `6f2e1e2` |
| F11 | high | Notification error logging | `sms/inbox/BankNotificationListener.kt:39` | Inject a parser exception whose message contains notification text. The listener logged the throwable, leaking that text. It now logs only a fixed diagnostic. This is a proven error-path leak, not evidence that normal successful parsing logged messages. | Fixed in `6ba72a3` |
| F12 | medium | CSV threading | `ui/viewmodel/CsvImportViewModel.kt:98` | Select a document from a slow ContentProvider. Its metadata query ran synchronously on the UI caller. It now runs on the IO dispatcher. | Fixed in `37b9019` |
| F13 | high | SMS amount boundary | `sms/BankSmsImport.kt:83` | Parse a completed HDFC payment with a 400-digit amount. The BigDecimal draft was accepted, then converted to infinity by the review UI; `signedMoney` cannot format infinity. Amounts outside the existing Double-based UI/ledger range are now rejected. | Fixed in `b4dc694` |
| F14 | medium | Transfer deletion | `ui/screens/AddTransferScreen.kt:223` | Edit a transfer and tap Delete transfer. Both ledger entries disappeared without confirmation. The existing confirmation dialog now supports cancel or deletion of both legs. | Fixed in `2de56eb` |
| F15 | medium | Dashboard period totals | `ui/screens/DashboardScreen.kt:105` | Add a current expense of 10 and an expense of 100,000 dated 40 days ahead. Spent today and this-month totals included the future month's entry. Day/month totals and the monthly spending average now have exclusive upper date bounds. | Fixed in `a99349e` |
| F16 | high | App-lock credentials | `MainActivity.kt:129`, `MainActivity.kt:760`, `ui/screens/ToolsScreen.kt:393` | Complete onboarding without setting a PIN, then cold-open the locked app. The lock screen discloses `1234`, and entering it opens financial data. A custom PIN is stored as plaintext preferences; no attempt throttling is implemented. PIN enrollment, migration/storage, and retry policy need a deliberate security design. | Open |
| F17 | high | App-lock lifecycle | `MainActivity.kt:97`, `MainActivity.kt:108` | Unlock, background the activity, and return. The vault remains unlocked. There is no pause/stop relock, and the unlocked flag is also restored from saved state. A background timeout and process-restoration policy need a product decision. | Open |
| F18 | high | Money representation | `utils/MoneyUtils.kt:39`, `data/model/Models.kt:11` | Parse `100000000000000.01`; conversion through Double yields `100000000000000.02`. Rounding after binary conversion cannot recover the lost cent. Ledger amounts, expression evaluation, and many totals still use Double; sufficiently large finite inputs can also overflow aggregate totals. An exact-money migration and/or explicit supported bounds require broader work. | Open |
| F19 | high | JSON semantic validation | `data/backup/BackupCodec.kt:191` | Decode a complete transaction with `amount=-5`. It is accepted and can be restored as a negative expense. JSON validation checks required fields but does not consistently enforce financial signs, scale, or legal enum values across tables. A shared validation contract is needed. | Open |
| F20 | high | JSON account integrity | `data/backup/BackupCodec.kt:200`, `data/model/Models.kt:20` | Decode a transaction with `accountId=99` and no accounts section. Only account 1 is created, leaving an orphan after restore. Room has no foreign-key enforcement here. Decide how invalid/legacy account links should be rejected or repaired before replacing the ledger. | Open |
| F21 | medium | Large-font accessibility | `ui/screens/DashboardScreen.kt:499` | Render at 200% font scale with today's spend of 1,561. The Spent amount is ellipsized; text-layout inspection confirms overflow. Screenshots also show crowded tab labels and category chips. Responsive layout changes are outside this small-fix audit. | Open |
| F22 | medium | Share-intent input | `MainActivity.kt:322` | Share a `text/plain` SMS using a styled CharSequence (for example SpannableString). `getStringExtra` discarded it, leaving the importer empty. Read EXTRA_TEXT as CharSequence and convert to plain text. Plain String and vault-gating integration checks also pass. | Fixed in `0743718` |
| F23 | medium | CSV dedupe identity | `utils/CsvParser.kt:185` | Parse semicolon CSV rows with description/category `A,B` / `C` versus `A` / `B,C`, with the same date and amount. Comma-joining fields gives identical hash input for distinct rows, so legitimate entries can be marked duplicate. Changing serialization needs compatibility with already stored hashes to avoid reimporting older batches. | Open |

## Regression tests and evidence

Each implemented defect was reproduced with a failing unit/Robolectric test before its fix. Initial targeted runs failed for the six money/CSV/privacy/version regressions; backup safety tests separately failed for corrupt files, wrong schema, and the authority mismatch. Further red runs reproduced within-file duplication, ambiguous CSV hash input, malformed grouping, the logging leak, caller-thread metadata queries, SMS overflow, transfer deletion, and future-period totals. Failures of the open expectations below were also recorded before disabling those expectations.

Added test classes:

- `data/backup/BackupFileSafetyTest`: installed FileProvider authority; rejection of unrelated SQLite and stale Room identity; versions 4, 5, and 6 validated and opened through Room with migrations, preserving transaction ID, amount, and default account link.
- `data/db/LegacyDatabaseFixtures`: frozen synthetic SQLite schema fixtures for migration tests. These are not historical customer backup files.
- `sms/BankMessageAuditTest`: realistic amounts and directions, Indian grouping, refunds, credit-card-bill messages, OTPs, declined/failed payments, reminders, requests, scheduled payments, blanks, personal chats, non-English text, missing dates, ambiguous dd/MM dates, future dates, zero/negative values, and overflow.
- `sms/AndroidSevenImportTest`: default platform gate on API 24 and 25, without overriding `supported`.
- `sms/inbox/NotificationPrivacyAuditTest`: listener processing, repeated posts, parsed-field persistence, raw database-file inspection, preference/log checks, and a fault-injected logging exception.
- `ui/viewmodel/CsvImportAuditTest`: duplicates within one file, reimport dedupe against Room, batch metadata, undo preserving a different batch, and metadata query threading.
- `ShareIntentAuditTest`: actual ACTION_SEND activity launches with String/styled CharSequence, and confirmation that sharing does not bypass the vault.
- `DashboardAuditTest`: future-day/month exclusion plus the documented large-font overflow expectation.
- `AuditOpenFindingsTest`: intended expectations for exact large money, negative JSON amounts, dangling account links, and unambiguous CSV hash inputs.
- `security/AppLockAuditTest`: intended expectations for rejecting the default PIN and relocking after backgrounding.

Extended existing `MoneyUtilsTest`, `CsvParserTest`, `BackupCodecTest`, `AutoImportCaptureTest`, `AppNavGraphTest`, and `ScreensScreenshotTest`. The transfer test checks both cancel and confirm. Existing transfer atomicity, account balances, budget totals, insights, recurring schedules, backup round trips, delete-dialog behavior, and pending-import unique-key tests are included in the final full suite.

**Known failures are explicit:** `AuditOpenFindingsTest` and `AppLockAuditTest` are ignored at class level, and `DashboardAuditTest.todayAmountMustRemainReadableAtDoubleFontScale` is ignored individually. Together these retain seven failing safety expectations for F16–F21 and F23. They are not fixes and must not be interpreted as passing coverage. Re-enable the relevant `@Ignore` tests when implementing those findings.

## Coverage and limits

- Notification capture now rejects ordinary contact titles, telephone numbers, missing senders, and copied bank templates from contacts. A bank-style title is a heuristic, not cryptographic sender authentication: a contact named like a commercial sender or personal text indistinguishable from a bank template cannot be distinguished using the available notification fields. The app/package allowlists also limit which notification sources are inspected.
- For the synthetic successful notification, raw text and a trailing sentinel were absent from persisted inbox database files, checked SharedPreferences, and logs; only parsed transaction fields and a SHA-256 dedupe key were kept. Error logging was hardened after fault injection. This does not prove every possible third-party parser/Android error path or every merchant-field extraction is free of sensitive substrings.
- The inbox unique index prevents duplicate queued rows for normalized source/text. Existing DAO tests and the listener test cover that behavior. Once a draft is dismissed/reviewed or aged out, its key is deleted too; there is no permanent processed-notification history. Identical text for separate real payments is also indistinguishable to the current key. Cross-app copies have different keys. These are dedupe-policy limits, not exactly-once ledger guarantees.
- No live bank/UPI app or OEM notification layout was exercised. MessagingStyle sender extraction, grouped/updated notifications, access revocation/reconnection, notification redaction, and multilingual bank templates require device testing. The synthetic corpus does not certify any bank/app version.
- API 24/25 Robolectric gating passed, and static review found parser/java.time work behind the API-26 guard. A JVM provides java.time even when emulating Android 7, so this does not prove Android 7 class verification/linking on a real ART runtime. No emulator or physical Android device was used.
- Backup codec tests cover malformed JSON, wrong section types, missing fields, legacy backups, future versions, and all-table/account-linked round trips. SQLite tests cover corrupt headers, unsupported versions, unrelated schemas, damaged current schemas, and synthetic migration paths 4→5→6 and 5→6. Validation works on a copy. Concurrent writer/WAL races during physical export or live restore, low storage, interrupted file replacement, and restart/process-exit behavior were not stress-tested. Those flows need device testing; a checkpoint followed by a raw file copy is not established here as a fully coordinated concurrent snapshot.
- Manual, CSV, SMS, and transfer paths were traced into Room; rounding uses HALF_UP where present. The remaining Double/semantic-validation findings apply across input, persistence, and aggregate calculations. Ordinary transfer pairs remain atomic and are excluded from income/expense screens; budget category/period tests and insights tests run in the suite. No exhaustive proof of all monetary bounds or all locale conventions is claimed.
- The activity and deep-link/share routes were reviewed: external routes open review/input UI behind the activity lock, not automatic ledger insertion. The notification service requires the platform binding permission; FileProvider is non-exported with URI grants and only cache paths; the widget shows a shortcut rather than financial balances. FileProvider's cache path covers the full cache directory, so future callers must continue granting only explicit export URIs.
- `android:allowBackup=false` is set. The merged debug manifest has no INTERNET permission, and source/dependency review found no app network calls or analytics/telemetry SDK. Transitive WorkManager permissions include ACCESS_NETWORK_STATE; it is not INTERNET access. Backup XML still lists databases/preferences, including device-transfer rules, so manufacturer-specific transfer behavior was not verified. No forbidden UI name was found; technical package identifiers were preserved.
- Biometric signature verification tests run, and source uses strong biometric authentication with a per-use Keystore signing operation. Actual hardware, enrollment changes, cancellations, secure recents behavior, and OS process-death restoration were not device-tested. Before unlock, the activity renders onboarding or the lock screen, while the widget contains no financial data.
- Notification parsing/insertion uses a service-owned IO scope canceled on destroy; Room suspend methods dispatch database work; SMS parsing uses a background dispatcher; file backup work uses IO. CSV metadata dispatch was fixed. Some pre-existing APIs still warrant follow-up: JSON decode is synchronous in `restoreDataFromJson`, and several screens use ordinary `remember`/ViewModel-only state. A complete process-death restoration campaign and save-acknowledgment/crash-window tests were not performed.
- The animation implementation uses finite settle animations and listens to the system reduced-motion setting. No infinite Compose animation loops were found. Background performance, reduced-motion rendering on actual devices, and shader fallback behavior on older GPUs were not measured.
- Light/dark captures were visually inspected at normal and 200% font scales. Empty states and semantics labels were reviewed in source and exercised by existing navigation/component tests. This is not a TalkBack session, exhaustive 48dp hit-target measurement, contrast certification, small-screen matrix, or complete accessibility audit.

## Final verification

| Check | Final result |
|---|---|
| `./gradlew compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` | **PASS**, 2m 4s; 58 tasks, 6 executed and 52 up-to-date. Unit tests actually ran. |
| Unit/Robolectric suite | **149 tests: 142 passed, 7 explicitly ignored known-failure expectations, 0 failures/errors.** The ignored tests correspond to the seven open findings above. |
| `lintDebug` | **PASS**, no errors; 86 warnings and 7 hints, unchanged from baseline. These pre-existing warnings are not claimed fixed. |
| `./gradlew recordRoborazziDebug --tests 'com.akshar.ScreensScreenshotTest'` | **PASS**, 2m 8s; all 8 capture tests passed and produced 48 screen PNGs (light/dark × normal/200% fonts). Visually inspected captures; F21 remains open. |
| `./scripts/repo_guard.sh` | **PASS**, repository guard passed after staging all audit files. |
| `git diff --check` | **PASS**. |

The final green verification includes the small fixes and retained ignored regressions; it does not certify resolution of the open findings or real-device behavior.

Screenshots remain untracked build outputs under `app/build/outputs/roborazzi/screens/`. No generated build files, test images, credentials, keys, or machine-specific paths are committed.
