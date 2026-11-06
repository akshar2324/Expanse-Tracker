package com.akshar.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Parsing and rounding rules for user-entered money.
 *
 * Amounts are stored as Double, so every value that reaches the database is rounded to
 * [MONEY_SCALE] decimal places with HALF_UP. Input may be a plain number ("1,234.50",
 * "12,50" in a comma-decimal locale) or a simple calculator expression ("120/3+5").
 */
object MoneyUtils {
    const val MONEY_SCALE = 2

    /**
     * The largest amount AkSpend accepts, in any currency. Amounts are stored as Double, which
     * keeps every cent exact only up to about 9e13; this limit leaves room for totals of many
     * such amounts. It is far above any personal transaction, even in currencies like IDR.
     */
    const val MAX_AMOUNT = 99_999_999_999.99

    /** True for a finite value no larger than [MAX_AMOUNT] either way. */
    fun isWithinLimit(value: Double): Boolean = value.isFinite() && kotlin.math.abs(value) <= MAX_AMOUNT

    private val numberToken = Regex("[0-9.,]+")
    private val groupedWithComma = Regex("^(?:\\d{1,3}(,\\d{3})+|\\d{1,2}(,\\d{2})*,\\d{3})$")
    private val groupedWithDot = Regex("^\\d{1,3}(\\.\\d{3})+$")
    private val allowedChars = Regex("[^0-9.,+\\-*/()]")

    /** Rounds a monetary value to two decimals, HALF_UP. Non-finite values are returned unchanged. */
    fun roundMoney(value: Double): Double {
        if (!value.isFinite()) return value
        return BigDecimal(value.toString()).setScale(MONEY_SCALE, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Parses user input into a rounded amount. Returns null for blank, malformed, infinite or
     * NaN results (for example "1/0"), and for anything beyond [MAX_AMOUNT]. The sign is kept, so callers decide whether negative or
     * zero values are acceptable.
     */
    fun parseAmount(input: String, locale: Locale = Locale.getDefault()): Double? {
        val cleaned = input.replace(allowedChars, "")
        if (cleaned.isBlank() || cleaned.none { it.isDigit() }) return null
        val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val normalized = numberToken.replace(cleaned) { normalizeNumber(it.value, decimalSeparator) ?: "\u0000" }
        if (normalized.contains('\u0000')) return null
        val value = MathUtils.evaluateMathExpression(normalized) ?: return null
        if (!value.isFinite()) return null
        val rounded = roundMoney(value)
        return rounded.takeIf { isWithinLimit(it) }
    }

    /** Converts one number token to a dot-decimal string with no grouping, or null if ambiguous garbage. */
    private fun normalizeNumber(token: String, decimalSeparator: Char): String? {
        val hasDot = token.contains('.')
        val hasComma = token.contains(',')
        return when {
            hasDot && hasComma -> {
                // Whichever separator comes last is the decimal one: "1,234.50" or "1.234,50".
                val decimal = if (token.lastIndexOf('.') > token.lastIndexOf(',')) '.' else ','
                val grouping = if (decimal == '.') ',' else '.'
                val parts = token.split(decimal)
                val groupingRule = if (grouping == ',') groupedWithComma else groupedWithDot
                if (parts.size != 2 || !groupingRule.matches(parts[0]) || parts[1].any { !it.isDigit() }) null
                else parts[0].replace(grouping.toString(), "") + "." + parts[1]
            }
            hasComma -> when {
                decimalSeparator == ',' && token.count { it == ',' } == 1 -> token.replace(',', '.')
                groupedWithComma.matches(token) -> token.replace(",", "")
                token.count { it == ',' } == 1 -> token.replace(',', '.')
                else -> null
            }
            hasDot -> when {
                decimalSeparator == ',' && groupedWithDot.matches(token) -> token.replace(".", "")
                token.count { it == '.' } == 1 -> token
                groupedWithDot.matches(token) -> token.replace(".", "")
                else -> null
            }
            else -> token
        }
    }
}
