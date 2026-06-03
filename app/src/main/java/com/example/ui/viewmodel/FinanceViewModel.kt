package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExpenseTrackerApp
import com.example.data.model.Budget
import com.example.data.model.RecurringTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
import com.example.data.model.SmsTemplate
import com.example.data.model.PendingTransaction
import com.example.data.model.ParsedSmsLog
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository = (application as ExpenseTrackerApp).repository

    // --- Core Database Flows ---
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavingsGoals: StateFlow<List<SavingsGoal>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurringTransactions: StateFlow<List<RecurringTransaction>> = repository.allRecurringTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSmsTemplates: StateFlow<List<SmsTemplate>> = repository.allSmsTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPendingTransactions: StateFlow<List<PendingTransaction>> = repository.allPendingTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allParsedSmsLogs: StateFlow<List<ParsedSmsLog>> = repository.allParsedSmsLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Backup & Restore UI Feedback States ---
    private val _backupStatus = MutableStateFlow("")
    val backupStatus: StateFlow<String> = _backupStatus.asStateFlow()

    init {
        // Trigger auto-processing of recurring entries when model is loaded
        viewModelScope.launch {
            repository.autoProcessRecurringTransactions()
        }
    }

    // --- SMS Parser Configurations and Actions ---
    fun updateSmsTemplate(id: String, exampleText: String, keywords: String) {
        viewModelScope.launch {
            repository.insertSmsTemplate(SmsTemplate(id = id, exampleText = exampleText, keywords = keywords))
        }
    }

    fun confirmPendingTransaction(
        pending: PendingTransaction,
        category: String,
        description: String,
        paymentMethod: String = "UPI/SMS"
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    amount = pending.amount,
                    type = pending.type,
                    category = category,
                    description = description,
                    date = pending.date,
                    paymentMethod = paymentMethod
                )
            )
            repository.deletePendingTransaction(pending)
            repository.updateParsedSmsLogStatusByBody(pending.smsBody, "CONFIRMED", category)
        }
    }

    fun deletePendingTransaction(pending: PendingTransaction) {
        viewModelScope.launch {
            repository.deletePendingTransaction(pending)
            repository.updateParsedSmsLogStatusByBody(pending.smsBody, "IGNORED", "")
        }
    }

    fun clearAllSmsLogs() {
        viewModelScope.launch {
            repository.clearAllParsedSmsLogs()
        }
    }

    // --- CRUD: Transactions ---
    fun addTransaction(
        amount: Double,
        type: String, // "EXPENSE", "INCOME"
        category: String,
        description: String,
        date: Long,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                amount = amount,
                type = type,
                category = category,
                description = description,
                date = date,
                paymentMethod = paymentMethod
            )
            repository.insertTransaction(tx)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    // --- CRUD: Budgets ---
    fun addBudget(
        category: String,
        limitAmount: Double,
        month: String,
        frequency: String = "MONTHLY",
        remindersEnabled: Boolean = false,
        reminderThreshold: Int = 90
    ) {
        viewModelScope.launch {
            // Check if budget for this month & category & frequency already exists
            val current = allBudgets.value.firstOrNull { 
                it.category == category && it.month == month && it.frequency == frequency 
            }
            val budget = if (current != null) {
                current.copy(
                    limitAmount = limitAmount,
                    remindersEnabled = remindersEnabled,
                    reminderThreshold = reminderThreshold
                )
            } else {
                Budget(
                    category = category, 
                    limitAmount = limitAmount, 
                    month = month,
                    frequency = frequency,
                    remindersEnabled = remindersEnabled,
                    reminderThreshold = reminderThreshold
                )
            }
            repository.insertBudget(budget)
        }
    }

    fun deleteBudgetById(id: Long) {
        viewModelScope.launch {
            repository.deleteBudgetById(id)
        }
    }

    // --- CRUD: Savings Goals ---
    fun addSavingsGoal(name: String, targetAmount: Double, currentAmount: Double, targetDate: String) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                name = name,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate
            )
            repository.insertSavingsGoal(goal)
        }
    }

    fun addSavingsProgress(goal: SavingsGoal, amount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = (goal.currentAmount + amount).coerceAtMost(goal.targetAmount))
            repository.insertSavingsGoal(updated)
        }
    }

    fun deleteSavingsGoalById(id: Long) {
        viewModelScope.launch {
            repository.deleteSavingsGoalById(id)
        }
    }

    // --- CRUD: Recurring Transactions ---
    fun addRecurringTransaction(
        amount: Double,
        type: String,
        category: String,
        description: String,
        frequency: String,
        paymentMethod: String
    ) {
        viewModelScope.launch {
            val rec = RecurringTransaction(
                amount = amount,
                type = type,
                category = category,
                description = description,
                frequency = frequency,
                paymentMethod = paymentMethod,
                lastTriggered = System.currentTimeMillis()
            )
            repository.insertRecurringTransaction(rec)
        }
    }

    fun deleteRecurringTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteRecurringTransactionById(id)
        }
    }

    // --- Bill Reminders & Payments ---
    fun triggerRecurringPayment(recurring: RecurringTransaction) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val tx = Transaction(
                amount = recurring.amount,
                type = recurring.type,
                category = recurring.category,
                description = "${recurring.description} (Paid Bill)",
                date = now,
                paymentMethod = recurring.paymentMethod
            )
            repository.insertTransaction(tx)
            
            // Calculate next trigger time accurately
            val cal = Calendar.getInstance().apply { timeInMillis = recurring.lastTriggered }
            when (recurring.frequency.uppercase()) {
                "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                "YEARLY" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.MONTH, 1)
            }
            val nextDueDate = cal.timeInMillis

            val updated = recurring.copy(lastTriggered = nextDueDate)
            repository.insertRecurringTransaction(updated)
        }
    }

    // --- Backup & Restore (JSON Export-Import) ---
    fun exportDataToJson(): String? {
        return try {
            val backupObj = JSONObject()

            // Transactions
            val txArray = JSONArray()
            allTransactions.value.forEach {
                val obj = JSONObject()
                obj.put("amount", it.amount)
                obj.put("type", it.type)
                obj.put("category", it.category)
                obj.put("description", it.description)
                obj.put("date", it.date)
                obj.put("paymentMethod", it.paymentMethod)
                txArray.put(obj)
            }
            backupObj.put("transactions", txArray)

            // Budgets
            val bgArray = JSONArray()
            allBudgets.value.forEach {
                val obj = JSONObject()
                obj.put("category", it.category)
                obj.put("limitAmount", it.limitAmount)
                obj.put("month", it.month)
                bgArray.put(obj)
            }
            backupObj.put("budgets", bgArray)

            // Savings Goals
            val sgArray = JSONArray()
            allSavingsGoals.value.forEach {
                val obj = JSONObject()
                obj.put("name", it.name)
                obj.put("targetAmount", it.targetAmount)
                obj.put("currentAmount", it.currentAmount)
                obj.put("targetDate", it.targetDate)
                sgArray.put(obj)
            }
            backupObj.put("savings_goals", sgArray)

            // Recurring Transactions
            val rcArray = JSONArray()
            allRecurringTransactions.value.forEach {
                val obj = JSONObject()
                obj.put("amount", it.amount)
                obj.put("type", it.type)
                obj.put("category", it.category)
                obj.put("description", it.description)
                obj.put("frequency", it.frequency)
                obj.put("paymentMethod", it.paymentMethod)
                obj.put("lastTriggered", it.lastTriggered)
                rcArray.put(obj)
            }
            backupObj.put("recurring_transactions", rcArray)

            backupObj.toString(4)
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "Backup Export failed", e)
            null
        }
    }

    fun restoreDataFromJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)

            viewModelScope.launch {
                // Clear and restored sequentially in transactions
                if (root.has("transactions")) {
                    val array = root.getJSONArray("transactions")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        repository.insertTransaction(
                            Transaction(
                                amount = obj.getDouble("amount"),
                                type = obj.getString("type"),
                                category = obj.getString("category"),
                                description = obj.getString("description"),
                                date = obj.getLong("date"),
                                paymentMethod = obj.getString("paymentMethod")
                            )
                        )
                    }
                }

                // Restore Budgets
                if (root.has("budgets")) {
                    val array = root.getJSONArray("budgets")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        repository.insertBudget(
                            Budget(
                                category = obj.getString("category"),
                                limitAmount = obj.getDouble("limitAmount"),
                                month = obj.getString("month")
                            )
                        )
                    }
                }

                // Restore Goals
                if (root.has("savings_goals")) {
                    val array = root.getJSONArray("savings_goals")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        repository.insertSavingsGoal(
                            SavingsGoal(
                                name = obj.getString("name"),
                                targetAmount = obj.getDouble("targetAmount"),
                                currentAmount = obj.getDouble("currentAmount"),
                                targetDate = obj.getString("targetDate")
                            )
                        )
                    }
                }

                // Restore Recurrings
                if (root.has("recurring_transactions")) {
                    val array = root.getJSONArray("recurring_transactions")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        repository.insertRecurringTransaction(
                            RecurringTransaction(
                                amount = obj.getDouble("amount"),
                                type = obj.getString("type"),
                                category = obj.getString("category"),
                                description = obj.getString("description"),
                                frequency = obj.getString("frequency"),
                                paymentMethod = obj.getString("paymentMethod"),
                                lastTriggered = obj.getLong("lastTriggered")
                            )
                        )
                    }
                }
            }
            _backupStatus.value = "Data Restored Successfully!"
            true
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "Restore failed", e)
            _backupStatus.value = "Failed to restore backup: invalid JSON format."
            false
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = ""
    }
}
