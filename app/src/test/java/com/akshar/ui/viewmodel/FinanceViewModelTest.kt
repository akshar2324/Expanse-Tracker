package com.akshar.ui.viewmodel

import androidx.test.core.app.ApplicationProvider
import com.akshar.ExpenseTrackerApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.akshar.data.model.Debt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinanceViewModelTest {

    private lateinit var viewModel: FinanceViewModel
    private lateinit var app: ExpenseTrackerApp

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext() as ExpenseTrackerApp
        viewModel = FinanceViewModel(app)
        // Set standard locale to ensure test consistency
        Locale.setDefault(Locale.US)
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
    fun `resolveDebt should update debt isResolved to true`() = runBlocking {
        // Given
        val debt = Debt(
            personName = "John",
            amount = 100.0,
            type = "BORROWED",
            date = System.currentTimeMillis(),
            description = "Lunch",
            isResolved = false
        )

        app.repository.insertDebt(debt)

        val allDebts = app.repository.allDebts.first()
        val insertedDebt = allDebts.first { it.personName == "John" && it.description == "Lunch" }

        // When
        viewModel.resolveDebt(insertedDebt)

        // Wait for coroutine inside viewModel to finish execution
        Thread.sleep(100)

        // Then
        val updatedDebts = app.repository.allDebts.first()
        val updatedDebt = updatedDebts.first { it.id == insertedDebt.id }
        assertTrue(updatedDebt.isResolved)
    }

    @Test
    fun `deleteDebt should remove debt from repository`() = runBlocking {
        // Given
        val debt = Debt(
            personName = "Bob",
            amount = 200.0,
            type = "BORROWED",
            date = System.currentTimeMillis(),
            description = "Rent",
            isResolved = false
        )

        app.repository.insertDebt(debt)

        val allDebts = app.repository.allDebts.first()
        val insertedDebt = allDebts.first { it.personName == "Bob" && it.description == "Rent" }

        // When
        viewModel.deleteDebt(insertedDebt)

        // Wait for coroutine inside viewModel to finish execution
        Thread.sleep(100)

        // Then
        val remainingDebts = app.repository.allDebts.first()
        assertFalse(remainingDebts.any { it.id == insertedDebt.id })
    }
}
