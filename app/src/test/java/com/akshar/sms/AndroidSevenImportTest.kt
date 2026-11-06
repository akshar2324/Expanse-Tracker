package com.akshar.sms

import com.akshar.sms.inbox.AutoImportCapture
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25])
class AndroidSevenImportTest {
    @Test
    fun platformGateRejectsBeforeParsing() {
        val text = "Sent Rs.99 From HDFC Bank A/C *4821 To X On 14/10/26 Ref 123"
        assertFalse(BankSmsImport.isSupported)
        assertEquals(SmsImportResult.Unsupported, BankSmsImport.parse(text))
        assertNull(AutoImportCapture.capture("com.google.android.apps.messaging", "JD-HDFCBK-S", text, 1L))
    }
}
