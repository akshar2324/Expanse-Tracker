package com.akshar.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.db.AppDatabase
import com.akshar.data.db.SeedDefaultAccountCallback
import com.akshar.data.model.Transaction
import com.akshar.utils.DuplicateCandidate
import com.akshar.utils.DuplicateCheck
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

/** Duplicate lookups and merchant category learning against a real (in-memory) Room database. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ImportGuardsRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private val zone = TimeZone.getTimeZone("Asia/Kolkata")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SeedDefaultAccountCallback)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(database.financeDao(), database)
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun day(d: Int, hour: Int = 10) = Calendar.getInstance(zone).apply { clear(); set(2026, Calendar.OCTOBER, d, hour, 0) }.timeInMillis

    private fun tx(amount: Double, date: Long, hash: String? = null) = Transaction(
        amount = amount, type = "EXPENSE", category = "Food", description = "Swiggy", date = date,
        paymentMethod = "UPI", originalCsvRowHash = hash
    )

    @Test
    fun `finds a same-day same-amount entry and ignores other days`() = runBlocking {
        repository.insertTransaction(tx(250.0, day(14, 21)))
        repository.insertTransaction(tx(250.0, day(15, 9)))

        val match = repository.findLikelyDuplicate(DuplicateCandidate(250.0, "EXPENSE", day(14, 0)), zone)
        assertEquals(day(14, 21), match?.existing?.date)
        assertNull(repository.findLikelyDuplicate(DuplicateCandidate(250.0, "EXPENSE", day(16)), zone))
        assertNull(repository.findLikelyDuplicate(DuplicateCandidate(99.0, "EXPENSE", day(14)), zone))
    }

    @Test
    fun `finds the same bank reference even on another day`() = runBlocking {
        val key = DuplicateCheck.referenceKey("EXPENSE", "629100004321")
        repository.insertTransaction(tx(232.0, day(10), hash = key))

        val match = repository.findLikelyDuplicate(DuplicateCandidate(232.0, "EXPENSE", day(14), key), zone)
        assertTrue(match!!.sameReference)
    }

    @Test
    fun `ledger across days spans whole first and last day`() = runBlocking {
        repository.insertTransaction(tx(1.0, day(13, 23)))
        repository.insertTransaction(tx(2.0, day(14, 0)))
        repository.insertTransaction(tx(3.0, day(16, 23)))
        repository.insertTransaction(tx(4.0, day(17, 0)))
        val amounts = repository.getTransactionsAcrossDays(day(14, 12), day(16, 1), zone).map { it.amount }.sorted()
        assertEquals(listOf(2.0, 3.0), amounts)
    }

    @Test
    fun `learning stores the latest pick per merchant and type`() = runBlocking {
        assertTrue(repository.learnCategory("SWIGGY", "EXPENSE", "Food"))
        assertTrue(repository.learnCategory("Swiggy order 88", "EXPENSE", "Grocery")) // different key
        assertTrue(repository.learnCategory("swiggy", "EXPENSE", "Entertainment")) // replaces the first
        assertTrue(repository.learnCategory("Swiggy", "INCOME", "Refund"))

        val all = repository.allMerchantCategories.first()
        assertEquals(3, all.size)
        val memory = repository.categoryMemory()
        assertEquals("Entertainment", memory.lookup("SWIGGY", "EXPENSE"))
        assertEquals("Grocery", memory.lookup("swiggy order 12", "EXPENSE"))
        assertEquals("Refund", memory.lookup("Swiggy", "INCOME"))
    }

    @Test
    fun `generic descriptions and unchosen categories are not learned`() = runBlocking {
        assertFalse(repository.learnCategory("UPI payment", "EXPENSE", "Food"))
        assertFalse(repository.learnCategory("Swiggy", "EXPENSE", "Other"))
        assertFalse(repository.learnCategory("", "EXPENSE", "Food"))
        assertTrue(repository.allMerchantCategories.first().isEmpty())
    }

    @Test
    fun `forget removes one mapping and clear removes all`() = runBlocking {
        repository.learnCategory("Swiggy", "EXPENSE", "Food")
        repository.learnCategory("Uber", "EXPENSE", "Transportation")
        val swiggy = repository.allMerchantCategories.first().first { it.merchantKey == "swiggy" }

        repository.forgetMerchantCategory(swiggy)
        assertEquals(listOf("uber"), repository.allMerchantCategories.first().map { it.merchantKey })

        repository.clearMerchantCategories()
        assertTrue(repository.categoryMemory().isEmpty)
    }
}
