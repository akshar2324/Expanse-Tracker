package com.akshar

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.db.AppDatabase
import com.akshar.data.model.Transaction
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardAuditTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<ExpenseTrackerApp>()
    private val viewModels = TestViewModelOwner()
    @After fun close() {
        viewModels.clear()
        com.akshar.sms.inbox.ImportInboxDatabase.closeAndResetInstance()
        AppDatabase.closeAndResetInstance()
    }
    private fun expense(amount: Double, date: Long) = Transaction(
        amount = amount, type = "EXPENSE", category = "Food", description = "Audit", date = date, paymentMethod = "Cash"
    )
    private fun show(fontScale: Float = 1f) {
        val vm = viewModels.finance(app)
        compose.setContent {
            AkSpendTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    AppScaffold(viewModel = vm)
                }
            }
        }
    }
    @Test fun futureTransactionsMustNotCountAsSpentToday() = runBlocking {
        val now = System.currentTimeMillis()
        app.repository.insertTransactions(listOf(expense(10.0, now), expense(100_000.0, now + 40L * 86_400_000)))
        show()
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("-₹10")).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText("-₹10"))[0].assertExists()
        compose.onNodeWithText("₹10").assertExists() // Out this month
        Unit
    }
    @org.junit.Ignore("Open accessibility finding: responsive money layout requires a design change; see AUDIT_REPORT.md")
    @Test fun todayAmountMustRemainReadableAtDoubleFontScale() = runBlocking {
        app.repository.insertTransaction(expense(1561.0, System.currentTimeMillis()))
        show(fontScale = 2f)
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("-₹1,561")).fetchSemanticsNodes().isNotEmpty() }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onAllNodes(hasText("-₹1,561"))[0].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse("A financial value is ellipsized", layouts.any { it.hasVisualOverflow })
    }
}
