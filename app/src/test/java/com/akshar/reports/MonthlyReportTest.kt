package com.akshar.reports

import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import com.akshar.ui.screens.adjacentReportMonth
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MonthlyReportTest {
    private val zone = TimeZone.getTimeZone("Asia/Kolkata")
    private fun date(value: String) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = zone }.parse(value)!!.time
    private fun expense(amount: Double, category: String = "Food", date: Long = date("2026-11-10 12:00")) =
        Transaction(amount = amount, type = "EXPENSE", category = category, description = "Private note", date = date, paymentMethod = "Cash")

    @Test fun `decimal totals use explicit rounding and exclude transfers and adjacent months`() {
        val rows = listOf(expense(0.1), expense(0.2), expense(0.005), expense(90.0).copy(transferId = "pair"),
            expense(90.0).copy(type = "INCOME", transferId = "pair"), expense(10.01).copy(type = "INCOME"),
            expense(999.0, date = date("2026-10-31 23:59")), expense(999.0, date = date("2026-12-01 00:00")))
        val result = MonthlyReports.build("2026-11", rows, emptyList(), zone)
        assertEquals(BigDecimal("0.31"), result.expense)
        assertEquals(BigDecimal("10.01"), result.income)
        assertEquals(BigDecimal("9.70"), result.net)
    }

    @Test fun `large expenses retain cents and negative net is exact`() {
        val result = MonthlyReports.build("2026-11", listOf(expense(99_999_999_999.99), expense(0.01)), emptyList(), zone)
        assertEquals(BigDecimal("100000000000.00"), result.expense)
        assertEquals(BigDecimal("-100000000000.00"), result.net)
        assertEquals("$-100000000000.00", reportMoney(result.net, "$"))
    }

    @Test fun `top expenses limited and sorted and monthly budget uses case insensitive category`() {
        val rows = (1..15).map { expense(it.toDouble()) } + expense(5.0, "food")
        val result = MonthlyReports.build("2026-11", rows, listOf(Budget(category = "FOOD", limitAmount = 100.0, month = "2026-11"),
            Budget(category = "Food", limitAmount = 10.0, month = "2026-11", frequency = "WEEKLY"),
            Budget(category = "Food", limitAmount = 10.0, month = "2026-10")), zone)
        assertEquals(10, result.topExpenses.size)
        assertEquals(BigDecimal("15.00"), result.topExpenses.first().amount)
        assertEquals(BigDecimal("6.00"), result.topExpenses.last().amount)
        assertEquals(BigDecimal("125.00"), result.budgets.single().spent)
        assertEquals(result.expense, result.categories.fold(BigDecimal("0.00")) { sum, item -> sum + item.spent })
    }

    @Test fun `empty month has exact zero totals and no expenses`() {
        val result = MonthlyReports.build("2026-11", emptyList(), emptyList(), zone)
        assertEquals(BigDecimal("0.00"), result.income)
        assertEquals(BigDecimal("0.00"), result.expense)
        assertEquals(BigDecimal("0.00"), result.net)
        assertTrue(result.categories.isEmpty())
        assertTrue(result.topExpenses.isEmpty())
    }

    @Test fun `export labels remove prohibited name and control characters`() {
        val report = MonthlyReports.build("2026-11", listOf(expense(1.0, "AKSHAR\nFood")), emptyList(), zone)
        assertEquals("[redacted] Food", report.categories.single().name)
        assertEquals("[redacted]1.00", reportMoney(BigDecimal.ONE, "Akshar"))
    }

    @Test fun `month navigation crosses year and leap February`() {
        assertEquals("2027-01", adjacentReportMonth("2026-12", 1))
        assertEquals("2025-12", adjacentReportMonth("2026-01", -1))
        assertEquals("2024-03", adjacentReportMonth("2024-02", 1))
    }
}
