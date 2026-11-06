package com.akshar.utils

import com.akshar.data.model.MerchantCategory
import java.util.Locale

/**
 * Remembers which category the user picks for a merchant, so the next payment to the same place
 * is filled in for them. Mappings are learned from saved transactions and stay on the device.
 */
object MerchantNames {

    /** Words that describe how money moved, not who it went to. */
    private val noiseWords = setOf(
        "upi", "pos", "ref", "txn", "imps", "neft", "rtgs", "ach", "nach", "ecom", "vpa",
        "payment", "paid", "to", "from", "via", "by", "at", "on", "the",
        "pvt", "ltd", "private", "limited", "llp", "inc",
    )

    /** Descriptions made only of these words are generic ("UPI payment", "Bank debit"), not merchants. */
    private val genericWords = setOf(
        "upi", "card", "neft", "imps", "rtgs", "atm", "payment", "credit", "debit", "bank",
        "withdrawal", "transfer", "cash", "deposit", "imported", "transaction", "purchase",
    )

    /** At most this many words: longer text is a note ("dinner with the team after..."), not a name. */
    private const val MAX_WORDS = 5
    private const val MAX_LENGTH = 48

    /**
     * Folds a description into a lookup key: lowercase letters and digits, punctuation and pure
     * numbers dropped (order ids, last four digits), payment noise removed. "SWIGGY*Order 81723"
     * and "Swiggy" both become "swiggy". Returns null when nothing merchant-like is left.
     */
    fun normalise(description: String?): String? {
        if (description.isNullOrBlank()) return null
        val lower = description.lowercase(Locale.ROOT)
        // A UPI id names the payee before the @ ("swiggy@icici"); keep just that part.
        val words = Regex("[\\p{L}\\p{N}@._-]+").findAll(lower).map { it.value }
            .map { token -> if ('@' in token) token.substringBefore('@') else token }
            .flatMap { it.split('.', '_', '-').asSequence() }
            .filter { it.isNotEmpty() && it.any(Char::isLetter) }
            .filter { it !in noiseWords }
            .toList()
        if (words.isEmpty() || words.size > MAX_WORDS) return null
        if (words.all { it in genericWords }) return null
        val key = words.joinToString(" ")
        return key.takeIf { it.length in 2..MAX_LENGTH }
    }

    /** Categories that mean "not chosen", so saving them teaches nothing. */
    fun isLearnable(category: String): Boolean =
        category.isNotBlank() && category !in setOf("Other", "Imported", "Transfer")
}

/** A read-only snapshot of the learned mappings, cheap to query many times (e.g. per CSV row). */
class CategoryMemory(mappings: List<MerchantCategory>) {

    private val byType: Map<String, List<MerchantCategory>> =
        mappings.groupBy { it.type }.mapValues { (_, list) -> list.sortedByDescending { it.merchantKey.length } }

    /**
     * The learned category for [description], or null. An exact key wins; otherwise the longest
     * learned name found as whole words inside it, so "swiggy" also covers "swiggy instamart order".
     */
    fun lookup(description: String?, type: String): String? {
        val key = MerchantNames.normalise(description) ?: return null
        val candidates = byType[type] ?: return null
        candidates.firstOrNull { it.merchantKey == key }?.let { return it.category }
        val padded = " $key "
        return candidates.firstOrNull { padded.contains(" ${it.merchantKey} ") }?.category
    }

    fun lookup(description: String?, isExpense: Boolean): String? =
        lookup(description, if (isExpense) "EXPENSE" else "INCOME")

    val isEmpty: Boolean get() = byType.isEmpty()

    companion object {
        val EMPTY = CategoryMemory(emptyList())
    }
}
