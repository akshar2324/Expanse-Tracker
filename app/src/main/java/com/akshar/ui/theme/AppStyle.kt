package com.akshar.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A complete visual style for the app. Each style supplies its own colours, typography, shapes
 * and panel treatment; screens only use the shared components and tokens, so a style can be
 * swapped at runtime without touching screen code.
 */
enum class AppStyle(val key: String, val label: String, val description: String) {
    SOFT(
        key = "soft",
        label = "Soft",
        description = "Raised and pressed shapes in calm colour",
    ),
    ATELIER(
        key = "atelier",
        label = "Atelier",
        description = "Ivory and ink, serif numbers, hairline details",
    ),
    GLASS(
        key = "glass",
        label = "Liquid glass",
        description = "Frosted panels over soft colour",
    );

    /** Flat styles draw a plain background and solid bars instead of blur and colour pools. */
    val isFlat: Boolean get() = this != GLASS

    companion object {
        val Default = SOFT
        fun fromKey(key: String?): AppStyle = entries.firstOrNull { it.key == key } ?: Default
    }
}

/** Whether the app follows the phone's light/dark setting or is fixed to one. */
enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "Match phone"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        val Default = SYSTEM
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: Default
    }
}

/**
 * The user's chosen style, kept on the device in SharedPreferences. Backed by Compose state so
 * changing it in More › Appearance re-themes the running app straight away.
 */
object Appearance {
    private const val PREFS = "appearance_settings"
    private const val KEY_STYLE = "style"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_SOFT_PALETTE = "soft_palette"
    private const val KEY_SOFT_BASE_HUE = "soft_base_hue"
    private const val KEY_SOFT_ACCENT_HUE = "soft_accent_hue"
    private var loaded = false

    var style: AppStyle by mutableStateOf(AppStyle.Default)
        private set

    /** Light, dark, or following the phone. */
    var themeMode: ThemeMode by mutableStateOf(ThemeMode.Default)
        private set

    /** Which Soft palette is used when [style] is [AppStyle.SOFT]. */
    var softPalette: SoftPalette by mutableStateOf(SoftPalette.Default)
        private set

    /** Hues for [SoftPalette.CUSTOM], 0 to 360. */
    var softBaseHue: Float by mutableStateOf(SoftPalettes.DEFAULT_BASE_HUE)
        private set
    var softAccentHue: Float by mutableStateOf(SoftPalettes.DEFAULT_ACCENT_HUE)
        private set

    fun load(context: Context) {
        if (loaded) return
        val p = prefs(context)
        style = AppStyle.fromKey(p.getString(KEY_STYLE, null))
        themeMode = ThemeMode.fromKey(p.getString(KEY_THEME_MODE, null))
        softPalette = SoftPalette.fromKey(p.getString(KEY_SOFT_PALETTE, null))
        softBaseHue = SoftColorMath.normalizeHue(p.getFloat(KEY_SOFT_BASE_HUE, SoftPalettes.DEFAULT_BASE_HUE))
        softAccentHue = SoftColorMath.safeAccentHue(p.getFloat(KEY_SOFT_ACCENT_HUE, SoftPalettes.DEFAULT_ACCENT_HUE))
        loaded = true
    }

    fun selectThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        prefs(context).edit().putString(KEY_THEME_MODE, mode.key).apply()
    }

    fun selectSoftPalette(context: Context, palette: SoftPalette) {
        softPalette = palette
        prefs(context).edit().putString(KEY_SOFT_PALETTE, palette.key).apply()
    }

    /** Updates the custom hues on screen only, while a slider is being dragged. */
    fun previewSoftHues(baseHue: Float, accentHue: Float) {
        softBaseHue = SoftColorMath.normalizeHue(baseHue)
        softAccentHue = SoftColorMath.safeAccentHue(accentHue)
    }

    /**
     * Sets the custom hues. The accent is moved out of the income/expense colour bands before it
     * is stored, so what is saved is what is shown.
     */
    fun setSoftHues(context: Context, baseHue: Float, accentHue: Float) {
        softBaseHue = SoftColorMath.normalizeHue(baseHue)
        softAccentHue = SoftColorMath.safeAccentHue(accentHue)
        prefs(context).edit()
            .putFloat(KEY_SOFT_BASE_HUE, softBaseHue)
            .putFloat(KEY_SOFT_ACCENT_HUE, softAccentHue)
            .apply()
    }

    fun select(context: Context, newStyle: AppStyle) {
        style = newStyle
        loaded = true
        prefs(context).edit().putString(KEY_STYLE, newStyle.key).apply()
    }

    /** The Soft palette currently chosen, for places outside Compose such as widgets. */
    fun softColors(dark: Boolean): SoftColors = SoftPalettes.resolve(softPalette, dark, softBaseHue, softAccentHue)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
