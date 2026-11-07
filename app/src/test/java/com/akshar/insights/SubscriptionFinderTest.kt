package com.akshar.insights

import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar
import java.util.TimeZone

class SubscriptionFinderTest {

    private val tz: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val finder = SubscriptionFinder(timeZone = tz)
    private val now = at(2026, 11, 1)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 10): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, day, hour, 0, 0)
        }.timeInMillis

    private var nextId = 1L
    private fun charge(description: String, amount: Double, date: Long, category: String = "Subscriptions", type: String = "EXPENSE", transferId: String? = null) =
        Transaction(id = nextId++, amount = amount, type = type, category = category, description = description, date = date, paymentMethod = "Credit Card", transferId = transferId)

    private fun monthly(description: String, amount: Double, vararg dates: Long) = dates.map { charge(description, amount, it) }

    private val netflix = monthly("Netflix", 649.0, at(2026, 8, 5), at(2026, 9, 5), at(2026, 10, 5))

    @Test
    fun threeMonthlyCharges_areASubscription() {
        val found = finder.find(now, netflix, emptyList())
        assertEquals(1, found.size)
        val s = found.single()
        assertEquals("netflix", s.key)
        assertEquals("Netflix", s.displayName)
        assertEquals(SubscriptionFrequency.MONTHLY, s.frequency)
        assertEquals(BigDecimal("649.00"), s.amount)
        assertEquals(3, s.occurrences)
        assertEquals(at(2026, 10, 5), s.lastChargeMs)
        assertEquals(at(2026, 11, 5), s.nextExpectedMs)
        assertEquals(at(2026, 10, 5), s.reminderAnchorMs)
        assertEquals("Credit Card", s.paymentMethod)
    }

    @Test
    fun twoCharges_areNotEnough() {
        assertTrue(finder.find(now, netflix.take(2), emptyList()).isEmpty())
    }

    @Test
    fun descriptionsAreNormalised() {
        val tx = listOf(
            charge("NETFLIX.COM 8812", 649.0, at(2026, 8, 5)),
            charge("Netflix", 649.0, at(2026, 9, 5)),
            charge("  netflix* ", 649.0, at(2026, 10, 5)),
        )
        val s = finder.find(now, tx, emptyList()).single()
        assertEquals("netflix", s.key)
        assertEquals("netflix*", s.displayName) // the latest wording, trimmed
    }

    @Test
    fun smallPriceChange_isTolerated_butAOneOffIsIgnored() {
        val tx = listOf(
            charge("Spotify", 119.0, at(2026, 7, 12)),
            charge("Spotify", 119.0, at(2026, 8, 12)),
            charge("Spotify", 1_189.0, at(2026, 8, 20)), // gift card, not the plan
            charge("Spotify", 129.0, at(2026, 9, 12)),
            charge("Spotify", 129.0, at(2026, 10, 12)),
        )
        val s = finder.find(now, tx, emptyList()).single()
        assertEquals(4, s.occurrences)
        assertEquals(BigDecimal("129.00"), s.amount)
    }

    @Test
    fun amountsTooDifferent_areNotASubscription() {
        val tx = monthly("Electricity", 900.0, at(2026, 8, 3)) +
            monthly("Electricity", 1_400.0, at(2026, 9, 3)) +
            monthly("Electricity", 2_100.0, at(2026, 10, 3))
        assertTrue(finder.find(now, tx, emptyList()).isEmpty())
    }

    @Test
    fun irregularGaps_areNotASubscription() {
        val tx = monthly("Swiggy", 400.0, at(2026, 8, 1), at(2026, 8, 19), at(2026, 9, 30), at(2026, 10, 4))
        assertTrue(finder.find(now, tx, emptyList()).isEmpty())
    }

    @Test
    fun oneMissedMonth_breaksTheSchedule() {
        val tx = monthly("Gym", 1_500.0, at(2026, 6, 1), at(2026, 7, 1), at(2026, 9, 1), at(2026, 10, 1))
        assertTrue(finder.find(now, tx, emptyList()).isEmpty())
    }

    @Test
    fun weeklyCharges() {
        val tx = monthly("Milk delivery", 210.0, at(2026, 10, 11), at(2026, 10, 18), at(2026, 10, 25))
        val s = finder.find(now, tx, emptyList()).single()
        assertEquals(SubscriptionFrequency.WEEKLY, s.frequency)
        // 25 Oct + 7 days is 1 Nov 10:00, which is exactly now, so the next one expected is 8 Nov.
        assertEquals(at(2026, 11, 8), s.nextExpectedMs)
    }

    @Test
    fun yearlyCharges_acrossALeapDay() {
        val tx = monthly("Domain renewal", 999.0, at(2023, 12, 1), at(2024, 12, 1), at(2025, 12, 1))
        val s = finder.find(at(2026, 6, 1), tx, emptyList()).single()
        assertEquals(SubscriptionFrequency.YEARLY, s.frequency)
        assertEquals(at(2026, 12, 1), s.nextExpectedMs)
    }

    @Test
    fun chargesOnThe31st_survive_shortMonths() {
        val tx = monthly("Cloud storage", 149.0, at(2027, 1, 31), at(2027, 2, 28), at(2027, 3, 31))
        val s = finder.find(at(2027, 4, 10), tx, emptyList()).single()
        assertEquals(SubscriptionFrequency.MONTHLY, s.frequency)
        assertEquals(at(2027, 4, 30), s.nextExpectedMs) // 31 Mar + 1 month, clamped
    }

    @Test
    fun alreadyAReminder_isLeftOut() {
        val reminder = RecurringTransaction(amount = 649.0, type = "EXPENSE", category = "Subscriptions", description = "Netflix subscription", frequency = "MONTHLY")
        assertTrue(finder.find(now, netflix, listOf(reminder)).isEmpty())
    }

    @Test
    fun entriesPostedByReminders_areIgnored() {
        val tx = monthly("Music plan (Auto-Recurring)", 99.0, at(2026, 8, 5), at(2026, 9, 5), at(2026, 10, 5))
        assertTrue(finder.find(now, tx, emptyList()).isEmpty())
    }

    @Test
    fun dismissedMerchants_stayHidden() {
        assertTrue(finder.find(now, netflix, emptyList(), dismissedKeys = setOf("netflix")).isEmpty())
        assertEquals(1, finder.find(now, netflix, emptyList(), dismissedKeys = setOf("hotstar")).size)
    }

    @Test
    fun stoppedSubscription_isNotSuggested() {
        // Last charge 5 Oct; by 25 Nov that is more than 1.5 months ago.
        assertTrue(finder.find(at(2026, 11, 25), netflix, emptyList()).isEmpty())
        assertEquals(1, finder.find(at(2026, 11, 15), netflix, emptyList()).size)
    }

    @Test
    fun lateCharge_rollsTheNextDateForward() {
        // 5 Nov has passed without a charge being logged: expect 5 Dec, and anchor the reminder
        // on 5 Nov so the app does not post the possibly-unlogged 5 Nov charge itself.
        val s = finder.find(at(2026, 11, 7), netflix, emptyList()).single()
        assertEquals(at(2026, 12, 5), s.nextExpectedMs)
        assertEquals(at(2026, 11, 5), s.reminderAnchorMs)
    }

    @Test
    fun sameDayDuplicates_countOnce() {
        val tx = netflix + charge("Netflix", 649.0, at(2026, 10, 5, 18))
        val s = finder.find(now, tx, emptyList()).single()
        assertEquals(3, s.occurrences)
        assertEquals(at(2026, 10, 5, 18), s.lastChargeMs)
        // Two charges in one day are still not three occurrences.
        assertTrue(finder.find(now, netflix.take(2) + charge("Netflix", 649.0, at(2026, 9, 5, 20)), emptyList()).isEmpty())
    }

    @Test
    fun incomeAndTransfers_areNotSubscriptions() {
        val salary = listOf(8, 9, 10).map { charge("Salary", 50_000.0, at(2026, it, 1), type = "INCOME") }
        val sweep = listOf(8, 9, 10).map { charge("To savings", 5_000.0, at(2026, it, 2), transferId = "t$it") }
        assertTrue(finder.find(now, salary + sweep, emptyList()).isEmpty())
    }

    @Test
    fun blankOrNumericDescriptions_areSkipped() {
        val tx = monthly("1234", 50.0, at(2026, 8, 5), at(2026, 9, 5), at(2026, 10, 5)) +
            monthly("", 50.0, at(2026, 8, 6), at(2026, 9, 6), at(2026, 10, 6))
        assertTrue(finder.find(now, tx, emptyList()).isEmpty())
    }

    @Test
    fun ties_areOrderedByDateThenAmountThenName() {
        val dates = longArrayOf(at(2026, 8, 5), at(2026, 9, 5), at(2026, 10, 5))
        val tx = monthly("Zee5", 99.0, *dates) + monthly("Hotstar", 299.0, *dates) + monthly("Apple TV", 99.0, *dates) +
            monthly("Gym", 1_500.0, at(2026, 8, 3), at(2026, 9, 3), at(2026, 10, 3))
        assertEquals(listOf("gym", "hotstar", "apple tv", "zee"), finder.find(now, tx, emptyList()).map { it.key })
    }

    @Test
    fun categoryTie_usesTheMostRecent() {
        val tx = listOf(
            charge("Prime", 299.0, at(2026, 7, 9), category = "Shopping"),
            charge("Prime", 299.0, at(2026, 8, 9), category = "Entertainment"),
            charge("Prime", 299.0, at(2026, 9, 9), category = "Shopping"),
            charge("Prime", 299.0, at(2026, 10, 9), category = "Entertainment"),
        )
        assertEquals("Entertainment", finder.find(now, tx, emptyList()).single().category)
    }

    @Test
    fun merchantKeys() {
        assertEquals("amazon prime", MerchantKey.normalize("AMAZON PRIME 2024 "))
        assertEquals("netflix", MerchantKey.normalize("www.Netflix.com"))
        assertEquals("", MerchantKey.normalize("#123 / 456"))
        assertEquals("rent", MerchantKey.normalize("Rent (Paid Bill)"))
        assertTrue(MerchantKey.isReminderGenerated("Rent (auto-recurring)"))
        assertFalse(MerchantKey.isReminderGenerated("Rent"))
        assertTrue(MerchantKey.sameMerchant("netflix", "netflix subscription"))
        assertFalse(MerchantKey.sameMerchant("tv", "apple tv")) // too short to match loosely
        assertFalse(MerchantKey.sameMerchant("net", "netflix"))
        assertFalse(MerchantKey.sameMerchant("", ""))
    }
    @Test
    fun toleranceBoundary_isInclusiveAndDecimalExact() {
        val tx = listOf(
            charge("Plan", 85.0, at(2026, 8, 5)),
            charge("Plan", 100.0, at(2026, 9, 5)),
            charge("Plan", 115.0, at(2026, 10, 5)),
        )
        assertEquals(3, finder.find(now, tx, emptyList()).single().occurrences)
        assertTrue(finder.find(now, tx.dropLast(1) + tx.last().copy(amount = 115.01), emptyList()).isEmpty())
    }

    @Test
    fun sameTimestampTie_usesHigherIdRegardlessOfInputOrder() {
        val tx = netflix + netflix.last().copy(id = 900, amount = 650.0, category = "Bills")
        val a = finder.find(now, tx, emptyList()).single()
        val b = finder.find(now, tx.reversed(), emptyList()).single()
        assertEquals(a, b)
        assertEquals(BigDecimal("650.00"), a.amount)
    }

    @Test
    fun invalidAndFutureAmounts_cannotSupplyOccurrences() {
        val tx = netflix.take(2) + netflix.last().copy(amount = Double.NaN) +
            netflix.last().copy(amount = 0.0) + netflix.last().copy(amount = -649.0) +
            netflix.last().copy(date = at(2026, 12, 5))
        assertTrue(finder.find(now, tx, emptyList()).isEmpty())
    }

}
