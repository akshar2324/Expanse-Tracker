package com.akshar.ui.components

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Formats an amount for display with an explicit sign, e.g. "+₹1,250" or "-₹80.50".
 *
 * A value that rounds to zero at [decimals] places is shown without a sign ("₹0"),
 * because "+₹0" and "-₹0" read like there was movement when there wasn't.
 *
 * @param amount magnitude or signed value; only its absolute value is printed.
 * @param positive whether the sign should be "+" (income, gains) or "-" (spending).
 */
fun signedMoney(amount: Double, positive: Boolean, currencySymbol: String, decimals: Int = 0): String {
    val magnitude = BigDecimal.valueOf(kotlin.math.abs(amount)).setScale(decimals, RoundingMode.HALF_UP)
    val body = "$currencySymbol${String.format("%,.${decimals}f", magnitude)}"
    return when {
        magnitude.signum() == 0 -> body
        positive -> "+$body"
        else -> "-$body"
    }
}

/** [signedMoney] where the sign follows the value itself (net figures). */
fun netMoney(net: Double, currencySymbol: String, decimals: Int = 0): String =
    signedMoney(net, positive = net >= 0, currencySymbol = currencySymbol, decimals = decimals)
