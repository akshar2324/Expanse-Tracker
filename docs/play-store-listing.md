# AkSpend Play Store listing

Copy and answers for the Play Console. Play doesn't render Markdown, so the listing text below uses plain lines and `•` bullets. Paste it as is.

## App name (30 characters max)

Pick one:

```text
AkSpend - Expense Tracker
```

```text
AkSpend: Budget & Expenses
```

## Short description (80 characters max)

```text
Private expense tracker that works offline. Budgets, goals and bank SMS import.
```

## Full description (4,000 characters max)

```text
AkSpend is a private expense tracker that works completely offline. There's no account to create, no cloud and no ads. Your money data stays on your phone.

LOG SPENDING IN SECONDS
• Add income, expenses and transfers between accounts
• Type sums like 120/3+5 right in the amount field
• Quick-add from the home screen widget
• Paste or share a bank SMS and AkSpend fills in the amount, merchant and date for you to check

AUTO-IMPORT PAYMENTS (OPTIONAL)
• Turn on auto-import and AkSpend picks up payments from your messages, bank and UPI app notifications
• Every payment waits in a review list. Nothing is added until you confirm it
• Message text is never saved, and personal chats and OTPs are ignored

BUDGETS, GOALS AND INSIGHTS
• Monthly and weekly budgets per category, with a budget widget for your home screen
• Savings goals, debts you lent or borrowed, and recurring payments
• A month-end balance forecast, plus a finder for subscriptions you might have forgotten
• Charts by category and over time

FIND ANY TRANSACTION
• Search by description, category or payment method
• Filter by type, account, amount and date range, and save filters you use often

ACCOUNTS AND IMPORT
• Cash, bank, wallet, savings and credit card accounts, with reconciliation against your statement
• Import bank statements from CSV, with duplicate detection
• AkSpend remembers the category you picked for a merchant and suggests it next time

PRIVATE BY DESIGN
• No internet permission: AkSpend can't send your data anywhere
• Lock the app with a passcode or your fingerprint
• Back up to a folder you choose, optionally protected with a password
• No ads, no analytics, no tracking

AkSpend is for keeping track of your own money. It doesn't connect to your bank, move money or offer loans.
```

## What's new (500 characters max)

```text
First public release of AkSpend.
• Track income, expenses, transfers, budgets and goals, fully offline
• Import bank SMS by pasting or sharing, or auto-import from payment notifications
• Month-end forecast and subscription finder
• Search, filters and saved filters
• Automatic local backups, with optional password protection
• App lock with passcode and fingerprint
```

## Graphics checklist

- [ ] App icon: 512 × 512 PNG, 32-bit, under 1 MB
- [ ] Feature graphic: 1024 × 500 PNG or JPG, no transparency
- [ ] Phone screenshots: 2 to 8, 16:9 or 9:16, each side 320 to 3840 px. Suggested order: Home, add transaction, budgets with widget, Insights forecast, Activity search, review list for imported payments, lock screen, backups.
- [ ] Use demo data only. No real names, account numbers or amounts from your own phone.

## Data safety form

Google counts data as "collected" only when it leaves the device. AkSpend has no internet permission, so it transmits nothing.

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not applicable: no data is collected |
| Do you provide a way for users to request that their data is deleted? | Not applicable: no data is collected. Users can delete entries in the app, or uninstall it to remove everything. |

The privacy policy explains that financial data and parsed notifications are processed only on the device.

## Privacy policy URL

Publish `docs/privacy.html` (the `deploy.yml` GitHub Pages workflow does this) and enter its public URL. The page must be reachable without signing in.

## App access (for reviewers)

Choose **All functionality is available without special access**. There is no account and no default passcode. The app lock stays off until the user sets a passcode in More › Vault & passcode.

## Content rating

Answer the IARC questionnaire as a utility or productivity app: no violence, sexual content, gambling, user-generated content sharing, or location sharing. The expected result is Everyone / PEGI 3.

## Target audience

18 and over. Finance tools for adults keep the app out of the Families policy requirements.

## Financial features declaration

Select **Personal finance: budgeting / expense tracking** only. AkSpend offers no loans, payments, money transfers, banking, crypto, trading or insurance.

## Notification access (in-app disclosure)

Play's User Data policy requires a clear disclosure before the app reads notifications. The Auto-import card shows it before the user is sent to Android's notification-access screen:

> AkSpend can read payment notifications from your messages, bank and UPI apps and list them here for you to confirm. It only looks at those apps, keeps no message text, and nothing leaves your phone.

Keep this text, and the "Allow notification access" button, ahead of the system settings screen in every release.

## Permissions to expect in the review

| Permission | Why |
| --- | --- |
| `BIND_NOTIFICATION_LISTENER_SERVICE` (on the service) | Optional auto-import, granted by the user in system settings |
| `USE_BIOMETRIC`, `USE_FINGERPRINT` | Fingerprint unlock |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `ACCESS_NETWORK_STATE` | Added by WorkManager for scheduled local backups. There is no `INTERNET` permission. |

## Release notes for closed testing

```text
Thanks for testing AkSpend! Please try:
• Adding a few expenses and a budget
• Pasting or sharing a bank SMS (More › Import bank SMS)
• Turning on auto-import, then reviewing a payment
• Setting a passcode, leaving the app for 30 seconds, and coming back
• Making a backup and restoring it
Tell us anything that's confusing, slow or wrong.
```
