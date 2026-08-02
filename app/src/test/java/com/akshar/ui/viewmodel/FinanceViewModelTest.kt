package com.akshar.ui.viewmodel

import androidx.test.core.app.ApplicationProvider
import com.akshar.ExpenseTrackerApp
import com.akshar.data.db.FinanceDao
import com.akshar.data.model.Budget
import com.akshar.data.model.Debt
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceViewModelTest {

    private lateinit var viewModel: FinanceViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val insertedDebts = mutableListOf<Debt>()

    private val fakeDao = object : FinanceDao {
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(emptyList())
        override suspend fun insertTransaction(transaction: Transaction) {}
        override suspend fun deleteTransaction(transaction: Transaction) {}
        override suspend fun deleteTransactionById(id: Long) {}
        override suspend fun getTransactionById(id: Long): Transaction? = null

        override fun getAllBudgets(): Flow<List<Budget>> = flowOf(emptyList())
        override fun getBudgetsForMonth(month: String): Flow<List<Budget>> = flowOf(emptyList())
        override suspend fun insertBudget(budget: Budget) {}
        override suspend fun deleteBudgetById(id: Long) {}

        override fun getAllSavingsGoals(): Flow<List<SavingsGoal>> = flowOf(emptyList())
        override suspend fun insertSavingsGoal(goal: SavingsGoal) {}
        override suspend fun deleteSavingsGoalById(id: Long) {}

        override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> = flowOf(emptyList())
        override suspend fun getRecurringTransactionsList(): List<RecurringTransaction> = emptyList()
        override suspend fun insertRecurringTransaction(recurring: RecurringTransaction) {}
        override suspend fun deleteRecurringTransactionById(id: Long) {}

        override fun getAllDebts(): Flow<List<Debt>> = flowOf(emptyList())
        override suspend fun insertDebt(debt: Debt) {
            insertedDebts.add(debt)
        }
        override suspend fun deleteDebt(debt: Debt) {}
        override suspend fun deleteDebtById(id: Long) {}
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val app = ApplicationProvider.getApplicationContext<ExpenseTrackerApp>()

        // Use reflection to inject FakeRepository
        val repositoryField: Field = ExpenseTrackerApp::class.java.getDeclaredField("repository")
        repositoryField.isAccessible = true
        repositoryField.set(app, FinanceRepository(fakeDao))

        viewModel = FinanceViewModel(app)
        insertedDebts.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun unresolveDebt_setsIsResolvedToFalse_andCallsRepository() = runTest {
        // Arrange
        val initialDebt = Debt(
            id = 1L,
            personName = "John Doe",
            amount = 100.0,
            type = "BORROWED",
            date = 1000L,
            description = "Test debt",
            isResolved = true // initially resolved
        )

        // Act
        viewModel.unresolveDebt(initialDebt)

        // Assert
        assertEquals("insertDebt should be called exactly once", 1, insertedDebts.size)
        val insertedDebt = insertedDebts.first()
        assertEquals("The id should remain the same", initialDebt.id, insertedDebt.id)
        assertFalse("isResolved should be false after unresolveDebt is called", insertedDebt.isResolved)
    }
}
