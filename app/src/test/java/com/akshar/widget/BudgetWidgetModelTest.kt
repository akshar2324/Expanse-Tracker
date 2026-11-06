package com.akshar.widget

import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import com.akshar.utils.BudgetMath
import com.akshar.utils.PeriodUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.Calendar

class BudgetWidgetModelTest {

    private val now = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.JUNE, 15, 12, 0)
    }.timeInMillis
    private val month = PeriodUtils.monthKey(now)
    private val week = PeriodUtils.weekKey(now)

    private fun expense(category: String, amount: Double, transferId: String? = null) = Transaction(
        amount = amount, type = "EXPENSE", category = category, description = "", date = now,
        paymentMethod = "Cash", transferId = transferId
    )

    private val budgets = listOf(
        Budget(id = 1, category = "Grocery", limitAmount = 8000.0, month = month),
        Budget(id = 2, category = "Food", limitAmount = 4000.0, month = month),
        Budget(id = 3, category = "Shopping", limitAmount = 5000.0, month = month),
        Budget(id = 4, category = "Fuel", limitAmount = 1000.0, month = month),
        Budget(id = 5, category = "Food", limitAmount = 999.0, month = "2001-01")
    )
    private val transactions = listOf(
        expense("Rent", 20000.0),
        expense("Grocery", 3000.0),
        expense("food", 1559.0),
        expense("Shopping", 2000.0),
        expense("Fuel", 1200.0),
        expense("Shopping", 500.0, transferId = "t")
    )

    @Test
    fun `totals match the shared budget math`() {
        val state = BudgetWidgetModel.build(budgets, transactions, now)
        val summary = BudgetMath.summary(BudgetMath.activeBudgets(budgets, now), transactions)
        assertEquals(summary.totalLimit, state.totalLimit, 0.0)
        assertEquals(BudgetMath.totalSpentInMonth(transactions, month), state.totalSpent, 0.0)
        assertEquals(18000.0, state.totalLimit, 0.0)
        assertEquals(27759.0, state.totalSpent, 0.0)
        assertEquals(0.0, state.remaining, 0.0)
        assertEquals(9759.0, state.overBy, 0.0)
        assertEquals(1f, state.fraction, 0f)
        assertEquals(4, state.budgetCount)
    }

    @Test
    fun `rows show the three budgets closest to their limit`() {
        val rows = BudgetWidgetModel.build(budgets, transactions, now).rows
        assertEquals(listOf("Fuel", "Shopping", "Food"), rows.map { it.label })
        assertTrue(rows[0].isOver)
        assertEquals(1f, rows[0].fraction, 0f)
        assertEquals(0.4f, rows[1].fraction, 1e-6f)
    }

    @Test
    fun `over budget total reports the overspend`() {
        val state = BudgetWidgetModel.build(
            listOf(Budget(id = 1, category = "Food", limitAmount = 100.0, month = month)),
            listOf(expense("Food", 130.25)),
            now
        )
        assertTrue(state.isOver)
        assertEquals(30.25, state.overBy, 0.0)
        assertEquals(0.0, state.remaining, 0.0)
        assertEquals(1f, state.fraction, 0f)
    }

    @Test
    fun `weekly limits are excluded from monthly totals and rows`() {
        val weekly = BudgetWidgetModel.build(
            listOf(Budget(id = 1, category = "Food", limitAmount = 50.0, month = week, frequency = "WEEKLY")),
            listOf(expense("Food", 10.0)),
            now
        )
        assertFalse(weekly.hasBudgets)
        assertTrue(weekly.rows.isEmpty())
        assertEquals(0.0, weekly.totalLimit, 0.0)
        assertEquals(10.0, weekly.totalSpent, 0.0)

        val empty = BudgetWidgetModel.build(emptyList(), transactions, now)
        assertFalse(empty.hasBudgets)
        assertEquals(0f, empty.fraction, 0f)
        assertEquals(27759.0, empty.totalSpent, 0.0)
    }

    @Test
    fun `zero limit budget does not divide by zero`() {
        val state = BudgetWidgetModel.build(
            listOf(Budget(id = 1, category = "Food", limitAmount = 0.0, month = month)),
            listOf(expense("Food", 5.0)),
            now
        )
        assertEquals(0f, state.rows.single().fraction, 0f)
        assertTrue(state.isOver)
    }

    @Test
    fun `monthly spending includes unbudgeted expenses but excludes income transfers and other months`() {
        val otherMonth = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -1)
        }.timeInMillis
        val rows = listOf(
            expense("Food", 1.01), expense("Rent", 2.02),
            expense("Food", 100.0).copy(type = "INCOME"),
            expense("Food", 100.0, transferId = "transfer"),
            expense("Food", 100.0).copy(date = otherMonth)
        )
        val state = BudgetWidgetModel.build(budgets, rows, now)
        assertEquals(3.03, state.totalSpent, 0.0)
        assertEquals(1.01, state.rows.first { it.label == "Food" }.spent, 0.0)
    }

    @Test
    fun `money is shown in whole units with the app currency`() {
        assertEquals("₹1,560", BudgetWidgetModel.formatShort(1559.5, BudgetWidgetModel.currencySymbol("IN"), Locale.US))
        assertEquals("$0", BudgetWidgetModel.formatShort(0.0, BudgetWidgetModel.currencySymbol(null), Locale.US))
        assertEquals("€", BudgetWidgetModel.currencySymbol("EU"))
    }
}
