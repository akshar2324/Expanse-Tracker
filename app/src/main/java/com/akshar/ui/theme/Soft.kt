package com.akshar.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

// AkSpend "Soft": neumorphism. Every surface is the page colour, lifted by a light highlight on
// the top-left and a shade on the bottom-right, or pressed in with the same pair reversed.
// Colour is kept for meaning: one accent for actions, green and red for money in and out.
//
// Every palette, preset or custom, goes through [SoftColors.guarded], which holds body text,
// muted text, the accent and income/expense at WCAG AA (4.5:1) on both the page and the
// pressed-in track colour, and keeps the label on filled accent buttons at 4.5:1 too.

/** The colours one Soft palette needs, for one of light or dark. */
@Immutable
data class SoftColors(
    val background: Color,
    /** Lit edge of a raised shape (top-left). */
    val highlight: Color,
    /** Shaded edge of a raised shape (bottom-right). */
    val shade: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val income: Color,
    val expense: Color,
    /** Fill of pressed-in wells: text fields, progress tracks, switch tracks. */
    val track: Color,
    val isDark: Boolean,
) {
    /** Returns a copy whose text colours all meet AA contrast. Idempotent. */
    fun guarded(): SoftColors {
        val grounds = listOf(background, track)
        val accentSafe = SoftColorMath.ensureContrast(accent, grounds, isDark)
        val label = SoftColorMath.bestLabel(accentSafe)
        val accentForLabel = SoftColorMath.ensureLabelContrast(accentSafe, label)
        return copy(
            text = SoftColorMath.ensureContrast(text, grounds, isDark),
            muted = SoftColorMath.ensureContrast(muted, grounds, isDark),
            accent = accentForLabel,
            onAccent = SoftColorMath.bestLabel(accentForLabel),
            income = SoftColorMath.ensureContrast(income, grounds, isDark),
            expense = SoftColorMath.ensureContrast(expense, grounds, isDark),
        )
    }
}

/** Built-in palettes. [CUSTOM] is generated from the user's base and accent hues. */
enum class SoftPalette(val key: String, val label: String) {
    LAVENDER("lavender", "Lavender"),
    MIST("mist", "Mist"),
    SAND("sand", "Sand"),
    CUSTOM("custom", "Your colours");

    companion object {
        val Default = LAVENDER
        fun fromKey(key: String?): SoftPalette = entries.firstOrNull { it.key == key } ?: Default
    }
}

object SoftPalettes {
    private val LavenderLight = SoftColors(
        background = Color(0xFFE6E3EF), highlight = Color(0xFFFFFFFF), shade = Color(0xFFB9B3CF),
        text = Color(0xFF26243A), muted = Color(0xFF5D5A73), accent = Color(0xFF6E56CF), onAccent = Color.White,
        income = Color(0xFF2E7D4F), expense = Color(0xFFB9452A), track = Color(0xFFD3CFE0), isDark = false,
    )
    private val LavenderDark = SoftColors(
        background = Color(0xFF2A2D34), highlight = Color(0xFF363A43), shade = Color(0xFF1C1E23),
        text = Color(0xFFECEAF3), muted = Color(0xFFA9A6BA), accent = Color(0xFFA796F2), onAccent = Color(0xFF1B1531),
        income = Color(0xFF7CCB98), expense = Color(0xFFF09A80), track = Color(0xFF23262C), isDark = true,
    )
    private val MistLight = SoftColors(
        background = Color(0xFFE2E7EE), highlight = Color(0xFFFFFFFF), shade = Color(0xFFB3BECD),
        text = Color(0xFF1F2633), muted = Color(0xFF566173), accent = Color(0xFF3F63E0), onAccent = Color.White,
        income = Color(0xFF23784B), expense = Color(0xFFB5432A), track = Color(0xFFCCD4DF), isDark = false,
    )
    private val MistDark = SoftColors(
        background = Color(0xFF262B33), highlight = Color(0xFF323843), shade = Color(0xFF191C21),
        text = Color(0xFFE9EDF3), muted = Color(0xFFA3ACBA), accent = Color(0xFF86A2FF), onAccent = Color(0xFF101A3A),
        income = Color(0xFF79C99A), expense = Color(0xFFF2987E), track = Color(0xFF20242B), isDark = true,
    )
    private val SandLight = SoftColors(
        background = Color(0xFFEBE6DF), highlight = Color(0xFFFFFFFF), shade = Color(0xFFC5BBAE),
        text = Color(0xFF2B2722), muted = Color(0xFF655D53), accent = Color(0xFF2F4E7A), onAccent = Color.White,
        income = Color(0xFF2D7650), expense = Color(0xFFB0442B), track = Color(0xFFDAD3C9), isDark = false,
    )
    private val SandDark = SoftColors(
        background = Color(0xFF2B2926), highlight = Color(0xFF373430), shade = Color(0xFF1D1B19),
        text = Color(0xFFEFEAE3), muted = Color(0xFFADA59A), accent = Color(0xFF9DB8E6), onAccent = Color(0xFF14223A),
        income = Color(0xFF7FC79B), expense = Color(0xFFEF9B82), track = Color(0xFF23211F), isDark = true,
    )

