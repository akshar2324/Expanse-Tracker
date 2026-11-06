package com.akshar.widget

import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import com.akshar.utils.BudgetMath
import com.akshar.utils.MoneyUtils
import java.util.Locale

data class BudgetWidgetRow(val label: String, val spent: Double, val limit: Double, val weekly: Boolean = false) {
    /** 0..1 share used, for the progress bar. */
    val fraction: Float get() = if (limit > 0) (spent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOver: Boolean get() = spent > limit
}

/** What the budget widget shows; the same numbers as Home and the Budgets tab. */
data class BudgetWidgetState(
    val totalSpent: Double = 0.0,
    val totalLimit: Double = 0.0,
    val rows: List<BudgetWidgetRow> = emptyList(),
    val budgetCount: Int = 0
) {
    val hasBudgets: Boolean get() = budgetCount > 0
    val fraction: Float get() = if (totalLimit > 0) (totalSpent / totalLimit).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOver: Boolean get() = totalSpent > totalLimit
    val remaining: Double get() = MoneyUtils.roundMoney((totalLimit - totalSpent).coerceAtLeast(0.0))
    val overBy: Double get() = MoneyUtils.roundMoney((totalSpent - totalLimit).coerceAtLeast(0.0))
}

object BudgetWidgetModel {

    /** The app's own lock setting ("Vault & passcode"); on unless the user turned it off. */
    const val LOCK_PREFS = "vault_settings"
    const val KEY_LOCK_ENABLED = "vault_enabled"

    fun isAppLockOn(context: android.content.Context): Boolean =
        context.getSharedPreferences(LOCK_PREFS, android.content.Context.MODE_PRIVATE).getBoolean(KEY_LOCK_ENABLED, true)

    const val MAX_ROWS = 3

    /**
     * The same totals as Home's budget bar: spending in budgeted categories against the active
     * budgets (this month's monthly ones and this week's weekly ones), plus the [maxRows]
     * budgets closest to or furthest over their limit. Unbudgeted spending, such as rent with no
     * budget, isn't counted, so the widget never reports "over budget" when Home doesn't.
     */
    fun build(
        budgets: List<Budget>,
        transactions: List<Transaction>,
        now: Long = System.currentTimeMillis(),
        maxRows: Int = MAX_ROWS
    ): BudgetWidgetState {
        val active = BudgetMath.activeBudgets(budgets, now)
        val summary = BudgetMath.summary(active, transactions)
        val rows = active
            .map { budget ->
                BudgetWidgetRow(
                    budget.category, BudgetMath.spentFor(budget, transactions), budget.limitAmount,
                    weekly = budget.frequency == "WEEKLY"
                )
            }
            .sortedWith(
                compareByDescending<BudgetWidgetRow> { if (it.limit > 0) it.spent / it.limit else 0.0 }
                    .thenBy { it.label.lowercase(Locale.ROOT) }
            )
            .take(maxRows)
        return BudgetWidgetState(summary.totalSpent, summary.totalLimit, rows, active.size)
    }

    /** Whole units keep the small widget readable; the app shows the exact cents. */
    fun formatShort(amount: Double, symbol: String, locale: Locale = Locale.getDefault()): String =
        symbol + String.format(locale, "%,.0f", MoneyUtils.roundMoney(amount))
}
