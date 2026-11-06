package com.akshar.sms.inbox

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationPrivacyAuditTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sentinel = "PRIVATE_SENTINEL_6e4c"
    private val text = "Sent Rs.232.00 From HDFC Bank A/C *4821 To SWIGGY On 14/10/26 Ref 629100004321 $sentinel"
    private fun notification(): StatusBarNotification {
        val n = Notification.Builder(context, "audit").setContentTitle("JD-HDFCBK-S").setContentText(text).build()
        return StatusBarNotification("com.google.android.apps.messaging", "com.google.android.apps.messaging", 7, null, 1000, 0, 0, n, android.os.Process.myUserHandle(), System.currentTimeMillis())
    }
    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue("Timed out waiting for listener", condition())
    }
    @After fun close() {
        unmockkAll()
        ImportInboxDatabase.closeAndResetInstance()
    }

    @Test fun listenerStoresOnlyParsedFieldsAndDeduplicatesReposts() = runBlocking {
        val controller = Robolectric.buildService(BankNotificationListener::class.java).create()
        val dao = ImportInboxDatabase.getInstance(context).pendingImportDao()
        try {
            repeat(10) { controller.get().onNotificationPosted(notification()) }
            await { runBlocking { dao.observeCount().first() == 1 } }
        } finally { controller.destroy() }
        val item = dao.observeAll().first().single()
        assertEquals("SWIGGY", item.description)
        assertFalse(item.toString().contains(sentinel))
        for (prefs in listOf("auto_import", "vault_settings", "country_settings")) {
            assertFalse(context.getSharedPreferences(prefs, Context.MODE_PRIVATE).all.toString().contains(sentinel))
        }
        assertFalse(ShadowLog.getLogs().any { it.msg.contains(sentinel) || it.throwable?.toString()?.contains(sentinel) == true })
        ImportInboxDatabase.closeAndResetInstance()
        context.getDatabasePath("import_inbox_db").parentFile!!.listFiles()!!
            .filter { it.name.startsWith("import_inbox_db") }.forEach {
                assertFalse("Raw text in ${it.name}", it.readBytes().toString(Charsets.ISO_8859_1).contains(sentinel))
            }
    }

    @Test fun listenerErrorMustNotLogExceptionThatContainsNotificationText() {
        mockkObject(AutoImportCapture)
        every { AutoImportCapture.capture(any(), any(), any(), any(), any()) } throws IllegalArgumentException(sentinel)
        val controller = Robolectric.buildService(BankNotificationListener::class.java).create()
        try {
            controller.get().onNotificationPosted(notification())
            await { ShadowLog.getLogsForTag("AutoImport").isNotEmpty() }
            val logs = ShadowLog.getLogsForTag("AutoImport")
            assertFalse(logs.any { it.msg.contains(sentinel) || it.throwable?.toString()?.contains(sentinel) == true })
        } finally { controller.destroy() }
    }
}
