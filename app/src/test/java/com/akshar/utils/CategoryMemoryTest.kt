package com.akshar.utils

import com.akshar.data.model.MerchantCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryMemoryTest {

    @Test
    fun normalise_foldsCaseNumbersAndPaymentNoise() {
        assertEquals("swiggy", MerchantNames.normalise("SWIGGY"))
        assertEquals("swiggy order", MerchantNames.normalise("Swiggy*Order 81723"))
        assertEquals("kumar general store", MerchantNames.normalise("  KUMAR  GENERAL STORE  "))
        assertEquals("swiggy", MerchantNames.normalise("UPI/swiggy@icici/401234"))
        assertEquals("amazon", MerchantNames.normalise("Amazon Pvt Ltd"))
        assertEquals("air india", MerchantNames.normalise("AIR INDIA"))
    }

    @Test
    fun normalise_rejectsGenericOrNoteLikeText() {
        assertNull(MerchantNames.normalise(null))
        assertNull(MerchantNames.normalise("   "))
        assertNull(MerchantNames.normalise("1234 5678"))
        assertNull(MerchantNames.normalise("UPI payment"))
        assertNull(MerchantNames.normalise("Bank debit"))
        assertNull(MerchantNames.normalise("ATM withdrawal"))
        assertNull(MerchantNames.normalise("NEFT credit"))
        assertNull(MerchantNames.normalise("dinner with the whole team after the launch party"))
    }

    @Test
    fun isLearnable_skipsNotChosenCategories() {
        assertTrue(MerchantNames.isLearnable("Food"))
        assertFalse(MerchantNames.isLearnable("Other"))
        assertFalse(MerchantNames.isLearnable("Imported"))
        assertFalse(MerchantNames.isLearnable("Transfer"))
        assertFalse(MerchantNames.isLearnable(""))
    }

    private fun mapping(key: String, category: String, type: String = "EXPENSE") =
        MerchantCategory(merchantKey = key, type = type, displayName = key, category = category, updatedAt = 0)

    @Test
    fun lookup_exactThenLongestWholeWordMatch() {
        val memory = CategoryMemory(
            listOf(
                mapping("swiggy", "Food"),
                mapping("swiggy instamart", "Grocery"),
                mapping("ola", "Transportation"),
            )
        )
        assertEquals("Food", memory.lookup("SWIGGY", isExpense = true))
        assertEquals("Food", memory.lookup("Swiggy order 123", isExpense = true))
        assertEquals("Grocery", memory.lookup("swiggy instamart #88", isExpense = true))
        // Whole words only: "ola" must not match "Coca Cola".
        assertNull(memory.lookup("Coca Cola", isExpense = true))
        assertNull(memory.lookup("Zomato", isExpense = true))
    }

    @Test
    fun lookup_keepsExpenseAndIncomeApart() {
        val memory = CategoryMemory(listOf(mapping("amazon", "Shopping"), mapping("amazon", "Refund", type = "INCOME")))
        assertEquals("Shopping", memory.lookup("Amazon", isExpense = true))
        assertEquals("Refund", memory.lookup("Amazon", isExpense = false))
        assertNull(CategoryMemory(listOf(mapping("amazon", "Shopping"))).lookup("Amazon", isExpense = false))
    }

    @Test
    fun lookup_ignoresGenericDescriptions() {
        assertNull(CategoryMemory(listOf(mapping("swiggy", "Food"))).lookup("UPI payment", isExpense = true))
        assertTrue(CategoryMemory.EMPTY.isEmpty)
    }
}
