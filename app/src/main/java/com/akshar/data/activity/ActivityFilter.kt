package com.akshar.data.activity

import com.akshar.data.model.Transaction
import com.akshar.utils.MoneyUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** Which kind of entry the Activity list shows. Transfer legs never count as expense or income. */
enum class ActivityType { ALL, EXPENSE, INCOME, TRANSFER }

/** Date presets are relative, so a saved "This month" filter keeps meaning the current month. */
enum class DatePreset { ALL_TIME, THIS_MONTH, LAST_MONTH, LAST_30_DAYS, LAST_7_DAYS, THIS_YEAR, CUSTOM }

enum class ActivitySort { NEWEST, OLDEST, HIGHEST, LOWEST }

/**
 * Everything the user has picked on the Activity tab. Amount bounds are kept as the text the user
 * typed so a half-finished entry survives rotation; [resolve] turns the filter into a query.
 *
 * [customStartDay] and [customEndDay] are calendar days encoded as yyyyMMdd (for example
 * 20261106), both inclusive, so a custom range means the same days in any time zone.
 */
data class ActivityFilter(
    val query: String = "",
    val type: ActivityType = ActivityType.ALL,
    val categories: Set<String> = emptySet(),
    val accountId: Long? = null,
    val paymentMethod: String? = null,
    val datePreset: DatePreset = DatePreset.ALL_TIME,
    val customStartDay: Int? = null,
    val customEndDay: Int? = null,
    val minAmountText: String = "",
    val maxAmountText: String = "",
    val sort: ActivitySort = ActivitySort.NEWEST,
) {
    /** True when anything narrows the list. Sort order alone does not hide entries. */
    val isFiltering: Boolean
        get() = query.isNotBlank() || type != ActivityType.ALL || categories.isNotEmpty() ||
            accountId != null || paymentMethod != null || datePreset != DatePreset.ALL_TIME ||
            minAmountText.isNotBlank() || maxAmountText.isNotBlank()

    /** Number of filter groups in use, for the filter button's badge. */
    val activeCount: Int
        get() = listOf(
            type != ActivityType.ALL, categories.isNotEmpty(), accountId != null, paymentMethod != null,
            datePreset != DatePreset.ALL_TIME, minAmountText.isNotBlank() || maxAmountText.isNotBlank()
        ).count { it }

    fun withCustomRange(startDay: Int, endDay: Int): ActivityFilter {
        val (start, end) = if (startDay <= endDay) startDay to endDay else endDay to startDay
        return copy(datePreset = DatePreset.CUSTOM, customStartDay = start, customEndDay = end)
    }

    fun resolve(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault(),
    ): TransactionQuery {
        val amounts = AmountRange.parse(minAmountText, maxAmountText, locale)
        val (from, until) = dateBounds(nowMillis, timeZone)
        return TransactionQuery(
            searchPattern = likePattern(query),
            type = type,
            categories = categories.sorted(),
            accountId = accountId,
            paymentMethod = paymentMethod,
            fromMillis = from,
            untilMillis = until,
            minAmount = amounts.min,
            maxAmount = amounts.max,
            sort = sort,
        )
    }

    /** [from, until) in epoch millis for the selected preset; nulls mean open-ended. */
    fun dateBounds(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Pair<Long?, Long?> {
        val cal = Calendar.getInstance(timeZone, Locale.ROOT).apply { timeInMillis = nowMillis }
        fun startOfDay(c: Calendar) = c.apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return when (datePreset) {
            DatePreset.ALL_TIME -> null to null
            DatePreset.THIS_MONTH -> {
                startOfDay(cal).set(Calendar.DAY_OF_MONTH, 1)
                val from = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                from to cal.timeInMillis
            }
            DatePreset.LAST_MONTH -> {
                startOfDay(cal).set(Calendar.DAY_OF_MONTH, 1)
                val until = cal.timeInMillis
                cal.add(Calendar.MONTH, -1)
                cal.timeInMillis to until
            }
            DatePreset.THIS_YEAR -> {
                startOfDay(cal).set(Calendar.DAY_OF_YEAR, 1)
                val from = cal.timeInMillis
                cal.add(Calendar.YEAR, 1)
                from to cal.timeInMillis
            }
            DatePreset.LAST_30_DAYS, DatePreset.LAST_7_DAYS -> {
                // Whole calendar days, including today, ending at midnight tonight.
                startOfDay(cal).add(Calendar.DAY_OF_YEAR, 1)
                val until = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, if (datePreset == DatePreset.LAST_7_DAYS) -7 else -30)
                cal.timeInMillis to until
            }
            DatePreset.CUSTOM -> {
                val start = customStartDay ?: return null to null
                val end = customEndDay ?: start
                dayStart(start, timeZone) to dayStart(end, timeZone, plusDays = 1)
            }
        }
    }

    companion object {
        /** yyyyMMdd for the calendar day that contains [millis] in [timeZone]. */
        fun dayKey(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): Int {
            val c = Calendar.getInstance(timeZone, Locale.ROOT).apply { timeInMillis = millis }
            return c.get(Calendar.YEAR) * 10_000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
        }

        /** Local midnight at the start of [day] (yyyyMMdd), optionally shifted by whole days. */
        fun dayStart(day: Int, timeZone: TimeZone = TimeZone.getDefault(), plusDays: Int = 0): Long =
            Calendar.getInstance(timeZone, Locale.ROOT).apply {
                clear()
                set(day / 10_000, (day / 100) % 100 - 1, day % 100)
                add(Calendar.DAY_OF_YEAR, plusDays)
            }.timeInMillis

        /**
         * A LIKE pattern for [query], or null when it is blank. `%`, `_` and the escape character
         * are escaped so they match literally (use with `ESCAPE '\'`).
         */
        fun likePattern(query: String): String? {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return null
            val escaped = buildString {
                trimmed.forEach { ch ->
                    if (ch == '\\' || ch == '%' || ch == '_') append('\\')
                    append(ch)
                }
            }
            return "%$escaped%"
        }
    }
}

