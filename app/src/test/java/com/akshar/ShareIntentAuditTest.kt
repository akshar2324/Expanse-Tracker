package com.akshar

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.db.AppDatabase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareIntentAuditTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val message = "Sent Rs.232.00 From HDFC Bank A/C *4821 To SWIGGY On 14/10/26 Ref 629100004321"
    @After fun close() {
        com.akshar.sms.inbox.ImportInboxDatabase.closeAndResetInstance()
        AppDatabase.closeAndResetInstance()
    }
    private fun share(text: CharSequence, locked: Boolean = false) {
        context.getSharedPreferences("vault_settings", Context.MODE_PRIVATE).edit().clear()
            .putBoolean("onboarding_completed", true).putBoolean("vault_enabled", locked)
            .putBoolean("vault_biometrics", false).putString("vault_pin", "2468").commit()
        val intent = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        ActivityScenario.launch<MainActivity>(intent).use {
            if (locked) {
                compose.onNodeWithText("Welcome back").assertExists()
                compose.onNodeWithText("SWIGGY").assertDoesNotExist()
            } else {
                compose.waitUntil(5_000) { compose.onAllNodes(hasText("SWIGGY")).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText("Import bank SMS").assertExists()
                compose.onNodeWithText("Review and save").assertExists()
            }
        }
    }
    @Test fun plainTextShareOpensReviewedDraft() = share(message)
    @Test fun styledTextShareOpensReviewedDraft() = share(SpannableString(message))
    @Test fun externalShareDoesNotBypassVault() = share(message, locked = true)
}
