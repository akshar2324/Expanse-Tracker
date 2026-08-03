package com.akshar

import android.app.Application
import com.akshar.data.db.AppDatabase
import com.akshar.data.repository.FinanceRepository

class ExpenseTrackerApp : Application() {

    lateinit var repository: FinanceRepository
        private set

    companion object {
        lateinit var instance: ExpenseTrackerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val database = AppDatabase.getInstance(this)
        repository = FinanceRepository(database.financeDao(), database)
    }
}
