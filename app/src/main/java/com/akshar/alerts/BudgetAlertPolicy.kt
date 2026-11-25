package com.akshar.alerts

import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import com.akshar.utils.PeriodUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/** One persistent identity per calendar month, category and threshold, independent of budget IDs. */
data class BudgetAlert(val month: String, val category: String, val level: Int) {
    val key: String get() = "$month:$level:$category"
}

object BudgetAlertPolicy {
    private fun money(value: Double): BigDecimal? = if (value.isFinite())
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP) else null

    fun pending(
        budgets: List<Budget>,
        transactions: List<Transaction>,
        sent: Set<String>,
        now: Long
    ): List<BudgetAlert> {
        val month = PeriodUtils.monthKey(now)
        val spending = transactions.asSequence().filter {
            it.type == "EXPENSE" && it.transferId == null && PeriodUtils.isInMonth(it.date, month)
        }.groupBy { it.category.lowercase(Locale.ROOT) }.mapValues { (_, rows) ->
            rows.fold(BigDecimal.ZERO) { total, row -> total + (money(row.amount) ?: BigDecimal.ZERO) }
        }
        return budgets.asSequence().filter {
            it.month == month && (it.frequency == "MONTHLY" || it.frequency.isBlank())
        }.flatMap { budget ->
            val limit = money(budget.limitAmount)
            val category = budget.category.lowercase(Locale.ROOT)
            val spent = spending[category] ?: BigDecimal.ZERO
            if (limit == null || limit.signum() <= 0) emptySequence()
            else sequenceOf(80, 100).filter { level ->
                // Cross multiplication avoids division or rounded percentage comparisons.
                spent.multiply(BigDecimal(100)) >= limit.multiply(BigDecimal(level))
            }.map { BudgetAlert(month, category, it) }
        }.distinctBy { it.key }.filter { it.key !in sent }.toList()
    }
}
