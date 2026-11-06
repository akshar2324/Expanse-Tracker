package com.akshar.data.db

import android.database.sqlite.SQLiteDatabase
import java.io.File

/** Historical table definitions frozen here so migrations are checked against real SQLite. */
internal object LegacyDatabaseFixtures {
    fun create(file: File, version: Int) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val tables = listOf(
                "CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `date` INTEGER NOT NULL, `paymentMethod` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `importBatchId` TEXT, `originalCsvRowHash` TEXT, `accountId` INTEGER NOT NULL DEFAULT 1, `transferId` TEXT)",
                "CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `limitAmount` REAL NOT NULL, `month` TEXT NOT NULL, `frequency` TEXT NOT NULL, `remindersEnabled` INTEGER NOT NULL, `reminderThreshold` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `savings_goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `currentAmount` REAL NOT NULL, `targetDate` TEXT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `recurring_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `frequency` TEXT NOT NULL, `paymentMethod` TEXT NOT NULL, `lastTriggered` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `debts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `personName` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `date` INTEGER NOT NULL, `description` TEXT NOT NULL, `isResolved` INTEGER NOT NULL, `dueDate` INTEGER)",
                "CREATE TABLE IF NOT EXISTS `csv_import_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `delimiter` TEXT NOT NULL, `hasHeader` INTEGER NOT NULL, `dateColumn` TEXT NOT NULL, `dateFormat` TEXT NOT NULL, `amountColumn` TEXT, `debitColumn` TEXT, `creditColumn` TEXT, `descriptionColumn` TEXT NOT NULL, `categoryColumn` TEXT, `locale` TEXT NOT NULL, `invertAmountSigns` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `openingBalance` REAL NOT NULL, `isArchived` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)",
                "CREATE TABLE IF NOT EXISTS `reconciliations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `statementBalance` REAL NOT NULL, `calculatedBalance` REAL NOT NULL, `date` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)"
            )
            tables.forEach { sql ->
                val isNewTable = sql.contains("`accounts`") || sql.contains("`reconciliations`")
                if (!isNewTable || version >= 6) {
                    if (!sql.contains("`csv_import_profiles`") || version >= 5) {
                        var historical = sql
                        if (version < 6) historical = historical
                            .replace(", `accountId` INTEGER NOT NULL DEFAULT 1, `transferId` TEXT", "")
                        if (version < 5) historical = historical
                            .replace(", `importBatchId` TEXT, `originalCsvRowHash` TEXT", "")
                        db.execSQL(historical)
                    }
                }
            }
            db.execSQL("INSERT INTO transactions (id, amount, type, category, description, date, paymentMethod, timestamp) VALUES (42, 123456.78, 'EXPENSE', 'Food', 'Legacy', 1, 'Cash', 1)")
            db.version = version
        }
    }
}
