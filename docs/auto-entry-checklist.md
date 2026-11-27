# Low-effort entry and passive notification capture

Date: 27 November 2026. Checked items are implemented or verified in source; unchecked items are proposed work or physical-device checks. The goal is a brief daily review rather than typing every purchase.

## 1. Watch report and current evidence

Reported setup: HONOR phone, Android 16, HONOR Health companion app. Watch message/notification delivery stopped after installing AkSpend and did not resume when AkSpend notification access was switched off. Exact phone/watch models, software versions and reproduction are still unknown.

Source inspection found no calls that dismiss or snooze notifications, mark messages read, request suppression hints, change interruption filters, or control another listener. Android's notification-listener API has methods that can affect notifications, so absence of those calls matters; notification access is not inherently a read-only permission. [Android API reference](https://developer.android.com/reference/android/service/notification/NotificationListenerService)

The failure continuing with AkSpend access disabled does not isolate the cause to AkSpend or exclude a persistent platform/companion issue. No physical device is connected for reproducing it. The changes below harden passive capture; they are not a demonstrated fix for this HONOR watch outage.

### Restore forwarding and establish a baseline

- [ ] Keep AkSpend's notification access off for the baseline; the in-app pause switch alone does not revoke it.
- [ ] Confirm that an ordinary new message appears in the phone notification shade. If it does not, troubleshoot the source app's notification permissions first.
- [ ] In HONOR Health, confirm that the watch is connected, then check Devices → watch → Notifications and individual source selections.
- [ ] In phone settings, check HONOR Health notification access; toggle that grant off/on, reopen Health and test a new message with the phone locked.
- [ ] Check watch Do Not Disturb/sleep settings and any setting that mutes watch alerts while the phone is in use; availability depends on model.
- [ ] If failure follows screen lock/background time, check HONOR Health's background running protection. Where available, allow auto-launch, secondary launch and background running for Health. Do not change all apps' battery settings.
- [ ] If necessary, restart phone and watch, reopen Health and repeat the same test. Avoid clearing health data or factory resetting as a first step.
- [ ] Once forwarding works with AkSpend off, repeat with AkSpend access granted but capture paused, then with capture enabled. Record which stage changes the result.

