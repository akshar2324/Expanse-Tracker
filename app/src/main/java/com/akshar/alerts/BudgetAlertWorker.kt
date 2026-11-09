package com.akshar.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.akshar.MainActivity
import com.akshar.R
import com.akshar.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BudgetAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        BudgetAlertStore.runLock.withLock {
            val store = BudgetAlertStore(applicationContext)
            if (!store.enabled.first() || !canNotify(applicationContext)) return@withLock Result.success()
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(NotificationChannel(CHANNEL, "Monthly budget alerts", NotificationManager.IMPORTANCE_DEFAULT))
                if (manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) {
                    return@withLock Result.success()
                }
            }
            val database = AppDatabase.getInstance(applicationContext)
            val (budgets, transactions) = database.withTransaction {
                database.financeDao().let { it.getBudgetsList() to it.getTransactionsList() }
            }
            val pending = BudgetAlertPolicy.pending(budgets, transactions, store.sent(), System.currentTimeMillis())
            val openApp = PendingIntent.getActivity(applicationContext, 0,
                Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            for (alert in pending) {
                if (!canNotify(applicationContext)) break
                if (store.claim(alert.key)) {
                    val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
                        .setSmallIcon(R.drawable.ic_budget_alert)
                        .setContentTitle("Monthly budget alert")
                        .setContentText("A category budget has reached ${alert.level}% this month. Open AkSpend to review.")
                        .setContentIntent(openApp)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .build()
                    // A revoked permission between checking and posting must not fail background work.
                    try { manager.notify(alert.key, 0, notification) } catch (_: SecurityException) { break }
                }
            }
            Result.success()
        }
    }

    companion object {
        const val CHANNEL = "monthly_budget_alerts"
        fun canNotify(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
        }
    }
}
