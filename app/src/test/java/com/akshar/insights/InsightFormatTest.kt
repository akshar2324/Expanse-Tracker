package com.akshar.insights

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class InsightFormatTest {

    private fun approx(value: String) = InsightFormat.approximate(BigDecimal(value))

    @Test
    fun roundsToFriendlyFigures() {
        assertEquals(BigDecimal("12400"), approx("12437.80"))
        assertEquals(BigDecimal("12500"), approx("12450.00")) // HALF_UP
        assertEquals(BigDecimal("440"), approx("437.00"))
        assertEquals(BigDecimal("46"), approx("45.50"))
        assertEquals(BigDecimal("1000"), approx("999.60"))
        assertEquals(BigDecimal("0"), approx("0.00"))
        assertEquals(BigDecimal("0"), approx("0.40"))
    }

    @Test
    fun negativeValuesRoundTheSameWayAsPositive() {
        assertEquals(BigDecimal("-1300"), approx("-1250.00"))
        assertEquals(BigDecimal("-440"), approx("-437.00"))
    }

    @Test
    fun wholeMoney_dropsTheSignAndGroupsDigits() {
        assertEquals("₹12,400", InsightFormat.wholeMoney(BigDecimal("12400"), "₹", Locale.US))
        assertEquals("₹2,300", InsightFormat.wholeMoney(BigDecimal("-2300"), "₹", Locale.US))
        assertEquals("$0", InsightFormat.wholeMoney(BigDecimal.ZERO, "$", Locale.US))
    }
}
