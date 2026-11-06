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
import com.akshar.data.model.CsvImportProfile
import com.akshar.data.model.Account
import com.akshar.data.model.Reconciliation
import com.akshar.data.repository.FinanceRepository
import com.akshar.data.backup.BackupCodec
import com.akshar.insights.SubscriptionCandidate
import com.akshar.insights.MerchantKey
import com.akshar.utils.MoneyUtils
import com.akshar.utils.PeriodUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    val allCsvImportProfiles: StateFlow<List<CsvImportProfile>> = repository.allCsvImportProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reconciliations: StateFlow<List<Reconciliation>> = repository.allReconciliations
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
        paymentMethod: String,
        existing: Transaction? = null // when editing, keeps id, account, and import metadata
    ) {
        val amount = MoneyUtils.roundMoney(amount)
        viewModelScope.launch {
            val tx = existing?.copy(
                amount = amount,
                type = type,
                category = category,
                description = description,
                date = date,
                paymentMethod = paymentMethod
            ) ?: Transaction(
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

    suspend fun getTransactionById(id: Long): Transaction? {
        return repository.getTransactionById(id)
    }


    /** Creates a transfer, or replaces both legs when [transferId] belongs to an existing one. */
    fun saveTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, description: String, date: Long, transferId: String = UUID.randomUUID().toString()) {
        val amount = MoneyUtils.roundMoney(amount)
        viewModelScope.launch(Dispatchers.IO) {
            val fromTx = Transaction(
                amount = amount,
                type = "EXPENSE",
                category = "Transfer",
                description = description,
                date = date,
                paymentMethod = "Transfer",
                accountId = fromAccountId,
                transferId = transferId
            )
            val toTx = fromTx.copy(
                type = "INCOME",
                accountId = toAccountId,
                id = 0L
            )
            repository.saveTransfer(fromTx, toTx)
        }
    }

    suspend fun getTransferLegs(transferId: String): List<Transaction> {
        return repository.getTransferLegs(transferId)
    }

    fun deleteTransfer(transferId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransfer(transferId)
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
            val updated = goal.copy(
                currentAmount = MoneyUtils.roundMoney((goal.currentAmount + amount).coerceIn(0.0, goal.targetAmount))
            )
            repository.insertSavingsGoal(updated)
        }
    }

    fun deleteSavingsGoalById(id: Long) {
        viewModelScope.launch {
            repository.deleteSavingsGoalById(id)
        }
    }

    // --- CRUD: Recurring Transactions ---
    private val subscriptionRemindersInFlight = mutableSetOf<String>()

    /** Protect one-tap creation from repeated taps before Room refreshes the suggestion list. */
    fun makeSubscriptionReminder(candidate: SubscriptionCandidate) {
        viewModelScope.launch {
            if (!subscriptionRemindersInFlight.add(candidate.key)) return@launch
            try {
                val existing = repository.allRecurringTransactions.first()
                if (existing.none { MerchantKey.sameMerchant(MerchantKey.normalize(it.description), candidate.key) }) {
                    addRecurringTransaction(
                        amount = candidate.amount.toDouble(), type = "EXPENSE", category = candidate.category,
                        description = candidate.displayName, frequency = candidate.frequency.reminderValue,
                        paymentMethod = candidate.paymentMethod, lastTriggered = candidate.reminderAnchorMs
                    ).join()
                }
            } finally {
                subscriptionRemindersInFlight.remove(candidate.key)
            }
        }
    }

    fun addRecurringTransaction(
        amount: Double,
        type: String,
        category: String,
        description: String,
        frequency: String,
        paymentMethod: String,
        // The next due date is one period after this. Defaults to now; a reminder made from a
        // detected subscription passes its last charge so it lines up with the real schedule.
        lastTriggered: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        val rec = RecurringTransaction(
            amount = MoneyUtils.roundMoney(amount),
            type = type,
            category = category,
            description = description,
            frequency = frequency,
            paymentMethod = paymentMethod,
            lastTriggered = lastTriggered
        )
        repository.insertRecurringTransaction(rec)
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
            
            // Mark this cycle paid: move the trigger to the next due date.
            val nextDueDate = PeriodUtils.addPeriods(recurring.lastTriggered, recurring.frequency, 1)

            val updated = recurring.copy(lastTriggered = nextDueDate)
            repository.insertRecurringTransaction(updated)
        }
    }

    // --- Backup & Restore (JSON Export-Import) ---

    fun addOrUpdateAccount(account: Account) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAccount(account)
        }
    }

    fun addReconciliation(reconciliation: Reconciliation) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertReconciliation(reconciliation)
        }
    }

    /**
     * Serialises every table to JSON. Reads straight from the database, so the backup is
     * complete even when no screen is currently observing a given table.
     */
    suspend fun exportDataToJson(): String? = withContext(Dispatchers.IO) {
        try {
            BackupCodec.encode(repository.exportSnapshot())
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "Backup Export failed", e)
            null
        }
    }

    /**
     * Replaces all data with the backup in [jsonString]. The whole file is parsed and validated
     * first, so a malformed backup is rejected without touching existing data; the replacement
     * itself runs in a single database transaction.
     */
    fun restoreDataFromJson(jsonString: String): Boolean {
        val snapshot = try {
            BackupCodec.decode(jsonString)
        } catch (e: Exception) {
            Log.e("FinanceViewModel", "Restore failed", e)
            _backupStatus.value = "Failed to restore backup: invalid JSON format."
            return false
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.replaceAllData(snapshot)
                _backupStatus.value = "Data Restored Successfully!"
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Restore failed", e)
                _backupStatus.value = "Failed to restore backup: ${e.message}"
            }
        }
        return true
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
                val backupFile = File(context.cacheDir, "akspend_ledger_backup.db")
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
            val staged = File(context.cacheDir, "restore_staging.db")
            try {
                // 1. Copy the picked file to a staging file and validate it before touching
                //    the live database, so a wrong or corrupt file cannot destroy the ledger.
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(staged).use { output -> input.copyTo(output) }
                } ?: throw Exception("Could not open input stream")

                val problem = DatabaseFileValidator.problemWith(staged, com.akshar.data.db.DATABASE_VERSION)
                if (problem != null) {
                    _backupStatus.value = "Failed to restore backup file: $problem"
                    return@launch
                }

                // 2. Close the current db connection
                com.akshar.data.db.AppDatabase.closeAndResetInstance()

                val dbFile = context.getDatabasePath("expense_tracker_pro_db")
                val backupOriginal = File(dbFile.parent, "expense_tracker_pro_db_original")
                if (dbFile.exists()) {
                    dbFile.renameTo(backupOriginal)
                }

                try {
                    staged.copyTo(dbFile, overwrite = true)

                    // 3. Clear companion temporary journals to avoid conflicts
                    val walFile = context.getDatabasePath("expense_tracker_pro_db-wal")
                    val shmFile = context.getDatabasePath("expense_tracker_pro_db-shm")
                    if (walFile.exists()) walFile.delete()
                    if (shmFile.exists()) shmFile.delete()

                    if (backupOriginal.exists()) {
                        backupOriginal.delete()
                    }
                } catch (e: Exception) {
                    // Restore back on failure
                    if (backupOriginal.exists()) {
                        if (dbFile.exists()) dbFile.delete()
                        backupOriginal.renameTo(dbFile)
                    }
                    throw e
                }

                _backupStatus.value = "Database Restored Successfully! Restarting app..."

                // 4. Restart the process. The Application keeps a repository bound to the old,
                //    now closed database, so relaunching the activity alone is not enough.
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }, 1000)
            } catch (e: Exception) {
                Log.e("FinanceViewModel", "Failed to restore database from file", e)
                _backupStatus.value = "Failed to restore backup file: ${e.message}"
            } finally {
                staged.delete()
            }
        }
    }
}

