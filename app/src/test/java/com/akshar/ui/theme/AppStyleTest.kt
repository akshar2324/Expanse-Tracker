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
    fun `only glass uses blur and colour pools`() {
        assertTrue(AppStyle.ATELIER.isFlat)
        assertTrue(AppStyle.SOFT.isFlat)
        assertFalse(AppStyle.GLASS.isFlat)
    }

    @Test
    fun `soft palette keys round trip and unknown keys fall back to Lavender`() {
        SoftPalette.entries.forEach { assertEquals(it, SoftPalette.fromKey(it.key)) }
        assertEquals(SoftPalette.LAVENDER, SoftPalette.fromKey(null))
        assertEquals(SoftPalette.LAVENDER, SoftPalette.fromKey("neon"))
    }
}
