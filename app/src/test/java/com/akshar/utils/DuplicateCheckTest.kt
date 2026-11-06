package com.akshar.utils

import com.akshar.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DuplicateCheckTest {

    private val zone = TimeZone.getTimeZone("Asia/Kolkata")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private fun tx(
        amount: Double,
        date: Long,
        type: String = "EXPENSE",
        hash: String? = null,
        transferId: String? = null,
        id: Long = 1,
    ) = Transaction(
        id = id, amount = amount, type = type, category = "Food", description = "Swiggy", date = date,
        paymentMethod = "UPI", originalCsvRowHash = hash, transferId = transferId
    )

    @Test
    fun referenceKey_ignoresFormattingButNotType() {
        val key = DuplicateCheck.referenceKey("EXPENSE", "6291-0000 4321")
        assertNotNull(key)
        assertTrue(key!!.startsWith("ref:"))
        assertEquals(key, DuplicateCheck.referenceKey("EXPENSE", "629100004321"))
        // The raw reference isn't stored.
        assertFalse(key.contains("629100004321"))
        // Debit and credit legs of one transfer share a reference but are different entries.
        assertNotEquals(key, DuplicateCheck.referenceKey("INCOME", "629100004321"))
        assertNull(DuplicateCheck.referenceKey("EXPENSE", null))
        assertNull(DuplicateCheck.referenceKey("EXPENSE", " - "))
    }

    @Test
    fun dayBounds_coverTheWholeLocalDay() {
        val (start, end) = DuplicateCheck.dayBounds(at(2026, 10, 14, 23, 59), zone)
        assertEquals(at(2026, 10, 14, 0, 0), start)
        assertEquals(at(2026, 10, 15, 0, 0), end)
    }

    @Test
    fun dayBounds_handleDaylightSavingDays() {
        val ny = TimeZone.getTimeZone("America/New_York")
        val noon = Calendar.getInstance(ny).apply { clear(); set(2026, Calendar.MARCH, 8, 12, 0) }.timeInMillis
        val (start, end) = DuplicateCheck.dayBounds(noon, ny)
        assertEquals(23L * 60 * 60 * 1000, end - start) // clocks go forward: a 23-hour day
    }

    @Test
    fun sameAmountTypeAndDay_isLikelyDuplicate() {
        val saved = tx(250.0, at(2026, 10, 14, 9, 30))
        val match = DuplicateCheck.findMatch(DuplicateCandidate(250.0, "EXPENSE", at(2026, 10, 14, 0, 0)), listOf(saved), zone)
        assertEquals(saved, match?.existing)
        assertFalse(match!!.sameReference)
    }

    @Test
    fun amountsCompareByCents() {
        val saved = tx(0.3, at(2026, 10, 14))
        assertNotNull(DuplicateCheck.findMatch(DuplicateCandidate(0.1 + 0.2, "EXPENSE", at(2026, 10, 14)), listOf(saved), zone))
        assertNull(DuplicateCheck.findMatch(DuplicateCandidate(0.31, "EXPENSE", at(2026, 10, 14)), listOf(saved), zone))
    }

    @Test
    fun differentTypeOrDay_isNotADuplicate() {
        val saved = tx(250.0, at(2026, 10, 14, 23, 59))
        assertNull(DuplicateCheck.findMatch(DuplicateCandidate(250.0, "INCOME", at(2026, 10, 14)), listOf(saved), zone))
        assertNull(DuplicateCheck.findMatch(DuplicateCandidate(250.0, "EXPENSE", at(2026, 10, 15, 0, 0)), listOf(saved), zone))
    }

    @Test
    fun transferLegsAreIgnored() {
        val leg = tx(250.0, at(2026, 10, 14), transferId = "t1")
        assertNull(DuplicateCheck.findMatch(DuplicateCandidate(250.0, "EXPENSE", at(2026, 10, 14)), listOf(leg), zone))
    }

    @Test
    fun sameReference_matchesOnAnyDayAndWins() {
        val key = DuplicateCheck.referenceKey("EXPENSE", "401234567890")
        val sameDay = tx(99.0, at(2026, 10, 14), id = 1)
        val sameRef = tx(99.0, at(2026, 10, 12), hash = key, id = 2)
        val match = DuplicateCheck.findMatch(DuplicateCandidate(99.0, "EXPENSE", at(2026, 10, 14), key), listOf(sameDay, sameRef), zone)
        assertEquals(2L, match?.existing?.id)
        assertTrue(match!!.sameReference)
    }

    @Test
    fun unknownReference_fallsBackToAmountAndDay() {
        val key = DuplicateCheck.referenceKey("EXPENSE", "111")
        val other = tx(99.0, at(2026, 10, 14), hash = DuplicateCheck.referenceKey("EXPENSE", "222"))
        val match = DuplicateCheck.findMatch(DuplicateCandidate(99.0, "EXPENSE", at(2026, 10, 14), key), listOf(other), zone)
        assertNotNull(match)
        assertFalse(match!!.sameReference)
    }
}
