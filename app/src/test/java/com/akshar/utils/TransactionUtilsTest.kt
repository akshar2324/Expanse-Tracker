package com.akshar.utils

import com.akshar.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionUtilsTest {

    private val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun createTransaction(
        type: String,
        amount: Double,
        dateStr: String
    ): Transaction {
        val date = df.parse(dateStr)?.time ?: 0L
        return Transaction(
            amount = amount,
            type = type,
            category = "Food",
            description = "Test",
            date = date,
            paymentMethod = "Cash"
        )
    }

    @Test
    fun `expencesGroupedByDay should group expenses by day and sum amounts`() {
        // Given
        val transactions = listOf(
            createTransaction("EXPENSE", 10.0, "2023-10-25"),
            createTransaction("EXPENSE", 15.0, "2023-10-25"),
            createTransaction("EXPENSE", 5.0, "2023-10-26")
        )

        // When
        val result = TransactionUtils.expencesGroupedByDay(transactions)

        // Then
        assertEquals(2, result.size)
        assertEquals(25.0, result["2023-10-25"])
        assertEquals(5.0, result["2023-10-26"])
    }

    @Test
    fun `expencesGroupedByDay should return empty map for empty list`() {
        // Given
        val transactions = emptyList<Transaction>()

        // When
        val result = TransactionUtils.expencesGroupedByDay(transactions)

        // Then
        assertEquals(true, result.isEmpty())
    }

    @Test
    fun `expencesGroupedByDay should ignore incomes`() {
        // Given
        val transactions = listOf(
            createTransaction("INCOME", 100.0, "2023-10-25"),
            createTransaction("INCOME", 50.0, "2023-10-26")
        )

        // When
        val result = TransactionUtils.expencesGroupedByDay(transactions)

        // Then
        assertEquals(true, result.isEmpty())
    }

    @Test
    fun `expencesGroupedByDay should only process expenses in a mixed list`() {
        // Given
        val transactions = listOf(
            createTransaction("EXPENSE", 20.0, "2023-10-25"),
            createTransaction("INCOME", 100.0, "2023-10-25"),
            createTransaction("EXPENSE", 30.0, "2023-10-26")
        )

        // When
        val result = TransactionUtils.expencesGroupedByDay(transactions)

        // Then
        assertEquals(2, result.size)
        assertEquals(20.0, result["2023-10-25"])
        assertEquals(30.0, result["2023-10-26"])
    }
}
