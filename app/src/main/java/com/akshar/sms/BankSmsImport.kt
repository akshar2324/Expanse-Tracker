package com.akshar.sms

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.txnparse.Channel
import io.github.txnparse.Confidence
import io.github.txnparse.ParseResult
import io.github.txnparse.TransactionParser
import io.github.txnparse.TransactionType
import java.math.RoundingMode
import java.time.ZoneId

/**
 * A transaction read from a bank message, ready to prefill the add-transaction form.
 * Nothing is saved until the user reviews it there.
 */
data class SmsDraft(
    /** Plain decimal string with two places, e.g. "250.00". Always greater than zero. */
    val amount: String,
    val isExpense: Boolean,
    val description: String,
    /** Start of the day printed in the message, or the received time when it has none. */
    val dateMillis: Long,
    val category: String,
    /** A payment method name, or null to keep the form's default. */
    val paymentMethod: String?,
    /** False when the generic fallback matched rather than a bank-specific rule. */
    val confident: Boolean,
    val bankId: String? = null,
    val accountTail: String? = null,
)

sealed interface SmsImportResult {
    data class Found(val draft: SmsDraft) : SmsImportResult

    /** Bank-related, but no money moved (OTP, reminder, failed payment...). */
    data class Skipped(val reason: String) : SmsImportResult

    data object NotFound : SmsImportResult

    /** The parser needs java.time and regex named groups, which arrived in Android 8.0. */
    data object Unsupported : SmsImportResult
}

object BankSmsImport {

    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    // Compiling the bundled rules takes a few milliseconds; do it once, off the main thread.
    private val parser by lazy { TransactionParser.withBundledRules() }

    /**
     * Reads [text] (one bank SMS or notification) into a draft. Runs the regex rules, so call it
     * from a background dispatcher.
     */
    fun parse(
        text: String,
        sender: String? = null,
        receivedAtMillis: Long = System.currentTimeMillis(),
        zone: ZoneId? = null, // null: the device's time zone
        supported: Boolean = isSupported,
    ): SmsImportResult {
        if (!supported) return SmsImportResult.Unsupported
        if (text.isBlank()) return SmsImportResult.NotFound
        // `supported` is false below Android 8.0 (and tests pass it explicitly), so the java.time
        // and named-group regex code in the parser never runs where it is missing.
        @SuppressLint("NewApi")
        val result = parseSupported(text, sender, receivedAtMillis, zone ?: ZoneId.systemDefault())
        return result
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseSupported(text: String, sender: String?, receivedAtMillis: Long, zone: ZoneId): SmsImportResult {
        return when (val result = parser.parse(body = text, sender = sender?.takeIf { it.isNotBlank() })) {
            is ParseResult.Ignored -> SmsImportResult.Skipped(skipReason(result.reason))
            ParseResult.NotATransaction -> SmsImportResult.NotFound
            is ParseResult.Parsed -> {
                val txn = result.transaction
                val amount = txn.amount.setScale(2, RoundingMode.HALF_UP)
                if (amount.signum() <= 0) return SmsImportResult.NotFound
                val isExpense = txn.type == TransactionType.DEBIT
                val merchant = txn.merchant?.trim()?.takeIf { it.isNotEmpty() }
                SmsImportResult.Found(
                    SmsDraft(
                        amount = amount.toPlainString(),
                        isExpense = isExpense,
                        description = describe(merchant, txn.channel, isExpense),
                        dateMillis = txn.date?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: receivedAtMillis,
                        category = guessCategory(merchant, isExpense),
                        paymentMethod = paymentMethodFor(txn.channel),
                        confident = txn.confidence == Confidence.HIGH,
                        bankId = txn.bankId,
                        accountTail = txn.accountTail,
                    )
                )
            }
        }
    }

    internal fun describe(merchant: String?, channel: Channel?, isExpense: Boolean): String = when {
        merchant != null -> merchant
        channel == Channel.ATM -> "ATM withdrawal"
        channel != null -> "${channel.name} ${if (isExpense) "payment" else "credit"}"
        else -> if (isExpense) "Bank debit" else "Bank credit"
    }

    internal fun paymentMethodFor(channel: Channel?): String? = when (channel) {
        Channel.UPI -> "UPI"
        Channel.CARD -> "Debit Card"
        Channel.ATM -> "Cash"
        Channel.NEFT, Channel.IMPS, Channel.RTGS -> "Bank Transfer"
        null -> null
    }

    // Keyword hints only; the user confirms the category on the next screen. Every value must be
    // one of the add-transaction screen's categories.
    private val expenseHints = listOf(
        "Food" to listOf("swiggy", "zomato", "domino", "pizza", "cafe", "restaurant", "mcdonald", "kfc", "starbucks"),
        "Grocery" to listOf("blinkit", "zepto", "bigbasket", "dmart", "grofers", "instamart", "grocery"),
        "Transportation" to listOf("uber", "ola", "rapido", "metro", "irctc", "redbus", "fastag"),
        "Fuel" to listOf("petrol", "fuel", "hpcl", "bpcl", "indian oil", "iocl", "shell"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa"),
        "Subscription" to listOf("netflix", "spotify", "hotstar", "prime video", "youtube", "apple.com"),
        "Bills" to listOf("electricity", "airtel", "jio", "vodafone", "bsnl", "broadband", "gas bill", "recharge"),
        "Health" to listOf("pharmacy", "apollo", "medplus", "hospital", "1mg", "pharmeasy"),
        "Travel" to listOf("makemytrip", "goibibo", "indigo", "air india", "oyo", "airbnb"),
    )
    private val incomeHints = listOf(
        "Salary" to listOf("salary", "payroll"),
        "Interest" to listOf("interest"),
        "Refund" to listOf("refund", "reversal", "cashback"),
    )

    internal fun guessCategory(merchant: String?, isExpense: Boolean): String {
        val name = merchant?.lowercase().orEmpty()
        val hints = if (isExpense) expenseHints else incomeHints
        return hints.firstOrNull { (_, words) -> words.any { name.contains(it) } }?.first ?: "Other"
    }

    private fun skipReason(code: String): String = when (code) {
        "otp" -> "This looks like a one-time password, not a payment."
        "due" -> "This looks like a bill or payment reminder. Nothing was paid yet."
        "failed" -> "This payment failed or was declined, so no money moved."
        "request" -> "This is a payment request. Nothing is paid until you approve it."
        "scheduled" -> "This is a scheduled or upcoming payment, not a completed one."
        "card_bill_payment" -> "This is a credit card bill payment. Your card spends are already counted."
        else -> "This message doesn't record a completed payment."
    }
}

