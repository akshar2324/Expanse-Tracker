# 🧹 Extract data calculation logic from TrendLineChart

## Description
* 🎯 **What:** The overly long function `TrendLineChart` in `AnalyticsScreen.kt` has been refactored. The data calculation logic was extracted into a new top-level helper function, `calculateTrendPoints`.
* 💡 **Why:** This improves maintainability and readability by cleanly separating the data points calculation from the UI drawing code.
* ✅ **Verification:** Verified by successfully running all unit tests (e.g. `./gradlew test`) which ensured there were no regressions introduced. Also passed review checking for safety.
* ✨ **Result:** The size and complexity of the `TrendLineChart` component is reduced and its concerns are properly separated.
