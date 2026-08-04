package com.akshar.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.akshar.data.model.Bill
import java.util.concurrent.TimeUnit

object NotificationHelper {

    private const val CHANNEL_ID = "bill_reminders_channel"
    private const val CHANNEL_NAME = "Bill Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled bills"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleBillReminder(context: Context, bill: Bill, dueMillis: Long) {
        if (bill.reminderLeadTimeDays < 0) return // Allow 0 for same-day reminders

        val reminderTime = dueMillis - (bill.reminderLeadTimeDays * 24L * 60 * 60 * 1000)
        val now = System.currentTimeMillis()
        val delayMillis = reminderTime - now

        if (delayMillis >= 0) {
            val inputData = Data.Builder()
                .putLong("BILL_ID", bill.id)
                .putString("BILL_TITLE", bill.title)
                .putDouble("BILL_AMOUNT", bill.amount)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<BillReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "BillReminder_${bill.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    fun cancelBillReminder(context: Context, billId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("BillReminder_$billId")
    }
}
