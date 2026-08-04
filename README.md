# AkSpend

AkSpend is an offline-first Android personal finance app built with Kotlin, Jetpack Compose, Room, coroutines, and Material 3.

It is designed for people who want a private daily spending tracker without depending on cloud sync, external analytics, or a backend service. Financial data is stored locally on the device and can be exported by the user when needed.

## Highlights

- Track income and expenses with category, date, payment method, and account context.
- Manage multiple accounts such as cash, bank, wallet, savings, and credit card accounts.
- Record transfers between accounts without inflating income or expense totals.
- Review account history and reconciliation records.
- Set monthly or weekly budgets and savings goals.
- Track borrowed and lent money.
- Create recurring transactions.
- Import bank or wallet statements from CSV using a staged, offline workflow.
- Detect duplicate imported CSV rows with SHA-256 row hashes.
- Export and restore local JSON backups.
- Export the local database file for advanced backup workflows.
- Protect the app with biometric-backed local authentication.
- Use the quick-add home screen widget for fast transaction entry.

## Privacy Model

AkSpend is built around local-first data ownership:

- Core finance data is stored in an on-device Room database.
- The app does not require an account or hosted backend for core workflows.
- Backup and export happen only when the user chooses them.
- Android Auto Backup is disabled for app data.
- User-visible branding is AkSpend. Existing `com.akshar` package names are technical identifiers kept for compatibility.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Coroutines and Flow
- Navigation Compose
- AndroidX Biometric
- Glance app widgets
- Gradle Kotlin DSL

## Project Structure

```text
app/src/main/java/com/akshar/
  data/
    db/              Room database and DAO
    model/           Finance entities
    repository/      Repository layer
  security/          Biometric crypto helpers
  ui/
    screens/         Compose screens
    theme/           App theme
    viewmodel/       FinanceViewModel and screen state
  utils/             Parsing and finance utilities
  widget/            Quick-add widget receiver
docs/                Static support pages
```

## Requirements

- Android Studio
- JDK 17 or Android Studio bundled JBR
- Android SDK matching the project compile SDK
- Gradle wrapper included in this repository

Current app version:

```text
versionName: 1.4.0
versionCode: 6
minSdk: 24
targetSdk: 36
```

## Run Locally

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync finish.
4. Run the `app` configuration on an emulator or Android device.

Command-line debug build:

```bash
./gradlew assembleDebug
```

Full local verification:

```bash
./gradlew compileDebugKotlin testDebugUnitTest lintDebug assembleDebug
```

## Release Signing

Release signing uses `keystore.properties` if present:

```properties
RELEASE_STORE_FILE=path/to/release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

Do not commit real signing keys, passwords, generated APKs/AABs, or private local configuration.

## Database Notes

AkSpend uses explicit Room migrations. Destructive migrations should not be used because users may already have local finance data.

Recent schema work includes:

- CSV import metadata and duplicate row hashes.
- Accounts, transfers, and reconciliation records.

When adding persisted models, bump the database version and add a non-destructive migration.

## Testing

The project includes unit tests for finance logic, repository behavior, CSV parsing, biometric verification helpers, and Room-backed flows.

Useful commands:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Roadmap

Planned and in-progress areas:

- Bill calendar and six-month cash-flow forecast.
- Credit-card statement cycles and payment tracking.
- Subscription and recurring-payment detection.
- Weekly financial digest.
- Richer shader-based motion and visual polish.
- More focused regression tests around backup, restore, migrations, and financial calculations.

## Contributing

This is an active learning and product-building project. Contributions should keep the app private, offline-first, and easy to maintain.

Before opening a pull request:

- Rebase onto the latest `main`.
- Keep the change focused.
- Avoid unrelated dependency, workflow, or Gradle wrapper churn.
- Do not add network-backed runtime features, telemetry, analytics, or cloud-only behavior.
- Preserve user data with explicit migrations.
- Run the full verification command.
