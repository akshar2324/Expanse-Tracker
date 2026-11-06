package com.akshar.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Period keys for budgets and date math for recurring entries.
 *
 * Keys are always written with ASCII digits (Locale.US) so a budget created under one device
 * language still matches after the language changes. Weekly keys use the week-based year, so
 * the days of a week that straddles New Year share one key.
 */
object PeriodUtils {

    /** "yyyy-MM" for the month that contains [timestamp]. */
    fun monthKey(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(timestamp))

    /** "yyyy-Wn" (week number not zero-padded, matching keys already stored) for [timestamp]. */
    fun weekKey(timestamp: Long, calendar: Calendar = Calendar.getInstance()): String {
        val cal = calendar.clone() as Calendar
        cal.timeInMillis = timestamp
        return "${cal.weekYear}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
    }

    fun isInMonth(timestamp: Long, monthKey: String): Boolean = monthKey(timestamp) == monthKey

    fun isInWeek(timestamp: Long, weekKey: String, calendar: Calendar = Calendar.getInstance()): Boolean =
        weekKey(timestamp, calendar) == weekKey

    /**
     * Returns the occurrence [steps] periods after [anchor], using calendar arithmetic so monthly
     * and yearly entries land on the same day of month (clamped to the month's last day).
     */
    fun addPeriods(anchor: Long, frequency: String, steps: Int, calendar: Calendar = Calendar.getInstance()): Long {
        val cal = calendar.clone() as Calendar
        cal.timeInMillis = anchor
        when (frequency.uppercase()) {
            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, steps)
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, steps)
            "YEARLY" -> cal.add(Calendar.YEAR, steps)
            else -> cal.add(Calendar.MONTH, steps) // MONTHLY and unknown values
        }
        return cal.timeInMillis
    }
}
