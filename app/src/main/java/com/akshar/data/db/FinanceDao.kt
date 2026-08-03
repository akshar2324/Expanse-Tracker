package com.akshar.data.db

import androidx.room.*
import com.akshar.data.model.Budget
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.data.model.Debt
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
    fun getAllCsvImportProfiles(): Flow<List<com.akshar.data.model.CsvImportProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCsvImportProfile(profile: com.akshar.data.model.CsvImportProfile): Long

    @Query("DELETE FROM csv_import_profiles WHERE id = :id")
    suspend fun deleteCsvImportProfileById(id: Long)

    @Query("SELECT * FROM csv_import_profiles")
    suspend fun getCsvImportProfilesList(): List<com.akshar.data.model.CsvImportProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCsvImportProfiles(profiles: List<com.akshar.data.model.CsvImportProfile>)
}
