package com.akshar.utils

import com.akshar.data.model.Bill
import com.akshar.data.model.BillOccurrence
import com.akshar.data.model.RecurringTransaction
import java.util.Calendar

class BillForecastEngine(private val fixedClockMillis: Long? = null) {

    fun getCurrentTimeMillis(): Long = fixedClockMillis ?: System.currentTimeMillis()

    /**
     * Deterministically expands a bill's recurrence into a list of specific due dates.
     * Generates up to `count` occurrences starting from `startDate` that are >= `fromDateMillis`.
     */
    fun expandRecurrence(bill: Bill, fromDateMillis: Long, count: Int = 12): List<Long> {
        val occurrences = mutableListOf<Long>()
        val startCal = Calendar.getInstance().apply { timeInMillis = bill.startDate }
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)

        if (bill.recurrence == "NONE") {
            if (startCal.timeInMillis >= fromDateMillis) {
                occurrences.add(startCal.timeInMillis)
            }
            return occurrences
        }

        // Loop forward to find occurrences
        var generated = 0
        // Cap iterations to avoid infinite loop on broken logic
        var iterations = 0
        var offset = 0

        while (generated < count && iterations < 1000) {
            val iterCal = Calendar.getInstance().apply { timeInMillis = bill.startDate }

            // Advance by recurrence
            when (bill.recurrence) {
                "DAILY" -> iterCal.add(Calendar.DAY_OF_YEAR, offset)
                "WEEKLY" -> iterCal.add(Calendar.WEEK_OF_YEAR, offset)
                "MONTHLY" -> {
                    iterCal.set(Calendar.DAY_OF_MONTH, 1) // prevent overflow before adding months
                    iterCal.add(Calendar.MONTH, offset)
                    val maxDayInMonth = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    iterCal.set(Calendar.DAY_OF_MONTH, minOf(startDay, maxDayInMonth))
                }
                "YEARLY" -> {
                    iterCal.set(Calendar.DAY_OF_MONTH, 1)
                    iterCal.add(Calendar.YEAR, offset)
                    val maxDayInMonth = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    iterCal.set(Calendar.DAY_OF_MONTH, minOf(startDay, maxDayInMonth))
                }
                else -> {
                    iterCal.set(Calendar.DAY_OF_MONTH, 1) // prevent overflow before adding months
                    iterCal.add(Calendar.MONTH, offset)
                    val maxDayInMonth = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    iterCal.set(Calendar.DAY_OF_MONTH, minOf(startDay, maxDayInMonth))
                }
            }

            if (iterCal.timeInMillis >= fromDateMillis) {
                occurrences.add(iterCal.timeInMillis)
                generated++
            }
            offset++
            iterations++
        }

        return occurrences
    }

    data class DailyBalance(
        val date: Long,
        val balance: Double,
        val isFirstNegative: Boolean
    )

    /**
     * Calculates a 6-month cash flow forecast.
     */
    fun generateSixMonthForecast(
        currentBalance: Double,
        bills: List<Bill>,
        occurrences: List<BillOccurrence>,
        recurringTransactions: List<RecurringTransaction>
    ): List<DailyBalance> {
        val now = getCurrentTimeMillis()
        val calNow = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calNow.timeInMillis

        val sixMonthsLater = Calendar.getInstance().apply {
            timeInMillis = startOfToday
            add(Calendar.MONTH, 6)
        }.timeInMillis

        val events = mutableMapOf<Long, Double>()

        // Add existing un-paid bill occurrences and future projected bills
        bills.filter { it.isActive }.forEach { bill ->
            val billOccurrences = occurrences.filter { it.billId == bill.id }
            val paidOrSkipped = billOccurrences.filter { it.state == "PAID" || it.state == "SKIPPED" }.map { it.dueDate }.toSet()

            // Expand the recurrence up to 6 months
            val projectedDates = expandRecurrence(bill, startOfToday, 30) // Up to 30 to cover 6 months

            projectedDates.forEach { date ->
                if (date <= sixMonthsLater && !paidOrSkipped.contains(date)) {
                    // Truncate to start of day
                    val dayStart = Calendar.getInstance().apply {
                        timeInMillis = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val amount = if (bill.type == "EXPENSE") -bill.amount else bill.amount
                    events[dayStart] = (events[dayStart] ?: 0.0) + amount
                }
            }
        }

        // Add confirmed RecurringTransactions
        recurringTransactions.forEach { rec ->
            // Fix: ensure legacy recurring transactions do not start from 0 if lastTriggered is not set.
            val anchorDate = if (rec.lastTriggered > 0) rec.lastTriggered else startOfToday
            val recCal = Calendar.getInstance().apply { timeInMillis = maxOf(anchorDate, startOfToday) }
            val startDay = Calendar.getInstance().apply { timeInMillis = anchorDate }.get(Calendar.DAY_OF_MONTH)

            var generated = 0
            var offset = 0
            while (recCal.timeInMillis <= sixMonthsLater && generated < 30) {
                // Determine iterative date
                val iterCal = Calendar.getInstance().apply { timeInMillis = anchorDate }
                when (rec.frequency) {
                    "DAILY" -> iterCal.add(Calendar.DAY_OF_YEAR, offset)
                    "WEEKLY" -> iterCal.add(Calendar.WEEK_OF_YEAR, offset)
                    "MONTHLY" -> {
                        iterCal.set(Calendar.DAY_OF_MONTH, 1)
                        iterCal.add(Calendar.MONTH, offset)
                        val maxD = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        iterCal.set(Calendar.DAY_OF_MONTH, minOf(startDay, maxD))
                    }
                    "YEARLY" -> {
                        iterCal.set(Calendar.DAY_OF_MONTH, 1)
                        iterCal.add(Calendar.YEAR, offset)
                        val maxD = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        iterCal.set(Calendar.DAY_OF_MONTH, minOf(startDay, maxD))
                    }
                    else -> {
                        iterCal.set(Calendar.DAY_OF_MONTH, 1)
                        iterCal.add(Calendar.MONTH, offset)
                        val maxD = iterCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        iterCal.set(Calendar.DAY_OF_MONTH, minOf(startDay, maxD))
                    }
                }

                val date = iterCal.timeInMillis
                recCal.timeInMillis = date

                if (date >= startOfToday && date <= sixMonthsLater) {
                    val dayStart = Calendar.getInstance().apply {
                        timeInMillis = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val amount = if (rec.type == "EXPENSE") -rec.amount else rec.amount
                    events[dayStart] = (events[dayStart] ?: 0.0) + amount
                }

                offset++
                generated++
            }
        }

        // Build daily balances
        val dailyBalances = mutableListOf<DailyBalance>()
        var runningBalance = currentBalance
        var foundFirstNegative = false

        val loopCal = Calendar.getInstance().apply { timeInMillis = startOfToday }
        while (loopCal.timeInMillis <= sixMonthsLater) {
            val dayStart = loopCal.timeInMillis
            val dayNet = events[dayStart] ?: 0.0

            runningBalance += dayNet

            val isFirstNegative = runningBalance < 0 && !foundFirstNegative
            if (isFirstNegative) {
                foundFirstNegative = true
            }

            dailyBalances.add(DailyBalance(dayStart, runningBalance, isFirstNegative))

            loopCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return dailyBalances
    }
}
