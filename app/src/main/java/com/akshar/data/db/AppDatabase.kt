package com.akshar.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.akshar.data.model.Budget
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import com.akshar.data.model.Debt
import com.akshar.data.model.CsvImportProfile
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

@Database(
    entities = [
        Transaction::class,
        Budget::class,
        SavingsGoal::class,
        RecurringTransaction::class,
        Debt::class,
        CsvImportProfile::class
    ],
    version = 5,
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
                .addMigrations(MIGRATION_4_5)
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
