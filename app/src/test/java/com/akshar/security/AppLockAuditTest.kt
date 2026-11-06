package com.akshar.security

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.akshar.MainActivity
import com.akshar.data.db.AppDatabase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@org.junit.Ignore("Open audit finding: PIN setup and relock policy need a product decision; see AUDIT_REPORT.md")
class AppLockAuditTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun configure(pin: String? = null) {
        context.getSharedPreferences("vault_settings", Context.MODE_PRIVATE).edit().clear()
            .putBoolean("onboarding_completed", true)
            .putBoolean("vault_enabled", true)
            .putBoolean("vault_biometrics", false)
            .apply { if (pin != null) putString("vault_pin", pin) }.commit()
    }

    @After fun close() {
        com.akshar.sms.inbox.ImportInboxDatabase.closeAndResetInstance()
        AppDatabase.closeAndResetInstance()
    }

    @Test fun defaultPinMustNotUnlockFinancialData() {
        configure()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Welcome back").assertExists()
            "1234".forEach { digit -> compose.onNodeWithText(digit.toString()).performClick() }
            compose.onNodeWithText("Home").assertDoesNotExist()
        }
    }

    @Test fun backgroundedVaultMustRequireUnlockOnReturn() {
        configure("2468")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            "2468".forEach { digit -> compose.onNodeWithText(digit.toString()).performClick() }
            compose.onNodeWithText("Home").assertExists()
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithText("Welcome back").assertExists()
        }
    }
}
