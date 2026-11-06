package com.akshar.sms

import com.akshar.sms.inbox.AutoImportCapture
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

/** Synthetic examples only; no real account, contact, or notification data. */
class BankMessageAuditTest {
    private val received = 1_799_000_000_000L
    private val zone = ZoneId.of("Asia/Kolkata")
    private val bank = "JD-HDFCBK-S"
    private fun parse(body: String) = BankSmsImport.parse(body, bank, received, zone, supported = true)

    @Test
    fun nonPaymentsNeverProduceDraftsOrQueueEntries() {
        val messages = listOf(
            "123456 is your OTP for Rs.500 at AMAZON. Do not share.",
            "Your UPI payment of Rs.250.00 to SWIGGY has failed. Money if debited will be refunded.",
            "Your payment of Rs.250 has been declined.",
            "Reminder: Rs.500 payment is due tomorrow.",
            "You have a collect request for Rs.500 from friend@upi.",
            "Your payment of Rs.500 is scheduled for tomorrow.",
            "Payment request: Please pay Rs.500 to friend@upi.",
            "Payment of Rs.5000 received towards your credit card bill. Thank you.",
            "See you at 7 tonight!", "", "   ",
            "कल ₹५०० भेजना, धन्यवाद", "நாளை சந்திப்போம்", "مرحبا كيف حالك",
        )
        val failures = messages.filter { body ->
            parse(body) is SmsImportResult.Found || AutoImportCapture.capture(
                "com.google.android.apps.messaging", bank, body, received, supported = true
            ) != null
        }
        assertTrue("Non-payments imported: $failures", failures.isEmpty())
    }

    @Test
    fun realisticAmountsKeepDirectionAndCents() {
        for ((textAmount, expected) in listOf("99" to "99.00", "1,23,456.78" to "123456.78", "0.01" to "0.01")) {
            val result = parse("Sent Rs.$textAmount From HDFC Bank A/C *4821 To SWIGGY On 14/10/26 Ref 629100004321")
            assertTrue("$textAmount: $result", result is SmsImportResult.Found)
            val draft = (result as SmsImportResult.Found).draft
            assertEquals(expected, draft.amount)
            assertTrue(draft.isExpense)
        }
        val refund = parse("Rs.1500.50 credited to HDFC Bank A/c XX4821 on 05-01-26 from VPA refund@okaxis (UPI 401234567890)")
        assertTrue(refund is SmsImportResult.Found)
        assertFalse((refund as SmsImportResult.Found).draft.isExpense)
    }

    @Test
    fun missingAndAmbiguousDayMonthDatesAreDeterministic() {
        val noDate = parse("Amt Deducted! Rs.11500 from your HDFC Bank A/c XX4821") as SmsImportResult.Found
        assertEquals(received, noDate.draft.dateMillis)
        val ambiguous = parse("Sent Rs.99 From HDFC Bank A/C *4821 To X On 05/01/26 Ref 123") as SmsImportResult.Found
        assertEquals(java.time.LocalDate.of(2026, 1, 5).atStartOfDay(zone).toInstant().toEpochMilli(), ambiguous.draft.dateMillis)
        val future = AutoImportCapture.capture(
            "com.google.android.apps.messaging", bank,
            "Sent Rs.99 From HDFC Bank A/C *4821 To X On 05/01/30 Ref 123", received, supported = true
        )!!
        assertEquals(received, future.dateMillis)
    }

    @Test
    fun amountBeyondDoubleRangeCannotReachTheMoneyUi() {
        val amount = "9".repeat(400)
        assertFalse(parse("Sent Rs.$amount From HDFC Bank A/C *4821 To X On 14/10/26 Ref 123") is SmsImportResult.Found)
    }

    @Test
    fun zeroAndNegativeAmountsDoNotProduceDrafts() {
        for (amount in listOf("0", "0.00", "-100.00")) {
            assertFalse("amount $amount", parse("Sent Rs.$amount From HDFC Bank A/C *4821 To X On 14/10/26 Ref 123") is SmsImportResult.Found)
        }
    }

    @Test
    fun contactChatsIncludingCopiedBankTemplatesNeverQueue() {
        for (title in listOf("Mom", "+919876543210", "Office group")) {
            for (body in listOf(
                "Rs 450.00 debited from your account at PETROL PUMP NH8.",
                "Sent Rs.232.00 From HDFC Bank A/C *4821 To SWIGGY On 14/10/26 Ref 629100004321",
                "Please send me Rs.500 for dinner", "मुझे ₹500 भेज देना"
            )) assertNull(AutoImportCapture.capture("com.google.android.apps.messaging", title, body, received, supported = true))
        }
    }
}
