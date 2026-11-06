package com.akshar.sms.inbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoImportCaptureTest {

    private val messages = "com.google.android.apps.messaging"
    private val gpay = "com.google.android.apps.nbu.paisa.user"
    private val now = 1_790_000_000_000L
    private val hdfcSms = "Sent Rs.232.00 From HDFC Bank A/C *4821 To KUMAR GENERAL STORE On 14/10/26 Ref 629100004321"

    private fun capture(pkg: String, title: String?, text: String?) =
        AutoImportCapture.capture(pkg, title, text, postedAt = now, supported = true)

    @Test
    fun bankSmsInMessagesApp_isQueued() {
        val item = capture(messages, "JD-HDFCBK-S", hdfcSms)
        assertNotNull(item)
        item!!
        assertEquals("232.00", item.amount)
        assertTrue(item.isExpense)
        assertEquals("KUMAR GENERAL STORE", item.description)
        assertEquals("Messages", item.source)
        assertTrue(item.confident)
        assertEquals(now, item.receivedAt)
    }

    @Test
    fun bankTemplateFromPersonalContact_isNeverQueued() {
        for (sender in listOf("Mom", "+919876543210", "", "Lunch group")) {
            assertNull("Bank wording does not authenticate a sender: $sender", capture(messages, sender, hdfcSms))
        }
        assertNull(capture(messages, null, hdfcSms))
    }

    @Test
    fun unwatchedApp_isIgnored() {
        assertNull(capture("com.whatsapp", "JD-HDFCBK-S", hdfcSms))
        assertFalse(AutoImportCapture.isWatched("com.example.game"))
    }

    @Test
    fun genericGuessFromAContact_isDropped_butKeptFromABankSender() {
        val vague = "Rs 450.00 debited from your account at PETROL PUMP NH8."
        assertNull("personal chats must not be imported on a guess", capture(messages, "Mom", vague))
        val fromBank = capture(messages, "AX-KOTAKB-S", vague)
        assertNotNull(fromBank)
        assertFalse(fromBank!!.confident)
    }

    @Test
    fun paymentAppGuess_isKeptAndFlagged() {
        val item = capture(gpay, "Payment sent", "Rs 450.00 debited from your account at PETROL PUMP NH8.")
        assertNotNull(item)
        assertEquals("Google Pay", item!!.source)
    }

    @Test
    fun otpAndEmpty_areNotQueued() {
        assertNull(capture(messages, "VM-HDFCBK", "123456 is your OTP for Rs.500 at AMAZON. Do not share."))
        assertNull(capture(messages, "VM-HDFCBK", "   "))
        assertNull(capture(messages, "VM-HDFCBK", null))
    }

    @Test
    fun futureDate_fallsBackToPostTime() {
        // 14/10/30 parses as 2030, years after the notification was posted.
        val item = capture(messages, "JD-HDFCBK-S", hdfcSms.replace("14/10/26", "14/10/30"))
        assertEquals(now, item!!.dateMillis)
    }

    @Test
    fun unsupportedAndroid_queuesNothing() {
        assertNull(AutoImportCapture.capture(messages, "JD-HDFCBK-S", hdfcSms, now, supported = false))
    }

    @Test
    fun dedupeKey_ignoresWhitespaceAndCase_butNotContentOrSource() {
        val a = AutoImportCapture.dedupeKey(messages, hdfcSms)
        assertEquals(a, AutoImportCapture.dedupeKey(messages, "  " + hdfcSms.replace(" ", "\n  ").uppercase()))
        assertNotEquals(a, AutoImportCapture.dedupeKey(messages, hdfcSms.replace("232.00", "233.00")))
        assertNotEquals(a, AutoImportCapture.dedupeKey(gpay, hdfcSms))
        assertEquals(64, a.length)
        assertFalse("hash must not contain the text", a.contains("kumar"))
    }

    @Test
    fun bankSenderIds() {
        assertTrue(AutoImportCapture.looksLikeBankSender("JD-HDFCBK-S"))
        assertTrue(AutoImportCapture.looksLikeBankSender("vm-sbiinb"))
        assertFalse(AutoImportCapture.looksLikeBankSender("Mom"))
        assertFalse(AutoImportCapture.looksLikeBankSender("+919876543210"))
    }
}