    /** Default hues for "Your colours" before the user changes them (a calm lavender). */
    const val DEFAULT_BASE_HUE = 252f
    const val DEFAULT_ACCENT_HUE = 255f

    fun resolve(palette: SoftPalette, dark: Boolean, baseHue: Float, accentHue: Float): SoftColors = when (palette) {
        SoftPalette.LAVENDER -> if (dark) LavenderDark else LavenderLight
        SoftPalette.MIST -> if (dark) MistDark else MistLight
        SoftPalette.SAND -> if (dark) SandDark else SandLight
        SoftPalette.CUSTOM -> custom(baseHue, accentHue, dark)
    }.guarded()

    /**
     * Builds a palette from two hues. The base is kept light (or dark) and desaturated so raised
     * shapes still read; the accent is moved out of the income green and expense red bands.
     */
    fun custom(baseHue: Float, accentHue: Float, dark: Boolean): SoftColors {
        val h = SoftColorMath.normalizeHue(baseHue)
        val a = SoftColorMath.safeAccentHue(accentHue)
        return if (dark) SoftColors(
            background = Color.hsl(h, 0.11f, 0.185f),
            highlight = Color.hsl(h, 0.11f, 0.24f),
            shade = Color.hsl(h, 0.12f, 0.12f),
            text = Color.hsl(h, 0.22f, 0.93f),
            muted = Color.hsl(h, 0.10f, 0.70f),
            accent = Color.hsl(a, 0.78f, 0.77f),
            onAccent = Color.hsl(a, 0.45f, 0.13f),
            income = Color(0xFF7CCB98),
            expense = Color(0xFFF09A80),
            track = Color.hsl(h, 0.11f, 0.155f),
            isDark = true,
        ).guarded() else SoftColors(
            background = Color.hsl(h, 0.20f, 0.91f),
            highlight = Color.White,
            shade = Color.hsl(h, 0.20f, 0.76f),
            text = Color.hsl(h, 0.24f, 0.17f),
            muted = Color.hsl(h, 0.12f, 0.40f),
            accent = Color.hsl(a, 0.58f, 0.46f),
            onAccent = Color.White,
            income = Color(0xFF2E7D4F),
            expense = Color(0xFFB9452A),
            track = Color.hsl(h, 0.18f, 0.85f),
            isDark = false,
        ).guarded()
    }
}

/** Contrast and hue helpers for Soft palettes. Pure functions so they can be unit tested. */
object SoftColorMath {
    const val MIN_TEXT_CONTRAST = 4.5f

    // Accent hues inside these bands would read as income (green) or expense (red).
    private const val GREEN_START = 85f
    private const val GREEN_END = 165f
    private const val RED_START = 340f
    private const val RED_END = 30f

    fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
    }

    fun normalizeHue(hue: Float): Float = ((hue % 360f) + 360f) % 360f

    /** True when [hue] is too close to the income green or expense red to be the accent. */
    fun isReservedHue(hue: Float): Boolean {
        val h = normalizeHue(hue)
        return h in GREEN_START..GREEN_END || h >= RED_START || h <= RED_END
    }

    /** Moves a reserved accent hue to the nearest edge of its band; other hues pass unchanged. */
    fun safeAccentHue(hue: Float): Float {
        val h = normalizeHue(hue)
        return when {
            h in GREEN_START..GREEN_END -> if (h - GREEN_START <= GREEN_END - h) GREEN_START - 1f else GREEN_END + 1f
            h >= RED_START -> RED_START - 1f
            h <= RED_END -> RED_END + 1f
            else -> h
        }
    }

    /**
     * Darkens (light mode) or lightens (dark mode) [color] in small steps until it reaches
     * [MIN_TEXT_CONTRAST] on every colour in [grounds].
     */
    fun ensureContrast(color: Color, grounds: List<Color>, dark: Boolean): Color {
        val target = if (dark) Color.White else Color.Black
        var step = 0
        var current = color
        while (grounds.any { contrast(current, it) < MIN_TEXT_CONTRAST } && step < 40) {
            step++
            current = lerp(color, target, step / 40f)
        }
        return current
    }

    /** Whichever of white or near-black reads better on [fill]. */
    fun bestLabel(fill: Color): Color {
        val white = Color.White
        val ink = Color(0xFF14121C)
        return if (contrast(white, fill) >= contrast(ink, fill)) white else ink
    }

    /** Nudges [fill] away from [label] until the label reads at AA on it. */
    fun ensureLabelContrast(fill: Color, label: Color): Color {
        val target = if (label.luminance() > 0.5f) Color.Black else Color.White
        var step = 0
        var current = fill
        while (contrast(label, current) < MIN_TEXT_CONTRAST && step < 40) {
            step++
            current = lerp(fill, target, step / 40f)
        }
        return current
    }
}

internal fun SoftColors.colorScheme(): ColorScheme {
    val container = lerp(background, accent, if (isDark) 0.22f else 0.14f)
    val errorContainer = lerp(background, expense, if (isDark) 0.22f else 0.14f)
    val outline = lerp(muted, background, 0.25f)
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = text,
        inversePrimary = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = container,
        onSecondaryContainer = text,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = container,
        onTertiaryContainer = text,
        background = Color.Transparent,
        onBackground = text,
        surface = background,
        onSurface = text,
        surfaceVariant = track,
        onSurfaceVariant = muted,
        surfaceTint = Color.Transparent,
        inverseSurface = text,
        inverseOnSurface = background,
        error = expense,
        onError = SoftColorMath.bestLabel(expense),
        errorContainer = errorContainer,
        onErrorContainer = text,
        outline = outline,
        outlineVariant = shade,
        scrim = Color.Black,
        surfaceBright = background,
        surfaceDim = track,
        surfaceContainerLowest = background,
        surfaceContainerLow = background,
        surfaceContainer = background,
        surfaceContainerHigh = background,
        surfaceContainerHighest = background,
    )
}

internal fun SoftColors.finance(): FinanceColors {
    val base = if (isDark) DarkFinanceColors else LightFinanceColors
    return base.copy(
        income = income,
        expense = expense,
        warning = SoftColorMath.ensureContrast(base.warning, listOf(background, track), isDark),
        info = accent,
        transfer = accent,
    )
}

internal fun SoftColors.surfaces(): GlassColors = GlassColors(
    base = background,
    baseBottom = background,
    blobs = listOf(background, background, background),
    fill = background,
    fillOpaque = background,
    fillStrong = background,
    highlight = highlight,
    edge = shade,
    sheen = Color.Transparent,
    shadow = shade,
    soft = this,
)
