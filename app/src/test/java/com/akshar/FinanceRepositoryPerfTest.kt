package com.akshar

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.akshar.data.db.FinanceDao
import com.akshar.data.db.AppDatabase
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.repository.FinanceRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceRepositoryPerfTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FinanceDao
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.financeDao()
        repository = FinanceRepository(dao)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun benchmarkAutoProcess() = runBlocking {
        // Setup 100 recurring transactions
        val now = System.currentTimeMillis()
        val oneDayMs = 86400000L
        for (i in 1..100) {
            dao.insertRecurringTransaction(
                RecurringTransaction(
                    id = i.toLong(),
                    amount = 10.0,
                    type = "EXPENSE",
                    category = "Test",
                    description = "Test $i",
                    frequency = "DAILY",
                    paymentMethod = "Cash",
                    lastTriggered = now - (30 * oneDayMs) // 30 days ago, meaning 30 transactions to generate for each
                )
            )
        }

        // We have 100 recurring transactions, each needs 30 new transactions. Total 3000 transactions.
        val timeMs = measureTimeMillis {
            repository.autoProcessRecurringTransactions()
        }

        println("BENCHMARK_RESULT: time taken = $timeMs ms")
    }
}
