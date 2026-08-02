package com.akshar.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akshar.ExpenseTrackerApp
import com.akshar.data.model.Budget
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.data.model.Debt
import com.akshar.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.net.Uri
import android.content.Intent
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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

    val allDebts: StateFlow<List<Debt>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Country & Currency Settings ---
    private val countryPrefs = application.getSharedPreferences("country_settings", android.content.Context.MODE_PRIVATE)

    private val _selectedCountry = MutableStateFlow(countryPrefs.getString("selected_country", "IN") ?: "IN")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    fun updateCountry(countryCode: String) {
        countryPrefs.edit().putString("selected_country", countryCode).apply()
        _selectedCountry.value = countryCode
    }

    fun getCurrencySymbol(): String {
        return when (selectedCountry.value) {
            "IN" -> "₹"
            "US" -> "$"
            "JP" -> "¥"
            "EU" -> "€"
            else -> "$"
        }
    }

    fun formatCurrencyValue(amount: Double): String {
        val symbol = getCurrencySymbol()
        return "$symbol${String.format(Locale.getDefault(), "%,.2f", amount)}"
    }

    val activePaymentMethods: StateFlow<List<String>> = selectedCountry.map { country ->
        getPaymentMethodsForCountry(country)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf("Cash", "UPI", "Credit Card", "Debit Card", "Bank Transfer"))

    fun getPaymentMethodsForCountry(country: String): List<String> {
        return when (country) {
            "IN" -> listOf("UPI", "Cash", "Credit Card", "Debit Card", "Bank Transfer", "Wallet")
            "US" -> listOf("Venmo/Zelle", "PayPal", "Cash", "Credit Card", "Debit Card", "Bank Transfer")
            "JP" -> listOf("Suica/IC", "Line Pay/PayPay", "Cash", "Credit Card", "Bank Transfer")
            "EU" -> listOf("SEPA Transfer", "Cash", "Credit Card", "Debit Card", "Mobile Pay")
            else -> listOf("Cash", "Credit Card", "Debit Card", "Bank Transfer")
        }
    }

    // --- CRUD: Debts (Borrowed & Lent) ---
    fun addDebt(
        personName: String,
        amount: Double,
        type: String, // "BORROWED" or "LENT"
        description: String,
        date: Long,
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val debt = Debt(
                personName = personName,
                amount = amount,
                type = type,
                description = description,
                date = date,
                dueDate = dueDate,
                isResolved = false
            )
            repository.insertDebt(debt)
        }
    }

    fun resolveDebt(debt: Debt) {
        viewModelScope.launch {
            repository.insertDebt(debt.copy(isResolved = true))
        }
    }

    fun unresolveDebt(debt: Debt) {
        viewModelScope.launch {
            repository.insertDebt(debt.copy(isResolved = false))
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

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
                obj.put("frequency", it.frequency)
                obj.put("remindersEnabled", it.remindersEnabled)
                obj.put("reminderThreshold", it.reminderThreshold)
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
            // Debts
            val dbArray = JSONArray()
            allDebts.value.forEach {
                val obj = JSONObject()
                obj.put("personName", it.personName)
                obj.put("amount", it.amount)
                obj.put("type", it.type)
                obj.put("date", it.date)
                obj.put("description", it.description)
                obj.put("isResolved", it.isResolved)
                if (it.dueDate != null) obj.put("dueDate", it.dueDate)
                dbArray.put(obj)
            }
            backupObj.put("debts", dbArray)

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
                    val transactions = mutableListOf<Transaction>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        transactions.add(
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
                    if (transactions.isNotEmpty()) {
                        repository.insertTransactions(transactions)
                    }
                }

                // Restore Budgets
                if (root.has("budgets")) {
                    val array = root.getJSONArray("budgets")
                    val budgets = mutableListOf<Budget>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        budgets.add(
                            Budget(
                                category = obj.getString("category"),
                                limitAmount = obj.getDouble("limitAmount"),
                                month = obj.getString("month"),
                                frequency = if (obj.has("frequency")) obj.getString("frequency") else "MONTHLY",
                                remindersEnabled = if (obj.has("remindersEnabled")) obj.getBoolean("remindersEnabled") else false,
                                reminderThreshold = if (obj.has("reminderThreshold")) obj.getInt("reminderThreshold") else 90
                            )
                        )
                    }
                    if (budgets.isNotEmpty()) {
                        repository.insertBudgets(budgets)
                    }
                }

                // Restore Goals
                if (root.has("savings_goals")) {
                    val array = root.getJSONArray("savings_goals")
                    val goals = mutableListOf<SavingsGoal>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        goals.add(
                            SavingsGoal(
                                name = obj.getString("name"),
                                targetAmount = obj.getDouble("targetAmount"),
                                currentAmount = obj.getDouble("currentAmount"),
                                targetDate = obj.getString("targetDate")
                            )
                        )
                    }
                    if (goals.isNotEmpty()) {
                        repository.insertSavingsGoals(goals)
                    }
                }

                // Restore Recurrings
                if (root.has("recurring_transactions")) {
                    val array = root.getJSONArray("recurring_transactions")
                    val recurrings = mutableListOf<RecurringTransaction>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        recurrings.add(
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
                    if (recurrings.isNotEmpty()) {
                        repository.insertRecurringTransactions(recurrings)
                    }
                }
                // Restore Debts
                if (root.has("debts")) {
                    val array = root.getJSONArray("debts")
                    val debts = mutableListOf<Debt>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        debts.add(
                            Debt(
                                personName = obj.getString("personName"),
                                amount = obj.getDouble("amount"),
                                type = obj.getString("type"),
                                date = obj.getLong("date"),
                                description = obj.getString("description"),
                                isResolved = obj.getBoolean("isResolved"),
                                dueDate = if (obj.has("dueDate")) obj.getLong("dueDate") else null
                            )
                        )
                    }
                    if (debts.isNotEmpty()) {
                        repository.insertDebts(debts)
                    }
                }
            _backupStatus.value = "Data Restored Successfully!"
            }
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

    fun exportDatabaseToDbFile(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Force checkpoint to compile WAL logs into the DB file
                val db = com.akshar.data.db.AppDatabase.getInstance(context)
                try {
                    db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
                } catch (e: Exception) {
                    Log.e("FinanceViewModel", "Pre-export checkpoint failed: ${e.message}")
                }

                // 2. Locate DB file
                val dbFile = context.getDatabasePath("expense_tracker_pro_db")
                if (!dbFile.exists()) {
                    _backupStatus.value = "Database file does not exist yet."
                    return@launch
                }

                // 3. Copy to a temp file in cache
                val backupFile = File(context.cacheDir, "akshar_ledger_backup.db")
                if (backupFile.exists()) {
                    backupFile.delete()
                }

                FileInputStream(dbFile).use { input ->
                    FileOutputStream(backupFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // 4. Create sharing Uri and launch share intent
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    backupFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Export Backup SQLite file (.db)")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

                _backupStatus.value = "Backup File Exported!"
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Failed to export physical database", e)
                _backupStatus.value = "Failed to export backup file: ${e.message}"
            }
        }
    }

    fun restoreDatabaseFromDbFile(context: Context, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Close current db connection
                com.akshar.data.db.AppDatabase.closeAndResetInstance()

                // 2. Copy the source Uri stream into our DB path
                val dbFile = context.getDatabasePath("expense_tracker_pro_db")
                
                // Backup existing DB file in case of failure
                val backupOriginal = File(dbFile.parent, "expense_tracker_pro_db_original")
                if (dbFile.exists()) {
                    dbFile.renameTo(backupOriginal)
                }

                try {
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(dbFile).use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw Exception("Could not open input stream")

                    // 3. Clear companion temporary journals to avoid conflicts
                    val walFile = context.getDatabasePath("expense_tracker_pro_db-wal")
                    val shmFile = context.getDatabasePath("expense_tracker_pro_db-shm")
                    if (walFile.exists()) walFile.delete()
                    if (shmFile.exists()) shmFile.delete()

                    if (backupOriginal.exists()) {
                        backupOriginal.delete()
                    }

                    _backupStatus.value = "Database Restored Successfully! Restarting app..."

                    // 4. Force activity recreation / restart
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        context.startActivity(intent)
                    }, 1000)

                } catch (e: Exception) {
                    // Restore back on failure
                    if (backupOriginal.exists()) {
                        if (dbFile.exists()) dbFile.delete()
                        backupOriginal.renameTo(dbFile)
                    }
                    throw e
                }

            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Failed to restore database from file", e)
                _backupStatus.value = "Failed to restore backup file: ${e.message}"
            }
        }
    }
}
