package com.akshar.insights

import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.TimeZone

/**
 * How often a charge repeats. [minDays]..[maxDays] is the accepted gap between two charges,
 * wide enough for a 31st that falls on the 28th in February, or a weekly charge a day late.
 */
enum class SubscriptionFrequency(val reminderValue: String, val label: String, val minDays: Int, val maxDays: Int, val nominalDays: Int) {
    WEEKLY("WEEKLY", "Weekly", 6, 8, 7),
    MONTHLY("MONTHLY", "Monthly", 26, 35, 30),
    YEARLY("YEARLY", "Yearly", 350, 380, 365);

    companion object {
        fun forGap(days: Long): SubscriptionFrequency? = entries.firstOrNull { days in it.minDays..it.maxDays }
    }
}

data class SubscriptionCandidate(
    /** Normalised merchant; also the key remembered when the user says "Not a subscription". */
    val key: String,
    val displayName: String,
    val category: String,
    val paymentMethod: String,
    /** The latest charge, rounded to 2 decimals. */
    val amount: BigDecimal,
    val frequency: SubscriptionFrequency,
    val occurrences: Int,
    val lastChargeMs: Long,
    /** First expected charge after now. */
    val nextExpectedMs: Long,
    /**
     * Last-triggered time for a reminder made from this candidate, one period before
     * [nextExpectedMs], so the reminder's first due date is the next expected charge and the app
     * does not post a charge that may already be in the ledger.
     */
    val reminderAnchorMs: Long,
)

/**
 * Finds payments that repeat on a schedule, entirely on the device.
 *
 * 1. Expenses (not transfers, not entries the app generated from reminders) are grouped by
 *    [MerchantKey.normalize]d description. Blank keys are skipped.
 * 2. Charges on the same local day are merged into one occurrence (the later one is kept).
 * 3. Amounts must be similar: within [amountTolerance] (default 15%) of the group's median, or
 *    1.00 for very small amounts. Charges outside that band are ignored, so a one-off purchase at
 *    the same shop does not count.
 * 4. At least [minOccurrences] similar charges are needed, and every gap between consecutive
 *    charges must fall inside the same [SubscriptionFrequency] band. One irregular gap and the
 *    group is not treated as a subscription.
 * 5. It must still be running: the last charge is no older than 1.5 periods.
 * 6. Anything matching an existing reminder ([MerchantKey.sameMerchant]) or a dismissed key is
 *    left out.
 *
 * Results are ordered by next expected date, then larger amount first, then key, so ties are
 * stable.
 */
class SubscriptionFinder(
    private val timeZone: TimeZone = TimeZone.getDefault(),
    private val minOccurrences: Int = 3,
    private val amountTolerance: BigDecimal = BigDecimal("0.15"),
) {
    init {
        require(minOccurrences >= 3)
        require(amountTolerance >= BigDecimal.ZERO && amountTolerance < BigDecimal.ONE)
    }

    fun find(
        nowMs: Long,
        transactions: List<Transaction>,
        reminders: List<RecurringTransaction>,
        dismissedKeys: Set<String> = emptySet(),
    ): List<SubscriptionCandidate> {
        val reminderKeys = reminders.map { MerchantKey.normalize(it.description) }.filter { it.isNotEmpty() }
        val todayIndex = InsightMath.dayIndex(nowMs, timeZone)

        return transactions.asSequence()
            .filter {
                it.transferId == null && it.type.equals("EXPENSE", ignoreCase = true) &&
                    it.amount.isFinite() && it.amount > 0.0 && it.date <= nowMs &&
                    !MerchantKey.isReminderGenerated(it.description)
            }
            .groupBy { MerchantKey.normalize(it.description) }
            .filterKeys { key -> key.isNotEmpty() && key !in dismissedKeys && reminderKeys.none { MerchantKey.sameMerchant(it, key) } }
            .mapNotNull { (key, charges) -> detect(key, charges, nowMs, todayIndex) }
            .sortedWith(
                compareBy<SubscriptionCandidate> { it.nextExpectedMs }
                    .thenByDescending { it.amount }
                    .thenBy { it.key }
            )
    }

    private fun detect(key: String, charges: List<Transaction>, nowMs: Long, todayIndex: Long): SubscriptionCandidate? {
        if (charges.size < minOccurrences) return null

        // One occurrence per day; ties within a day keep the later entry (then the higher id).
        val perDay = charges
            .sortedWith(compareBy<Transaction> { it.date }.thenBy { it.id })
            .associateBy { InsightMath.dayIndex(it.date, timeZone) }
            .toSortedMap()
            .values.toList()
        if (perDay.size < minOccurrences) return null

        val median = InsightMath.quantile(perDay.map { InsightMath.money(it.amount) }.sorted(), 0.5)
        val tolerance = median.multiply(amountTolerance).max(BigDecimal.ONE)
        val similar = perDay.filter { InsightMath.money(it.amount).subtract(median).abs() <= tolerance }
        if (similar.size < minOccurrences) return null

        val days = similar.map { InsightMath.dayIndex(it.date, timeZone) }
        val gaps = days.zipWithNext { a, b -> b - a }
        val frequency = SubscriptionFrequency.forGap(gaps.first()) ?: return null
        if (gaps.any { it !in frequency.minDays..frequency.maxDays }) return null

        val last = similar.last()
        val lastIndex = days.last()
        if ((todayIndex - lastIndex) * 2 > frequency.nominalDays * 3) return null

        var anchor = last.date
        var next = InsightMath.addPeriods(anchor, frequency.reminderValue, 1, timeZone)
        var guard = 0
        while (next <= nowMs && guard++ < 1000) {
            anchor = next
            next = InsightMath.addPeriods(anchor, frequency.reminderValue, 1, timeZone)
        }

        val category = similar.groupingBy { it.category }.eachCount().let { counts ->
            val top = counts.values.max()
            // Most common category; on a tie, the one used most recently.
            similar.last { counts[it.category] == top }.category
        }

        return SubscriptionCandidate(
            key = key,
            displayName = MerchantKey.displayName(last.description).ifBlank { key },
            category = category,
            paymentMethod = last.paymentMethod,
            amount = InsightMath.money(last.amount).setScale(2, RoundingMode.HALF_UP),
            frequency = frequency,
            occurrences = similar.size,
            lastChargeMs = last.date,
            nextExpectedMs = next,
            reminderAnchorMs = anchor,
        )
    }
}
