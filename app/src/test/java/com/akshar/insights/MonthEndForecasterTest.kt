package com.akshar.insights

import com.akshar.data.model.Account
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Calendar
import java.util.TimeZone

class MonthEndForecasterTest {

    private val tz: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val forecaster = MonthEndForecaster(timeZone = tz)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    private fun daysBefore(ms: Long, days: Int): Long = InsightMath.addPeriods(ms, "DAILY", -days, tz)

    private fun expense(amount: Double, date: Long, description: String = "Groceries", transferId: String? = null) =
        Transaction(amount = amount, type = "EXPENSE", category = "Food", description = description, date = date, paymentMethod = "UPI", transferId = transferId)

    private fun income(amount: Double, date: Long, description: String = "Salary") =
        Transaction(amount = amount, type = "INCOME", category = "Salary", description = description, date = date, paymentMethod = "Bank Transfer")

    private fun reminder(amount: Double, frequency: String, lastTriggered: Long, description: String = "Rent", type: String = "EXPENSE") =
        RecurringTransaction(amount = amount, type = type, category = "Bills", description = description, frequency = frequency, lastTriggered = lastTriggered)

    private val now = at(2026, 11, 10, 12)
    private val account = Account(id = 1, name = "Bank", type = "Bank", openingBalance = 10_000.0)

    /** Salary on day -28 and ₹100 of groceries on each of the 28 full days before today. */
    private fun steadyHistory(): List<Transaction> =
        listOf(income(50_000.0, daysBefore(now, 28))) + (1..28).map { expense(100.0, daysBefore(now, it)) }

    private fun ready(result: MonthEndForecast): MonthEndForecast.Ready {
        assertTrue("expected a forecast but got $result", result is MonthEndForecast.Ready)
        return result as MonthEndForecast.Ready
    }

    @Test
    fun brandNewUser_needsHistory() {
        assertEquals(MonthEndForecast.NotEnoughData(14), forecaster.forecast(now, emptyList(), emptyList(), emptyList()))
        val emptyAccount = account.copy(openingBalance = 0.0)
        assertEquals(MonthEndForecast.NotEnoughData(14), forecaster.forecast(now, listOf(emptyAccount), emptyList(), emptyList()))
    }

    @Test
    fun shortHistory_saysHowManyMoreDays() {
        val tx = (1..5).map { expense(100.0, daysBefore(now, it)) }
        assertEquals(MonthEndForecast.NotEnoughData(moreDaysNeeded = 9), forecaster.forecast(now, listOf(account), tx, emptyList()))
    }

    @Test
    fun openingBalanceOnly_isNotEnoughData() {
        assertEquals(MonthEndForecast.NotEnoughData(moreDaysNeeded = 14), forecaster.forecast(now, listOf(account), emptyList(), emptyList()))
    }

    @Test
    fun longHistoryButFewExpenses_isNotEnoughData() {
        val tx = listOf(income(1_000.0, daysBefore(now, 30)), expense(50.0, daysBefore(now, 3)), expense(60.0, daysBefore(now, 2)))
        assertEquals(MonthEndForecast.NotEnoughData(moreDaysNeeded = 0), forecaster.forecast(now, listOf(account), tx, emptyList()))
    }

    @Test
    fun projection_addsBalanceRemindersAndEverydaySpending() {
        val tx = steadyHistory() + expense(30.0, at(2026, 11, 10, 9))
        val rent = reminder(15_000.0, "MONTHLY", at(2026, 10, 25, 10))
        val salary = reminder(50_000.0, "MONTHLY", at(2026, 11, 1, 10), description = "Salary", type = "INCOME")

        val f = ready(forecaster.forecast(now, listOf(account), tx, listOf(rent, salary)))

        assertEquals(BigDecimal("57170.00"), f.currentBalance) // 10,000 + 50,000 − 2,800 − 30
        assertEquals(BigDecimal("100.00"), f.dailyVariableSpend)
        assertEquals(28, f.basedOnDays)
        // 20 days after today at ₹100, plus ₹70 still expected today.
        assertEquals(BigDecimal("2070.00"), f.expectedVariableSpend)
        assertEquals(21, f.remainingDays)
        assertEquals(BigDecimal("15000.00"), f.upcomingBills)
        assertEquals(BigDecimal("0.00"), f.upcomingIncome) // next salary is 1 Dec
        assertEquals(listOf(at(2026, 11, 25, 10)), f.upcoming.map { it.dueMs })
        assertEquals(BigDecimal("40100.00"), f.projectedBalance)
        assertEquals(at(2026, 11, 30, 0), f.monthEndDayMs)
        assertFalse(f.isShortfall)
    }

