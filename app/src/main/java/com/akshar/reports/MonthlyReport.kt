package com.akshar.reports

import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Exact decimal calculations; transfers never contribute to cash flow or budget spending. */
data class ReportCategory(val name: String, val spent: BigDecimal)
data class ReportExpense(val date: String, val category: String, val amount: BigDecimal)
data class ReportBudget(val category: String, val limit: BigDecimal, val spent: BigDecimal)
data class MonthlyReport(
    val month: String,
    val income: BigDecimal,
    val expense: BigDecimal,
    val categories: List<ReportCategory>,
    val topExpenses: List<ReportExpense>,
    val budgets: List<ReportBudget>,
) {
    val net: BigDecimal get() = income - expense
}

object MonthlyReports {
    fun build(month: String, transactions: List<Transaction>, budgets: List<Budget>, zone: TimeZone = TimeZone.getDefault()): MonthlyReport {
        require(Regex("[0-9]{4}-(0[1-9]|1[0-2])").matches(month))
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = zone }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }
        val ledger = transactions.filter { it.transferId == null && monthFormat.format(Date(it.date)) == month }
        val expenses = ledger.filter { it.type == "EXPENSE" }
        fun total(rows: List<Transaction>) = rows.fold(BigDecimal.ZERO) { sum, row -> sum + BigDecimal.valueOf(row.amount) }
            .setScale(2, RoundingMode.HALF_UP)
        return MonthlyReport(
            month, total(ledger.filter { it.type == "INCOME" }), total(expenses),
            expenses.groupBy { it.category }.map { (category, rows) -> ReportCategory(safeReportText(category), total(rows)) }
                .sortedWith(compareByDescending<ReportCategory> { it.spent }.thenBy { it.name }),
            expenses.sortedWith(compareByDescending<Transaction> { it.amount }.thenBy { it.date }.thenBy { it.id }).take(10)
                .map { ReportExpense(dateFormat.format(Date(it.date)), safeReportText(it.category), BigDecimal.valueOf(it.amount).setScale(2, RoundingMode.HALF_UP)) },
            budgets.filter { it.month == month && (it.frequency == "MONTHLY" || it.frequency.isBlank()) }
                .sortedWith(compareBy<Budget> { it.category }.thenBy { it.id }).map { budget ->
                    ReportBudget(safeReportText(budget.category), BigDecimal.valueOf(budget.limitAmount).setScale(2, RoundingMode.HALF_UP),
                        total(expenses.filter { it.category.equals(budget.category, ignoreCase = true) }))
                },
        )
    }
}

/** Exported user labels must never contain the prohibited display name or embedded newlines. */
fun safeReportText(value: String): String = value.replace(Regex("akshar", RegexOption.IGNORE_CASE), "[redacted]")
    .replace(Regex("[\\p{Cc}\\p{Cf}]"), " ")

fun reportMoney(amount: BigDecimal, symbol: String): String =
    safeReportText(symbol) + amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
