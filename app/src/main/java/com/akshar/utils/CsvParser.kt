package com.akshar.utils

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.InputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.text.ParseException
import java.text.ParsePosition
import java.security.MessageDigest

data class ParsedCsvRow(
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val category: String,
    val description: String,
    val date: Long,
    val hash: String,
    val rawValues: List<String>
)

data class CsvParseResult(
    val rows: List<ParsedCsvRow>,
    val errors: List<CsvParseError>
)

data class CsvParseError(
    val rowIndex: Int,
    val rawRow: List<String>,
    val errorMessage: String
)

object CsvParser {

    /** Category given to rows whose file has no category value. */
    const val DEFAULT_CATEGORY = "Imported"

    private fun sha256(string: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(string.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Parses [text] with [format] and requires the whole string to be consumed. NumberFormat
     * otherwise stops at the first character it does not understand, so "12,50" in an en_US
     * profile silently became 1250 and "1.234,56" became 1.234.
     */
    internal fun parseNumberStrict(format: NumberFormat, text: String, indianGrouping: Boolean = false): Double {
        val normalized = text.trim().let { if (it.startsWith("+")) it.substring(1) else it }
        // DecimalFormat accepts grouping separators anywhere ("12,50" -> 1250), so require
        // proper groups of three digits whenever the locale's grouping separator appears.
        val symbols = (format as? DecimalFormat)?.decimalFormatSymbols
        if (symbols != null) {
            val group = Regex.escape(symbols.groupingSeparator.toString())
            val decimal = Regex.escape(symbols.decimalSeparator.toString())
            val digits = normalized.removePrefix("-")
            val western = Regex("^\\d{1,3}($group\\d{3})+($decimal\\d+)?$")
            val indian = Regex("^\\d{1,2}($group\\d{2})*$group\\d{3}($decimal\\d+)?$")
            if (digits.contains(symbols.groupingSeparator) &&
                !western.matches(digits) && !(indianGrouping && indian.matches(digits))
            ) {
                throw ParseException("Invalid amount format: $text", 0)
            }
        }
        val position = ParsePosition(0)
        val number = format.parse(normalized, position)
        if (number == null || position.index != normalized.length) {
            throw ParseException("Invalid amount format: $text", position.errorIndex.coerceAtLeast(0))
        }
        val value = number.toDouble()
        if (!value.isFinite()) throw ParseException("Invalid amount format: $text", 0)
        return value
    }

    fun parse(
        inputStream: InputStream,
        delimiter: Char = ',',
        hasHeader: Boolean = true,
        dateColumnName: String,
        dateFormatString: String,
        amountColumnName: String?,
        debitColumnName: String?,
        creditColumnName: String?,
        descriptionColumnName: String,
        categoryColumnName: String?,
        localeString: String,
        invertAmountSigns: Boolean
    ): CsvParseResult {
        val reader = csvReader {
            this.delimiter = delimiter
            this.quoteChar = '"'
        }

        val allRows = reader.readAll(inputStream)
        if (allRows.isEmpty()) {
            return CsvParseResult(emptyList(), emptyList())
        }

        var headerRow = if (hasHeader) allRows.first() else null
        val dataRows = if (hasHeader) allRows.drop(1) else allRows

        val dateColIdx = headerRow?.indexOf(dateColumnName) ?: dateColumnName.toIntOrNull() ?: -1
        val amountColIdx = amountColumnName?.let { headerRow?.indexOf(it) ?: it.toIntOrNull() } ?: -1
        val debitColIdx = debitColumnName?.let { headerRow?.indexOf(it) ?: it.toIntOrNull() } ?: -1
        val creditColIdx = creditColumnName?.let { headerRow?.indexOf(it) ?: it.toIntOrNull() } ?: -1
        val descColIdx = headerRow?.indexOf(descriptionColumnName) ?: descriptionColumnName.toIntOrNull() ?: -1
        val catColIdx = categoryColumnName?.let { headerRow?.indexOf(it) ?: it.toIntOrNull() } ?: -1

        val parsedRows = mutableListOf<ParsedCsvRow>()
        val errors = mutableListOf<CsvParseError>()

        val localeParts = localeString.split("_", "-").filter { it.isNotBlank() }
        val locale = when (localeParts.size) {
            0 -> Locale.getDefault()
            1 -> Locale(localeParts[0])
            else -> Locale(localeParts[0], localeParts[1])
        }
        val numberFormat = NumberFormat.getInstance(locale)
        val dateFormat = SimpleDateFormat(dateFormatString, Locale.getDefault())
        dateFormat.isLenient = false

        dataRows.forEachIndexed { index, row ->
            val rowIndex = if (hasHeader) index + 1 else index

            if (row.isEmpty() || row.all { it.isBlank() }) {
                return@forEachIndexed // Skip empty rows
            }

            try {
                if (dateColIdx !in row.indices) throw IllegalArgumentException("Date column index out of bounds")
                if (descColIdx !in row.indices) throw IllegalArgumentException("Description column index out of bounds")

                val dateStr = row[dateColIdx].trim()
                val datePosition = ParsePosition(0)
                val date = dateFormat.parse(dateStr, datePosition)?.time
                    ?.takeIf { datePosition.index == dateStr.length }
                    ?: throw ParseException("Invalid date format", 0)

                val desc = row[descColIdx].trim()
                val category = if (catColIdx in row.indices) row[catColIdx].trim().takeIf { it.isNotEmpty() } ?: DEFAULT_CATEGORY else DEFAULT_CATEGORY

                var finalAmount = 0.0
                var type = "EXPENSE"

                if (amountColIdx in row.indices) {
                    val amountStr = row[amountColIdx].trim().replace(Regex("[^\\d.,+-]"), "")
                    if (amountStr.isEmpty()) throw IllegalArgumentException("Empty amount")
                    val parsedNum = parseNumberStrict(numberFormat, amountStr, locale.country == "IN")
                    finalAmount = parsedNum
                    if (invertAmountSigns) finalAmount = -finalAmount

                    if (finalAmount < 0) {
                        type = "EXPENSE"
                        finalAmount = kotlin.math.abs(finalAmount)
                    } else {
                        type = "INCOME"
                    }
                } else if (debitColIdx in row.indices || creditColIdx in row.indices) {
                    val debitStr = if (debitColIdx in row.indices) row[debitColIdx].trim().replace(Regex("[^\\d.,+-]"), "") else ""
                    val creditStr = if (creditColIdx in row.indices) row[creditColIdx].trim().replace(Regex("[^\\d.,+-]"), "") else ""

                    val debitAmt = if (debitStr.isNotEmpty()) parseNumberStrict(numberFormat, debitStr, locale.country == "IN") else 0.0
                    val creditAmt = if (creditStr.isNotEmpty()) parseNumberStrict(numberFormat, creditStr, locale.country == "IN") else 0.0

                    if (debitAmt < 0 || creditAmt < 0 || (debitAmt > 0 && creditAmt > 0)) {
                        throw IllegalArgumentException("Expected one positive debit or credit")
                    }
                    if (debitAmt > 0) {
                        finalAmount = debitAmt
                        type = "EXPENSE"
                    } else if (creditAmt > 0) {
                        finalAmount = creditAmt
                        type = "INCOME"
                    } else {
                        throw IllegalArgumentException("Both debit and credit are empty or zero")
                    }
                } else {
                     throw IllegalArgumentException("Amount column(s) not specified correctly")
                }

                finalAmount = MoneyUtils.roundMoney(finalAmount)
                if (finalAmount == 0.0) {
                    throw IllegalArgumentException("Amount cannot be zero")
                }

                val rowHash = sha256(row.joinToString(","))

                parsedRows.add(
                    ParsedCsvRow(
                        amount = finalAmount,
                        type = type,
                        category = category,
                        description = desc,
                        date = date,
                        hash = rowHash,
                        rawValues = row
                    )
                )

            } catch (e: Exception) {
                errors.add(CsvParseError(rowIndex, row, e.message ?: "Unknown parsing error"))
            }
        }

        return CsvParseResult(parsedRows, errors)
    }
}
