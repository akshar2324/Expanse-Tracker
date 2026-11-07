package com.akshar.ui.viewmodel

import com.akshar.data.model.MerchantCategory
import com.akshar.data.model.Transaction
import com.akshar.utils.CategoryMemory
import com.akshar.utils.CsvParser
import com.akshar.utils.ParsedCsvRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class CsvValidationRowsTest {

    private val zone = TimeZone.getTimeZone("UTC")
    private val day = 1_790_000_000_000L - 1_790_000_000_000L % 86_400_000L // midnight UTC

    private fun row(amount: Double, description: String, hash: String, date: Long = day + 3_600_000, category: String = CsvParser.DEFAULT_CATEGORY) =
        ParsedCsvRow(amount, "EXPENSE", category, description, date, hash, emptyList())

    private val memory = CategoryMemory(listOf(MerchantCategory("swiggy", "EXPENSE", "Swiggy", "Food", 0L)))

    @Test
    fun exactRowSeenBefore_isSkippedAsImported() {
        val rows = buildValidationRows(listOf(row(10.0, "Tea", "h1")), setOf("h1"), emptyList(), memory, zone)
        assertTrue(rows.single().isDuplicate)
        assertTrue(rows.single().skip)
        assertEquals("Already imported from a statement", rows.single().duplicateNote)
    }

    @Test
    fun sameAmountSameDayAsSavedEntry_isFlaggedAndSkipped() {
        val typedByHand = Transaction(amount = 250.0, type = "EXPENSE", category = "Food", description = "lunch", date = day + 10 * 3_600_000, paymentMethod = "Cash")
        val rows = buildValidationRows(
            listOf(row(250.0, "SWIGGY", "a"), row(250.0, "SWIGGY", "b", date = day + 86_400_000), row(99.0, "Tea", "c")),
            emptySet(), listOf(typedByHand), memory, zone
        )
        assertTrue(rows[0].isDuplicate && rows[0].skip)
        assertTrue(rows[0].duplicateNote!!.startsWith("Possible duplicate"))
        assertFalse(rows[1].isDuplicate) // next day
        assertFalse(rows[2].isDuplicate) // other amount
        assertNull(rows[2].duplicateNote)
    }

    @Test
    fun learnedCategoryFillsOnlyRowsWithoutOne() {
        val rows = buildValidationRows(
            listOf(row(10.0, "SWIGGY*ORDER", "a"), row(10.0, "Swiggy", "b", category = "Dining"), row(10.0, "Tea stall", "c")),
            emptySet(), emptyList(), memory, zone
        )
        assertEquals("Food", rows[0].parsedRow.category)
        assertTrue(rows[0].learnedCategory)
        assertEquals("Dining", rows[1].parsedRow.category) // the file's own category is kept
        assertFalse(rows[1].learnedCategory)
        assertEquals(CsvParser.DEFAULT_CATEGORY, rows[2].parsedRow.category)
    }
}