HONOR documents notification access, Bluetooth connection, per-app selection and background operation as forwarding dependencies. Some instructions describe older software; use equivalent settings on the actual MagicOS build. [HONOR wearable troubleshooting](https://iknow-dl.service.hihonor.com/ctkbfm/applet/simulator/en-us15822722/index.html), [HONOR background-notification guidance](https://www.honor.com/global/support/content/en-us00685514/)

## 2. Non-interference contract

- [x] Observe posted notifications without dismissing, snoozing or replacing them.
- [x] Do not execute notification content/action/delete intents or mark source messages as read.
- [x] Do not request listener suppression hints, alter DND/sounds, or change other apps' permissions/settings.
- [x] Do not add accessibility, default-SMS role, overlays or a notification-assistant service for capture.
- [x] Keep parsing and inbox database writes on a background dispatcher.
- [x] Stop lifecycle-owned work on listener destruction; propagate coroutine cancellation.
- [x] Catch malformed notification text extraction without logging input or exceptions containing it.
- [x] Keep raw notification text out of storage/logs and preserve offline operation.
- [x] Add repository guard checks against notification mutation APIs in capture code.
- [x] Test that payment capture, unrelated notifications and clearing inbox drafts preserve original active notifications and alert policy on simulated Android 14 and 16.
- [ ] Verify real HONOR Health watch delivery; simulator assertions cannot prove vendor compatibility.

Android may bind the listener while its grant remains enabled even when AkSpend capture is paused. The app must distinguish 'capture paused' from 'system access revoked'. Android/OEM/watch behavior cannot be guaranteed by a unit test.

## 3. Implemented capture refinements

- [x] Capture defaults to paused when there is no saved choice. Notification access alone no longer activates parsing. Existing explicit on/off preferences remain unchanged.
- [x] Provide per-source switches for the supported messaging, bank and payment apps. Excluding one source does not disable its notifications or watch forwarding.
- [x] Check source/global choices before extraction and again before parsing/persistence, reducing queued work after pause. A database insertion already in progress may finish; pause does not delete existing drafts.
- [x] Keep a clearly labeled shortcut to Android notification-access settings after granting access.
- [x] Explain that inbox dismissal removes only the draft and that pause is different from permission revocation.
- [x] Preserve parsed bank-reference fingerprints in the inbox and when opening the review form, so the existing saved-transaction duplicate guard can use them.
- [x] Prefer direction-scoped reference fingerprints plus amount for new draft deduplication across bank/payment sources; debit and credit remain distinct through the existing reference-key contract, and a different amount stays reviewable.
- [x] Preserve old inbox rows using a tested non-destructive schema migration. Old drafts without references remain reviewable.
- [x] Continue to require review before any draft changes financial balances. Confident parsing alone is insufficient for safe automatic ledger entry.

Deduplication limits: references can be missing, malformed, reused or inconsistent across providers. Old drafts are not retroactively rewritten. Content-only fallback can mistake identical notifications for reposts. Reference presence is not proof of correct category, account or expense-versus-transfer classification. Do not market this as perfect duplicate prevention.

## 4. Next work to remove repetitive typing

### Priority A: review quickly

- [ ] Add an editable review list showing amount, merchant, category, funding account and reason for uncertainty together.
- [ ] Let users select and confirm several eligible drafts in one action; ambiguous items stay unselected.
- [ ] Resolve funding account explicitly. A generic 'card' notification must not silently choose a debit card or bank account.
- [ ] Require explicit treatment of transfers, wallet loads, ATM withdrawals, card repayments, loans and refunds before batch save.
- [ ] Use remembered merchant categories with a quick correction; category confidence and account confidence are separate.
- [ ] Recheck duplicates at commit time, with atomic writes and idempotent import identifiers that survive retries/process death.
- [ ] Show a completion summary and reliable undo. Prevent a crash between ledger save and inbox removal from recording the payment twice.
- [ ] Explain same-amount match suggestions rather than automatically deleting potentially separate purchases.

### Priority B: quick capture for cash and missing Lite transactions

- [ ] Make existing widget/Quick Settings entry support amount-only save with a visible remembered account/category.
- [ ] Add editable repeat shortcuts such as Tea ₹20, Lunch ₹90 and Bus ₹20, based on user choices.
- [ ] Support 'categorize later' so saving a cash payment does not require every field.
- [ ] Preserve unfinished amount drafts through configuration/process changes; distinguish deliberate repeats from accidental double taps.
- [ ] Allow a quick balance reconciliation when Lite history or cash entries are incomplete; disclose unallocated spending instead of inventing merchants.
- [ ] Optional voice input only after evaluating on-device availability; always retain typing, and do not silently send financial speech to a cloud service.

### Priority C: optional trusted automatic saving

- [ ] Make opt-in separate from notification access and draft capture.
- [ ] Limit automatic save to an explicitly configured source/account/merchant rule with a clear completed-payment classification.
- [ ] Exclude uncertain accounts, transfers, repayments, reversals, duplicate candidates and incomplete split bills.
- [ ] Use durable source-event identity and transaction-safe inbox/ledger reconciliation before enabling this mode.
- [ ] Offer pause, rule editing, correction and undo; show source/rule on every automatically saved entry.
- [ ] Keep background capture silent: no floating overlay, forced activity launch or notification after every purchase.
- [ ] Add an optional single daily review reminder with quiet hours and its own notification controls. No reminder is required for capture to work.

## 5. Reliability and privacy release gates

- [x] Cash entry, SMS paste/share and CSV workflows remain available without notification access.
- [x] No network permission, telemetry or external transmission added.
- [x] Unknown packages, personal-message senders, OTPs, summaries and unsupported Android versions retain filtering.
- [x] Tests cover source selection, pause defaults, cross-source reference identity, original-notification preservation and migration/reopen.
- [ ] Add source health indicators distinguishing grant, listener connection, last captured draft and no recent payments. Never label lack of captures as proof of broken delivery.
- [ ] Test notification bursts, updated/reused notification IDs and identical cash/UPI payments without references.
- [ ] Test Android redacted/hidden notifications and explain incomplete capture instead of requesting broader privileges.
- [ ] Confirm listener recovery after OS restart/upgrade, permission revocation and vendor background restrictions without tight rebind loops.
- [ ] Complete the physical matrix below before claiming watch compatibility.

| Configuration | Required observation | Status |
| --- | --- | --- |
| HONOR + Health, AkSpend access off | Phone and watch receive ordinary new messages | User reports watch failure persists; unresolved |
| HONOR + Health, access on/capture paused | Same forwarding, no new inbox drafts | Physical test pending |
| HONOR + Health, capture on | Same forwarding plus payment drafts | Physical test pending |
| Source excluded in AkSpend | Source still alerts phone/watch, no draft from that source | Source filtering tested; real watch pending |
| Remove one/all AkSpend drafts | Original phone/watch notifications remain unaffected by AkSpend | Original active notifications tested; real watch pending |
| Health background + phone locked | Watch forwarding persists under its configured settings | Physical test pending |

## 6. Verification and scope

The first delivered increment is passive capture controls, reference preservation/deduplication, migration, tests and this checklist. Bulk confirm, amount-only entry, repeat shortcuts and trusted auto-save remain unchecked work; none is implied to have shipped here.

Required project verification: `./gradlew compileDebugKotlin testDebugUnitTest lintDebug assembleDebug`, then `./scripts/repo_guard.sh`. See the implementation report for actual results. No device installation, signing/release publication or phone/watch setting changes are performed automatically.
