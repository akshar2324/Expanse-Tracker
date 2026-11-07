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
    val isFlat: Boolean get() = this == ATELIER

    companion object {
        val Default = ATELIER
        fun fromKey(key: String?): AppStyle = entries.firstOrNull { it.key == key } ?: Default
    }
}

/**
 * The user's chosen style, kept on the device in SharedPreferences. Backed by Compose state so
 * changing it in More › Appearance re-themes the running app straight away.
 */
object Appearance {
    private const val PREFS = "appearance_settings"
    private const val KEY_STYLE = "style"
    private var loaded = false

    var style: AppStyle by mutableStateOf(AppStyle.Default)
        private set

    fun load(context: Context) {
        if (loaded) return
        style = AppStyle.fromKey(prefs(context).getString(KEY_STYLE, null))
        loaded = true
    }

    fun select(context: Context, newStyle: AppStyle) {
        style = newStyle
        loaded = true
        prefs(context).edit().putString(KEY_STYLE, newStyle.key).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
