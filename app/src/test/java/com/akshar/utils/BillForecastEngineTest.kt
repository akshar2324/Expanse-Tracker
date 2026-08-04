package com.akshar.utils

import com.akshar.data.model.Bill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BillForecastEngineTest {

    @Test
    fun testRecurrenceExpansion_MonthlyDayDrift() {
        val fixedClock = Calendar.getInstance().apply {
            set(2023, Calendar.JANUARY, 31, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val engine = BillForecastEngine(fixedClock)

        val bill = Bill(
            id = 1,
            title = "Test Bill",
            amount = 100.0,
            type = "EXPENSE",
            category = "Test",
            startDate = fixedClock,
            recurrence = "MONTHLY"
        )

        val occurrences = engine.expandRecurrence(bill, fixedClock, 3)

        assertEquals(3, occurrences.size)

        // Jan 31
        assertEquals(fixedClock, occurrences[0])

        // Feb 28
        val feb28 = Calendar.getInstance().apply {
            set(2023, Calendar.FEBRUARY, 28, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(feb28, occurrences[1])

        // Mar 31 (Not Mar 28!)
        val mar31 = Calendar.getInstance().apply {
            set(2023, Calendar.MARCH, 31, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(mar31, occurrences[2])
    }

    @Test
    fun testRecurrenceExpansion_LeapYear() {
        val fixedClock = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 31, 0, 0, 0) // 2024 is a leap year
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val engine = BillForecastEngine(fixedClock)

        val bill = Bill(
            id = 1,
            title = "Test Leap Year",
            amount = 100.0,
            type = "EXPENSE",
            category = "Test",
            startDate = fixedClock,
            recurrence = "MONTHLY"
        )

        val occurrences = engine.expandRecurrence(bill, fixedClock, 3)

        // Feb 29 2024
        val feb29 = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 29, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(feb29, occurrences[1])

        // Mar 31 2024
        val mar31 = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 31, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(mar31, occurrences[2])
    }

    @Test
    fun testRecurrenceExpansion_None() {
        val fixedClock = Calendar.getInstance().apply {
            set(2023, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val engine = BillForecastEngine(fixedClock)

        val bill = Bill(
            id = 1,
            title = "Test None",
            amount = 100.0,
            type = "EXPENSE",
            category = "Test",
            startDate = fixedClock,
            recurrence = "NONE"
        )

        val occurrences = engine.expandRecurrence(bill, fixedClock, 5) // ask for 5

        assertEquals(1, occurrences.size) // Should only yield 1
        assertEquals(fixedClock, occurrences[0])
    }

    @Test
    fun testForecast_FirstNegativeHighlight() {
        val fixedClock = Calendar.getInstance().apply {
            set(2023, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val engine = BillForecastEngine(fixedClock)

        val jan5 = Calendar.getInstance().apply {
            set(2023, Calendar.JANUARY, 5, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val bill = Bill(
            id = 1,
            title = "Test Bill",
            amount = 100.0,
            type = "EXPENSE",
            category = "Test",
            startDate = jan5,
            recurrence = "MONTHLY"
        )

        val forecast = engine.generateSixMonthForecast(
            currentBalance = 150.0,
            bills = listOf(bill),
            occurrences = emptyList(),
            recurringTransactions = emptyList()
        )

        // Check Jan 5 balance
        val jan5Forecast = forecast.find { it.date == jan5 }
        assertEquals(50.0, jan5Forecast?.balance)
        assertEquals(false, jan5Forecast?.isFirstNegative)

        // Check Feb 5 balance
        val feb5 = Calendar.getInstance().apply { timeInMillis = jan5; add(Calendar.MONTH, 1) }.timeInMillis
        val feb5Forecast = forecast.find { it.date == feb5 }
        assertEquals(-50.0, feb5Forecast?.balance)
        assertEquals(true, feb5Forecast?.isFirstNegative)
    }

    @Test
    fun testForecast_InactiveBillsAreIgnored() {
        val fixedClock = Calendar.getInstance().apply {
            set(2023, Calendar.JANUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val engine = BillForecastEngine(fixedClock)

        val bill = Bill(
            id = 1,
            title = "Test Inactive",
            amount = 100.0,
            type = "EXPENSE",
            category = "Test",
            startDate = fixedClock,
            recurrence = "MONTHLY",
            isActive = false // Inactive
        )

        val forecast = engine.generateSixMonthForecast(
            currentBalance = 150.0,
            bills = listOf(bill),
            occurrences = emptyList(),
            recurringTransactions = emptyList()
        )

        // Balance should remain 150 throughout
        assertTrue(forecast.all { it.balance == 150.0 })
    }
}