    @Test
    fun largeOneOffSpends_areLeftOutOfTheDailyRate() {
        val tx = steadyHistory() +
            expense(18_000.0, daysBefore(now, 10), description = "Landlord") +
            expense(25_000.0, daysBefore(now, 4), description = "New laptop")
        val f = ready(forecaster.forecast(now, listOf(account), tx, emptyList()))
        assertEquals(BigDecimal("100.00"), f.dailyVariableSpend)
        assertEquals(2, f.excludedLargeCount)
        assertEquals(BigDecimal("43000.00"), f.excludedLargeTotal)
        // Still part of the balance: they really were spent.
        assertEquals(BigDecimal("14200.00"), f.currentBalance)
    }

    @Test
    fun spendingThatVariesNormally_isKept() {
        // 50, 100, 150, 200 repeating: no single amount is far from the rest.
        val tx = listOf(income(10_000.0, daysBefore(now, 28))) + (1..28).map { expense(50.0 * (1 + it % 4), daysBefore(now, it)) }
        val f = ready(forecaster.forecast(now, listOf(account), tx, emptyList()))
        assertEquals(0, f.excludedLargeCount)
        assertEquals(BigDecimal("125.00"), f.dailyVariableSpend)
    }

    @Test
    fun billsFromReminders_areNotCountedTwice() {
        val gym = reminder(2_000.0, "MONTHLY", at(2026, 11, 2, 8), description = "Gym")
        val tx = steadyHistory() +
            expense(2_000.0, at(2026, 10, 20, 8), description = "Gym (Auto-Recurring)") +
            expense(2_000.0, at(2026, 11, 2, 8), description = "gym") +
            expense(900.0, daysBefore(now, 3), description = "Electricity (Paid Bill)")
        val f = ready(forecaster.forecast(now, listOf(account), tx, listOf(gym)))
        assertEquals(BigDecimal("100.00"), f.dailyVariableSpend)
        assertEquals(0, f.excludedLargeCount)
    }

    @Test
    fun transfers_cancelOutAndAreNotSpending() {
        val savings = Account(id = 2, name = "Savings", type = "Savings", openingBalance = 0.0)
        val leg = daysBefore(now, 5)
        val tx = steadyHistory() +
            expense(5_000.0, leg, description = "To savings", transferId = "t1") +
            Transaction(amount = 5_000.0, type = "INCOME", category = "Transfer", description = "To savings", date = leg, paymentMethod = "Transfer", accountId = 2, transferId = "t1")
        val f = ready(forecaster.forecast(now, listOf(account, savings), tx, emptyList()))
        assertEquals(BigDecimal("57200.00"), f.currentBalance)
        assertEquals(BigDecimal("100.00"), f.dailyVariableSpend)
    }

    @Test
    fun negativeProjection_isAShortfall() {
        val broke = account.copy(openingBalance = -49_000.0)
        val rent = reminder(15_000.0, "MONTHLY", at(2026, 10, 25, 10))
        val f = ready(forecaster.forecast(now, listOf(broke), steadyHistory(), listOf(rent)))
        // −49,000 + 50,000 − 2,800 = −1,800; minus rent and 21 × 100 of spending.
        assertEquals(BigDecimal("-18900.00"), f.projectedBalance)
        assertTrue(f.isShortfall)
    }

    @Test
    fun todayAlreadyAboveTheRate_addsNothingMoreForToday() {
        val tx = steadyHistory() + expense(250.0, at(2026, 11, 10, 9))
        val f = ready(forecaster.forecast(now, listOf(account), tx, emptyList()))
        assertEquals(BigDecimal("2000.00"), f.expectedVariableSpend)
        assertEquals(20, f.remainingDays)
    }

    @Test
    fun leapFebruary_endsOnThe29th() {
        val leapNow = at(2028, 2, 10)
        val tx = listOf(income(1_000.0, daysBefore(leapNow, 20))) + (1..20).map { expense(10.0, daysBefore(leapNow, it)) }
        val f = ready(forecaster.forecast(leapNow, listOf(account), tx, emptyList()))
        assertEquals(at(2028, 2, 29, 0), f.monthEndDayMs)
        assertEquals(20, f.remainingDays) // 19 days after today + today
        assertEquals(BigDecimal("200.00"), f.expectedVariableSpend)
    }

    @Test
    fun ordinaryFebruary_endsOnThe28th() {
        val febNow = at(2027, 2, 10)
        val tx = listOf(income(1_000.0, daysBefore(febNow, 20))) + (1..20).map { expense(10.0, daysBefore(febNow, it)) }
        val f = ready(forecaster.forecast(febNow, listOf(account), tx, emptyList()))
        assertEquals(at(2027, 2, 28, 0), f.monthEndDayMs)
        assertEquals(19, f.remainingDays)
    }

