package com.akshar.ui.components

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountUpMemoryTest {
    @After fun reset() = CountUpMemory.reset()

    @Test fun firstShowCountsUpFromZero() {
        assertNull(CountUpMemory.lastShown("balance"))
        assertEquals(0.0, CountUpMemory.start("balance", shownNow = 0.0, target = 524_723.88)!!, 0.0)
    }

    @Test fun returningToTheScreenDoesNotCountUpAgain() {
        CountUpMemory.start("balance", shownNow = 0.0, target = 524_723.88)
        // A new composition (tab switch) starts from the remembered value, which equals the target.
        val from = CountUpMemory.lastShown("balance")!!
        assertNull(CountUpMemory.start("balance", shownNow = from, target = 524_723.88))
    }

    @Test fun aChangedBalanceAnimatesOnlyTheDifference() {
        CountUpMemory.start("balance", shownNow = 0.0, target = 1_000.0)
        assertEquals(1_000.0, CountUpMemory.start("balance", shownNow = 1_000.0, target = 950.5)!!, 0.0)
        assertEquals(950.5, CountUpMemory.lastShown("balance")!!, 0.0)
    }

    @Test fun keysAreIndependent() {
        CountUpMemory.start("a", shownNow = 0.0, target = 10.0)
        assertNull(CountUpMemory.lastShown("b"))
    }
}
