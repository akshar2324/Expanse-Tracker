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

@Database(
    entities = [
        Transaction::class,
        Budget::class,
        SavingsGoal::class,
        RecurringTransaction::class,
        Debt::class
    ],
    version = 4,
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
                .fallbackToDestructiveMigration() // Simple approach for developments
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