    @Test
    fun lastDayOfMonth_onlyCountsWhatIsLeftOfToday() {
        val lateNow = at(2026, 11, 30, 23, 0)
        val tx = listOf(income(10_000.0, daysBefore(lateNow, 28))) + (1..28).map { expense(100.0, daysBefore(lateNow, it)) }
        val tonight = reminder(500.0, "MONTHLY", at(2026, 10, 30, 23, 30), description = "Phone")
        val tomorrow = reminder(700.0, "MONTHLY", at(2026, 11, 1, 0, 0), description = "Internet")
        val f = ready(forecaster.forecast(lateNow, listOf(account), tx, listOf(tonight, tomorrow)))
        assertEquals(BigDecimal("100.00"), f.expectedVariableSpend)
        assertEquals(1, f.remainingDays)
        assertEquals(listOf("Phone"), f.upcoming.map { it.description }) // Internet falls on 1 Dec 00:00
        assertEquals(at(2026, 11, 30, 0), f.monthEndDayMs)
    }

    @Test
    fun weeklyAndNeverTriggeredReminders() {
        val weekly = reminder(300.0, "WEEKLY", at(2026, 11, 3, 9), description = "Cleaner")
        val fresh = reminder(1_000.0, "MONTHLY", 0L, description = "New plan")
        val freshWeekly = reminder(50.0, "WEEKLY", 0L, description = "Paper")
        val f = ready(forecaster.forecast(now, listOf(account), steadyHistory(), listOf(weekly, fresh, freshWeekly)))
        // Cleaner on 10 Nov 09:00 is overdue but not yet posted, so it still counts; New plan's
        // schedule starts now, so its first due date is 10 Dec.
        assertEquals(
            listOf("Cleaner" to at(2026, 11, 10, 9), "Cleaner" to at(2026, 11, 17, 9), "Paper" to daysBefore(now, -7), "Cleaner" to at(2026, 11, 24, 9), "Paper" to daysBefore(now, -14)),
            f.upcoming.map { it.description to it.dueMs }
        )
        assertEquals(BigDecimal("1000.00"), f.upcomingBills)
    }

    @Test
    fun futureDatedEntries_inThisMonthAreUpcoming() {
        val tx = steadyHistory() +
            income(4_000.0, at(2026, 11, 20), description = "Refund") +
            expense(1_500.0, at(2026, 11, 28), description = "Concert") +
            expense(9_999.0, at(2026, 12, 2), description = "Next month")
        val f = ready(forecaster.forecast(now, listOf(account), tx, emptyList()))
        assertEquals(BigDecimal("57200.00"), f.currentBalance)
        assertEquals(BigDecimal("4000.00"), f.upcomingIncome)
        assertEquals(BigDecimal("1500.00"), f.upcomingBills)
        assertEquals(BigDecimal("57200.00").add(BigDecimal("4000")).subtract(BigDecimal("1500")).subtract(BigDecimal("2100")).setScale(2), f.projectedBalance)
    }

    @Test
    fun decimalAmounts_stayExact() {
        val tx = listOf(income(0.1, daysBefore(now, 20)), income(0.2, daysBefore(now, 20))) +
            (1..20).map { expense(0.1, daysBefore(now, it)) }
        val f = ready(forecaster.forecast(now, listOf(account.copy(openingBalance = 0.0)), tx, emptyList()))
        assertEquals(BigDecimal("-1.70"), f.currentBalance) // 0.1 + 0.2 − 2.0, no binary drift
        assertEquals(BigDecimal("0.10"), f.dailyVariableSpend)
    }

    @Test
    fun nonFiniteAmounts_areIgnored() {
        val tx = steadyHistory() + expense(Double.NaN, daysBefore(now, 2)) + income(Double.POSITIVE_INFINITY, daysBefore(now, 2))
        val f = ready(forecaster.forecast(now, listOf(account), tx, emptyList()))
        assertEquals(BigDecimal("57200.00"), f.currentBalance)
    }

    @Test
    fun fence_usesTukeyWithAMedianFloor() {
        fun fence(vararg values: Int) = MonthEndForecaster.largeSpendFence(values.map { BigDecimal(it) }.sorted())
        // Q1 = 100, Q3 = 100, IQR = 0, so the 5 × median floor applies.
        assertEquals(0, BigDecimal(500).compareTo(fence(100, 100, 100, 100, 100)))
        // 10..100: Q1 = 32.5, median = 55, Q3 = 77.5 → Q3 + 3 × 45 = 212.5 < 275.
        assertEquals(0, BigDecimal(275).compareTo(fence(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)))
        assertEquals(BigDecimal.ZERO, MonthEndForecaster.largeSpendFence(emptyList()))
    }

