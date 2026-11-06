package com.akshar

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.db.AppDatabase
import com.akshar.data.model.Transaction
import com.akshar.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppNavGraphTest {

    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var app: ExpenseTrackerApp
    private lateinit var viewModel: FinanceViewModel
    private lateinit var navController: NavHostController

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext() as ExpenseTrackerApp
        viewModel = FinanceViewModel(app)
        composeTestRule.setContent {
            navController = rememberNavController()
            AppNavGraph(navController = navController, viewModel = viewModel)
        }
    }

    @After
    fun tearDown() {
        AppDatabase.closeAndResetInstance()
    }

    @Test
    fun `dashboard add button opens add transaction screen`() {
        composeTestRule.onNodeWithContentDescription("Add transaction").performClick()
        composeTestRule.waitForIdle()

        assertEquals(TransactionRoutes.ADD, navController.currentDestination?.route)
        composeTestRule.onNodeWithText("Save expense").assertExists()
    }

    @Test
    fun `edit route loads existing transaction and saves it in place`() {
        val existing = Transaction(
            id = 42L,
            amount = 250.5,
            type = "EXPENSE",
            category = "Food",
            description = "Lunch",
            date = 1_700_000_000_000L,
            paymentMethod = "Cash"
        )
        runBlocking { app.repository.insertTransaction(existing) }

        composeTestRule.runOnUiThread { navController.navigate(TransactionRoutes.edit(42L)) }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("250.5")).fetchSemanticsNodes().isNotEmpty()
        }

        assertEquals(TransactionRoutes.EDIT, navController.currentDestination?.route)
        composeTestRule.onNodeWithText("Edit transaction").assertExists()

        // Switching to income resets "Food" to the first income category
        composeTestRule.onNodeWithText("Income").performClick()
        composeTestRule.onNodeWithText("Save income").assertIsDisplayed().performClick() // pinned: visible without scrolling
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.repository.getTransactionById(42L)?.type == "INCOME" }
        }

        val saved = runBlocking { app.repository.allTransactions.first() }
        assertEquals(1, saved.size)
        assertEquals(existing.copy(type = "INCOME", category = "Salary"), saved.single())
    }

    @Test
    fun `bank sms is read, reviewed and saved as an expense`() {
        composeTestRule.runOnUiThread { navController.navigate(SmsImportRoutes.IMPORT) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bank message").performTextInput(
            "Sent Rs.1,240.50 From HDFC Bank A/C *4821 To SWIGGY On 14/10/26 Ref 629100004321"
        )
        composeTestRule.onNodeWithText("Read message").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Review and save")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("SWIGGY").assertExists()
        composeTestRule.onNodeWithText("Review and save").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(SmsImportRoutes.REVIEW, navController.currentDestination?.route)
        composeTestRule.onNodeWithText("1240.50").assertExists()
        composeTestRule.onNodeWithText("Save expense").assertIsDisplayed().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.repository.allTransactions.first().isNotEmpty() }
        }

        val saved = runBlocking { app.repository.allTransactions.first() }.single()
        assertEquals(1240.5, saved.amount, 0.0)
        assertEquals("EXPENSE", saved.type)
        assertEquals("Food", saved.category)
        assertEquals("SWIGGY", saved.description)
        // Back on the importer, cleared and ready for the next message.
        composeTestRule.waitForIdle()
        assertEquals(SmsImportRoutes.IMPORT, navController.currentDestination?.route)
        composeTestRule.onNodeWithText("Saved. Paste or share the next message to keep going.").assertExists()
    }
}
