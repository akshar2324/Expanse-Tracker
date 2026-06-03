package com.example.data.model

import androidx.room.Entity
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
    val timestamp: Long = System.currentTimeMillis()
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

@Entity(tableName = "sms_templates")
data class SmsTemplate(
    @PrimaryKey val id: String, // "CREDIT" or "DEBIT"
    val exampleText: String,    // The message text fed by user
    val keywords: String        // Derived keywords for parsing
)

@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,           // "EXPENSE" (debit) or "INCOME" (credit)
    val smsSender: String,
    val smsBody: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "parsed_sms_logs")
data class ParsedSmsLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val smsSender: String,
    val smsBody: String,
    val amount: Double,
    val type: String,           // "EXPENSE" or "INCOME"
    val date: Long,
    val status: String,         // "PENDING", "CONFIRMED", "IGNORED"
    val category: String = ""   // Category if confirmed
)
