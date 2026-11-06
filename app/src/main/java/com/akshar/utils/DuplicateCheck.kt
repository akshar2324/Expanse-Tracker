package com.akshar.utils

import com.akshar.data.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Calendar
import java.util.TimeZone

/** An imported entry (bank SMS or CSV row) about to be saved. */
data class DuplicateCandidate(
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val date: Long,
    /** [DuplicateCheck.referenceKey] when the bank gave a reference number, else null. */
    val referenceKey: String? = null,
)

data class DuplicateMatch(
    val existing: Transaction,
    /** True when the bank reference matched, so this is almost certainly the same payment. */
    val sameReference: Boolean,
)

/**
 * Finds a transaction that an imported entry probably repeats. Only imports are checked; entries
 * typed by hand are never flagged, because two equal coffees on one day are normal.
 */
object DuplicateCheck {

    private const val REFERENCE_PREFIX = "ref:"

    /**
     * A stable key for a bank reference number, stored in [Transaction.originalCsvRowHash]. The
     * reference is hashed so the raw number isn't kept, and the type is part of the key because a
     * transfer between two of your own banks carries the same reference on the debit and the credit.
     * Returns null for a blank reference.
     */
    fun referenceKey(type: String, reference: String?): String? {
        val ref = reference?.filter { it.isLetterOrDigit() }?.uppercase()
        if (ref.isNullOrEmpty()) return null
        val bytes = MessageDigest.getInstance("SHA-256").digest("$type|$ref".toByteArray(Charsets.UTF_8))
        return REFERENCE_PREFIX + bytes.joinToString("") { "%02x".format(it) }
    }

    /** Start (inclusive) and end (exclusive) of the calendar day holding [millis] in [zone]. */
    fun dayBounds(millis: Long, zone: TimeZone = TimeZone.getDefault()): Pair<Long, Long> {
        val cal = Calendar.getInstance(zone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1) // calendar arithmetic, so 23- and 25-hour days work
        return start to cal.timeInMillis
    }

    /** Whole cents, so 99.999 rounding errors can never make equal amounts look different. */
    internal fun cents(amount: Double): Long =
        BigDecimal(amount.toString()).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()

    /**
     * The best match for [candidate] in [existing]: a transaction with the same bank reference
     * first, otherwise one with the same amount, type and calendar day. Transfer legs are ignored.
     */
    fun findMatch(
        candidate: DuplicateCandidate,
        existing: List<Transaction>,
        zone: TimeZone = TimeZone.getDefault(),
    ): DuplicateMatch? {
        val ledger = existing.filter { it.transferId == null }
        candidate.referenceKey?.let { key ->
            ledger.firstOrNull { it.originalCsvRowHash == key }?.let { return DuplicateMatch(it, sameReference = true) }
        }
        val (start, end) = dayBounds(candidate.date, zone)
        val cents = cents(candidate.amount)
        return ledger.firstOrNull {
            it.type == candidate.type && it.date >= start && it.date < end && cents(it.amount) == cents
        }?.let { DuplicateMatch(it, sameReference = false) }
    }
}
