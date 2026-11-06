package com.akshar.ui.screens.analytics

import com.akshar.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TrendPointsTest {

    private val tz = TimeZone.getTimeZone("UTC")
    private fun cal(month: Int, day: Int, hour: Int = 12) =
        Calendar.getInstance(tz, Locale.US).apply { clear(); set(2026, month, day, hour, 0, 0) }
    private fun tx(month: Int, day: Int, amount: Double, hour: Int = 12) = Transaction(
        amount = amount, type = "EXPENSE", category = "Food", description = "",
        date = cal(month, day, hour).timeInMillis, paymentMethod = "Cash"
    )

    @Test
    fun `last seven days cross a month boundary in date order`() {
        val points = calculateTrendPoints(emptyList(), cal(Calendar.NOVEMBER, 3))
        assertEquals(listOf("28", "29", "30", "31", "01", "02", "03"), points.map { it.first })
    }

    @Test
    fun `only transactions inside the window are counted, on their real day`() {
        val transactions = listOf(
            tx(Calendar.NOVEMBER, 3, 10.0, hour = 23),
            tx(Calendar.OCTOBER, 31, 5.0, hour = 0),
            tx(Calendar.OCTOBER, 3, 999.0),   // same day-of-month a month earlier: excluded
            tx(Calendar.OCTOBER, 27, 50.0),   // one day before the window: excluded
            tx(Calendar.NOVEMBER, 4, 77.0, hour = 0) // tomorrow: excluded
        )
        val points = calculateTrendPoints(transactions, cal(Calendar.NOVEMBER, 3))
        assertEquals(7, points.size)
        assertEquals(listOf(0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 10.0), points.map { it.second })
    }
}
