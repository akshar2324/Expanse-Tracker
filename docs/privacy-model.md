# Privacy Model

AkSpend is designed from the ground up with privacy and data ownership as its core principles. This document outlines how your data is handled.

## Local Data Storage

- All core financial data, including accounts, transactions, and categories, is stored exclusively on your device using a local Room (SQLite) database.
- App lock settings are kept in private SharedPreferences (`vault_settings.xml`). The passcode is stored only as a salted PBKDF2 hash, never as text, and there is no default passcode.
- Auto-import from notifications is optional and off until the user grants notification access. It keeps the parsed payment fields and a one-way fingerprint, never the message text, and drops unreviewed items after 60 days.
- We explicitly disable Android Auto Backup for sensitive app data to ensure that your financial information does not get silently uploaded to cloud servers without your explicit consent.

## No Telemetry, Analytics, or Backend

- AkSpend operates entirely **offline**. There are no background syncs to a developer server.
- The app does not include any runtime telemetry, crash reporting (unless manually opted-in through open source frameworks on developer builds), or user tracking analytics.
- The app does not require `android.permission.INTERNET` for its core functions.

## Backups and Export

Because your data is local-only, managing backups is in your hands:
- You can manually trigger JSON backups or database exports within the app.
- These exported files can be securely transferred by you to another device, hard drive, or your own personal cloud storage.
- Restoring from these backups is handled strictly via local file selection.

## Summary

In short: **Your data is yours.** AkSpend does not see, store, share, or sell your financial information.
