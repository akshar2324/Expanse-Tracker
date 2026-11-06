package com.akshar.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class PeriodUtilsTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private fun usCalendar() = Calendar.getInstance(utc, Locale.US)
    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        usCalendar().apply { clear(); set(year, month, day, hour, 0, 0) }.timeInMillis

    @Test
    fun `week key uses the week-based year across New Year`() {
        // In the US calendar, Sun 28 Dec 2025 .. Sat 3 Jan 2026 is week 1 of 2026.
        val dec29 = at(2025, Calendar.DECEMBER, 29)
        val jan2 = at(2026, Calendar.JANUARY, 2)
        assertEquals("2026-W1", PeriodUtils.weekKey(dec29, usCalendar()))
        assertEquals(PeriodUtils.weekKey(dec29, usCalendar()), PeriodUtils.weekKey(jan2, usCalendar()))
        assertTrue(PeriodUtils.isInWeek(dec29, "2026-W1", usCalendar()))
    }

    @Test
    fun `month key uses ASCII digits regardless of default locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))
            val ts = System.currentTimeMillis()
            assertTrue(PeriodUtils.monthKey(ts).matches(Regex("\\d{4}-\\d{2}")))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `month boundaries`() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(utc)
            val lastMoment = usCalendar().apply { clear(); set(2026, Calendar.JANUARY, 31, 23, 59, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
            assertTrue(PeriodUtils.isInMonth(lastMoment, "2026-01"))
            assertFalse(PeriodUtils.isInMonth(lastMoment + 1, "2026-01"))
            assertTrue(PeriodUtils.isInMonth(lastMoment + 1, "2026-02"))
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test
    fun `monthly occurrences keep the day of month and clamp short months`() {
        val jan31 = at(2026, Calendar.JANUARY, 31)
        val feb = usCalendar().apply { timeInMillis = PeriodUtils.addPeriods(jan31, "MONTHLY", 1, usCalendar()) }
        assertEquals(Calendar.FEBRUARY, feb.get(Calendar.MONTH))
        assertEquals(28, feb.get(Calendar.DAY_OF_MONTH))
        val mar = usCalendar().apply { timeInMillis = PeriodUtils.addPeriods(jan31, "MONTHLY", 2, usCalendar()) }
        assertEquals(31, mar.get(Calendar.DAY_OF_MONTH))

        val jan15 = at(2026, Calendar.JANUARY, 15)
        val dec = usCalendar().apply { timeInMillis = PeriodUtils.addPeriods(jan15, "MONTHLY", 11, usCalendar()) }
        assertEquals(15, dec.get(Calendar.DAY_OF_MONTH)) // 30-day steps would have drifted to Dec 10
    }

    @Test
    fun `daily weekly and yearly steps`() {
        val start = at(2028, Calendar.FEBRUARY, 29)
        assertEquals(start + 86_400_000L, PeriodUtils.addPeriods(start, "DAILY", 1, usCalendar()))
        assertEquals(start + 7 * 86_400_000L, PeriodUtils.addPeriods(start, "weekly", 1, usCalendar()))
        val nextYear = usCalendar().apply { timeInMillis = PeriodUtils.addPeriods(start, "YEARLY", 1, usCalendar()) }
        assertEquals(2029, nextYear.get(Calendar.YEAR))
        assertEquals(28, nextYear.get(Calendar.DAY_OF_MONTH))
    }
}
