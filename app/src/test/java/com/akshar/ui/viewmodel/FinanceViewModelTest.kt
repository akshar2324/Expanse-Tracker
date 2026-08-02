package com.akshar.ui.viewmodel

import android.app.Application
import com.akshar.ExpenseTrackerApp
import com.akshar.data.model.Budget
import com.akshar.data.model.Debt
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.data.repository.FinanceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.*
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import android.util.Log

class FinanceViewModelTest {

    private lateinit var viewModel: FinanceViewModel
    private val application = mockk<ExpenseTrackerApp>(relaxed = true)
    private val repository = mockk<FinanceRepository>(relaxed = true)

    @Before
    fun setup() {
        every { application.repository } returns repository

        every { repository.allTransactions } returns MutableStateFlow(emptyList())
        every { repository.allBudgets } returns MutableStateFlow(emptyList())
        every { repository.allSavingsGoals } returns MutableStateFlow(emptyList())
        every { repository.allRecurringTransactions } returns MutableStateFlow(emptyList())
        every { repository.allDebts } returns MutableStateFlow(emptyList())
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testExportDataToJsonExceptionHandling() {
        viewModel = FinanceViewModel(application)

        // Mock JSONObject constructor to throw an exception
        mockkConstructor(JSONObject::class)
        every { anyConstructed<JSONObject>().put(any<String>(), any<Any>()) } throws Exception("Mocked Exception for Testing")

        val result = viewModel.exportDataToJson()

        assertNull("Result should be null when an exception occurs during export", result)
    }
}
