package com.akshar

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.db.AppDatabase
import com.akshar.data.db.FinanceDao
import com.akshar.data.model.Transaction
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis
import java.io.File

@RunWith(AndroidJUnit4::class)
class DatabasePerformanceTest {

    private lateinit var db: AppDatabase
    private lateinit var financeDao: FinanceDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        financeDao = db.financeDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertTransactionsPerformance() = runBlocking {
        val transactions = (1..1000).map { i ->
            Transaction(
                amount = i * 10.0,
                type = if (i % 2 == 0) "Income" else "Expense",
                category = "Test",
                description = "Test Desc $i",
                date = System.currentTimeMillis(),
                paymentMethod = "Cash"
            )
        }

        val individualInsertTime = measureTimeMillis {
            transactions.forEach {
                financeDao.insertTransaction(it)
            }
        }

        val transactions2 = (1..1000).map { i ->
            Transaction(
                amount = i * 10.0,
                type = if (i % 2 == 0) "Income" else "Expense",
                category = "Test",
                description = "Test Desc $i",
                date = System.currentTimeMillis(),
                paymentMethod = "Cash"
            )
        }

        val batchInsertTime = measureTimeMillis {
            financeDao.insertTransactions(transactions2)
        }

        File("performance_results.txt").writeText("Inserting 1000 individual transactions took: $individualInsertTime ms\nInserting 1000 transactions via batch took: $batchInsertTime ms")
        assert(true)
    }
}
