package com.akshar

import com.akshar.data.backup.BackupCodec
import com.akshar.utils.MoneyUtils
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.util.Locale

/** Safety expectations from the audit. F23 (CSV hash input) is still open. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditOpenFindingsTest {
    @Test(expected = JSONException::class)
    fun negativeTransactionBackupIsRejected() {
        BackupCodec.decode("""{"transactions":[{"amount":-5,"type":"EXPENSE","category":"Food","date":1}]}""")
    }

    @Test(expected = JSONException::class)
    fun danglingAccountBackupIsRejected() {
        BackupCodec.decode("""{"transactions":[{"amount":5,"type":"EXPENSE","category":"Food","date":1,"accountId":99}]}""")
    }

    @org.junit.Ignore("Open audit finding F23: changing the hash input needs a migration for stored hashes; see AUDIT_REPORT.md")
    @Test fun csvHashMustDistinguishDifferentFieldBoundaries() {
        fun hash(description: String, category: String) = com.akshar.utils.CsvParser.parse(
            "Date;Amount;Description;Category\n2026-01-05;-3;$description;$category\n".byteInputStream(),
            delimiter = ';', dateColumnName = "Date", dateFormatString = "yyyy-MM-dd",
            amountColumnName = "Amount", debitColumnName = null, creditColumnName = null,
            descriptionColumnName = "Description", categoryColumnName = "Category",
            localeString = "en_US", invertAmountSigns = false
        ).rows.single().hash
        assertNotEquals(hash("A,B", "C"), hash("A", "B,C"))
    }

    @Test fun veryLargeAmountsKeepEveryCent() {
        // F18: amounts are capped where Double still holds every cent; larger input is refused.
        assertNull(MoneyUtils.parseAmount("100000000000000.01", Locale.US))
        val parsed = MoneyUtils.parseAmount("99999999999.99", Locale.US)!!
        assertEquals(BigDecimal("99999999999.99"), BigDecimal.valueOf(parsed).setScale(2))
        val cents = MoneyUtils.parseAmount("12345678901.23", Locale.US)!!
        assertEquals(BigDecimal("12345678901.23"), BigDecimal.valueOf(cents).setScale(2))
    }

}
