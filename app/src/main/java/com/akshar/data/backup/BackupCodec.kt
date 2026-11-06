package com.akshar.data.backup

import com.akshar.data.model.Account
import com.akshar.data.model.Budget
import com.akshar.data.model.CsvImportProfile
import com.akshar.data.model.Debt
import com.akshar.data.model.Reconciliation
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.utils.MoneyUtils
import org.json.JSONArray
import org.json.JSONObject

/** Everything a JSON backup holds. */
data class BackupSnapshot(
    val transactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val debts: List<Debt> = emptyList(),
    val csvImportProfiles: List<CsvImportProfile> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val reconciliations: List<Reconciliation> = emptyList()
)

/**
 * JSON encoding of [BackupSnapshot].
 *
 * Row ids are written so a restore keeps the links between transactions, reconciliations and
 * their accounts. Backups made before ids were written still decode: rows get new ids, and a
 * backup with no accounts gets the default account (id 1) that transactions point at.
 */
object BackupCodec {

    private val SECTIONS = listOf(
        "transactions", "budgets", "savings_goals", "recurring_transactions",
        "debts", "csv_import_profiles", "accounts", "reconciliations"
    )

    fun encode(snapshot: BackupSnapshot): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("transactions", JSONArray().apply {
            snapshot.transactions.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("amount", it.amount)
                    put("type", it.type)
                    put("category", it.category)
                    put("description", it.description)
                    put("date", it.date)
                    put("paymentMethod", it.paymentMethod)
                    put("timestamp", it.timestamp)
                    it.importBatchId?.let { id -> put("importBatchId", id) }
                    it.originalCsvRowHash?.let { hash -> put("originalCsvRowHash", hash) }
                    put("accountId", it.accountId)
                    it.transferId?.let { tid -> put("transferId", tid) }
                })
            }
        })
        root.put("budgets", JSONArray().apply {
            snapshot.budgets.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("category", it.category)
                    put("limitAmount", it.limitAmount)
                    put("month", it.month)
                    put("frequency", it.frequency)
                    put("remindersEnabled", it.remindersEnabled)
                    put("reminderThreshold", it.reminderThreshold)
                })
            }
        })
        root.put("savings_goals", JSONArray().apply {
            snapshot.savingsGoals.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("targetAmount", it.targetAmount)
                    put("currentAmount", it.currentAmount)
                    put("targetDate", it.targetDate)
                })
            }
        })
        root.put("recurring_transactions", JSONArray().apply {
            snapshot.recurringTransactions.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("amount", it.amount)
                    put("type", it.type)
                    put("category", it.category)
                    put("description", it.description)
                    put("frequency", it.frequency)
                    put("paymentMethod", it.paymentMethod)
                    put("lastTriggered", it.lastTriggered)
                })
            }
        })
        root.put("debts", JSONArray().apply {
            snapshot.debts.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("personName", it.personName)
                    put("amount", it.amount)
                    put("type", it.type)
                    put("date", it.date)
                    put("description", it.description)
                    put("isResolved", it.isResolved)
                    it.dueDate?.let { due -> put("dueDate", due) }
                })
            }
        })
        root.put("csv_import_profiles", JSONArray().apply {
            snapshot.csvImportProfiles.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("delimiter", it.delimiter)
                    put("hasHeader", it.hasHeader)
                    put("dateColumn", it.dateColumn)
                    put("dateFormat", it.dateFormat)
                    it.amountColumn?.let { col -> put("amountColumn", col) }
                    it.debitColumn?.let { col -> put("debitColumn", col) }
                    it.creditColumn?.let { col -> put("creditColumn", col) }
                    put("descriptionColumn", it.descriptionColumn)
                    it.categoryColumn?.let { col -> put("categoryColumn", col) }
                    put("locale", it.locale)
                    put("invertAmountSigns", it.invertAmountSigns)
                })
            }
        })
        root.put("accounts", JSONArray().apply {
            snapshot.accounts.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("type", it.type)
                    put("openingBalance", it.openingBalance)
                    put("isArchived", it.isArchived)
                    put("timestamp", it.timestamp)
                })
            }
        })
        root.put("reconciliations", JSONArray().apply {
            snapshot.reconciliations.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("accountId", it.accountId)
                    put("statementBalance", it.statementBalance)
                    put("calculatedBalance", it.calculatedBalance)
                    put("date", it.date)
                    put("timestamp", it.timestamp)
                })
            }
        })
        return root.toString(4)
    }

    /** Parses a backup. Throws [org.json.JSONException] if the JSON or any required field is invalid. */
    fun decode(json: String): BackupSnapshot {
        val root = JSONObject(json)
        if (SECTIONS.none { root.has(it) }) {
            throw org.json.JSONException("Not an AkSpend backup: no known sections")
        }
        // Missing version is the original export format. Never reinterpret a future schema
        // as today's snapshot: replacing the ledger would silently discard unknown data.
        if (root.has("version") && root.getInt("version") !in 1..2) {
            throw org.json.JSONException("Unsupported backup version")
        }
        val now = System.currentTimeMillis()

        // Backups written before ids were exported listed accounts in id order, which is
        // what their transactions' accountId values refer to, so number them from 1.
        val accounts = root.objects("accounts").mapIndexed { index, it ->
            Account(
                id = it.optLong("id", index + 1L),
                name = it.getString("name"),
                type = it.getString("type"),
                openingBalance = it.getDouble("openingBalance"),
                isArchived = it.optBoolean("isArchived", false),
                timestamp = it.optLong("timestamp", now)
            )
        }.ifEmpty {
            listOf(Account(id = 1L, name = "Cash", type = "Cash", openingBalance = 0.0))
        }

        val snapshot = BackupSnapshot(
            transactions = root.objects("transactions").map {
                Transaction(
                    id = it.optLong("id", 0L),
                    amount = it.getDouble("amount"),
                    type = it.getString("type"),
                    category = it.getString("category"),
                    description = it.optString("description", ""),
                    date = it.getLong("date"),
                    paymentMethod = it.optString("paymentMethod", "Cash"),
                    timestamp = it.optLong("timestamp", now),
                    importBatchId = it.optStringOrNull("importBatchId"),
                    originalCsvRowHash = it.optStringOrNull("originalCsvRowHash"),
                    accountId = it.optLong("accountId", 1L),
                    transferId = it.optStringOrNull("transferId")
                )
            },
            budgets = root.objects("budgets").map {
                Budget(
                    id = it.optLong("id", 0L),
                    category = it.getString("category"),
                    limitAmount = it.getDouble("limitAmount"),
                    month = it.getString("month"),
                    frequency = it.optString("frequency", "MONTHLY"),
                    remindersEnabled = it.optBoolean("remindersEnabled", false),
                    reminderThreshold = it.optInt("reminderThreshold", 90)
                )
            },
            savingsGoals = root.objects("savings_goals").map {
                SavingsGoal(
                    id = it.optLong("id", 0L),
                    name = it.getString("name"),
                    targetAmount = it.getDouble("targetAmount"),
                    currentAmount = it.getDouble("currentAmount"),
                    targetDate = it.optString("targetDate", "")
                )
            },
            recurringTransactions = root.objects("recurring_transactions").map {
                RecurringTransaction(
                    id = it.optLong("id", 0L),
                    amount = it.getDouble("amount"),
                    type = it.getString("type"),
                    category = it.getString("category"),
                    description = it.optString("description", ""),
                    frequency = it.getString("frequency"),
                    paymentMethod = it.optString("paymentMethod", "Cash"),
                    lastTriggered = it.optLong("lastTriggered", 0L)
                )
            },
            debts = root.objects("debts").map {
                Debt(
                    id = it.optLong("id", 0L),
                    personName = it.getString("personName"),
                    amount = it.getDouble("amount"),
                    type = it.getString("type"),
                    date = it.getLong("date"),
                    description = it.optString("description", ""),
                    isResolved = it.optBoolean("isResolved", false),
                    dueDate = if (it.has("dueDate") && !it.isNull("dueDate")) it.getLong("dueDate") else null
                )
            },
            csvImportProfiles = root.objects("csv_import_profiles").map {
                CsvImportProfile(
                    id = it.optLong("id", 0L),
                    name = it.getString("name"),
                    delimiter = it.getString("delimiter"),
                    hasHeader = it.getBoolean("hasHeader"),
                    dateColumn = it.getString("dateColumn"),
                    dateFormat = it.getString("dateFormat"),
                    amountColumn = it.optStringOrNull("amountColumn"),
                    debitColumn = it.optStringOrNull("debitColumn"),
                    creditColumn = it.optStringOrNull("creditColumn"),
                    descriptionColumn = it.getString("descriptionColumn"),
                    categoryColumn = it.optStringOrNull("categoryColumn"),
                    locale = it.getString("locale"),
                    invertAmountSigns = it.optBoolean("invertAmountSigns", false)
                )
            },
            accounts = accounts,
            reconciliations = root.objects("reconciliations").map {
                Reconciliation(
                    id = it.optLong("id", 0L),
                    accountId = it.getLong("accountId"),
                    statementBalance = it.getDouble("statementBalance"),
                    calculatedBalance = it.getDouble("calculatedBalance"),
                    date = it.getLong("date"),
                    timestamp = it.optLong("timestamp", now)
                )
            }
        )
        validate(snapshot)
        return snapshot
    }

    /**
     * Rejects a backup that would restore impossible data, before anything is replaced: amounts
     * that are negative, zero where that means nothing, or too large; unknown types and
     * frequencies; bad budget periods; and transactions or reconciliations pointing at an account
     * the backup doesn't have. The message names the first bad row, for example
     * "transactions[3]: amount must be above zero".
     */
    private fun validate(s: BackupSnapshot) {
        fun fail(message: String): Nothing = throw org.json.JSONException(message)
        fun positive(where: String, value: Double) {
            if (!(value > 0) || !MoneyUtils.isWithinLimit(value)) fail("$where must be above zero and within the supported limit")
        }
        fun notNegative(where: String, value: Double) {
            if (!(value >= 0) || !MoneyUtils.isWithinLimit(value)) fail("$where must not be negative")
        }
        fun finite(where: String, value: Double) {
            if (!MoneyUtils.isWithinLimit(value)) fail("$where is not a supported amount")
        }
        fun oneOf(where: String, value: String, allowed: Set<String>) {
            if (value !in allowed) fail("$where must be one of ${allowed.joinToString()}")
        }

        val known = HashSet<Long>()
        s.accounts.forEachIndexed { i, a -> if (!known.add(a.id)) fail("accounts[$i]: id ${a.id} is used twice") }
        s.accounts.forEachIndexed { i, a ->
            if (a.name.isBlank()) fail("accounts[$i]: name is empty")
            finite("accounts[$i]: openingBalance", a.openingBalance)
        }
        s.transactions.forEachIndexed { i, t ->
            positive("transactions[$i]: amount", t.amount)
            oneOf("transactions[$i]: type", t.type, TRANSACTION_TYPES)
            if (t.date <= 0) fail("transactions[$i]: date is missing")
            if (t.accountId !in known) fail("transactions[$i]: account ${t.accountId} is not in the backup")
        }
        s.budgets.forEachIndexed { i, b ->
            positive("budgets[$i]: limitAmount", b.limitAmount)
            oneOf("budgets[$i]: frequency", b.frequency, BUDGET_FREQUENCIES)
            val period = if (b.frequency == "WEEKLY") WEEK else MONTH
            if (!period.matches(b.month)) fail("budgets[$i]: period \"${b.month}\" doesn't match ${b.frequency}")
            if (b.reminderThreshold !in 1..100) fail("budgets[$i]: reminderThreshold must be 1 to 100")
        }
        s.savingsGoals.forEachIndexed { i, g ->
            positive("savings_goals[$i]: targetAmount", g.targetAmount)
            notNegative("savings_goals[$i]: currentAmount", g.currentAmount)
        }
        s.recurringTransactions.forEachIndexed { i, r ->
            positive("recurring_transactions[$i]: amount", r.amount)
            oneOf("recurring_transactions[$i]: type", r.type, TRANSACTION_TYPES)
            oneOf("recurring_transactions[$i]: frequency", r.frequency, RECURRING_FREQUENCIES)
        }
        s.debts.forEachIndexed { i, d ->
            positive("debts[$i]: amount", d.amount)
            oneOf("debts[$i]: type", d.type, DEBT_TYPES)
        }
        s.reconciliations.forEachIndexed { i, r ->
            finite("reconciliations[$i]: statementBalance", r.statementBalance)
            finite("reconciliations[$i]: calculatedBalance", r.calculatedBalance)
            if (r.accountId !in known) fail("reconciliations[$i]: account ${r.accountId} is not in the backup")
        }
    }

    private val TRANSACTION_TYPES = setOf("EXPENSE", "INCOME")
    private val BUDGET_FREQUENCIES = setOf("MONTHLY", "WEEKLY")
    private val RECURRING_FREQUENCIES = setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")
    private val DEBT_TYPES = setOf("BORROWED", "LENT")
    private val MONTH = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
    // Week numbers are stored without zero padding ("2026-W5"), see PeriodUtils.weekKey.
    private val WEEK = Regex("^\\d{4}-W0?([1-9]|[1-4]\\d|5[0-3])$")

    private fun JSONObject.objects(key: String): List<JSONObject> {
        if (!has(key)) return emptyList()
        val array = getJSONArray(key) // throws if the section is present but not an array
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
