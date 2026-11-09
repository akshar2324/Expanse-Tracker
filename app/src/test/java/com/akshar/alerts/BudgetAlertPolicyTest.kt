package com.akshar.alerts

import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import com.akshar.utils.PeriodUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BudgetAlertPolicyTest {
    private val now = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 9, 12, 0, 0) }.timeInMillis
    private val month = PeriodUtils.monthKey(now)
    private fun budget(limit: Double = 100.0, category: String = "Food") = Budget(category = category, limitAmount = limit, month = month)
    private fun expense(amount: Double, category: String = "Food", date: Long = now) = Transaction(
        amount = amount, category = category, date = date, type = "EXPENSE", description = "", paymentMethod = "Cash")
    private fun alerts(amount: Double, sent: Set<String> = emptySet()) =
        BudgetAlertPolicy.pending(listOf(budget()), listOf(expense(amount)), sent, now)

    @Test fun `exact 80 percent and 100 percent boundaries`() {
        assertTrue(alerts(79.99).isEmpty())
        assertEquals(listOf(80), alerts(80.0).map { it.level })
        assertEquals(listOf(80), alerts(99.99).map { it.level })
        assertEquals(listOf(80, 100), alerts(100.0).map { it.level })
        assertEquals(listOf(80, 100), alerts(120.0).map { it.level })
    }

    @Test fun `decimal sums and thresholds use exact cents with half up rounding`() {
        val rows = List(8) { expense(0.1) }
        assertEquals(listOf(80), BudgetAlertPolicy.pending(listOf(budget(1.0)), rows, emptySet(), now).map { it.level })
        assertTrue(BudgetAlertPolicy.pending(listOf(budget(0.03)), listOf(expense(0.02)), emptySet(), now).isEmpty())
        assertEquals(listOf(80, 100), BudgetAlertPolicy.pending(listOf(budget(1.0)), listOf(expense(0.995)), emptySet(), now).map { it.level })
    }

    @Test fun `each level sends once even after spending falls and rises or budget is recreated`() {
        val sent80 = alerts(80.0).map { it.key }.toSet()
        assertTrue(alerts(50.0, sent80).isEmpty())
        assertTrue(alerts(80.0, sent80).isEmpty())
        val hundred = alerts(100.0, sent80)
        assertEquals(listOf(100), hundred.map { it.level })
        val sent = sent80 + hundred.map { it.key }
        assertTrue(BudgetAlertPolicy.pending(listOf(budget().copy(id = 42)), listOf(expense(120.0)), sent, now).isEmpty())
    }

    @Test fun `zero negative nonfinite and missing budgets never notify`() {
        val budgets = listOf(budget(0.0), budget(-1.0), budget(Double.NaN), budget(Double.POSITIVE_INFINITY))
        assertTrue(BudgetAlertPolicy.pending(budgets, listOf(expense(100.0)), emptySet(), now).isEmpty())
        assertTrue(BudgetAlertPolicy.pending(emptyList(), listOf(expense(100.0)), emptySet(), now).isEmpty())
    }

    @Test fun `new month resets levels but ignores previous spending and budgets`() {
        val next = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, 1) }.timeInMillis
        val nextMonth = PeriodUtils.monthKey(next)
        val sent = alerts(100.0).map { it.key }.toSet()
        assertTrue(BudgetAlertPolicy.pending(listOf(budget()), listOf(expense(100.0)), sent, next).isEmpty())
        val nextBudget = budget().copy(month = nextMonth)
        assertTrue(BudgetAlertPolicy.pending(listOf(nextBudget), listOf(expense(100.0)), sent, next).isEmpty())
        assertEquals(listOf(80, 100), BudgetAlertPolicy.pending(listOf(nextBudget), listOf(expense(100.0, date = next)), sent, next).map { it.level })
    }

    @Test fun `only monthly category expenses count excluding income and transfers`() {
        val budgets = listOf(budget(), budget().copy(frequency = "WEEKLY"), budget(category = "Rent"))
        val rows = listOf(expense(80.0, "food"), expense(100.0).copy(type = "INCOME"), expense(100.0).copy(transferId = "transfer"))
        val pending = BudgetAlertPolicy.pending(budgets, rows, emptySet(), now)
        assertEquals(listOf(BudgetAlert(month, "food", 80)), pending)
    }

    @Test fun `duplicate category budgets share delivery identity and categories are independent`() {
        val budgets = listOf(budget(), budget().copy(id = 2, category = "FOOD"), budget(category = "Travel"))
        val pending = BudgetAlertPolicy.pending(budgets, listOf(expense(100.0), expense(80.0, "Travel")), emptySet(), now)
        assertEquals(3, pending.size)
        assertEquals(3, pending.map { it.key }.toSet().size)
    }

    @Test fun `invalid amounts cannot poison category totals and refunds reduce spending`() {
        val rows = listOf(expense(100.0), expense(-30.0), expense(Double.NaN), expense(Double.POSITIVE_INFINITY))
        assertTrue(BudgetAlertPolicy.pending(listOf(budget()), rows, emptySet(), now).isEmpty())
    }
}