    @Test
    fun quantile_interpolates() {
        val sorted = listOf(1, 2, 3, 4).map { BigDecimal(it) }
        assertEquals(0, BigDecimal("2.5").compareTo(InsightMath.quantile(sorted, 0.5)))
        assertEquals(0, BigDecimal("1.75").compareTo(InsightMath.quantile(sorted, 0.25)))
        assertEquals(BigDecimal(7), InsightMath.quantile(listOf(BigDecimal(7)), 0.75))
    }

    @Test
    fun localDays_followTheDeviceTimeZone() {
        // 23:30 and 00:30 in Kolkata are different days even though both are 18:00–19:00 UTC.
        val lateEvening = at(2026, 11, 9, 23, 30)
        val pastMidnight = at(2026, 11, 10, 0, 30)
        assertEquals(1L, InsightMath.dayIndex(pastMidnight, tz) - InsightMath.dayIndex(lateEvening, tz))
    }
    @Test
    fun dailyAndYearlyReminders_includeEveryDueOccurrenceAndIncome() {
        val daily = reminder(0.10, "DAILY", at(2026, 11, 9, 9), "Daily")
        val yearly = reminder(500.0, "YEARLY", at(2025, 11, 30, 23, 59), "Rebate", "INCOME")
        val after = reminder(999.0, "YEARLY", at(2025, 12, 1, 0), "Next month")
        val f = ready(forecaster.forecast(now, listOf(account), steadyHistory(), listOf(daily, yearly, after)))
        assertEquals(21, f.upcoming.count { it.description == "Daily" })
        assertEquals(BigDecimal("2.10"), f.upcomingBills)
        assertEquals(BigDecimal("500.00"), f.upcomingIncome)
    }

    @Test
    fun staleDailyReminder_neverTruncatesTheTotal() {
        val old = daysBefore(now, 1000)
        val f = ready(forecaster.forecast(now, listOf(account), steadyHistory(), listOf(reminder(1.25, "DAILY", old))))
        assertEquals(BigDecimal("1275.00"), f.upcomingBills) // 1000 overdue + 20 future days
        assertTrue(f.upcoming.size <= 401)
    }

    @Test
    fun futureRefundsAndNegativeIncome_preserveTheirDirection() {
        val tx = steadyHistory() + expense(-25.10, daysBefore(now, -1)) + income(-10.20, daysBefore(now, -2))
        val f = ready(forecaster.forecast(now, listOf(account), tx, emptyList()))
        assertEquals(BigDecimal("25.10"), f.upcomingIncome)
        assertEquals(BigDecimal("10.20"), f.upcomingBills)
    }

    @Test
    fun rounding_reconcilesTheDisplayedBreakdown() {
        val tx = listOf(income(0.005, daysBefore(now, 21))) + (1..21).map { expense(0.01, daysBefore(now, it)) }
        val f = ready(forecaster.forecast(now, listOf(account.copy(openingBalance = 0.0)), tx, emptyList()))
        assertEquals(f.projectedBalance, f.currentBalance.add(f.upcomingIncome).subtract(f.upcomingBills).subtract(f.expectedVariableSpend))
    }

    @Test
    fun newYearAndMonthEndClamping() {
        val date = at(2027, 12, 31)
        val tx = (1..28).map { expense(10.0, daysBefore(date, it)) }
        val f = ready(forecaster.forecast(date, listOf(account), tx, listOf(reminder(500.0, "MONTHLY", at(2027, 11, 30)))))
        assertEquals(at(2027, 12, 31, 0), f.monthEndDayMs)
        assertEquals(BigDecimal("500.00"), f.upcomingBills)
        assertEquals(at(2028, 2, 29), InsightMath.addPeriods(at(2028, 1, 31), "MONTHLY", 1, tz))
        assertEquals(at(2028, 3, 31), InsightMath.addPeriods(at(2028, 1, 31), "MONTHLY", 2, tz))
    }

    @Test
    fun sparseHistory_includesZeroSpendDaysAndRoundsOnlyTheTotal() {
        val tx = listOf(income(10.0, daysBefore(now, 28))) +
            (1..5).map { expense(0.10, daysBefore(now, it)) }
        val f = ready(forecaster.forecast(now, listOf(account), tx, emptyList()))
        assertEquals(28, f.basedOnDays)
        assertEquals(BigDecimal("0.02"), f.dailyVariableSpend)
        // 0.50 / 28 * 21 = 0.375; using the displayed rounded rate would incorrectly give 0.42.
        assertEquals(BigDecimal("0.38"), f.expectedVariableSpend)
    }

}
