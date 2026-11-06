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
        runBlocking { com.akshar.sms.inbox.ImportInboxDatabase.getInstance(app).pendingImportDao().clear() }
        com.akshar.sms.inbox.ImportInboxDatabase.closeAndResetInstance()
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
    fun `deleting a transfer requires confirmation and cancel preserves both legs`() {
        val from = Transaction(amount = 25.0, type = "EXPENSE", category = "Transfer", description = "Move", date = 1L, paymentMethod = "Transfer", accountId = 1L, transferId = "audit-transfer")
        runBlocking {
            app.repository.insertAccount(com.akshar.data.model.Account(id = 2L, name = "Bank", type = "Bank"))
            app.repository.saveTransfer(from, from.copy(type = "INCOME", accountId = 2L))
        }
        composeTestRule.runOnUiThread { navController.navigate("addTransfer/audit-transfer") }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete transfer").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Delete transfer?").assertExists()
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertEquals(2, runBlocking { app.repository.getTransferLegs("audit-transfer").size })
        composeTestRule.onNodeWithText("Delete transfer").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitUntil(5_000) { runBlocking { app.repository.getTransferLegs("audit-transfer").isEmpty() } }
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

    private fun readAndReview(message: String) {
        composeTestRule.runOnUiThread { navController.navigate(SmsImportRoutes.IMPORT) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Bank message").performTextInput(message)
        composeTestRule.onNodeWithText("Read message").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Review and save")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Review and save").performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `saving the same bank sms twice warns, and save anyway adds it`() {
        val sms = "Sent Rs.232.00 From HDFC Bank A/C *4821 To KUMAR STORE On 14/10/26 Ref 629100004321"
        readAndReview(sms)
        composeTestRule.onNodeWithText("Save expense").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { runBlocking { app.repository.allTransactions.first().size == 1 } }
        val first = runBlocking { app.repository.allTransactions.first() }.single()
        assertEquals(com.akshar.utils.DuplicateCheck.referenceKey("EXPENSE", "629100004321"), first.originalCsvRowHash)

        // Same message again: warned, and Cancel keeps the form without saving.
        composeTestRule.waitForIdle()
        readAndReview(sms)
        composeTestRule.onNodeWithText("Save expense").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Already saved")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        assertEquals(SmsImportRoutes.REVIEW, navController.currentDestination?.route)
        assertEquals(1, runBlocking { app.repository.allTransactions.first().size })

        composeTestRule.onNodeWithText("Save expense").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Save anyway")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Save anyway").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { runBlocking { app.repository.allTransactions.first().size == 2 } }
    }

    @Test
    fun `hand-entered duplicates save without a warning`() {
        val today = System.currentTimeMillis()
        runBlocking {
            app.repository.insertTransaction(
                Transaction(amount = 50.0, type = "EXPENSE", category = "Food", description = "Tea", date = today, paymentMethod = "Cash")
            )
        }
        composeTestRule.runOnUiThread { navController.navigate(TransactionRoutes.ADD) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Amount").performTextInput("50")
        composeTestRule.onNodeWithText("Save expense").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { runBlocking { app.repository.allTransactions.first().size == 2 } }
    }

    @Test
    fun `manual add preselects the category learned for a merchant`() {
        runBlocking { app.repository.learnCategory("Blue Tokai", "EXPENSE", "Entertainment") }
        composeTestRule.runOnUiThread { navController.navigate(TransactionRoutes.ADD) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Amount").performTextInput("180")
        composeTestRule.onNodeWithText("Description").performScrollTo().performTextInput("BLUE TOKAI")
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("Category set from what you picked for this before.")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Save expense").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { runBlocking { app.repository.allTransactions.first().isNotEmpty() } }
        assertEquals("Entertainment", runBlocking { app.repository.allTransactions.first() }.single().category)
    }

    @Test
    fun `queued notification payment is reviewed, saved and removed from the inbox`() {
        val dao = com.akshar.sms.inbox.ImportInboxDatabase.getInstance(app).pendingImportDao()
        val queued = com.akshar.sms.inbox.AutoImportCapture.capture(
            "com.google.android.apps.messaging", "JD-HDFCBK-S",
            "Sent Rs.799.00 From HDFC Bank A/C *4821 To NETFLIX On 14/10/26 Ref 629100004321",
            postedAt = System.currentTimeMillis(), supported = true
        )!!
        runBlocking { dao.insert(queued) }

        composeTestRule.runOnUiThread { navController.navigate(SmsImportRoutes.INBOX) }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(hasText("NETFLIX")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Review").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(SmsImportRoutes.REVIEW, navController.currentDestination?.route)

        composeTestRule.onNodeWithText("Save expense").assertIsDisplayed().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.repository.allTransactions.first().isNotEmpty() && dao.observeCount().first() == 0 }
        }
        val saved = runBlocking { app.repository.allTransactions.first() }.single()
        assertEquals(799.0, saved.amount, 0.0)
        assertEquals("Subscription", saved.category)
        assertEquals(SmsImportRoutes.INBOX, navController.currentDestination?.route)
    }
}
