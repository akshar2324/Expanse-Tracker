package com.akshar.data.db

import androidx.room.*
import com.akshar.data.model.Budget
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.data.model.SmsTemplate
import com.akshar.data.model.PendingTransaction
import com.akshar.data.model.ParsedSmsLog
import com.akshar.data.model.Debt
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

    // --- Debts (Borrowed & Lent) ---
    @Query("SELECT * FROM debts ORDER BY date DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)
}
