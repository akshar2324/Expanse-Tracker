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
