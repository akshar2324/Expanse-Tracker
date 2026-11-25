package com.akshar.alerts

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object BudgetAlertScheduler {
    const val DAILY = "monthly_budget_alerts_daily"
    const val AFTER_SAVE = "monthly_budget_alerts_after_save"

    fun start(context: Context, database: RoomDatabase, scope: CoroutineScope) {
        val app = context.applicationContext
        val store = BudgetAlertStore(app)
        scope.launch {
            // Default-off must not initialize background scheduling. Once opted in, retain the
            // manager so switching off cancels both kinds of work immediately.
            var work: WorkManager? = null
            store.enabled.distinctUntilChanged().collect { enabled ->
                if (enabled) {
                    val manager = WorkManager.getInstance(app)
                    work = manager
                    manager.enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.KEEP,
                        PeriodicWorkRequestBuilder<BudgetAlertWorker>(1, TimeUnit.DAYS).build())
                    checkNow(app)
                } else {
                    work?.cancelUniqueWork(DAILY)
                    work?.cancelUniqueWork(AFTER_SAVE)
                }
            }
        }
        database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(arrayOf("transactions", "budgets")) {
            override fun onInvalidated(tables: Set<String>) {
                scope.launch { if (store.enabled.first()) checkNow(app) }
            }
        })
    }

    private fun checkNow(context: Context) {
        // Append rather than KEEP: a save during a running check must get a subsequent check.
        WorkManager.getInstance(context).enqueueUniqueWork(AFTER_SAVE, ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<BudgetAlertWorker>().build())
    }
}
