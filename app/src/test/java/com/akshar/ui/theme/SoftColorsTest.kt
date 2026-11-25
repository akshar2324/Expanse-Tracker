package com.akshar.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftColorsTest {

    private fun allPalettes(): List<Pair<String, SoftColors>> {
        val presets = SoftPalette.entries.filter { it != SoftPalette.CUSTOM }.flatMap { p ->
            listOf(false, true).map { dark -> "${p.key}/${if (dark) "dark" else "light"}" to SoftPalettes.resolve(p, dark, 0f, 0f) }
        }
        // Every base and accent hue in 15° steps, light and dark: the full custom space.
        val custom = (0 until 360 step 15).flatMap { base ->
            (0 until 360 step 15).flatMap { accent ->
                listOf(false, true).map { dark ->
                    "custom $base/$accent/${if (dark) "dark" else "light"}" to
                        SoftPalettes.resolve(SoftPalette.CUSTOM, dark, base.toFloat(), accent.toFloat())
                }
            }
        }
        return presets + custom
    }

    private fun assertAA(name: String, what: String, fg: Color, bg: Color) {
        val ratio = SoftColorMath.contrast(fg, bg)
        assertTrue("$name: $what contrast $ratio < 4.5", ratio >= SoftColorMath.MIN_TEXT_CONTRAST)
    }

    @Test
    fun `every palette keeps text, accent and money colours at AA on page and wells`() {
        allPalettes().forEach { (name, c) ->
            listOf(c.background, c.track).forEach { ground ->
                assertAA(name, "text", c.text, ground)
                assertAA(name, "muted", c.muted, ground)
                assertAA(name, "accent", c.accent, ground)
                assertAA(name, "income", c.income, ground)
                assertAA(name, "expense", c.expense, ground)
            }
            assertAA(name, "label on accent", c.onAccent, c.accent)
        }
    }

    @Test
    fun `raised shapes have a lighter highlight and darker shade than the page`() {
        allPalettes().forEach { (name, c) ->
            assertTrue("$name highlight", SoftColorMath.contrast(c.highlight, Color.Black) > SoftColorMath.contrast(c.background, Color.Black))
            assertTrue("$name shade", SoftColorMath.contrast(c.shade, Color.Black) < SoftColorMath.contrast(c.background, Color.Black))
        }
    }

    @Test
    fun `accent hues near income green or expense red are moved out of the band`() {
        (0 until 360).forEach { h ->
            val safe = SoftColorMath.safeAccentHue(h.toFloat())
            assertFalse("hue $h → $safe", SoftColorMath.isReservedHue(safe))
        }
        assertTrue(SoftColorMath.isReservedHue(120f))
        assertTrue(SoftColorMath.isReservedHue(5f))
        assertTrue(SoftColorMath.isReservedHue(350f))
        assertEquals(255f, SoftColorMath.safeAccentHue(255f), 0f)
        assertEquals(84f, SoftColorMath.safeAccentHue(90f), 0f)
        assertEquals(166f, SoftColorMath.safeAccentHue(160f), 0f)
        assertEquals(31f, SoftColorMath.safeAccentHue(10f), 0f)
        assertEquals(339f, SoftColorMath.safeAccentHue(345f), 0f)
    }

    @Test
    fun `hues wrap into 0 to 360`() {
        assertEquals(10f, SoftColorMath.normalizeHue(370f), 0.001f)
        assertEquals(350f, SoftColorMath.normalizeHue(-10f), 0.001f)
        assertEquals(0f, SoftColorMath.normalizeHue(360f), 0.001f)
    }

    @Test
    fun `guarding is idempotent`() {
        allPalettes().take(40).forEach { (name, c) -> assertEquals(name, c, c.guarded()) }
    }

    @Test
    fun `offered accent swatches are never reserved`() {
        com.akshar.ui.screens.SoftAccentSwatches.forEach { assertFalse("swatch $it", SoftColorMath.isReservedHue(it)) }
    }

    @Test
    fun `scheme maps soft colours onto Material roles`() {
        val c = SoftPalettes.resolve(SoftPalette.MIST, dark = false, 0f, 0f)
        val scheme = c.colorScheme()
        assertEquals(c.accent, scheme.primary)
        assertEquals(c.onAccent, scheme.onPrimary)
        assertEquals(c.background, scheme.surface)
        assertEquals(c.background, scheme.surfaceContainerHigh)
        assertEquals(c.text, scheme.onSurface)
        assertEquals(Color.Transparent, scheme.background)
        val finance = c.finance()
        assertEquals(c.income, finance.income)
        assertEquals(c.expense, finance.expense)
        assertEquals(c, c.surfaces().soft)
    }
}
