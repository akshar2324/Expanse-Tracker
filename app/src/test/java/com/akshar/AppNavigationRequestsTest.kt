package com.akshar

import android.content.Intent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.db.AppDatabase
import com.akshar.ui.theme.AkSpendTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppNavigationRequestsTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<ExpenseTrackerApp>()
    private val viewModels = TestViewModelOwner()

    @After fun close() {
        AppNavigationRequests.consumed()
        viewModels.clear()
        com.akshar.sms.inbox.ImportInboxDatabase.closeAndResetInstance()
        AppDatabase.closeAndResetInstance()
    }

    @Test fun openIntentCarriesTheRouteAndOnlyKnownRoutesAreAccepted() {
        val intent = AppNavigationRequests.openIntent(app, AppNavigationRequests.ADD_TRANSACTION)
        assertEquals(AppNavigationRequests.ADD_TRANSACTION, intent.getStringExtra(AppNavigationRequests.EXTRA_OPEN_TAB))
        assertEquals(MainActivity::class.java.name, intent.component?.className)

        AppNavigationRequests.handle(Intent().putExtra(AppNavigationRequests.EXTRA_OPEN_TAB, "settings/delete_everything"))
        assertNull(AppNavigationRequests.pendingTab.value)

        AppNavigationRequests.handle(intent)
        assertEquals(AppNavigationRequests.ADD_TRANSACTION, AppNavigationRequests.pendingTab.value)
    }

    @Test fun addRequestOpensTheAddFormOnceTheScaffoldShows() {
        // Requested before the scaffold exists, as when the app lock is showing first.
        AppNavigationRequests.handle(AppNavigationRequests.openIntent(app, AppNavigationRequests.ADD_TRANSACTION))
        val vm = viewModels.finance(app)
        compose.setContent { AkSpendTheme { AppScaffold(viewModel = vm) } }
        compose.waitUntil(5_000) { AppNavigationRequests.pendingTab.value == null }
        compose.onNodeWithText("Save expense").assertExists()
    }
}
