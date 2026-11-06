package com.akshar.insights

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Shared helpers for the on-device insights: money conversion, local days and description keys. */
internal object InsightMath {
    const val DAY_MS = 24L * 60 * 60 * 1000

    /** Money is stored as Double; convert through its decimal string so 0.1 stays 0.1. */
    fun money(value: Double): BigDecimal =
        if (value.isFinite()) BigDecimal(value.toString()) else BigDecimal.ZERO

    fun roundMoney(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)

    /** Days since the epoch in [timeZone], so "today" follows the device's wall clock. */
    fun dayIndex(ms: Long, timeZone: TimeZone): Long =
        Math.floorDiv(ms + timeZone.getOffset(ms), DAY_MS)

    fun calendar(ms: Long, timeZone: TimeZone): Calendar =
        Calendar.getInstance(timeZone, Locale.US).apply { timeInMillis = ms }

    /** Midnight at the start of the day containing [ms]. */
    fun startOfDay(ms: Long, timeZone: TimeZone): Long = calendar(ms, timeZone).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Midnight at the start of the last day of the month containing [ms] (handles leap Februaries). */
    fun startOfLastDayOfMonth(ms: Long, timeZone: TimeZone): Long = calendar(startOfDay(ms, timeZone), timeZone).apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    }.timeInMillis

    /** The last millisecond of the month containing [ms]. */
    fun endOfMonth(ms: Long, timeZone: TimeZone): Long = calendar(startOfDay(ms, timeZone), timeZone).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
    }.timeInMillis - 1

    /** Calendar step used by reminders: same day of month for MONTHLY, clamped to short months. */
    fun addPeriods(anchor: Long, frequency: String, steps: Int, timeZone: TimeZone): Long {
        val cal = calendar(anchor, timeZone)
        when (frequency.uppercase(Locale.ROOT)) {
            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, steps)
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, steps)
            "YEARLY" -> cal.add(Calendar.YEAR, steps)
            else -> cal.add(Calendar.MONTH, steps)
        }
        return cal.timeInMillis
    }

    /**
     * Quantile with linear interpolation between closest ranks (the "R-7" definition used by
     * most spreadsheets). [sorted] must be ascending and non-empty.
     */
    fun quantile(sorted: List<BigDecimal>, q: Double): BigDecimal {
        require(sorted.isNotEmpty())
        if (sorted.size == 1) return sorted[0]
        val position = BigDecimal(q.toString()).multiply(BigDecimal(sorted.size - 1))
        val lower = position.setScale(0, RoundingMode.FLOOR).toInt()
        val upper = minOf(lower + 1, sorted.size - 1)
        val fraction = position.subtract(BigDecimal(lower))
        return sorted[lower].add(sorted[upper].subtract(sorted[lower]).multiply(fraction))
    }
}

/** Matches descriptions the way a person would: "NETFLIX 8812", "Netflix." and "netflix" are one merchant. */
object MerchantKey {
    /** Suffixes the app itself adds to ledger entries created from reminders. */
    private val generatedSuffixes = listOf("(auto-recurring)", "(paid bill)")
    private val noiseTokens = setOf("com", "www", "net", "org", "co", "pvt", "ltd", "inc", "llc")

    /** True when the app created this entry from a reminder, so it is already a known bill. */
    fun isReminderGenerated(description: String): Boolean {
        val lower = description.trim().lowercase(Locale.ROOT)
        return generatedSuffixes.any { lower.endsWith(it) }
    }

    /** Strips the app's own suffixes, keeping the user's wording and case. */
    fun displayName(description: String): String {
        var text = description.trim()
        generatedSuffixes.forEach { suffix ->
            if (text.lowercase(Locale.ROOT).endsWith(suffix)) text = text.dropLast(suffix.length).trim()
        }
        return text
    }

    /**
     * Lower-cases, drops the app's suffixes, digits, punctuation and filler like ".com", and
     * collapses spaces. Returns "" when nothing meaningful is left.
     */
    fun normalize(description: String): String =
        displayName(description)
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}]+"), " ")
            .split(' ')
            .filter { it.isNotBlank() && it !in noiseTokens }
            .joinToString(" ")

    /** Same merchant when the keys match, or one contains the other and is at least 4 letters. */
    fun sameMerchant(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        val (short, long) = if (a.length <= b.length) a to b else b to a
        return short.length >= 4 && (" $long ").contains(" $short ")
    }
}

/** Display rounding for "About ₹12,400" style headlines. */
object InsightFormat {
    /**
     * Rounds to a friendly figure: nearest 100 from 1,000 up, nearest 10 from 100 up, otherwise
     * nearest 1. HALF_UP on the magnitude, so -1,250 becomes -1,300 just as 1,250 becomes 1,300.
     */
    fun approximate(value: BigDecimal): BigDecimal {
        val magnitude = value.abs()
        val scale = when {
            magnitude >= BigDecimal(1000) -> -2
            magnitude >= BigDecimal(100) -> -1
            else -> 0
        }
        return value.setScale(scale, RoundingMode.HALF_UP).setScale(0)
    }

    /** "₹12,400" for an already rounded whole amount; the sign is left to the caller's wording. */
    fun wholeMoney(value: BigDecimal, currencySymbol: String, locale: Locale = Locale.getDefault()): String =
        currencySymbol + String.format(locale, "%,d", value.abs().setScale(0, RoundingMode.HALF_UP).toBigInteger())
}
