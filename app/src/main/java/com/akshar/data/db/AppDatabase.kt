package com.akshar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.akshar.data.model.Account
import com.akshar.data.model.Budget
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.Reconciliation
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.data.model.Debt
import com.akshar.data.model.CsvImportProfile

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN importBatchId TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN originalCsvRowHash TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `csv_import_profiles` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`name` TEXT NOT NULL, " +
            "`delimiter` TEXT NOT NULL, " +
            "`hasHeader` INTEGER NOT NULL, " +
            "`dateColumn` TEXT NOT NULL, " +
            "`dateFormat` TEXT NOT NULL, " +
            "`amountColumn` TEXT, " +
            "`debitColumn` TEXT, " +
            "`creditColumn` TEXT, " +
            "`descriptionColumn` TEXT NOT NULL, " +
            "`categoryColumn` TEXT, " +
            "`locale` TEXT NOT NULL, " +
            "`invertAmountSigns` INTEGER NOT NULL)"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `accounts` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`openingBalance` REAL NOT NULL, " +
                "`isArchived` INTEGER NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reconciliations` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`accountId` INTEGER NOT NULL, " +
                "`statementBalance` REAL NOT NULL, " +
                "`calculatedBalance` REAL NOT NULL, " +
                "`date` INTEGER NOT NULL, " +
                "`timestamp` INTEGER NOT NULL)"
        )

        val currentTime = System.currentTimeMillis()
        db.execSQL(
            "INSERT OR IGNORE INTO `accounts` (`id`, `name`, `type`, `openingBalance`, `isArchived`, `timestamp`) " +
                "VALUES (1, 'Default', 'Cash', 0.0, 0, $currentTime)"
        )
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `accountId` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `transferId` TEXT DEFAULT NULL")
    }
}

@Database(
    entities = [
        Transaction::class,
        Budget::class,
        SavingsGoal::class,
        RecurringTransaction::class,
        Debt::class,
        CsvImportProfile::class,
        Account::class,
        Reconciliation::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_pro_db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeAndResetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
