package com.akshar.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.akshar.data.db.AppDatabase
import com.akshar.data.db.FinanceDao
import com.akshar.data.model.Account
import com.akshar.data.model.Budget
import com.akshar.data.model.CsvImportProfile
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.Reconciliation
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.data.model.Debt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FinanceRepository(private val financeDao: FinanceDao, private val database: AppDatabase) {

    // --- Transactions ---
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()

    suspend fun insertTransaction(transaction: Transaction) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<Transaction>) {
        financeDao.insertTransactions(transactions)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        financeDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        financeDao.deleteTransactionById(id)
    }

    suspend fun saveTransfer(fromTransaction: Transaction, toTransaction: Transaction) {
        database.withTransaction {
            financeDao.insertTransaction(fromTransaction)
            financeDao.insertTransaction(toTransaction)
        }
    }

    suspend fun deleteTransfer(transferId: String) {
        database.withTransaction {
            val transactions = financeDao.getAllTransactions().first()
            val toDelete = transactions.filter { it.transferId == transferId }
            toDelete.forEach { tx ->
                financeDao.deleteTransaction(tx)
            }
        }
    }


    // --- Budgets ---
    val allBudgets: Flow<List<Budget>> = financeDao.getAllBudgets()

    fun getBudgetsForMonth(month: String): Flow<List<Budget>> {
        return financeDao.getBudgetsForMonth(month)
    }

    suspend fun insertBudget(budget: Budget) {
        financeDao.insertBudget(budget)
    }

    suspend fun insertBudgets(budgets: List<Budget>) {
        financeDao.insertBudgets(budgets)
    }

    suspend fun deleteBudgetById(id: Long) {
        financeDao.deleteBudgetById(id)
    }


    // --- Savings Goals ---
    val allSavingsGoals: Flow<List<SavingsGoal>> = financeDao.getAllSavingsGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoal) {
        financeDao.insertSavingsGoal(goal)
    }

    suspend fun insertSavingsGoals(goals: List<SavingsGoal>) {
        financeDao.insertSavingsGoals(goals)
    }

    suspend fun deleteSavingsGoalById(id: Long) {
        financeDao.deleteSavingsGoalById(id)
    }


    // --- Recurring Transactions ---
    val allRecurringTransactions: Flow<List<RecurringTransaction>> = financeDao.getAllRecurringTransactions()

    suspend fun insertRecurringTransaction(recurring: RecurringTransaction) {
        financeDao.insertRecurringTransaction(recurring)
    }

    suspend fun insertRecurringTransactions(recurrings: List<RecurringTransaction>) {
        financeDao.insertRecurringTransactions(recurrings)
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
            val newTransactions = mutableListOf<Transaction>()
            val updatedRecurrings = mutableListOf<RecurringTransaction>()
            for (recurring in list) {
                // If never triggered, we can set start to 1 cycle ago or now. Let's say now
                if (recurring.lastTriggered == 0L) {
                    val updated = recurring.copy(lastTriggered = now)
                    updatedRecurrings.add(updated)
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
                    newTransactions.add(tx)
                    generatedAny = true
                }

                if (generatedAny) {
                    val updated = recurring.copy(lastTriggered = tempLast)
                    updatedRecurrings.add(updated)
                }
            }

            if (newTransactions.isNotEmpty()) {
                financeDao.insertTransactions(newTransactions)
            }
            if (updatedRecurrings.isNotEmpty()) {
                financeDao.insertRecurringTransactions(updatedRecurrings)
            }
        } catch (e: Exception) {
            Log.e("FinanceRepository", "Error processing recurring transactions: ${e.message}", e)
        }
    }

    // --- Debts (Borrowed & Lent) ---
    val allDebts: Flow<List<Debt>> = financeDao.getAllDebts()

    suspend fun insertDebt(debt: Debt) {
        financeDao.insertDebt(debt)
    }

    suspend fun insertDebts(debts: List<Debt>) {
        financeDao.insertDebts(debts)
    }

    suspend fun deleteDebt(debt: Debt) {
        financeDao.deleteDebt(debt)
    }

    suspend fun deleteDebtById(id: Long) {
        financeDao.deleteDebtById(id)
    }

    suspend fun getExistingCsvHashes(hashes: List<String>): List<String> {
        return financeDao.getExistingCsvHashes(hashes)
    }

    suspend fun deleteTransactionsByBatchId(batchId: String) {
        financeDao.deleteTransactionsByBatchId(batchId)
    }

    // --- CSV Import Profiles ---
    val allCsvImportProfiles: Flow<List<CsvImportProfile>> = financeDao.getAllCsvImportProfiles()

    suspend fun insertCsvImportProfile(profile: CsvImportProfile): Long {
        return financeDao.insertCsvImportProfile(profile)
    }

    suspend fun deleteCsvImportProfileById(id: Long) {
        financeDao.deleteCsvImportProfileById(id)
    }

    suspend fun getCsvImportProfilesList(): List<CsvImportProfile> {
        return financeDao.getCsvImportProfilesList()
    }

    suspend fun insertCsvImportProfiles(profiles: List<CsvImportProfile>) {
        financeDao.insertCsvImportProfiles(profiles)
    }

    // --- Accounts ---
    val allAccounts: Flow<List<Account>> = financeDao.getAllAccounts()

    suspend fun insertAccount(account: Account) {
        financeDao.insertAccount(account)
    }

    suspend fun insertAccounts(accounts: List<Account>) {
        financeDao.insertAccounts(accounts)
    }

    suspend fun deleteAccount(account: Account) {
        financeDao.deleteAccount(account)
    }

    suspend fun deleteAccountById(id: Long) {
        financeDao.deleteAccountById(id)
    }

    // --- Reconciliations ---
    val allReconciliations: Flow<List<Reconciliation>> = financeDao.getAllReconciliations()

    suspend fun insertReconciliation(reconciliation: Reconciliation) {
        financeDao.insertReconciliation(reconciliation)
    }

    suspend fun insertReconciliations(reconciliations: List<Reconciliation>) {
        financeDao.insertReconciliations(reconciliations)
    }

    suspend fun deleteReconciliationById(id: Long) {
        financeDao.deleteReconciliationById(id)
    }
}
