package com.akshar.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PluralsTest {
    @Test fun singularOnlyForExactlyOne() {
        assertEquals("0 transactions", countLabel(0, "transaction", locale = Locale.US))
        assertEquals("1 transaction", countLabel(1, "transaction", locale = Locale.US))
        assertEquals("2 transactions", countLabel(2, "transaction", locale = Locale.US))
    }

    @Test fun groupsLargeCountsAndAcceptsIrregularPlurals() {
        assertEquals("1,200 entries", countLabel(1200, "entry", "entries", Locale.US))
    }
}