/**
 * Validated amount bounds. Blank fields mean "no bound". A field that does not parse, a negative
 * value, or a minimum above the maximum produces an [error] and drops both bounds, so a typo never
 * silently hides entries.
 */
data class AmountRange(val min: Double?, val max: Double?, val error: String?) {
    companion object {
        fun parse(minText: String, maxText: String, locale: Locale = Locale.getDefault()): AmountRange {
            val min = parseBound(minText, locale)
            val max = parseBound(maxText, locale)
            val error = when {
                min is Bound.Invalid -> "Enter a valid minimum"
                max is Bound.Invalid -> "Enter a valid maximum"
                min is Bound.Negative || max is Bound.Negative -> "Amounts can't be negative"
                min is Bound.Value && max is Bound.Value && min.amount > max.amount -> "Minimum is more than maximum"
                else -> null
            }
            if (error != null) return AmountRange(null, null, error)
            return AmountRange((min as? Bound.Value)?.amount, (max as? Bound.Value)?.amount, null)
        }

        private sealed interface Bound {
            data object Empty : Bound
            data object Invalid : Bound
            data object Negative : Bound
            data class Value(val amount: Double) : Bound
        }

        private fun parseBound(text: String, locale: Locale): Bound {
            if (text.isBlank()) return Bound.Empty
            val value = MoneyUtils.parseAmount(text, locale) ?: return Bound.Invalid
            return if (value < 0) Bound.Negative else Bound.Value(value)
        }
    }
}

/** Concrete criteria for [com.akshar.data.db.FinanceDao.searchTransactions]. */
data class TransactionQuery(
    val searchPattern: String? = null,
    val type: ActivityType = ActivityType.ALL,
    val categories: List<String> = emptyList(),
    val accountId: Long? = null,
    val paymentMethod: String? = null,
    val fromMillis: Long? = null,
    val untilMillis: Long? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val sort: ActivitySort = ActivitySort.NEWEST,
)

/**
 * Totals for a filtered list. Transfers move money between the user's own accounts, so they are
 * counted in [count] but are neither spending nor income. Sums use BigDecimal and are rounded to
 * two places HALF_UP, so adding many Doubles never drifts (0.1 + 0.2 is 0.30, not 0.30000000000000004).
 */
data class ActivityTotals(val count: Int, val spent: BigDecimal, val received: BigDecimal) {
    companion object {
        val EMPTY = ActivityTotals(0, BigDecimal.ZERO.setScale(MoneyUtils.MONEY_SCALE), BigDecimal.ZERO.setScale(MoneyUtils.MONEY_SCALE))

        fun of(transactions: List<Transaction>): ActivityTotals {
            var spent = BigDecimal.ZERO
            var received = BigDecimal.ZERO
            transactions.forEach { tx ->
                if (tx.transferId != null) return@forEach
                val amount = BigDecimal(tx.amount.toString())
                when (tx.type) {
                    "EXPENSE" -> spent += amount
                    "INCOME" -> received += amount
                }
            }
            return ActivityTotals(
                count = transactions.size,
                spent = spent.setScale(MoneyUtils.MONEY_SCALE, RoundingMode.HALF_UP),
                received = received.setScale(MoneyUtils.MONEY_SCALE, RoundingMode.HALF_UP),
            )
        }
    }
}

/** A filter the user saved under a name, shown as a chip on the Activity tab. */
data class SavedActivityFilter(val name: String, val filter: ActivityFilter) {
    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
