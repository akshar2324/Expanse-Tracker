package com.akshar

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.model.Account
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.Transaction
import com.akshar.insights.SubscriptionFinder
import com.akshar.insights.SubscriptionDismissals
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.viewmodel.FinanceViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar
import java.util.Locale

/**
 * Month-end forecast on Home and subscription suggestions on Insights, in light and dark, with a
 * seeded history long enough for both. Images go to build/outputs/roborazzi/insights.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class InsightsScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()
    private val stores = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    @After
    fun clearViewModels() { stores.viewModelStore.clear() }

    @Test
    fun insights_light() = capture(dark = false)

    @Test
    fun insights_dark() = capture(dark = true)

    private fun capture(dark: Boolean) {
        Locale.setDefault(Locale.US)
        if (dark) RuntimeEnvironment.setQualifiers("+night")
        val app = ApplicationProvider.getApplicationContext<ExpenseTrackerApp>()
        seed(app)
        runBlocking {
            val dismissed = SubscriptionDismissals(app).keys.first()
            assertEquals("Seed must clear device dismissals", emptySet<String>(), dismissed)
            val found = SubscriptionFinder().find(System.currentTimeMillis(), app.repository.allTransactions.first(),
                app.repository.allRecurringTransactions.first(), dismissed)
            assertEquals("Seed should contain two subscriptions: $found", 2, found.size)
        }
        val viewModel = FinanceViewModel(app)
        val mode = if (dark) "dark" else "light"

        composeTestRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides stores, LocalContext provides app) {
                AkSpendTheme(darkTheme = dark) { AppScaffold(viewModel = viewModel) }
            }
        }
        settle()

        // Home: the forecast card, expanded to show the breakdown.
        waitForText("MONTH-END FORECAST")
        composeTestRule.onNodeWithText("MONTH-END FORECAST").performScrollTo()
        composeTestRule.onNodeWithText("MONTH-END FORECAST").performSemanticsAction(SemanticsActions.OnClick)
        settle()
        composeTestRule.onNodeWithText("Balance now", useUnmergedTree = true).performScrollTo()
        settle()
        composeTestRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/insights/$mode-1-forecast.png")

        // Insights: detected subscriptions.
        tab("Insights")
        waitForText("Possible subscriptions · 2")
        composeTestRule.onNodeWithText("Possible subscriptions · 2").performScrollTo()
        settle()
        composeTestRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/insights/$mode-2-subscriptions.png")

        // One tap makes a reminder through FinanceViewModel; the suggestion then disappears.
        val candidate = runBlocking {
            SubscriptionFinder().find(System.currentTimeMillis(), app.repository.allTransactions.first(), emptyList())
                .single { it.key == "netflix" }
        }
        composeTestRule.onNodeWithContentDescription("Make a reminder for Netflix").performScrollTo().performClick()
        composeTestRule.runOnIdle {
            viewModel.makeSubscriptionReminder(candidate)
            viewModel.makeSubscriptionReminder(candidate)
        }
        settle()
        val reminders = runBlocking { withContext(Dispatchers.IO) { app.repository.allRecurringTransactions.first() } }
        val netflix = reminders.single { it.description == "Netflix" }
        assertEquals(649.0, netflix.amount, 0.0)
        assertEquals("MONTHLY", netflix.frequency)
        assertEquals(seededLastNetflix, netflix.lastTriggered)
        waitForText("Possible subscriptions · 1")

        // "Not a subscription" is remembered on the device.
        composeTestRule.onNodeWithContentDescription("Cloud storage is not a subscription").performScrollTo().performClick()
        settle()
        assertTrue(composeTestRule.onAllNodesWithText("Possible subscriptions", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue("cloud storage" in runBlocking { SubscriptionDismissals(app).keys.first() })
    }

    private fun tab(name: String) {
        composeTestRule.onAllNodes(hasText(name) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))[0].performClick()
        settle()
    }

    /** The insight flows compute on a background dispatcher, so wait for real time to pass. */
    private fun waitForText(text: String) {
        try {
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                Thread.sleep(20)
                composeTestRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            val shown = composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("", substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes().flatMap { it.config.getOrElse(SemanticsProperties.Text) { emptyList() } }.map { it.text }
            throw AssertionError("'$text' not shown; on screen: $shown", e)
        }
    }

    private fun settle() {
        repeat(30) {
            composeTestRule.mainClock.advanceTimeBy(250)
            Thread.sleep(50)
        }
        composeTestRule.waitForIdle()
    }

    private fun monthsAgo(base: Long, months: Int): Long =
        Calendar.getInstance().apply { timeInMillis = base; add(Calendar.MONTH, -months) }.timeInMillis

    private var seededLastNetflix = 0L

    private fun seed(app: ExpenseTrackerApp) = runBlocking {
        withContext(Dispatchers.IO) { com.akshar.data.db.AppDatabase.getInstance(app).clearAllTables() }
        app.getSharedPreferences("insight_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        val repo = app.repository
        val day = 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        repo.insertAccounts(
            listOf(
                Account(id = 1, name = "Everyday Bank", type = "Bank", openingBalance = 42_000.0),
                Account(id = 2, name = "Cash Wallet", type = "Cash", openingBalance = 3_500.0),
            )
        )
        val lastNetflix = now - 3 * day
        seededLastNetflix = lastNetflix
        val lastCloud = now - 10 * day
        val tx = buildList {
            add(Transaction(amount = 85_000.0, type = "INCOME", category = "Salary", description = "Monthly salary", date = now - 40 * day, paymentMethod = "Bank Transfer"))
            add(Transaction(amount = 18_000.0, type = "EXPENSE", category = "Rent", description = "Landlord", date = now - 20 * day, paymentMethod = "Bank Transfer"))
            for (d in 1..40) {
                add(Transaction(amount = 250.0 + (d % 5) * 60, type = "EXPENSE", category = "Groceries", description = "Groceries", date = now - d * day, paymentMethod = "UPI"))
            }
            (0..2).forEach {
                add(Transaction(amount = 649.0, type = "EXPENSE", category = "Entertainment", description = "Netflix", date = monthsAgo(lastNetflix, it), paymentMethod = "Credit Card"))
                add(Transaction(amount = 149.0, type = "EXPENSE", category = "Subscriptions", description = "Cloud storage", date = monthsAgo(lastCloud, it), paymentMethod = "UPI"))
            }
        }
        repo.insertTransactions(tx)
        repo.insertRecurringTransactions(
            listOf(RecurringTransaction(amount = 1_200.0, type = "EXPENSE", category = "Bills", description = "Internet", frequency = "MONTHLY", lastTriggered = now - 25 * day))
        )
    }
}
