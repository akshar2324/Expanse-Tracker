package com.akshar.ui.viewmodel

import androidx.test.core.app.ApplicationProvider
import com.akshar.ExpenseTrackerApp
import com.akshar.data.model.Debt
import com.akshar.data.model.Transaction
import com.akshar.data.repository.FinanceRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinanceViewModelTest {

    private lateinit var viewModel: FinanceViewModel
    private lateinit var app: ExpenseTrackerApp

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext() as ExpenseTrackerApp
        viewModel = FinanceViewModel(app)
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getCurrencySymbol returns correct symbol for all supported countries`() {
        viewModel.updateCountry("IN")
        assertEquals("₹", viewModel.getCurrencySymbol())

        viewModel.updateCountry("US")
        assertEquals("$", viewModel.getCurrencySymbol())

        viewModel.updateCountry("JP")
        assertEquals("¥", viewModel.getCurrencySymbol())

        viewModel.updateCountry("EU")
        assertEquals("€", viewModel.getCurrencySymbol())
    }

    @Test
    fun `getCurrencySymbol returns fallback symbol for unsupported country`() {
        viewModel.updateCountry("XYZ")
        assertEquals("$", viewModel.getCurrencySymbol())
    }

    @Test
    fun `formatCurrencyValue formats values correctly for IN`() {
        viewModel.updateCountry("IN")
        assertEquals("₹1,234.56", viewModel.formatCurrencyValue(1234.56))
        assertEquals("₹1,000,000.00", viewModel.formatCurrencyValue(1000000.0))
        assertEquals("₹0.00", viewModel.formatCurrencyValue(0.0))
        assertEquals("₹-50.50", viewModel.formatCurrencyValue(-50.50))
    }

    @Test
    fun `formatCurrencyValue formats values correctly for US`() {
        viewModel.updateCountry("US")
        assertEquals("$1,234.56", viewModel.formatCurrencyValue(1234.56))
    }

    @Test
    fun `formatCurrencyValue formats values correctly for JP`() {
        viewModel.updateCountry("JP")
        assertEquals("¥1,234.56", viewModel.formatCurrencyValue(1234.56))
    }

    @Test
    fun `formatCurrencyValue formats values correctly for EU`() {
        viewModel.updateCountry("EU")
        assertEquals("€1,234.56", viewModel.formatCurrencyValue(1234.56))
    }

    @Test
    fun `exportDataToJson returns null when JSON creation fails`() {
        val repository = mockk<FinanceRepository>(relaxed = true)
        val mockApp = mockk<ExpenseTrackerApp>(relaxed = true)
        val invalidTransaction = mockk<Transaction>()
        every { invalidTransaction.amount } throws IllegalStateException("Forced export failure")
        every { mockApp.repository } returns repository
        every { repository.allTransactions } returns MutableStateFlow(listOf(invalidTransaction))
        every { repository.allBudgets } returns MutableStateFlow(emptyList())
        every { repository.allSavingsGoals } returns MutableStateFlow(emptyList())
        every { repository.allRecurringTransactions } returns MutableStateFlow(emptyList())
        every { repository.allDebts } returns MutableStateFlow(emptyList())
        val subject = FinanceViewModel(mockApp)
        val collector = CoroutineScope(Dispatchers.Main.immediate).launch {
            subject.allTransactions.collect { }
        }
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertNull(subject.exportDataToJson())
        collector.cancel()
    }

    @Test
    fun `unresolveDebt preserves debt and marks it unresolved`() {
        val repository = mockk<FinanceRepository>(relaxed = true)
        val mockApp = mockk<ExpenseTrackerApp>(relaxed = true)
        every { mockApp.repository } returns repository
        every { repository.allTransactions } returns MutableStateFlow(emptyList())
        every { repository.allBudgets } returns MutableStateFlow(emptyList())
        every { repository.allSavingsGoals } returns MutableStateFlow(emptyList())
        every { repository.allRecurringTransactions } returns MutableStateFlow(emptyList())
        every { repository.allDebts } returns MutableStateFlow(emptyList())
        val subject = FinanceViewModel(mockApp)
        val debt = Debt(
            id = 1L,
            personName = "Test Person",
            amount = 100.0,
            type = "BORROWED",
            date = 1_000L,
            description = "Test debt",
            isResolved = true
        )

        subject.unresolveDebt(debt)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        coVerify(exactly = 1) {
            repository.insertDebt(match { it.copy(isResolved = true) == debt && !it.isResolved })
        }
        assertFalse(debt.copy(isResolved = false).isResolved)
    }

    @Test
    fun `restoreDataFromJson rejects invalid JSON and sets error status`() {
        assertFalse(viewModel.restoreDataFromJson("this is not valid json"))
        val status = viewModel.backupStatus.value
        assertEquals("Failed to restore backup: invalid JSON format.", status)
    }
}
