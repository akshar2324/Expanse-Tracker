package com.akshar.insights

import com.akshar.data.model.Budget
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.Transaction
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class InsightEngineTest {

    class MockTimeProvider(private val fixedTimeMs: Long) : TimeProvider {
        override fun nowMs(): Long = fixedTimeMs
    }

    private fun createTx(
        id: Long,
        amount: Double,
        type: String,
        category: String,
        date: Long,
        transferId: String? = null
    ) = Transaction(
        id = id,
        amount = amount,
        type = type,
        category = category,
        description = "Test",
        date = date,
        paymentMethod = "Cash",
        transferId = transferId
    )

    private fun getFixedTimeMs(year: Int, month: Int, date: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, date, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun shiftDays(timestamp: Long, days: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.timeInMillis
    }

    @Test
    fun `generateWeeklyDigest with no data should return empty summary`() {
        val fixedTimeMs = getFixedTimeMs(2023, Calendar.OCTOBER, 18) // Wednesday
        val engine = InsightEngine(MockTimeProvider(fixedTimeMs))

        val summary = engine.generateWeeklyDigest(emptyList(), emptyList(), emptyList())
        assertTrue(summary.hasInsufficientData)
        assertEquals(0.0, summary.totalSpent, 0.0)
        assertEquals(0.0, summary.totalIncome, 0.0)
        assertTrue(summary.topCategories.isEmpty())
        assertTrue(summary.anomalies.isEmpty())
    }

    @Test
    fun `generateWeeklyDigest should filter transfers correctly`() {
        val fixedTimeMs = getFixedTimeMs(2023, Calendar.OCTOBER, 16) // Monday
        val engine = InsightEngine(MockTimeProvider(fixedTimeMs))

        val dateMs = fixedTimeMs
        val tx1 = createTx(1, 100.0, "EXPENSE", "Food", dateMs)
        val txTransfer = createTx(2, 50.0, "EXPENSE", "Bank", dateMs, transferId = "trans1")

        val summary = engine.generateWeeklyDigest(listOf(tx1, txTransfer), emptyList(), emptyList())

        assertEquals(100.0, summary.totalSpent, 0.0)
    }

    @Test
    fun `generateWeeklyDigest handles anomalies and budgets`() {
        val fixedTimeMs = getFixedTimeMs(2023, Calendar.OCTOBER, 18) // Wednesday
        val engine = InsightEngine(MockTimeProvider(fixedTimeMs))

        val thisWeekMs = fixedTimeMs
        val prevWeekMs = shiftDays(thisWeekMs, -7)

        val txOld = createTx(1, 100.0, "EXPENSE", "Shopping", prevWeekMs)
        val txNew = createTx(2, 200.0, "EXPENSE", "Shopping", thisWeekMs)
        
        val budget = Budget(id = 1, category = "Shopping", limitAmount = 150.0, month = "2023-10")

        val summary = engine.generateWeeklyDigest(
            listOf(txOld, txNew),
            listOf(budget),
            emptyList()
        )

        assertFalse(summary.hasInsufficientData)
        assertEquals(200.0, summary.totalSpent, 0.0)
        
        // Check anomaly
        assertEquals(1, summary.anomalies.size)
        assertEquals("Shopping", summary.anomalies.first().relatedCategory)

        // Check budget
        assertEquals(1, summary.budgetProgress.size)
        assertTrue(summary.budgetProgress.first().isOverBudget)
        assertEquals(200.0, summary.budgetProgress.first().spentSoFar, 0.0)
    }
    
    @Test
    fun `generateWeeklyDigest calculates refund or negative expense correctly`() {
        val fixedTimeMs = getFixedTimeMs(2023, Calendar.OCTOBER, 18)
        val engine = InsightEngine(MockTimeProvider(fixedTimeMs))
        val dateMs = fixedTimeMs

        val tx1 = createTx(1, 100.0, "EXPENSE", "Food", dateMs)
        val tx2 = createTx(2, -20.0, "EXPENSE", "Food", dateMs) // refund

        val summary = engine.generateWeeklyDigest(listOf(tx1, tx2), emptyList(), emptyList())

        assertEquals(80.0, summary.totalSpent, 0.0)
    }

    @Test
    fun `generateWeeklyDigest applies refunds to category and budget totals`() {
        val fixedTimeMs = getFixedTimeMs(2023, Calendar.OCTOBER, 18)
        val engine = InsightEngine(MockTimeProvider(fixedTimeMs))
        val budget = Budget(id = 1, category = "Food", limitAmount = 90.0, month = "2023-10")

        val summary = engine.generateWeeklyDigest(
            listOf(
                createTx(1, 100.0, "EXPENSE", "Food", fixedTimeMs),
                createTx(2, -20.0, "EXPENSE", "Food", fixedTimeMs)
            ),
            listOf(budget),
            emptyList()
        )

        assertEquals(80.0, summary.topCategories.first().amountSpent, 0.0)
        assertEquals(80.0, summary.budgetProgress.first().spentSoFar, 0.0)
        assertFalse(summary.budgetProgress.first().isOverBudget)
    }

    @Test
    fun `generateWeeklyDigest respects Monday week boundary`() {
        val wednesday = getFixedTimeMs(2023, Calendar.OCTOBER, 18)
        val engine = InsightEngine(MockTimeProvider(wednesday))
        val monday = shiftDays(wednesday, -2)
        val previousSunday = shiftDays(wednesday, -3)

        val summary = engine.generateWeeklyDigest(
            listOf(
                createTx(1, 75.0, "EXPENSE", "Food", monday),
                createTx(2, 500.0, "EXPENSE", "Travel", previousSunday)
            ),
            emptyList(),
            emptyList()
        )

        assertEquals(75.0, summary.totalSpent, 0.0)
        assertEquals("Food", summary.topCategories.single().categoryName)
    }

    @Test
    fun `generateWeeklyDigest estimates next week recurring outlook`() {
        val fixedTimeMs = getFixedTimeMs(2023, Calendar.OCTOBER, 18)
        val engine = InsightEngine(MockTimeProvider(fixedTimeMs))
        val recurring = listOf(
            RecurringTransaction(amount = 70.0, type = "expense", category = "Bills", description = "Weekly bill", frequency = "weekly"),
            RecurringTransaction(amount = 280.0, type = "EXPENSE", category = "Rent", description = "Monthly rent", frequency = "MONTHLY")
        )

        val summary = engine.generateWeeklyDigest(
            listOf(createTx(1, 100.0, "INCOME", "Salary", fixedTimeMs)),
            emptyList(),
            recurring
        )

        assertEquals(140.0, summary.outlook?.expectedRecurringSpend ?: 0.0, 0.0)
    }
}
