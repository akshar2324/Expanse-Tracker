package com.akshar.sms.inbox

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Watches notifications from messaging, UPI and bank apps (see [AutoImportCapture]) and queues
 * bank payments for review. Runs only after the user grants notification access in system
 * settings, and does nothing while auto-import is switched off in the app. Everything happens on
 * the device; notification text is parsed in memory and never written anywhere.
 */
class BankNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (!AutoImportCapture.isWatched(notification.packageName)) return
        if (notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (!AutoImportSettings.isEnabled(this)) return

        val (title, text) = readText(notification.notification)
        val packageName = notification.packageName
        val postedAt = notification.postTime
        scope.launch {
            runCatching {
                val item = AutoImportCapture.capture(packageName, title, text, postedAt) ?: return@runCatching
                val dao = ImportInboxDatabase.getInstance(applicationContext).pendingImportDao()
                dao.insert(item)
                dao.deleteOlderThan(System.currentTimeMillis() - KEEP_MS)
            }.onFailure {
                // Parser/database exceptions can include input text; never pass them to logs.
                Log.w(TAG, "Could not queue a payment notification")
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Title and full text; messaging apps put the latest message in MessagingStyle. */
    private fun readText(notification: Notification): Pair<String?, String?> {
        val messaging = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val last = messaging?.messages?.lastOrNull()
        if (last?.text != null) {
            val sender = last.person?.name?.toString() ?: messaging.conversationTitle?.toString()
                ?: notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            return sender to last.text.toString()
        }
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        return title to text
    }

    private companion object {
        const val TAG = "AutoImport"
        const val KEEP_MS = 60L * 24 * 60 * 60 * 1000 // unreviewed items are dropped after 60 days
    }
}
