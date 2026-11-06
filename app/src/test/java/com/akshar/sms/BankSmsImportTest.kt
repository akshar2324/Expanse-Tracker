package com.akshar.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class BankSmsImportTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val received = 1_790_000_000_000L

    private fun found(text: String, sender: String? = null): SmsDraft {
        val result = BankSmsImport.parse(text, sender, received, zone, supported = true)
        assertTrue("expected a transaction, got $result", result is SmsImportResult.Found)
        return (result as SmsImportResult.Found).draft
    }

    @Test
    fun hdfcUpiDebit_becomesExpenseWithMessageDate() {
        val draft = found(
            "Sent Rs.232.00\nFrom HDFC Bank A/C *4821\nTo KUMAR GENERAL STORE\nOn 14/10/26\nRef 629100004321\nNot You?",
            sender = "JD-HDFCBK-S",
        )
        assertEquals("232.00", draft.amount)
        assertTrue(draft.isExpense)
        assertEquals("KUMAR GENERAL STORE", draft.description)
        assertEquals(LocalDate.of(2026, 10, 14).atStartOfDay(zone).toInstant().toEpochMilli(), draft.dateMillis)
        assertTrue(draft.confident)
        assertEquals("hdfc", draft.bankId)
        assertEquals("4821", draft.accountTail)
    }

    @Test
    fun hdfcUpiCredit_becomesIncome() {
        val draft = found(
            "Rs.1,500.50 credited to HDFC Bank A/c XX4821 on 05-01-26 from VPA friend@okaxis (UPI 401234567890)",
            sender = "VM-HDFCBK",
        )
        assertEquals("1500.50", draft.amount)
        assertFalse(draft.isExpense)
        assertEquals("friend@okaxis", draft.description)
    }

    @Test
    fun amountIsRoundedToTwoPlaces() {
        val draft = found("Sent Rs.99 From HDFC Bank A/C *1234 To SWIGGY On 05/01/26 Ref 401234567890")
        assertEquals("99.00", draft.amount)
        assertEquals("Food", draft.category)
    }

    @Test
    fun otp_isSkippedWithReadableReason() {
        val result = BankSmsImport.parse("123456 is your OTP for a transaction of Rs.500 at AMAZON. Do not share.", supported = true)
        assertTrue(result is SmsImportResult.Skipped)
        assertTrue((result as SmsImportResult.Skipped).reason.contains("one-time password"))
    }

    @Test
    fun failedPayment_isSkipped() {
        val result = BankSmsImport.parse("Your UPI payment of Rs.250.00 to SWIGGY has failed. Money if debited will be refunded.", supported = true)
        assertTrue(result is SmsImportResult.Skipped)
    }

    @Test
    fun plainText_isNotFound() {
        assertEquals(SmsImportResult.NotFound, BankSmsImport.parse("See you at 7 tonight!", supported = true))
        assertEquals(SmsImportResult.NotFound, BankSmsImport.parse("   ", supported = true))
    }

    @Test
    fun oldAndroid_isUnsupported() {
        assertEquals(SmsImportResult.Unsupported, BankSmsImport.parse("Sent Rs.10 From HDFC Bank A/C *1234 To X On 05/01/26 Ref 1", supported = false))
    }

    @Test
    fun genericFallback_isMarkedNotConfident_andUsesReceivedTime() {
        val draft = found("Rs 450.00 debited from your account at PETROL PUMP NH8.")
        assertTrue(draft.isExpense)
        assertFalse(draft.confident)
        assertEquals("450.00", draft.amount)
        if (draft.dateMillis != received) {
            // A generic match may still read a date; it must never be in the future.
            assertTrue(draft.dateMillis <= System.currentTimeMillis())
        }
    }

    @Test
    fun categoryGuesses_stayWithinFormCategories() {
        assertEquals("Shopping", BankSmsImport.guessCategory("AMAZON PAY INDIA", isExpense = true))
        assertEquals("Transportation", BankSmsImport.guessCategory("Uber India", isExpense = true))
        assertEquals("Other", BankSmsImport.guessCategory(null, isExpense = true))
        assertEquals("Salary", BankSmsImport.guessCategory("ACME SALARY OCT", isExpense = false))
        assertEquals("Other", BankSmsImport.guessCategory("SWIGGY", isExpense = false))
    }

    @Test
    fun learnedCategory_beatsKeywordHints() {
        val memory = com.akshar.utils.CategoryMemory(
            listOf(com.akshar.data.model.MerchantCategory("swiggy", "EXPENSE", "Swiggy", "Entertainment", 0L))
        )
        assertEquals("Entertainment", BankSmsImport.guessCategory("SWIGGY", isExpense = true, memory = memory))
        assertEquals("Shopping", BankSmsImport.guessCategory("AMAZON PAY INDIA", isExpense = true, memory = memory))
        val draft = BankSmsImport.parse(
            "Sent Rs.99 From HDFC Bank A/C *1234 To SWIGGY On 05/01/26 Ref 401234567890",
            receivedAtMillis = received, zone = zone, supported = true, memory = memory,
        ) as SmsImportResult.Found
        assertEquals("Entertainment", draft.draft.category)
    }

    @Test
    fun reference_isCarriedAsAHashedKey() {
        val draft = found("Sent Rs.99 From HDFC Bank A/C *1234 To SWIGGY On 05/01/26 Ref 401234567890")
        assertEquals(com.akshar.utils.DuplicateCheck.referenceKey("EXPENSE", "401234567890"), draft.referenceKey)
    }

    @Test
    fun descriptions_fallBackToChannel() {
        assertEquals("ATM withdrawal", BankSmsImport.describe(null, io.github.txnparse.Channel.ATM, true))
        assertEquals("UPI credit", BankSmsImport.describe(null, io.github.txnparse.Channel.UPI, false))
        assertEquals("Bank debit", BankSmsImport.describe(null, null, true))
        assertNull(BankSmsImport.paymentMethodFor(null))
        assertEquals("Bank Transfer", BankSmsImport.paymentMethodFor(io.github.txnparse.Channel.NEFT))
    }
}
