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
import org.junit.Assert.assertFalse
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
        // F16: there is no default passcode. Without one the lock is off and never hints at a code.
        configure()
        assertFalse(AppLock.from(context).isActive)
        configure("2468")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Welcome back").assertExists()
            compose.onNodeWithText("Default passcode", substring = true).assertDoesNotExist()
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
            ShadowSystemClock.advanceBy(Duration.ofMillis(AppLock.RELOCK_AFTER_MILLIS))
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithText("Welcome back").assertExists()
        }
    }

    @Test fun shortTripAway_staysUnlocked() {
        // A file picker or share sheet shorter than the relock delay keeps the session open.
        configure("2468")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            "2468".forEach { digit -> compose.onNodeWithText(digit.toString()).performClick() }
            scenario.moveToState(Lifecycle.State.CREATED)
            ShadowSystemClock.advanceBy(Duration.ofSeconds(5))
            scenario.moveToState(Lifecycle.State.RESUMED)
            compose.onNodeWithText("Welcome back").assertDoesNotExist()
            compose.onNodeWithText("Home").assertExists()
        }
    }
}
