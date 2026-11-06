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
import com.akshar.data.model.MerchantCategory
import com.akshar.utils.CategoryMemory
import com.akshar.utils.DuplicateCandidate
import com.akshar.utils.DuplicateCheck
import com.akshar.utils.DuplicateMatch
import com.akshar.utils.MerchantNames
import java.util.TimeZone
import kotlinx.coroutines.flow.Flow
import com.akshar.data.backup.BackupSnapshot
import com.akshar.utils.PeriodUtils

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

    suspend fun getTransactionById(id: Long): Transaction? {
        return financeDao.getTransactionById(id)
    }

    /**
     * Saves both legs of a transfer atomically. Any legs already stored under the same
     * transferId are replaced in the same database transaction, so editing a transfer can never
     * leave it half-written or deleted.
     */
    suspend fun saveTransfer(fromTransaction: Transaction, toTransaction: Transaction) {
        database.withTransaction {
            fromTransaction.transferId?.let { financeDao.deleteTransactionsByTransferId(it) }
            financeDao.insertTransaction(fromTransaction)
            financeDao.insertTransaction(toTransaction)
        }
    }

    suspend fun deleteTransfer(transferId: String) {
        financeDao.deleteTransactionsByTransferId(transferId)
    }

    /** Both legs of a transfer: the EXPENSE (source) leg first, then the INCOME (destination) leg. */
    suspend fun getTransferLegs(transferId: String): List<Transaction> {
        return financeDao.getTransactionsByTransferId(transferId)
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
    suspend fun autoProcessRecurringTransactions(now: Long = System.currentTimeMillis()) {
        try {
            val list = financeDao.getRecurringTransactionsList()
            val newTransactions = mutableListOf<Transaction>()
            val updatedRecurrings = mutableListOf<RecurringTransaction>()
            for (recurring in list) {
                // Never triggered: start the schedule now without back-filling.
                if (recurring.lastTriggered == 0L) {
                    updatedRecurrings.add(recurring.copy(lastTriggered = now))
                    continue
                }

                // Occurrences are counted from the last trigger with calendar arithmetic, so a
                // monthly entry stays on the same day of the month instead of drifting by
                // 30-day steps (and yearly entries ignore leap days).
                var steps = 1
                var lastGenerated = recurring.lastTriggered
                while (true) {
                    val occurrence = PeriodUtils.addPeriods(recurring.lastTriggered, recurring.frequency, steps)
                    if (occurrence > now) break
                    newTransactions.add(
                        Transaction(
                            amount = recurring.amount,
                            type = recurring.type,
                            category = recurring.category,
                            description = "${recurring.description} (Auto-Recurring)",
                            date = occurrence,
                            paymentMethod = recurring.paymentMethod
                        )
                    )
                    lastGenerated = occurrence
                    steps++
                }

                if (lastGenerated != recurring.lastTriggered) {
                    updatedRecurrings.add(recurring.copy(lastTriggered = lastGenerated))
                }
            }

            // Ledger entries and the new trigger times are written together, so a crash
            // part-way cannot generate the same occurrences twice on the next launch.
            database.withTransaction {
                if (newTransactions.isNotEmpty()) {
                    financeDao.insertTransactions(newTransactions)
                }
                if (updatedRecurrings.isNotEmpty()) {
                    financeDao.insertRecurringTransactions(updatedRecurrings)
                }
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

    /**
     * A saved transaction that [candidate] (an SMS or CSV import) probably repeats: same bank
     * reference anywhere in the ledger, or same amount, type and calendar day.
     */
    suspend fun findLikelyDuplicate(candidate: DuplicateCandidate, zone: TimeZone = TimeZone.getDefault()): DuplicateMatch? {
        val (start, end) = DuplicateCheck.dayBounds(candidate.date, zone)
        val sameRef = candidate.referenceKey?.let { financeDao.getTransactionsByRowHash(it) }.orEmpty()
        return DuplicateCheck.findMatch(candidate, sameRef + financeDao.getTransactionsBetween(start, end), zone)
    }

    /** Ledger entries from the start of [fromMillis]'s day to the end of [toMillis]'s day. */
    suspend fun getTransactionsAcrossDays(fromMillis: Long, toMillis: Long, zone: TimeZone = TimeZone.getDefault()): List<Transaction> {
        val start = DuplicateCheck.dayBounds(fromMillis, zone).first
        val end = DuplicateCheck.dayBounds(toMillis, zone).second
        return financeDao.getTransactionsBetween(start, end)
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

    // --- Learned merchant categories ---
    val allMerchantCategories: Flow<List<MerchantCategory>> = financeDao.getAllMerchantCategories()

    suspend fun categoryMemory(): CategoryMemory = CategoryMemory(financeDao.getMerchantCategoriesList())

    /**
     * Remembers [category] for the merchant in [description]. Generic descriptions ("UPI payment")
     * and "not chosen" categories ("Other") are ignored. Returns true when a mapping was stored.
     */
    suspend fun learnCategory(description: String, type: String, category: String, now: Long = System.currentTimeMillis()): Boolean {
        if (!MerchantNames.isLearnable(category)) return false
        val key = MerchantNames.normalise(description) ?: return false
        financeDao.upsertMerchantCategory(
            MerchantCategory(merchantKey = key, type = type, displayName = description.trim(), category = category, updatedAt = now)
        )
        return true
    }

    suspend fun forgetMerchantCategory(mapping: MerchantCategory) {
        financeDao.deleteMerchantCategory(mapping.merchantKey, mapping.type)
    }

    suspend fun clearMerchantCategories() {
        financeDao.clearMerchantCategories()
    }

    // --- Backup & Restore ---

    /** Reads every table directly from the database (not from UI state) for a JSON backup. */
    suspend fun exportSnapshot(): BackupSnapshot = database.withTransaction {
        BackupSnapshot(
            transactions = financeDao.getTransactionsList(),
            budgets = financeDao.getBudgetsList(),
            savingsGoals = financeDao.getSavingsGoalsList(),
            recurringTransactions = financeDao.getRecurringTransactionsList(),
            debts = financeDao.getDebtsList(),
            csvImportProfiles = financeDao.getCsvImportProfilesList(),
            accounts = financeDao.getAccountsList(),
            reconciliations = financeDao.getReconciliationsList()
        )
    }

    /**
     * Replaces all data with [snapshot] in a single transaction. If any insert fails the
     * previous data is kept untouched. Restoring the same backup twice does not duplicate rows.
     */
    suspend fun replaceAllData(snapshot: BackupSnapshot) {
        database.withTransaction {
            financeDao.clearTransactions()
            financeDao.clearBudgets()
            financeDao.clearSavingsGoals()
            financeDao.clearRecurringTransactions()
            financeDao.clearDebts()
            financeDao.clearCsvImportProfiles()
            financeDao.clearAccounts()
            financeDao.clearReconciliations()

            financeDao.insertAccounts(snapshot.accounts)
            financeDao.insertTransactions(snapshot.transactions)
            financeDao.insertBudgets(snapshot.budgets)
            financeDao.insertSavingsGoals(snapshot.savingsGoals)
            financeDao.insertRecurringTransactions(snapshot.recurringTransactions)
            financeDao.insertDebts(snapshot.debts)
            financeDao.insertCsvImportProfiles(snapshot.csvImportProfiles)
            financeDao.insertReconciliations(snapshot.reconciliations)
        }
    }
}
