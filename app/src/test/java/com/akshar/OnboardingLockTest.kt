package com.akshar

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.db.AppDatabase
import com.akshar.security.AppLock
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingLockTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After fun close() {
        context.getSharedPreferences(AppLock.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        com.akshar.sms.inbox.ImportInboxDatabase.closeAndResetInstance()
        AppDatabase.closeAndResetInstance()
    }

    @Test fun currencyStepExplainsOptInLock_andFinishesWithoutDefaultPasscode() {
        context.getSharedPreferences(AppLock.PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("Continue").performClick()
            compose.onNodeWithText("Continue").performClick()
            compose.onNodeWithText("App lock is off until you set a passcode.", substring = true).assertExists()
            compose.onNodeWithText("1234", substring = true).assertDoesNotExist()
            compose.onNodeWithText("Get started").performClick()
            compose.onNodeWithText("NET BALANCE").assertExists()
            assertFalse(AppLock.from(context).hasPin)
            assertFalse(AppLock.from(context).isActive)
        }
    }
}
