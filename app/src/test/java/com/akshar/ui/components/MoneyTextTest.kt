package com.akshar.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MoneyTextTest {

    private fun inUs(block: () -> Unit) {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try { block() } finally { Locale.setDefault(previous) }
    }

    @Test
    fun `signs non-zero amounts`() = inUs {
        assertEquals("+₹62,000", signedMoney(62000.0, positive = true, currencySymbol = "₹"))
        assertEquals("-₹25,979", signedMoney(25979.0, positive = false, currencySymbol = "₹"))
        assertEquals("-₹80.50", signedMoney(80.5, positive = false, currencySymbol = "₹", decimals = 2))
    }

    @Test
    fun `zero has no sign`() = inUs {
        assertEquals("₹0", signedMoney(0.0, positive = true, currencySymbol = "₹"))
        assertEquals("₹0", signedMoney(0.0, positive = false, currencySymbol = "₹"))
        assertEquals("$0.00", signedMoney(-0.0, positive = false, currencySymbol = "$", decimals = 2))
    }

    @Test
    fun `values that round to zero have no sign`() = inUs {
        assertEquals("₹0", signedMoney(0.4, positive = false, currencySymbol = "₹"))
        assertEquals("-₹1", signedMoney(0.5, positive = false, currencySymbol = "₹"))
        assertEquals("₹0.00", signedMoney(0.004, positive = true, currencySymbol = "₹", decimals = 2))
    }

    @Test
    fun `net takes its sign from the value`() = inUs {
        assertEquals("+₹36,021", netMoney(36021.0, "₹"))
        assertEquals("-₹450", netMoney(-450.0, "₹"))
        assertEquals("₹0", netMoney(0.0, "₹"))
    }
}
