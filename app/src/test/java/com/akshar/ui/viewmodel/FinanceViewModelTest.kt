package com.akshar.ui.viewmodel

import androidx.test.core.app.ApplicationProvider
import com.akshar.ExpenseTrackerApp
import io.mockk.anyConstructed
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
        mockkConstructor(JSONObject::class)
        every { anyConstructed<JSONObject>().put(any<String>(), any<Any>()) } throws
            IllegalStateException("Forced JSON failure")

        assertNull(viewModel.exportDataToJson())
    }
}
