package com.akshar.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStyleTest {

    @Test
    fun `saved keys map back to their style`() {
        AppStyle.entries.forEach { assertEquals(it, AppStyle.fromKey(it.key)) }
    }

    @Test
    fun `missing or unknown keys fall back to Atelier`() {
        assertEquals(AppStyle.ATELIER, AppStyle.fromKey(null))
        assertEquals(AppStyle.ATELIER, AppStyle.fromKey("midnight"))
        assertEquals(AppStyle.ATELIER, AppStyle.Default)
    }

    @Test
    fun `only Atelier is flat`() {
        assertTrue(AppStyle.ATELIER.isFlat)
        assertFalse(AppStyle.GLASS.isFlat)
    }
}
