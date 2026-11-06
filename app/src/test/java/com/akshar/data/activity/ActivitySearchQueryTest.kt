package com.akshar.data.activity

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.db.AppDatabase
import com.akshar.data.db.SeedDefaultAccountCallback
import com.akshar.data.model.Account
import com.akshar.data.model.Transaction
import com.akshar.data.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Runs the Activity search against a real in-memory Room database. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ActivitySearchQueryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SeedDefaultAccountCallback)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(database.financeDao(), database)
        runBlocking {
            repository.insertAccounts(listOf(Account(id = 2, name = "Bank", type = "Bank")))
            repository.insertTransactions(
                listOf(
                    tx(1, "EXPENSE", 120.0, "Food", "Morning coffee", "UPI", date = 1_000),
                    tx(2, "EXPENSE", 99.99, "Food", "Lunch", "Cash", date = 2_000),
                    tx(3, "EXPENSE", 2500.0, "Rent", "50% deposit", "Bank Transfer", date = 3_000, account = 2),
                    tx(4, "INCOME", 50000.0, "Salary", "November pay", "Bank Transfer", date = 4_000, account = 2),
                    tx(5, "EXPENSE", 100.0, "Shopping", "a_b store", "Credit Card", date = 5_000),
                    tx(6, "EXPENSE", 1000.0, "Transfer", "Move", "Transfer", date = 6_000, account = 2, transferId = "t1"),
                    tx(7, "INCOME", 1000.0, "Transfer", "Move", "Transfer", date = 6_000, account = 1, transferId = "t1"),
                )
            )
        }
    }

    @After
    fun teardown() = database.close()

    private fun tx(
        id: Long, type: String, amount: Double, category: String, description: String, payment: String,
        date: Long, account: Long = 1, transferId: String? = null
    ) = Transaction(
        id = id, amount = amount, type = type, category = category, description = description,
        date = date, paymentMethod = payment, accountId = account, transferId = transferId
    )

    private fun ids(query: TransactionQuery): List<Long> = runBlocking { repository.searchTransactions(query).first().map { it.id } }
    private fun search(filter: ActivityFilter): List<Long> = ids(filter.resolve(nowMillis = 10_000))

    @Test
    fun `no filter returns everything newest first`() {
        assertEquals(listOf(7L, 6L, 5L, 4L, 3L, 2L, 1L), ids(TransactionQuery()))
    }

    @Test
    fun `text search matches description, category and payment method, ignoring case`() {
        assertEquals(listOf(1L), search(ActivityFilter(query = "COFFEE")))
        assertEquals(listOf(4L), search(ActivityFilter(query = "salary")))
        assertEquals(listOf(5L), search(ActivityFilter(query = "credit")))
        assertEquals(listOf(2L, 1L), search(ActivityFilter(query = "food")))
    }

    @Test
    fun `percent and underscore in the search are literal`() {
        assertEquals(listOf(3L), search(ActivityFilter(query = "50%")))
        assertEquals(listOf(5L), search(ActivityFilter(query = "a_b")))
        assertEquals(emptyList<Long>(), search(ActivityFilter(query = "50_")))
    }

    @Test
    fun `type filter keeps transfers apart from expenses and income`() {
        assertEquals(listOf(5L, 3L, 2L, 1L), search(ActivityFilter(type = ActivityType.EXPENSE)))
        assertEquals(listOf(4L), search(ActivityFilter(type = ActivityType.INCOME)))
        assertEquals(listOf(7L, 6L), search(ActivityFilter(type = ActivityType.TRANSFER)))
    }

    @Test
    fun `several categories can be selected`() {
        assertEquals(listOf(3L, 2L, 1L), search(ActivityFilter(categories = setOf("Food", "Rent"))))
        assertEquals(emptyList<Long>(), search(ActivityFilter(categories = setOf("Travel"))))
    }

    @Test
    fun `account filter shows only that account's entries and transfer legs`() {
        assertEquals(listOf(6L, 4L, 3L), search(ActivityFilter(accountId = 2)))
    }

    @Test
    fun `payment method filter is exact`() {
        assertEquals(listOf(4L, 3L), search(ActivityFilter(paymentMethod = "Bank Transfer")))
    }

    @Test
    fun `date range is half open`() {
        assertEquals(listOf(4L, 3L, 2L), ids(TransactionQuery(fromMillis = 2_000, untilMillis = 5_000)))
    }

    @Test
    fun `amount bounds are inclusive`() {
        assertEquals(
            listOf(5L, 1L),
            search(ActivityFilter(type = ActivityType.EXPENSE, minAmountText = "100", maxAmountText = "120"))
        )
        assertEquals(listOf(5L, 2L), search(ActivityFilter(maxAmountText = "100")))
    }

    @Test
    fun `exact amount bounds exclude the adjacent cents and accept zero`() = runBlocking {
        repository.insertTransactions(listOf(
            tx(8, "EXPENSE", 0.0, "Food", "Zero", "Cash", date = 7_000),
            tx(9, "EXPENSE", 100.01, "Food", "Above", "Cash", date = 8_000),
        ))
        assertEquals(listOf(5L), search(ActivityFilter(minAmountText = "100", maxAmountText = "100")))
        assertEquals(listOf(8L), search(ActivityFilter(maxAmountText = "0")))
    }

    @Test
    fun `invalid amount range does not hide anything`() {
        assertEquals(7, search(ActivityFilter(minAmountText = "500", maxAmountText = "100")).size)
    }

    @Test
    fun `filters combine`() {
        val filter = ActivityFilter(query = "bank", type = ActivityType.EXPENSE, accountId = 2, minAmountText = "1000")
        assertEquals(listOf(3L), search(filter))
    }

    @Test
    fun `sort orders`() {
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L), search(ActivityFilter(sort = ActivitySort.OLDEST)))
        assertEquals(listOf(4L, 3L), search(ActivityFilter(sort = ActivitySort.HIGHEST)).take(2))
        assertEquals(listOf(2L, 5L, 1L), search(ActivityFilter(sort = ActivitySort.LOWEST)).take(3))
    }

    @Test
    fun `totals of a filtered result`() {
        val list = runBlocking { repository.searchTransactions(ActivityFilter(accountId = 2).resolve(10_000)).first() }
        val totals = ActivityTotals.of(list)
        assertEquals(3, totals.count)
        assertEquals("2500.00", totals.spent.toPlainString())
        assertEquals("50000.00", totals.received.toPlainString())
    }

    @Test
    fun `helper flows list used categories and payment methods without transfers`() = runBlocking {
        assertEquals(listOf("Food", "Rent", "Salary", "Shopping"), repository.usedCategories.first())
        assertEquals(listOf("Bank Transfer", "Cash", "Credit Card", "UPI"), repository.usedPaymentMethods.first())
        assertTrue(repository.hasAnyTransactions.first())
        database.clearAllTables()
        assertFalse(repository.hasAnyTransactions.first())
    }

    // --- Saved filters on DataStore ---

    @Test
    fun `saved filters persist, replace by name and delete`() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File.createTempFile("activity_filters", ".preferences_pb").apply { delete() }
        try {
            val store = DataStoreSavedFilterStore(PreferenceDataStoreFactory.create(scope = scope) { file })
            val food = ActivityFilter(categories = setOf("Food"), datePreset = DatePreset.THIS_MONTH)
            store.save("  Food this month  ", food)
            store.save("Big spends", ActivityFilter(minAmountText = "1000"))
            assertEquals(listOf("Food this month", "Big spends"), store.savedFilters.first().map { it.name })
            assertEquals(food, store.savedFilters.first().first().filter)

            store.save("food THIS month", food.copy(type = ActivityType.EXPENSE))
            val afterReplace = store.savedFilters.first()
            assertEquals(2, afterReplace.size)
            assertEquals(ActivityType.EXPENSE, afterReplace.first().filter.type)

            store.delete("BIG SPENDS")
            assertEquals(listOf("food THIS month"), store.savedFilters.first().map { it.name })

            // A new DataStore instance must read the saved state back from disk.
            scope.coroutineContext[Job]!!.cancelAndJoin()
            val reopenedScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            try {
                val reopened = DataStoreSavedFilterStore(PreferenceDataStoreFactory.create(scope = reopenedScope) { file })
                assertEquals(afterReplace.take(1), reopened.savedFilters.first())
            } finally {
                reopenedScope.coroutineContext[Job]!!.cancelAndJoin()
            }
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
