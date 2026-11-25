package com.akshar.reports

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.db.AppDatabase
import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import com.akshar.data.repository.FinanceRepository
import com.akshar.utils.BudgetMath
import com.akshar.utils.MoneyUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
@Config(sdk = [32], application = Application::class)
class MonthlyReportRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var previousZone: TimeZone
    @Before fun setup() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repository = FinanceRepository(database.financeDao(), database)
    }
    @After fun close() { database.close(); TimeZone.setDefault(previousZone) }

    @Test fun `empty repository month matches report zero totals`() = runTest {
        val report = repository.monthlyReport("2026-11")
        assertEquals(BigDecimal("0.00"), report.income)
        assertEquals(BigDecimal("0.00"), report.expense)
        assertEquals(BigDecimal("0.00"), report.net)
    }

    @Test fun `report matches repository ledger totals with transfers and budgets`() = runTest {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-11-15")!!.time
        fun row(amount: Double, type: String) = Transaction(amount = amount, type = type, category = "Food", description = "Demo", date = date, paymentMethod = "Cash")
        repository.insertTransactions(listOf(row(10.10, "EXPENSE"), row(20.20, "EXPENSE"), row(100.01, "INCOME")))
        repository.saveTransfer(row(500.0, "EXPENSE").copy(transferId = "demo-transfer"), row(500.0, "INCOME").copy(transferId = "demo-transfer"))
        val budget = Budget(category = "Food", limitAmount = 50.0, month = "2026-11")
        repository.insertBudget(budget)
        val ledger = repository.allTransactions.first().filter { it.transferId == null }
        val report = repository.monthlyReport("2026-11")
        val income = MoneyUtils.roundMoney(ledger.filter { it.type == "INCOME" }.sumOf { it.amount })
        val expense = MoneyUtils.roundMoney(ledger.filter { it.type == "EXPENSE" }.sumOf { it.amount })
        assertEquals(income, report.income.toDouble(), 0.0)
        assertEquals(expense, report.expense.toDouble(), 0.0)
        assertEquals(MoneyUtils.roundMoney(income - expense), report.net.toDouble(), 0.0)
        assertEquals(BudgetMath.spentFor(budget, ledger), report.budgets.single().spent.toDouble(), 0.0)
        assertEquals(2, report.topExpenses.size)
    }
}
