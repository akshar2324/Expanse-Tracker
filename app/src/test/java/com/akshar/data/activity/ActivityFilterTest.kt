package com.akshar.data.activity

import com.akshar.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ActivityFilterTest {

    private val zone = TimeZone.getTimeZone("Asia/Kolkata")

    private fun millis(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0, tz: TimeZone = zone): Long =
        Calendar.getInstance(tz).apply { clear(); set(y, m - 1, d, h, min) }.timeInMillis

    // --- Search pattern ---

    @Test
    fun `blank query has no pattern`() {
        assertNull(ActivityFilter.likePattern(""))
        assertNull(ActivityFilter.likePattern("   "))
    }

    @Test
    fun `query is trimmed and wrapped for a contains match`() {
        assertEquals("%coffee%", ActivityFilter.likePattern("  coffee "))
    }

    @Test
    fun `LIKE wildcards and the escape character match literally`() {
        assertEquals("%50\\% off%", ActivityFilter.likePattern("50% off"))
        assertEquals("%a\\_b%", ActivityFilter.likePattern("a_b"))
        assertEquals("%c:\\\\x%", ActivityFilter.likePattern("c:\\x"))
    }

    // --- Amount range ---

    @Test
    fun `empty amount fields mean no bounds and no error`() {
        assertEquals(AmountRange(null, null, null), AmountRange.parse("", "  "))
    }

    @Test
    fun `one-sided bounds are allowed`() {
        assertEquals(AmountRange(100.0, null, null), AmountRange.parse("100", ""))
        assertEquals(AmountRange(null, 250.5, null), AmountRange.parse("", "250.50"))
    }

    @Test
    fun `bounds use MoneyUtils parsing and rounding`() {
        assertEquals(AmountRange(1234.5, 2000.0, null), AmountRange.parse("1,234.50", "2,000", Locale.US))
        assertEquals(AmountRange(12.5, null, null), AmountRange.parse("12,50", "", Locale.GERMANY))
        assertEquals(AmountRange(10.01, null, null), AmountRange.parse("10.005", "", Locale.US))
    }

    @Test
    fun `zero is a valid bound`() {
        assertEquals(AmountRange(0.0, 0.0, null), AmountRange.parse("0", "0"))
    }

    @Test
    fun `equal min and max are allowed`() {
        assertEquals(AmountRange(50.0, 50.0, null), AmountRange.parse("50", "50.00"))
    }

    @Test
    fun `min above max is an error and drops both bounds`() {
        val range = AmountRange.parse("500", "100")
        assertNull(range.min)
        assertNull(range.max)
        assertEquals("Minimum is more than maximum", range.error)
    }

    @Test
    fun `unparseable or negative input is an error`() {
        assertEquals("Enter a valid minimum", AmountRange.parse("abc", "").error)
        assertEquals("Enter a valid maximum", AmountRange.parse("", "1..2").error)
        assertEquals("Amounts can't be negative", AmountRange.parse("-5", "").error)
        assertNull(AmountRange.parse("-5", "").min)
    }

    @Test
    fun `resolve carries validated amount bounds into the query`() {
        val ok = ActivityFilter(minAmountText = "10", maxAmountText = "20").resolve(0L, zone, Locale.US)
        assertEquals(10.0, ok.minAmount!!, 0.0)
        assertEquals(20.0, ok.maxAmount!!, 0.0)
        val bad = ActivityFilter(minAmountText = "30", maxAmountText = "20").resolve(0L, zone, Locale.US)
        assertNull(bad.minAmount)
        assertNull(bad.maxAmount)
    }

    // --- Dates ---

    private val now = millis(2026, 11, 6, 15, 30)

    @Test
    fun `all time is unbounded`() {
        assertEquals(null to null, ActivityFilter().dateBounds(now, zone))
    }

    @Test
    fun `this month runs from the first to the first of next month`() {
        val (from, until) = ActivityFilter(datePreset = DatePreset.THIS_MONTH).dateBounds(now, zone)
        assertEquals(millis(2026, 11, 1), from)
        assertEquals(millis(2026, 12, 1), until)
    }

    @Test
    fun `last month is the whole previous calendar month`() {
        val (from, until) = ActivityFilter(datePreset = DatePreset.LAST_MONTH).dateBounds(now, zone)
        assertEquals(millis(2026, 10, 1), from)
        assertEquals(millis(2026, 11, 1), until)
    }

    @Test
    fun `last month in January is December of the previous year`() {
        val (from, until) = ActivityFilter(datePreset = DatePreset.LAST_MONTH).dateBounds(millis(2027, 1, 15), zone)
        assertEquals(millis(2026, 12, 1), from)
        assertEquals(millis(2027, 1, 1), until)
    }

    @Test
    fun `last 30 days is today and the 29 days before it`() {
        val (from, until) = ActivityFilter(datePreset = DatePreset.LAST_30_DAYS).dateBounds(now, zone)
        assertEquals(millis(2026, 10, 8), from)
        assertEquals(millis(2026, 11, 7), until)
    }

    @Test
    fun `existing week and year options use whole calendar days`() {
        assertEquals(millis(2026, 10, 31) to millis(2026, 11, 7),
            ActivityFilter(datePreset = DatePreset.LAST_7_DAYS).dateBounds(now, zone))
        assertEquals(millis(2026, 1, 1) to millis(2027, 1, 1),
            ActivityFilter(datePreset = DatePreset.THIS_YEAR).dateBounds(now, zone))
    }

    @Test
    fun `custom day follows daylight saving transitions`() {
        val ny = TimeZone.getTimeZone("America/New_York")
        val (from, until) = ActivityFilter().withCustomRange(20260308, 20260308).dateBounds(now, ny)
        assertEquals(23 * 60 * 60 * 1000L, until!! - from!!)
    }

    @Test
    fun `custom range includes both end days`() {
        val filter = ActivityFilter().withCustomRange(20261001, 20261015)
        val (from, until) = filter.dateBounds(now, zone)
        assertEquals(millis(2026, 10, 1), from)
        assertEquals(millis(2026, 10, 16), until)
    }

    @Test
    fun `reversed custom range is swapped`() {
        val filter = ActivityFilter().withCustomRange(20261015, 20261001)
        assertEquals(20261001, filter.customStartDay)
        assertEquals(20261015, filter.customEndDay)
    }

    @Test
    fun `single-day custom range covers that whole day`() {
        val (from, until) = ActivityFilter().withCustomRange(20260301, 20260301).dateBounds(now, zone)
        assertEquals(millis(2026, 3, 1), from)
        assertEquals(millis(2026, 3, 2), until)
    }

    @Test
    fun `day key round trips through day start`() {
        val utc = TimeZone.getTimeZone("UTC")
        val pickerMillis = millis(2026, 2, 28, tz = utc)
        assertEquals(20260228, ActivityFilter.dayKey(pickerMillis, utc))
        assertEquals(pickerMillis, ActivityFilter.dayStart(20260228, utc))
        assertEquals(millis(2026, 3, 1, tz = utc), ActivityFilter.dayStart(20260228, utc, plusDays = 1))
    }

    @Test
    fun `saved calendar days stay Gregorian in non Gregorian locales`() {
        val previous = Locale.getDefault()
        val expectedMidnight = millis(2026, 11, 6)
        try {
            Locale.setDefault(Locale.forLanguageTag("th-TH"))
            assertEquals(20261106, ActivityFilter.dayKey(now, zone))
            assertEquals(expectedMidnight, ActivityFilter.dayStart(20261106, zone))
        } finally {
            Locale.setDefault(previous)
        }
    }

    // --- Flags ---

    @Test
    fun `sort alone is not filtering`() {
        assertFalse(ActivityFilter(sort = ActivitySort.HIGHEST).isFiltering)
        assertEquals(0, ActivityFilter(sort = ActivitySort.HIGHEST).activeCount)
    }

    @Test
    fun `each filter group counts once`() {
        val filter = ActivityFilter(
            query = "x", type = ActivityType.EXPENSE, categories = setOf("Food", "Fun"),
            minAmountText = "1", maxAmountText = "2", datePreset = DatePreset.THIS_MONTH
        )
        assertTrue(filter.isFiltering)
        assertEquals(4, filter.activeCount)
    }

    // --- Totals ---

    private fun tx(type: String, amount: Double, transferId: String? = null) = Transaction(
        amount = amount, type = type, category = "Food", description = "", date = 0L,
        paymentMethod = "Cash", transferId = transferId
    )

    @Test
    fun `totals of nothing are zero`() {
        assertEquals(ActivityTotals.EMPTY, ActivityTotals.of(emptyList()))
        assertEquals(BigDecimal("0.00"), ActivityTotals.EMPTY.spent)
    }

    @Test
    fun `totals split spending and income and ignore transfers`() {
        val totals = ActivityTotals.of(
            listOf(
                tx("EXPENSE", 120.0), tx("EXPENSE", 30.5), tx("INCOME", 1000.0),
                tx("EXPENSE", 500.0, transferId = "t1"), tx("INCOME", 500.0, transferId = "t1")
            )
        )
        assertEquals(5, totals.count)
        assertEquals(BigDecimal("150.50"), totals.spent)
        assertEquals(BigDecimal("1000.00"), totals.received)
    }

    @Test
    fun `totals do not drift when adding many decimals`() {
        val totals = ActivityTotals.of(List(10) { tx("EXPENSE", 0.1) } + tx("INCOME", 0.1) + tx("INCOME", 0.2))
        assertEquals(BigDecimal("1.00"), totals.spent)
        assertEquals(BigDecimal("0.30"), totals.received)
    }
}
