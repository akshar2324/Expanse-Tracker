package com.akshar.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String,
    val description: String,
    val date: Long, // timestamp in ms
    val paymentMethod: String,
    val timestamp: Long = System.currentTimeMillis(),
    val importBatchId: String? = null,
    val originalCsvRowHash: String? = null,
    @ColumnInfo(defaultValue = "1") val accountId: Long = 1L,
    val transferId: String? = null // if part of a transfer, both sides share this ID
)

@Entity(tableName = "csv_import_profiles")
data class CsvImportProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val delimiter: String,
    val hasHeader: Boolean,
    val dateColumn: String,
    val dateFormat: String,
    val amountColumn: String? = null,
    val debitColumn: String? = null,
    val creditColumn: String? = null,
    val descriptionColumn: String,
    val categoryColumn: String? = null,
    val locale: String,
    val invertAmountSigns: Boolean = false
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val limitAmount: Double,
    val month: String, // format: "YYYY-MM", or "YYYY-Www"
    val frequency: String = "MONTHLY", // "MONTHLY" or "WEEKLY"
    val remindersEnabled: Boolean = false,
    val reminderThreshold: Int = 90 // warning percentage, e.g. 90
)

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String = "" // e.g. "Dec 2026"
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String,
    val description: String,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val paymentMethod: String = "Cash",
    val lastTriggered: Long = 0 // timestamp of last auto-generation
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amount: Double,
    val type: String, // "BORROWED" or "LENT"
    val date: Long,
    val description: String,
    val isResolved: Boolean = false,
    val dueDate: Long? = null
)

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // e.g., "Cash", "Bank", "Wallet", "Savings", "Credit Card"
    val openingBalance: Double = 0.0,
    val isArchived: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reconciliations")
data class Reconciliation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val statementBalance: Double,
    val calculatedBalance: Double,
    val date: Long,
    val timestamp: Long = System.currentTimeMillis()
)
