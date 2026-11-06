package com.akshar.utils

import android.content.Context

/** The region setting and the currency symbol it implies, shared by the app and its widgets. */
object CurrencyUtils {
    const val PREFS = "country_settings"
    const val KEY_COUNTRY = "selected_country"
    const val DEFAULT_COUNTRY = "IN"

    fun symbolFor(countryCode: String?): String = when (countryCode) {
        "IN" -> "₹"
        "US" -> "$"
        "JP" -> "¥"
        "EU" -> "€"
        else -> "$"
    }

    fun selectedCountry(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_COUNTRY, DEFAULT_COUNTRY) ?: DEFAULT_COUNTRY
}
