package com.akshar.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.db.AppDatabase
import com.akshar.data.model.Account
import com.akshar.data.model.Transaction
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.math.RoundingMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ActivityUndoRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private val entry = Transaction(
        id = 41, amount = 1234.57, type = "EXPENSE", category = "Food", description = "Lunch",
        date = 1785000000123L, paymentMethod = "UPI", timestamp = 1785000000456L,
        importBatchId = "test-batch", originalCsvRowHash = "test-hash", accountId = 1,
    )

    @Before fun setup() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
        repository = FinanceRepository(database.financeDao(), database)
    }
    @After fun teardown() = database.close()

    private fun balance(rows: List<Transaction>, account: Long): BigDecimal = rows
        .filter { it.accountId == account }
        .fold(BigDecimal.ZERO) { total, row ->
            val amount = BigDecimal.valueOf(row.amount).setScale(2, RoundingMode.HALF_UP)
            if (row.type == "EXPENSE") total - amount else total + amount
        }.setScale(2, RoundingMode.HALF_UP)

    @Test fun `delete and undo preserve every field and account effect`() = runTest {
        repository.insertTransaction(entry)
        val before = database.financeDao().getTransactionsList()
        val snapshot = repository.deleteActivityTransaction(entry.id)
        assertEquals(listOf(entry), snapshot)
        assertTrue(database.financeDao().getTransactionsList().isEmpty())
        assertEquals(BigDecimal("0.00"), balance(database.financeDao().getTransactionsList(), 1))
        repository.restoreActivityTransactions(snapshot)
        val restored = database.financeDao().getTransactionsList()
        assertEquals(before, restored)
        assertEquals(balance(before, 1), balance(restored, 1))
    }

    @Test fun `deleting either transfer leg removes and restores both legs exactly`() = runTest {
        database.financeDao().insertAccounts(listOf(Account(1, "Cash", "Cash"), Account(2, "Bank", "Bank")))
        val debit = entry.copy(transferId = "transfer-1", category = "Transfer")
        val credit = debit.copy(id = 42, accountId = 2, type = "INCOME")
        val unrelated = entry.copy(id = 99, amount = 0.01)
        for (selected in listOf(debit, credit)) {
            repository.insertTransactions(listOf(debit, credit, unrelated))
            val before = database.financeDao().getTransactionsList()
            val snapshot = repository.deleteActivityTransaction(selected.id)
            assertEquals(setOf(debit, credit), snapshot.toSet())
            assertEquals(listOf(unrelated), database.financeDao().getTransactionsList())
            assertEquals(BigDecimal("-0.01"), balance(database.financeDao().getTransactionsList(), 1))
            assertEquals(BigDecimal("0.00"), balance(database.financeDao().getTransactionsList(), 2))
            repository.restoreActivityTransactions(snapshot)
            val restored = database.financeDao().getTransactionsList()
            assertEquals(before, restored)
            for (account in listOf(1L, 2L)) assertEquals(balance(before, account), balance(restored, account))
        }
    }

    @Test fun `repeated delete produces no second undo offer`() = runTest {
        repository.insertTransaction(entry)
        assertEquals(listOf(entry), repository.deleteActivityTransaction(entry.id))
        assertTrue(repository.deleteActivityTransaction(entry.id).isEmpty())
    }

    @Test fun `undo conflict rolls back without restoring half a transfer`() = runTest {
        val debit = entry.copy(transferId = "transfer-1")
        val credit = debit.copy(id = 42, accountId = 2, type = "INCOME")
        repository.insertTransactions(listOf(debit, credit))
        val snapshot = repository.deleteActivityTransaction(debit.id)
        val edited = credit.copy(description = "Later edit")
        repository.insertTransaction(edited)
        try {
            repository.restoreActivityTransactions(snapshot)
            fail("Must refuse to overwrite later data")
        } catch (_: IllegalStateException) { }
        assertEquals(listOf(edited), database.financeDao().getTransactionsList())
    }
}
