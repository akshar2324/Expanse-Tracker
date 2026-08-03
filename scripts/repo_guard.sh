#!/usr/bin/env bash
set -e

echo "Running repository guard..."

# 1. Check for tracked secrets / unwanted files
# We use git ls-files to only check files currently tracked or staged.
# The regex matches exactly what is required.
unwanted_files=$(git ls-files | grep -iE '\.jks$|\.keystore$|\.p12$|\.pem$|\.key$|\.base64$|keystore\.properties|\.env$|\.env\..*|google-services\.json|service-account.*\.json|play-store.*\.json' || true)

if [ -n "$unwanted_files" ]; then
    echo "ERROR: Found restricted files tracked in the repository:"
    echo "$unwanted_files"
    exit 1
fi

# 2. Check for INTERNET permission
manifest_path="app/src/main/AndroidManifest.xml"
if [ -f "$manifest_path" ]; then
    if grep -q "android.permission.INTERNET" "$manifest_path"; then
        echo "ERROR: INTERNET permission found in $manifest_path"
        exit 1
    fi
fi

# 3. Check for Firebase, Crashlytics, Google Services, Secrets Gradle plugin, GEMINI, AI Studio in build files
# Using grep -i to be case-insensitive.
build_files=$(find . -type f \( -name "*.gradle" -o -name "*.gradle.kts" -o -name "libs.versions.toml" \))
for f in $build_files; do
    if grep -iqE 'firebase|crashlytics|google-services|secrets-gradle-plugin|gemini|ai.?studio' "$f"; then
        echo "ERROR: Found restricted keywords (Firebase, Crashlytics, Google Services, Secrets, Gemini, AI Studio) in $f"
        grep -inE 'firebase|crashlytics|google-services|secrets-gradle-plugin|gemini|ai.?studio' "$f"
        exit 1
    fi
done

echo "Repository guard passed."
exit 0
