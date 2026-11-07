package com.akshar.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvParserTest {

    private fun parse(csv: String, locale: String = "en_US", invert: Boolean = false, delimiter: Char = ',') =
        CsvParser.parse(
            inputStream = csv.byteInputStream(),
            delimiter = delimiter,
            hasHeader = true,
            dateColumnName = "Date",
            dateFormatString = "yyyy-MM-dd",
            amountColumnName = "Amount",
            debitColumnName = null,
            creditColumnName = null,
            descriptionColumnName = "Description",
            categoryColumnName = "Category",
            localeString = locale,
            invertAmountSigns = invert
        )

    @Test
    fun `signed amounts map to expense and income`() {
        val result = parse("Date,Amount,Description,Category\n2026-01-05,-12.50,Coffee,Food\n2026-01-06,1000,Pay,Salary\n")
        assertEquals(0, result.errors.size)
        assertEquals("EXPENSE", result.rows[0].type)
        assertEquals(12.5, result.rows[0].amount, 0.0)
        assertEquals("INCOME", result.rows[1].type)
        assertEquals(1000.0, result.rows[1].amount, 0.0)
    }

    @Test
    fun `comma decimal amount in a dot-decimal profile is an error, not a 100x value`() {
        val result = parse("Date;Amount;Description;Category\n2026-01-05;-12,50;Coffee;Food\n", delimiter = ';')
        assertEquals(0, result.rows.size)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `german profile parses grouped comma decimals`() {
        val result = parse("Date;Amount;Description;Category\n2026-01-05;-1.234,56;Rent;Rent\n", locale = "de_DE", delimiter = ';')
        assertEquals(0, result.errors.size)
        assertEquals(1234.56, result.rows.single().amount, 0.0)
    }

    @Test
    fun `language-only locale is honoured`() {
        val result = parse("Date;Amount;Description;Category\n2026-01-05;-12,50;Coffee;Food\n", locale = "de", delimiter = ';')
        assertEquals(12.5, result.rows.single().amount, 0.0)
    }

    @Test
    fun `zero amount and bad dates are reported as errors`() {
        val result = parse("Date,Amount,Description,Category\n2026-01-05,0,Nothing,Food\n05/01/2026,-3,Bad date,Food\n")
        assertEquals(0, result.rows.size)
        assertEquals(2, result.errors.size)
    }

    @Test
    fun `amounts are rounded to cents`() {
        val result = parse("Date,Amount,Description,Category\n2026-01-05,-3.456,Snack,Food\n")
        assertEquals(3.46, result.rows.single().amount, 0.0)
    }

    @Test
    fun `Indian grouped amounts are accepted`() {
        val result = parse("Date;Amount;Description;Category\n2026-01-05;-1,23,456.78;Rent;Rent\n", locale = "en_IN", delimiter = ';')
        assertEquals(0, result.errors.size)
        assertEquals(123456.78, result.rows.single().amount, 0.0)
    }

    @Test
    fun `date with trailing garbage is rejected`() {
        val result = parse("Date,Amount,Description,Category\n2026-01-05garbage,-3,Snack,Food\n")
        assertEquals(0, result.rows.size)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `debit credit columns reject negative and conflicting values`() {
        val result = CsvParser.parse(
            "Date,Debit,Credit,Description\n2026-01-05,-12,,Refund\n2026-01-06,10,20,Ambiguous\n".byteInputStream(),
            dateColumnName = "Date", dateFormatString = "yyyy-MM-dd",
            amountColumnName = null, debitColumnName = "Debit", creditColumnName = "Credit",
            descriptionColumnName = "Description", categoryColumnName = null,
            localeString = "en_US", invertAmountSigns = false
        )
        assertEquals(0, result.rows.size)
        assertEquals(2, result.errors.size)
    }

    @Test
    fun `row hash is stable for identical rows and differs otherwise`() {
        val a = parse("Date,Amount,Description,Category\n2026-01-05,-3,Snack,Food\n").rows.single().hash
        val b = parse("Date,Amount,Description,Category\n2026-01-05,-3,Snack,Food\n").rows.single().hash
        val c = parse("Date,Amount,Description,Category\n2026-01-05,-4,Snack,Food\n").rows.single().hash
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertTrue(a.matches(Regex("[0-9a-f]{64}")))
    }
}
