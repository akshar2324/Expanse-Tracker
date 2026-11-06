package com.akshar.utils

import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction

/** Totals across the budgets that are active right now. */
data class BudgetSummary(val totalLimit: Double, val totalSpent: Double) {
    val remaining: Double get() = MoneyUtils.roundMoney((totalLimit - totalSpent).coerceAtLeast(0.0))
    /** 0..1 share of the limit used; 0 when there is no limit. */
    val usedFraction: Double get() = if (totalLimit > 0) (totalSpent / totalLimit).coerceIn(0.0, 1.0) else 0.0
}

/**
 * Budget progress, shared by Home and the Budgets tab so both show the same numbers.
 * Only expenses in a budget's own category and period count; transfers never count.
 */
object BudgetMath {

    /** Budgets for the current week (WEEKLY) or month (MONTHLY, or legacy blank frequency). */
    fun activeBudgets(budgets: List<Budget>, now: Long = System.currentTimeMillis()): List<Budget> {
        val monthKey = PeriodUtils.monthKey(now)
        val weekKey = PeriodUtils.weekKey(now)
        return budgets.filter {
            (it.frequency == "WEEKLY" && it.month == weekKey) ||
                ((it.frequency == "MONTHLY" || it.frequency.isBlank()) && it.month == monthKey)
        }
    }

    fun spentFor(budget: Budget, transactions: List<Transaction>): Double =
        MoneyUtils.roundMoney(
            transactions.filter {
                it.transferId == null &&
                    it.type == "EXPENSE" &&
                    it.category.equals(budget.category, ignoreCase = true) &&
                    if (budget.frequency == "WEEKLY") PeriodUtils.isInWeek(it.date, budget.month)
                    else PeriodUtils.isInMonth(it.date, budget.month)
            }.sumOf { it.amount }
        )

    /** All monthly expenses, including categories without a budget; transfers never count. */
    fun totalSpentInMonth(transactions: List<Transaction>, month: String): Double =
        MoneyUtils.roundMoney(transactions.filter {
            it.transferId == null && it.type == "EXPENSE" && PeriodUtils.isInMonth(it.date, month)
        }.sumOf { it.amount })

    fun summary(activeBudgets: List<Budget>, transactions: List<Transaction>): BudgetSummary =
        BudgetSummary(
            totalLimit = MoneyUtils.roundMoney(activeBudgets.sumOf { it.limitAmount }),
            totalSpent = MoneyUtils.roundMoney(activeBudgets.sumOf { spentFor(it, transactions) })
        )
}
