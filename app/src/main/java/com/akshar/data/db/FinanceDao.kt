package com.akshar.data.db

import androidx.room.*
import com.akshar.data.model.Account
import com.akshar.data.model.Reconciliation
import com.akshar.data.model.Budget
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.data.model.Debt
import com.akshar.data.model.CsvImportProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    /**
     * Activity tab search. Every argument is optional (null, or an empty list via
     * [categoryCount] = 0). [pattern] is a LIKE pattern escaped with '\'; SQLite LIKE ignores case
     * for ASCII letters only. [kind] is ALL, EXPENSE, INCOME or TRANSFER, where EXPENSE and INCOME
     * exclude transfer legs. The date range is [from, until). Amount bounds are inclusive and compare the same rounded
     * two-decimal values produced by MoneyUtils.parseAmount when entries are saved.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:pattern IS NULL
               OR description LIKE :pattern ESCAPE '\'
               OR category LIKE :pattern ESCAPE '\'
               OR paymentMethod LIKE :pattern ESCAPE '\')
          AND (:kind = 'ALL'
               OR (:kind = 'TRANSFER' AND transferId IS NOT NULL)
               OR (transferId IS NULL AND type = :kind))
          AND (:categoryCount = 0 OR category IN (:categories))
          AND (:accountId IS NULL OR accountId = :accountId)
          AND (:paymentMethod IS NULL OR paymentMethod = :paymentMethod)
          AND (:from IS NULL OR date >= :from)
          AND (:until IS NULL OR date < :until)
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
        ORDER BY
          CASE WHEN :sort = 'HIGHEST' THEN amount END DESC,
          CASE WHEN :sort = 'LOWEST' THEN amount END ASC,
          CASE WHEN :sort = 'OLDEST' THEN date END ASC,
          CASE WHEN :sort = 'OLDEST' THEN id END ASC,
          date DESC,
          id DESC
        """
    )
    fun searchTransactions(
        pattern: String?,
        kind: String,
        categories: List<String>,
        categoryCount: Int,
        accountId: Long?,
        paymentMethod: String?,
        from: Long?,
        until: Long?,
        minAmount: Double?,
        maxAmount: Double?,
        sort: String,
    ): Flow<List<Transaction>>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions)")
    fun hasAnyTransactions(): Flow<Boolean>

    @Query("SELECT DISTINCT category FROM transactions WHERE transferId IS NULL AND category != '' ORDER BY category COLLATE NOCASE")
    fun getUsedCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT paymentMethod FROM transactions WHERE transferId IS NULL AND paymentMethod != '' ORDER BY paymentMethod COLLATE NOCASE")
    fun getUsedPaymentMethods(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("DELETE FROM transactions WHERE importBatchId = :batchId")
    suspend fun deleteTransactionsByBatchId(batchId: String)

    @Query("SELECT originalCsvRowHash FROM transactions WHERE originalCsvRowHash IN (:hashes)")
    suspend fun getExistingCsvHashes(hashes: List<String>): List<String>

    @Query("SELECT * FROM transactions WHERE transferId = :transferId ORDER BY type ASC")
    suspend fun getTransactionsByTransferId(transferId: String): List<Transaction>

    @Query("DELETE FROM transactions WHERE transferId = :transferId")
    suspend fun deleteTransactionsByTransferId(transferId: String)

    // One-shot reads used by backup export, so it never depends on a UI collector being active.
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getTransactionsList(): List<Transaction>
    @Query("SELECT * FROM budgets") suspend fun getBudgetsList(): List<Budget>
    @Query("SELECT * FROM savings_goals") suspend fun getSavingsGoalsList(): List<SavingsGoal>
    @Query("SELECT * FROM debts ORDER BY date DESC") suspend fun getDebtsList(): List<Debt>
    @Query("SELECT * FROM accounts ORDER BY id ASC") suspend fun getAccountsList(): List<Account>
    @Query("SELECT * FROM reconciliations ORDER BY date DESC, id DESC") suspend fun getReconciliationsList(): List<Reconciliation>

    // Used by a full JSON restore, which replaces the ledger inside one database transaction.
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM savings_goals") suspend fun clearSavingsGoals()
    @Query("DELETE FROM recurring_transactions") suspend fun clearRecurringTransactions()
    @Query("DELETE FROM debts") suspend fun clearDebts()
    @Query("DELETE FROM csv_import_profiles") suspend fun clearCsvImportProfiles()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM reconciliations") suspend fun clearReconciliations()


    // --- Budgets ---
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun getBudgetsForMonth(month: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)


    // --- Savings Goals ---
    @Query("SELECT * FROM savings_goals")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(goals: List<SavingsGoal>)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Long)


    // --- Recurring Transactions ---
    @Query("SELECT * FROM recurring_transactions")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getRecurringTransactionsList(): List<RecurringTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurring: RecurringTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransactions(recurrings: List<RecurringTransaction>)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransactionById(id: Long)

    // --- Debts (Borrowed & Lent) ---
    @Query("SELECT * FROM debts ORDER BY date DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<Debt>)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)

    // --- CSV Import Profiles ---
    @Query("SELECT * FROM csv_import_profiles")
    fun getAllCsvImportProfiles(): Flow<List<CsvImportProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCsvImportProfile(profile: CsvImportProfile): Long

    @Query("DELETE FROM csv_import_profiles WHERE id = :id")
    suspend fun deleteCsvImportProfileById(id: Long)

    @Query("SELECT * FROM csv_import_profiles")
    suspend fun getCsvImportProfilesList(): List<CsvImportProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCsvImportProfiles(profiles: List<CsvImportProfile>)

    // --- Accounts ---
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<Account>)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    // --- Reconciliations ---
    @Query("SELECT * FROM reconciliations ORDER BY date DESC, id DESC")
    fun getAllReconciliations(): Flow<List<Reconciliation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliation(reconciliation: Reconciliation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReconciliations(reconciliations: List<Reconciliation>)

    @Query("DELETE FROM reconciliations WHERE id = :id")
    suspend fun deleteReconciliationById(id: Long)
}
