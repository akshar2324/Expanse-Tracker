package com.akshar.sms.inbox

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34, 36])
class PassiveNotificationCaptureTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val messages = "com.google.android.apps.messaging"
    private val payment = "Sent Rs.232.00 From HDFC Bank A/C *4821 To SWIGGY On 14/10/26 Ref 629100004321"

    private fun notification(pkg: String = messages, summary: Boolean = false): StatusBarNotification {
        val builder = Notification.Builder(context, "passive-test")
            .setContentTitle("JD-HDFCBK-S").setContentText(payment)
        if (summary) builder.setGroup("messages").setGroupSummary(true)
        return StatusBarNotification(pkg, pkg, 7, null, 1000, 0, 0, builder.build(),
            android.os.Process.myUserHandle(), System.currentTimeMillis())
    }

    @After fun close() { ImportInboxDatabase.closeAndResetInstance() }

    @Test fun captureRequiresExplicitOptInAndHonorsEachSource() {
        assertFalse(AutoImportSettings.isEnabled(context))
        assertFalse(AutoImportSettings.canCapture(context, messages))
        AutoImportSettings.setEnabled(context, true)
        assertTrue(AutoImportSettings.canCapture(context, messages))
        AutoImportSettings.setSourceEnabled(context, messages, false)
        assertFalse(AutoImportSettings.canCapture(context, messages))
        assertTrue(AutoImportSettings.canCapture(context, "com.phonepe.app"))
        assertFalse(AutoImportSettings.canCapture(context, "com.huawei.health"))
        AutoImportSettings.setEnabled(context, false)
        assertFalse(AutoImportSettings.canCapture(context, "com.phonepe.app"))
        AutoImportSettings.setEnabled(context, true)
        assertFalse("Source choice survives pause/resume", AutoImportSettings.canCapture(context, messages))
    }

    @Test fun captureLeavesOriginalAndOtherAppNotificationsAndAlertPolicyUnchanged() = runBlocking {
        AutoImportSettings.setEnabled(context, true)
        val controller = Robolectric.buildService(BankNotificationListener::class.java).create()
        val service = controller.get()
        val shadow = shadowOf(service)
        val original = notification()
        val unrelated = notification("com.example.watchcompanion")
        shadow.addActiveNotification(original)
        shadow.addActiveNotification(unrelated)
        val keys = service.activeNotifications.map { it.key }.toSet()
        val hints = service.currentListenerHints
        val filter = service.currentInterruptionFilter
        val dao = ImportInboxDatabase.getInstance(context).pendingImportDao()
        try {
            service.onNotificationPosted(original)
            service.onNotificationPosted(unrelated)
            val deadline = System.nanoTime() + 5_000_000_000L
            while (dao.observeCount().first() == 0 && System.nanoTime() < deadline) Thread.sleep(10)
            assertEquals(1, dao.observeCount().first())
            assertEquals(keys, service.activeNotifications.map { it.key }.toSet())
            assertEquals(payment, original.notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
            assertEquals(hints, service.currentListenerHints)
            assertEquals(filter, service.currentInterruptionFilter)
            dao.clear() // Removing all inbox drafts must not dismiss their source notifications.
            assertEquals(keys, service.activeNotifications.map { it.key }.toSet())
            assertEquals(0, shadow.unbindRequestCount)
        } finally { controller.destroy() }
    }

    @Test fun pausedDisabledAndSummaryNotificationsDoNotQueueAnything() = runBlocking {
        val controller = Robolectric.buildService(BankNotificationListener::class.java).create()
        try {
            controller.get().onNotificationPosted(notification()) // off by default
            AutoImportSettings.setEnabled(context, true)
            AutoImportSettings.setSourceEnabled(context, messages, false)
            controller.get().onNotificationPosted(notification())
            AutoImportSettings.setSourceEnabled(context, messages, true)
            controller.get().onNotificationPosted(notification(summary = true))
            assertEquals(0, ImportInboxDatabase.getInstance(context).pendingImportDao().observeCount().first())
        } finally { controller.destroy() }
    }
}
