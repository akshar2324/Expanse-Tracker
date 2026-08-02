# Repository Guidelines

AkSpend is an offline-first Android personal finance app built with Kotlin, Jetpack Compose, Room, coroutines, and Gradle.

## Product Requirements

- Every core workflow must work without internet access. Do not add a runtime network dependency, cloud-only feature, telemetry, advertising, or analytics.
- Do not display the name "Akshar" anywhere in the UI, notifications, widgets, exported content, or store-facing copy. Existing `com.akshar` package names, namespaces, application IDs, and source paths are technical identifiers and should remain unchanged unless a migration is explicitly requested.
- Financial data stays on the device unless the user explicitly exports or shares it.
- Shader and animation effects are progressive enhancement. Navigation, input, charts, and financial values must remain usable when effects are unavailable or reduced motion is enabled.

## Engineering Expectations

- Follow the existing Compose, ViewModel, repository, and Room patterns.
- Keep database and file work off the main thread and scope coroutines to a lifecycle owner.
- Use precise representations and explicit rounding rules for money. Validate signs, zero values, locale parsing, and boundary cases.
- Keep UI state stable across recomposition and configuration changes. Avoid endless animation work when a composable is not visible.
- Never commit signing keys, API keys, credentials, local paths, generated builds, or personal data.
- Add focused tests for changed financial calculations, persistence, backup/restore, and ViewModel behavior.

## Verification

Run the following before submitting a pull request:

```bash
./gradlew compileDebugKotlin testDebugUnitTest lintDebug assembleDebug
```
