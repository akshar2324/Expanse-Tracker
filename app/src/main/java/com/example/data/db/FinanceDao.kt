package com.example.data.db

import androidx.room.*
import com.example.data.model.Budget
import com.example.data.model.RecurringTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import com.example.data.model.SmsTemplate
import com.example.data.model.PendingTransaction
import com.example.data.model.ParsedSmsLog
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?


    // --- Budgets ---
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun getBudgetsForMonth(month: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)


    // --- Savings Goals ---
    @Query("SELECT * FROM savings_goals")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Long)


    // --- Recurring Transactions ---
    @Query("SELECT * FROM recurring_transactions")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getRecurringTransactionsList(): List<RecurringTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurring: RecurringTransaction)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringTransactionById(id: Long)

    // --- SMS Parsing Templates ---
    @Query("SELECT * FROM sms_templates")
    fun getAllSmsTemplates(): Flow<List<SmsTemplate>>

    @Query("SELECT * FROM sms_templates")
    suspend fun getSmsTemplatesList(): List<SmsTemplate>

    @Query("SELECT * FROM sms_templates WHERE id = :id")
    suspend fun getSmsTemplateById(id: String): SmsTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsTemplate(smsTemplate: SmsTemplate)

    // --- Pending SMS Transactions ---
    @Query("SELECT * FROM pending_transactions ORDER BY date DESC")
    fun getAllPendingTransactions(): Flow<List<PendingTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransaction(pending: PendingTransaction)

    @Delete
    suspend fun deletePendingTransaction(pending: PendingTransaction)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deletePendingTransactionById(id: Long)

    // --- Parsed SMS Logs ---
    @Query("SELECT * FROM parsed_sms_logs ORDER BY date DESC")
    fun getAllParsedSmsLogs(): Flow<List<ParsedSmsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParsedSmsLog(log: ParsedSmsLog)

    @Query("SELECT * FROM parsed_sms_logs WHERE smsBody = :smsBody LIMIT 1")
    suspend fun getParsedSmsLogByBody(smsBody: String): ParsedSmsLog?

    @Query("UPDATE parsed_sms_logs SET status = :status, category = :category WHERE id = :id")
    suspend fun updateParsedSmsLogStatus(id: Long, status: String, category: String)

    @Query("UPDATE parsed_sms_logs SET status = :status, category = :category WHERE smsBody = :smsBody")
    suspend fun updateParsedSmsLogStatusByBody(smsBody: String, status: String, category: String)

    @Query("DELETE FROM parsed_sms_logs")
    suspend fun clearAllParsedSmsLogs()
}
