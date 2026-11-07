package com.akshar.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyUtilsTest {

    private fun parse(input: String, locale: Locale = Locale.US) = MoneyUtils.parseAmount(input, locale)

    @Test
    fun `plain and grouped amounts parse in a dot-decimal locale`() {
        assertEquals(42.0, parse("42")!!, 0.0)
        assertEquals(12.5, parse("12.50")!!, 0.0)
        assertEquals(1234.5, parse("1,234.50")!!, 0.0)
        assertEquals(1_234_567.0, parse("1,234,567")!!, 0.0)
        assertEquals(9.99, parse("$9.99")!!, 0.0)
    }

    @Test
    fun `comma decimals parse in a comma-decimal locale`() {
        assertEquals(12.5, parse("12,50", Locale.GERMANY)!!, 0.0)
        assertEquals(1234.56, parse("1.234,56", Locale.GERMANY)!!, 0.0)
        assertEquals(1234.0, parse("1.234", Locale.GERMANY)!!, 0.0)
    }

    @Test
    fun `single comma with two decimals is read as a decimal comma even in en_US`() {
        assertEquals(12.5, parse("12,50")!!, 0.0)
    }

    @Test
    fun `expressions are evaluated and rounded half up to cents`() {
        assertEquals(3.33, parse("10/3")!!, 0.0)
        assertEquals(0.3, parse("0.1+0.2")!!, 0.0)
        assertEquals(2.68, parse("2.675")!!, 0.0) // HALF_UP on the decimal string, not on binary 2.67499..
        assertEquals(125.0, parse("120+5")!!, 0.0)
    }

    @Test
    fun `sign is preserved so callers can validate it`() {
        assertEquals(-500.0, parse("-500")!!, 0.0)
        assertEquals(0.0, parse("0")!!, 0.0)
    }

    @Test
    fun `invalid and non-finite input is rejected`() {
        assertNull(parse(""))
        assertNull(parse("   "))
        assertNull(parse("abc"))
        assertNull(parse("1/0"))
        assertNull(parse("0/0"))
        assertNull(parse("1.2.3"))
        assertNull(parse("5+"))
    }

    @Test
    fun `Indian integer grouping is accepted`() {
        assertEquals(123456.0, parse("1,23,456")!!, 0.0)
        assertEquals(12345678.0, parse("1,23,45,678")!!, 0.0)
        assertEquals(123456.78, parse("1,23,456.78")!!, 0.0)
    }

    @Test
    fun `malformed mixed grouping cannot silently change the amount`() {
        assertNull(parse("1,2.34"))
        assertNull(parse("1.2,34", Locale.GERMANY))
    }

    @Test
    fun `roundMoney uses half up on two decimals`() {
        assertEquals(1.01, MoneyUtils.roundMoney(1.005), 0.0)
        assertEquals(-1.01, MoneyUtils.roundMoney(-1.005), 0.0)
        assertEquals(0.0, MoneyUtils.roundMoney(0.004), 0.0)
    }
}
