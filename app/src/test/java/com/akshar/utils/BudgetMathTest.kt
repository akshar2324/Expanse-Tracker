package com.akshar.utils

import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetMathTest {

    private val now = System.currentTimeMillis()
    private val month = PeriodUtils.monthKey(now)
    private val week = PeriodUtils.weekKey(now)

    private fun expense(category: String, amount: Double, transferId: String? = null, date: Long = now) = Transaction(
        amount = amount, type = "EXPENSE", category = category, description = "", date = date,
        paymentMethod = "Cash", transferId = transferId
    )

    @Test
    fun `remaining only counts spending in budgeted categories`() {
        // Mirrors the Home vs Budgets mismatch: unbudgeted Rent must not use up the budgets.
        val budgets = listOf(
            Budget(id = 1, category = "Grocery", limitAmount = 8000.0, month = month),
            Budget(id = 2, category = "Food", limitAmount = 4000.0, month = month),
            Budget(id = 3, category = "Shopping", limitAmount = 5000.0, month = month)
        )
        val txs = listOf(
            expense("Rent", 20000.0),
            expense("Grocery", 3000.0),
            expense("food", 1559.0), // category match ignores case
            expense("Shopping", 2000.0),
            expense("Transfer", 500.0, transferId = "t")
        )
        val summary = BudgetMath.summary(BudgetMath.activeBudgets(budgets, now), txs)
        assertEquals(17000.0, summary.totalLimit, 0.0)
        assertEquals(6559.0, summary.totalSpent, 0.0)
        assertEquals(10441.0, summary.remaining, 0.0)
    }

    @Test
    fun `active budgets include the current week and month only`() {
        val budgets = listOf(
            Budget(id = 1, category = "Food", limitAmount = 100.0, month = month),
            Budget(id = 2, category = "Food", limitAmount = 30.0, month = week, frequency = "WEEKLY"),
            Budget(id = 3, category = "Food", limitAmount = 100.0, month = "2001-01"),
            Budget(id = 4, category = "Food", limitAmount = 100.0, month = month, frequency = "")
        )
        assertEquals(listOf(1L, 2L, 4L), BudgetMath.activeBudgets(budgets, now).map { it.id })
    }

    @Test
    fun `overspending clamps remaining at zero and used at one`() {
        val budget = Budget(category = "Food", limitAmount = 50.0, month = month)
        val summary = BudgetMath.summary(listOf(budget), listOf(expense("Food", 80.0)))
        assertEquals(0.0, summary.remaining, 0.0)
        assertEquals(1.0, summary.usedFraction, 0.0)
    }

    @Test
    fun `no budgets means nothing used`() {
        val summary = BudgetMath.summary(emptyList(), listOf(expense("Food", 80.0)))
        assertEquals(0.0, summary.totalLimit, 0.0)
        assertEquals(0.0, summary.usedFraction, 0.0)
    }
}
