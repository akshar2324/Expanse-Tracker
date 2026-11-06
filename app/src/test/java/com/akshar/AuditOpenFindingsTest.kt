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

/** Intended safety expectations requiring broader validation, money, or hash-migration work. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@org.junit.Ignore("Open audit findings: restore validation, exact money, and CSV hash migration require broader changes; see AUDIT_REPORT.md")
class AuditOpenFindingsTest {
    @Test(expected = JSONException::class)
    fun negativeTransactionBackupIsRejected() {
        BackupCodec.decode("""{"transactions":[{"amount":-5,"type":"EXPENSE","category":"Food","date":1}]}""")
    }

    @Test(expected = JSONException::class)
    fun danglingAccountBackupIsRejected() {
        BackupCodec.decode("""{"transactions":[{"amount":5,"type":"EXPENSE","category":"Food","date":1,"accountId":99}]}""")
    }

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
        val parsed = MoneyUtils.parseAmount("100000000000000.01", Locale.US)!!
        assertEquals(BigDecimal("100000000000000.01"), BigDecimal.valueOf(parsed).setScale(2))
    }

}
