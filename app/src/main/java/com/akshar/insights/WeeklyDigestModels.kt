package com.akshar.insights

interface TimeProvider {
    fun nowMs(): Long
}

class DefaultTimeProvider : TimeProvider {
    override fun nowMs(): Long = System.currentTimeMillis()
}

data class WeeklyDigestSummary(
    val weekStartDateMs: Long,
    val weekEndDateMs: Long,
    val totalSpent: Double,
    val totalIncome: Double,
    val netCashFlow: Double,
    val topCategories: List<CategoryInsight>,
    val anomalies: List<SpendingAnomaly>,
    val budgetProgress: List<BudgetDigestItem>,
    val outlook: NextWeekOutlook?,
    val hasInsufficientData: Boolean
)

data class CategoryInsight(
    val categoryName: String,
    val amountSpent: Double,
    val percentageOfTotal: Double
)

data class SpendingAnomaly(
    val description: String,
    val relatedCategory: String?,
    val anomalyAmount: Double
)

data class BudgetDigestItem(
    val categoryName: String,
    val budgetLimit: Double,
    val spentSoFar: Double,
    val isOverBudget: Boolean
)

data class NextWeekOutlook(
    val expectedRecurringSpend: Double,
    val suggestion: String
)
