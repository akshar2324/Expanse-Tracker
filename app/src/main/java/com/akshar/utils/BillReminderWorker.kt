package com.akshar.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.akshar.MainActivity
import java.text.NumberFormat
import java.util.Locale

class BillReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val billId = inputData.getLong("BILL_ID", -1L)
        val title = inputData.getString("BILL_TITLE") ?: "Scheduled Bill"
        val amount = inputData.getDouble("BILL_AMOUNT", 0.0)

        if (billId == -1L) return Result.failure()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Return success so WorkManager doesn't infinitely retry when permission is merely denied
                return Result.success()
            }
        }

        NotificationHelper.createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "calendar")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            billId.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Try format with locale matching the context
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        val formattedAmount = try {
             currencyFormatter.format(amount)
        } catch (e: Exception) {
             amount.toString()
        }

        val notification = NotificationCompat.Builder(context, "bill_reminders_channel")
            // Use the app launcher icon or standard icon
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("Bill Reminder: $title")
            .setContentText("Amount due: $formattedAmount")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED) {
                try {
                    val method = notificationManager.javaClass.getMethod("notify", Int::class.java, android.app.Notification::class.java)
                    method.invoke(notificationManager, billId.toInt(), notification)
                } catch (e: Exception) {
                    // Ignored
                }
            }
        } catch (e: SecurityException) {
            // Android 13+ strict permission enforcement fallback
        }

        return Result.success()
    }
}
