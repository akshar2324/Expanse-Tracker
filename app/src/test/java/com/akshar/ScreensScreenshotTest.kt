package com.akshar

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.akshar.ui.screens.AddTransactionScreen
import com.akshar.ui.screens.OnboardingScreen
import com.akshar.ui.theme.GlassBackdrop
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.model.Account
import com.akshar.data.model.Budget
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.viewmodel.FinanceViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the real app shell with a seeded on-device database and captures every main tab.
 * Images go to build/outputs/roborazzi/screens so routine test runs never touch tracked files.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ScreensScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val tabs = listOf("Home", "Activity", "Budgets", "Insights", "More")

  @Test
  fun mainScreens_light() = captureMainScreens(dark = false)

  @Test
  fun mainScreens_dark() = captureMainScreens(dark = true)

  private fun captureMainScreens(dark: Boolean) {
    Locale.setDefault(Locale.US)
    if (dark) RuntimeEnvironment.setQualifiers("+night")
    val app = ApplicationProvider.getApplicationContext<ExpenseTrackerApp>()
    seed(app)
    val viewModel = FinanceViewModel(app)
    val mode = if (dark) "dark" else "light"

    composeTestRule.setContent { AkSpendTheme(darkTheme = dark) { AppScaffold(viewModel = viewModel) } }
    settle()

    tabs.forEachIndexed { index, tab ->
      composeTestRule.onAllNodes(hasText(tab) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))[0].performClick()
      settle()
      composeTestRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/screens/$mode-${index + 1}-${tab.lowercase()}.png")
    }

    // Pushed screens reached from Home: they hide the tab bar and show a back button.
    composeTestRule.onAllNodes(hasText("Home") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))[0].performClick()
    settle()
    composeTestRule.onNodeWithContentDescription("Open accounts").performClick()
    settle()
    composeTestRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/screens/$mode-6-accounts.png")
    composeTestRule.onAllNodesWithContentDescription("Back")[0].performClick()
    settle()
    // Invoke the action directly: once scrolled into view the card can sit under the floating tab bar.
    composeTestRule.onNodeWithText("Your weekly digest").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
    settle()
    composeTestRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/screens/$mode-7-weekly-digest.png")
  }

  @Test
  fun standaloneScreens_light() = captureStandalone(dark = false)

  @Test
  fun standaloneScreens_dark() = captureStandalone(dark = true)

  private fun captureStandalone(dark: Boolean) {
    Locale.setDefault(Locale.US)
    if (dark) RuntimeEnvironment.setQualifiers("+night")
    val app = ApplicationProvider.getApplicationContext<ExpenseTrackerApp>()
    seed(app)
    val viewModel = FinanceViewModel(app)
    val mode = if (dark) "dark" else "light"
    var screen by mutableStateOf("add")

    composeTestRule.setContent {
      AkSpendTheme(darkTheme = dark) {
        when (screen) {
          "add" -> Box(Modifier.fillMaxSize()) {
            GlassBackdrop()
            Box(Modifier.fillMaxSize().safeDrawingPadding()) { AddTransactionScreen(viewModel = viewModel, onNavigateBack = {}) }
          }
          "onboarding" -> OnboardingScreen(viewModel = viewModel, onFinish = {})
          else -> LockScreen(correctPin = "1234", useBiometrics = true, onUnlockSuccess = {}, onTriggerBiometric = {})
        }
      }
    }
    listOf("add" to "8-add-transaction", "onboarding" to "9-onboarding", "lock" to "10-lock").forEach { (key, name) ->
      screen = key
      settle()
      composeTestRule.onRoot().captureRoboImage(filePath = "build/outputs/roborazzi/screens/$mode-$name.png")
    }
  }

  /** Lets Room deliver on its own threads while the test clock runs the one-shot animations. */
  private fun settle() {
    repeat(30) {
      composeTestRule.mainClock.advanceTimeBy(250)
      Thread.sleep(50)
    }
    composeTestRule.waitForIdle()
  }

  private fun seed(app: ExpenseTrackerApp) = runBlocking {
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.akshar.data.db.AppDatabase.getInstance(app).clearAllTables() }
    val repo = app.repository
    val day = 24L * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    repo.insertAccounts(
      listOf(
        Account(id = 1, name = "Everyday Bank", type = "Bank", openingBalance = 42_000.0),
        Account(id = 2, name = "Cash Wallet", type = "Cash", openingBalance = 3_500.0),
        Account(id = 3, name = "Rainy Day", type = "Savings", openingBalance = 120_000.0),
      )
    )
    repo.insertTransactions(
      listOf(
        Transaction(amount = 85_000.0, type = "INCOME", category = "Salary", description = "Monthly salary", date = now - 2 * day, paymentMethod = "Bank Transfer"),
        Transaction(amount = 1_240.50, type = "EXPENSE", category = "Groceries", description = "Groceries", date = now, paymentMethod = "UPI"),
        Transaction(amount = 320.0, type = "EXPENSE", category = "Food", description = "Coffee with friends", date = now, paymentMethod = "Cash", accountId = 2),
        Transaction(amount = 18_000.0, type = "EXPENSE", category = "Rent", description = "Apartment rent", date = now - day, paymentMethod = "Bank Transfer"),
        Transaction(amount = 2_150.0, type = "EXPENSE", category = "Transport", description = "Metro card top-up", date = now - 3 * day, paymentMethod = "UPI"),
        Transaction(amount = 4_999.0, type = "EXPENSE", category = "Shopping", description = "Running shoes", date = now - 4 * day, paymentMethod = "Credit Card"),
        Transaction(amount = 6_500.0, type = "INCOME", category = "Freelance", description = "Logo design", date = now - 5 * day, paymentMethod = "UPI"),
        Transaction(amount = 799.0, type = "EXPENSE", category = "Entertainment", description = "Streaming plan", date = now - 6 * day, paymentMethod = "Credit Card"),
      )
    )
    val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    repo.insertBudgets(
      listOf(
        Budget(category = "Groceries", limitAmount = 8_000.0, month = month),
        Budget(category = "Food", limitAmount = 4_000.0, month = month),
        Budget(category = "Shopping", limitAmount = 5_000.0, month = month),
      )
    )
    repo.insertSavingsGoals(listOf(SavingsGoal(name = "Japan trip", targetAmount = 150_000.0, currentAmount = 62_000.0, targetDate = "Mar 2027")))
    repo.insertRecurringTransactions(
      listOf(
        RecurringTransaction(amount = 499.0, type = "EXPENSE", category = "Subscription", description = "Music plan", frequency = "MONTHLY", lastTriggered = now - 27 * day),
        RecurringTransaction(amount = 18_000.0, type = "EXPENSE", category = "Rent", description = "Apartment rent", frequency = "MONTHLY", lastTriggered = now - day),
      )
    )
  }
}
