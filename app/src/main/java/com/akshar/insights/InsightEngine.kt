package com.akshar.insights

import com.akshar.data.model.Budget
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.Transaction
import java.util.Calendar
import java.util.Locale

class InsightEngine(
    private val timeProvider: TimeProvider = DefaultTimeProvider()
) {

    fun generateWeeklyDigest(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        recurring: List<RecurringTransaction>
    ): WeeklyDigestSummary {
        val startOfWeekMs = weekStartMs(timeProvider.nowMs())
        val nextWeekStartMs = shiftWeeks(startOfWeekMs, 1)
        val endOfWeekMs = nextWeekStartMs - 1

        val currentWeekTx = transactions.filter {
            it.transferId == null && it.date in startOfWeekMs..endOfWeekMs
        }

        val totalSpent = currentWeekTx
            .filter { it.isType("EXPENSE") }
            .sumOf { it.amount }
        val totalIncome = currentWeekTx
            .filter { it.isType("INCOME") }
            .sumOf { it.amount }
        val netCashFlow = totalIncome - totalSpent

        val netExpenseByCategory = currentWeekTx
            .filter { it.isType("EXPENSE") }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .filterValues { it > 0.0 }

        val totalPositiveExpense = netExpenseByCategory.values.sum()
        val topCategories = netExpenseByCategory.map { (cat, amt) ->
            CategoryInsight(
                categoryName = cat,
                amountSpent = amt,
                percentageOfTotal = if (totalPositiveExpense > 0) (amt / totalPositiveExpense) * 100 else 0.0
            )
        }.sortedByDescending { it.amountSpent }.take(3)

        // Previous week boundaries for anomalies
        val prevWeekMsStart = shiftWeeks(startOfWeekMs, -1)
        val prevWeekMsEnd = startOfWeekMs - 1

        val prevWeekTx = transactions.filter {
            it.transferId == null && it.date in prevWeekMsStart..prevWeekMsEnd
        }

        val anomalies = mutableListOf<SpendingAnomaly>()
        if (prevWeekTx.isNotEmpty() && currentWeekTx.isNotEmpty()) {
            val prevExpenseByCategory = prevWeekTx
                .filter { it.isType("EXPENSE") }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .filterValues { it > 0.0 }

            for ((cat, currentAmt) in netExpenseByCategory) {
                val prevAmt = prevExpenseByCategory[cat] ?: 0.0
                if (prevAmt > 0 && currentAmt > prevAmt * 1.5) {
                    anomalies.add(
                        SpendingAnomaly(
                            description = "Unusually high spending in $cat compared to last week.",
                            relatedCategory = cat,
                            anomalyAmount = currentAmt - prevAmt
                        )
                    )
                }
            }
        }

        val budgetProgress = budgets.mapNotNull { budget ->
            val spentSoFar = netExpenseByCategory[budget.category] ?: 0.0
            if (spentSoFar > 0 || budget.limitAmount > 0) {
                BudgetDigestItem(
                    categoryName = budget.category,
                    budgetLimit = budget.limitAmount,
                    spentSoFar = spentSoFar,
                    isOverBudget = spentSoFar > budget.limitAmount
                )
            } else null
        }

        var expectedNextWeekSpend = 0.0
        recurring.filter { it.isType("EXPENSE") }.forEach {
            when (it.frequency.uppercase(Locale.US)) {
                "WEEKLY" -> expectedNextWeekSpend += it.amount
                "DAILY" -> expectedNextWeekSpend += it.amount * 7
                "MONTHLY" -> expectedNextWeekSpend += (it.amount / 4.0)
                "YEARLY" -> expectedNextWeekSpend += (it.amount / 52.0)
            }
        }
        val suggestion = if (expectedNextWeekSpend > totalIncome && totalIncome > 0) {
            "Expected recurring spend might exceed your usual income next week. Plan carefully."
        } else if (expectedNextWeekSpend > 0) {
            "You have recurring expenses expected next week."
        } else {
            "No major recurring expenses expected next week."
        }
        val outlook = NextWeekOutlook(
            expectedRecurringSpend = expectedNextWeekSpend,
            suggestion = suggestion
        )

        return WeeklyDigestSummary(
            weekStartDateMs = startOfWeekMs,
            weekEndDateMs = endOfWeekMs,
            totalSpent = totalSpent,
            totalIncome = totalIncome,
            netCashFlow = netCashFlow,
            topCategories = topCategories,
            anomalies = anomalies,
            budgetProgress = budgetProgress,
            outlook = outlook,
            hasInsufficientData = currentWeekTx.isEmpty()
        )
    }

    private fun weekStartMs(timestampMs: Long): Long {
        return Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = timestampMs
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun shiftWeeks(timestampMs: Long, amount: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestampMs
            add(Calendar.WEEK_OF_YEAR, amount)
        }.timeInMillis
    }

    private fun Transaction.isType(expected: String): Boolean =
        type.equals(expected, ignoreCase = true)

    private fun RecurringTransaction.isType(expected: String): Boolean =
        type.equals(expected, ignoreCase = true)
}