/** Checks that a file is an SQLite database this app can open before it replaces the live one. */
object DatabaseFileValidator {
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /** Returns null when [file] looks restorable, otherwise a short reason. */
    fun problemWith(file: File, currentSchemaVersion: Int): String? {
        if (!file.exists() || file.length() < 100) return "the file is not a database backup."
        val header = ByteArray(100)
        FileInputStream(file).use { input ->
            var read = 0
            while (read < header.size) {
                val n = input.read(header, read, header.size - read)
                if (n < 0) break
                read += n
            }
            if (read < header.size) return "the file is not a database backup."
        }
        if (!header.copyOfRange(0, SQLITE_HEADER.size).contentEquals(SQLITE_HEADER)) {
            return "the file is not a database backup."
        }
        // PRAGMA user_version lives at offset 60 (big-endian). Room stores the schema version there.
        val userVersion = ((header[60].toInt() and 0xFF) shl 24) or
            ((header[61].toInt() and 0xFF) shl 16) or
            ((header[62].toInt() and 0xFF) shl 8) or
            (header[63].toInt() and 0xFF)
        if (userVersion > currentSchemaVersion) {
            return "the backup comes from a newer version of the app."
        }
        if (userVersion < MIN_RESTORABLE_VERSION) {
            return "the backup is too old to restore."
        }
        return null
    }

    private const val MIN_RESTORABLE_VERSION = 4
}
