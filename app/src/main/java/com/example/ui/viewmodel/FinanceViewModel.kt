package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExpenseTrackerApp
import com.example.ai.GeminiClient
import com.example.data.model.Budget
import com.example.data.model.RecurringTransaction
import com.example.data.model.SavingsGoal
import com.example.data.model.Transaction
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

    // --- AI Spending Analysis States ---
    private val _aiAnalysis = MutableStateFlow<String>("")
    val aiAnalysis: StateFlow<String> = _aiAnalysis.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    // --- Backup & Restore UI Feedback States ---
    private val _backupStatus = MutableStateFlow("")
    val backupStatus: StateFlow<String> = _backupStatus.asStateFlow()

    init {
        // Trigger auto-processing of recurring entries when model is loaded
        viewModelScope.launch {
            repository.autoProcessRecurringTransactions()
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
    fun addBudget(category: String, limitAmount: Double, month: String) {
        viewModelScope.launch {
            // Check if budget for this month & category already exists
            val current = allBudgets.value.firstOrNull { it.category == category && it.month == month }
            val budget = if (current != null) {
                current.copy(limitAmount = limitAmount)
            } else {
                Budget(category = category, limitAmount = limitAmount, month = month)
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

    // --- Advanced Features: AI Spending Analysis with fallback engine ---
    fun runAiSpendingAnalysis() {
        viewModelScope.launch {
            _aiLoading.value = true
            try {
                // Compile transaction insights
                val txs = allTransactions.value
                val expenses = txs.filter { it.type == "EXPENSE" }
                val incomes = txs.filter { it.type == "INCOME" }

                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                val monthlySpent = expenses.sumOf { it.amount }
                val monthlyIncome = incomes.sumOf { it.amount }

                val highestExpense = expenses.maxByOrNull { it.amount }
                val topCategory = expenses.groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
                    .maxByOrNull { it.value }

                val prompt = """
                    You are an expert AI Personal Finance Advisor analyzing transactions for the application 'Expense Tracker Pro'.
                    Below is the user's financial log data:
                    - Total transactions: ${txs.size}
                    - Total Income: ₹$monthlyIncome
                    - Total Expense: ₹$monthlySpent
                    - Net Savings Rate: ${if (monthlyIncome > 0) ((monthlyIncome - monthlySpent) / monthlyIncome * 100).toInt() else 0}%
                    - Highest individual expense item: ${highestExpense?.category ?: "None"} with value ₹${highestExpense?.amount ?: 0}
                    - Highest expense category: ${topCategory?.key ?: "None"} with value ₹${topCategory?.value ?: 0}
                    
                    Please generate 3 sections of constructive, professional advice:
                    1. AI Spending Analysis (Analyze current leaks, balance patterns, overspending signs based on limits).
                    2. Smart Recommendations (Practical adjustments like 'Reduce ${topCategory?.key ?: "dining"} expenses to save ₹1500').
                    3. Future Expense Predictions (Based on these transactions, predict estimated monthly expenses under similar trends).
                    
                    Return in clear, beautifully-formatted plain English text without system codes. Use bullet points.
                """.trimIndent()

                val result = GeminiClient.generateAnalysis(prompt)
                _aiAnalysis.value = result
            } catch (e: Exception) {
                _aiAnalysis.value = "Unable to fetch AI suggestions due to: ${e.message}"
            } finally {
                _aiLoading.value = false
            }
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
