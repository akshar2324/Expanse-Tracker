# Release Checklist

This document provides a checklist for maintaining, building, and releasing new versions of AkSpend. Follow these steps to ensure that the app remains secure, offline-first, and Play Store ready.

## 1. Version Bump

Before building a release, update the application version in the app-level `build.gradle.kts`:
- [ ] Increment the `versionCode` (must be an integer, larger than the previous release).
- [ ] Update the `versionName` (e.g., from `1.4.0` to `1.4.1` or `1.5.0`).

## 2. Privacy & Offline Checks

AkSpend is committed to being offline-first and privacy-focused:
- [ ] Confirm no network permissions (`android.permission.INTERNET`) have been accidentally added to `AndroidManifest.xml` unless absolutely required for a specific, documented feature (currently none).
- [ ] Verify no telemetry, analytics, or remote tracking libraries were introduced.
- [ ] Check that biometric authentication and local JSON backups are functioning as expected without external dependencies.
- [ ] Ensure that sensitive data (like SharedPreferences) is excluded from Android Auto Backup.

## 3. Debug & Release Build Verification

Run the full verification suite locally before attempting a release:
- [ ] Execute `./gradlew compileDebugKotlin testDebugUnitTest lintDebug assembleDebug` to ensure there are no compilation or linting errors.
- [ ] Verify that all Room database migrations are explicit and non-destructive.

## 4. Signing-Key Handling

When building for the Google Play Store or producing a release APK/AAB:
- [ ] Ensure your `keystore.properties` is configured correctly but **not committed** to the repository.
- [ ] Keep your production signing keys securely backed up offline.
- [ ] **Play Upload Key Warning:** If you are using Play App Signing, make sure you are signing the bundle with your specific Upload Key. Keep this Upload Key secure. If you lose it, you will need to contact Google Support to reset it.
- [ ] Build the release artifact: `./gradlew bundleRelease` or `./gradlew assembleRelease`.

## 5. Final Review

- [ ] Check for hardcoded API keys, private credentials, or secrets in the codebase.
- [ ] Validate that the app branding correctly says "AkSpend" and not the technical identifier package name.
- [ ] Review documentation updates and ensure `README.md` and this checklist are up-to-date.
