package com.akshar

import android.app.Application
import com.akshar.data.db.AppDatabase
import com.akshar.data.repository.FinanceRepository
import com.akshar.widget.BudgetWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ExpenseTrackerApp : Application() {

    lateinit var repository: FinanceRepository
        private set

    /** Lives as long as the process; for work that follows the database, not a screen. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        lateinit var instance: ExpenseTrackerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val database = AppDatabase.getInstance(this)
        repository = FinanceRepository(database.financeDao(), database)
        BudgetWidgetUpdater.start(this, database, appScope)
    }
}
