package com.example.data.repository

import android.util.Log
import com.example.data.db.FinanceDao
import com.example.data.model.Budget
import com.example.data.model.RecurringTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FinanceRepository(private val financeDao: FinanceDao) {

    // --- Transactions ---
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()

    suspend fun insertTransaction(transaction: Transaction) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        financeDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        financeDao.deleteTransactionById(id)
    }


    // --- Budgets ---
    val allBudgets: Flow<List<Budget>> = financeDao.getAllBudgets()

    fun getBudgetsForMonth(month: String): Flow<List<Budget>> {
        return financeDao.getBudgetsForMonth(month)
    }

    suspend fun insertBudget(budget: Budget) {
        financeDao.insertBudget(budget)
    }

    suspend fun deleteBudgetById(id: Long) {
        financeDao.deleteBudgetById(id)
    }


    // --- Savings Goals ---
    val allSavingsGoals: Flow<List<SavingsGoal>> = financeDao.getAllSavingsGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoal) {
        financeDao.insertSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoalById(id: Long) {
        financeDao.deleteSavingsGoalById(id)
    }


    // --- Recurring Transactions ---
    val allRecurringTransactions: Flow<List<RecurringTransaction>> = financeDao.getAllRecurringTransactions()

    suspend fun insertRecurringTransaction(recurring: RecurringTransaction) {
        financeDao.insertRecurringTransaction(recurring)
    }

    suspend fun deleteRecurringTransactionById(id: Long) {
        financeDao.deleteRecurringTransactionById(id)
    }

    /**
     * Examines all recurring transactions and generates actual ledger entries if overdue.
     */
    suspend fun autoProcessRecurringTransactions() {
        try {
            val now = System.currentTimeMillis()
            val list = financeDao.getRecurringTransactionsList()
            for (recurring in list) {
                // If never triggered, we can set start to 1 cycle ago or now. Let's say now
                if (recurring.lastTriggered == 0L) {
                    val updated = recurring.copy(lastTriggered = now)
                    financeDao.insertRecurringTransaction(updated)
                    continue
                }

                val periodMs = when (recurring.frequency.uppercase()) {
                    "DAILY" -> 86400000L
                    "WEEKLY" -> 7 * 86400000L
                    "MONTHLY" -> 30 * 86400000L // approximate
                    "YEARLY" -> 365 * 86400000L // approximate
                    else -> 30 * 86400000L
                }

                var tempLast = recurring.lastTriggered
                var generatedAny = false
                while (now - tempLast >= periodMs) {
                    tempLast += periodMs
                    // Generate a real transaction
                    val tx = Transaction(
                        amount = recurring.amount,
                        type = recurring.type,
                        category = recurring.category,
                        description = "${recurring.description} (Auto-Recurring)",
                        date = tempLast,
                        paymentMethod = recurring.paymentMethod
                    )
                    financeDao.insertTransaction(tx)
                    generatedAny = true
                }

                if (generatedAny) {
                    val updated = recurring.copy(lastTriggered = tempLast)
                    financeDao.insertRecurringTransaction(updated)
                }
            }
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Error processing recurring transactions: ${e.message}", e)
        }
    }
}
