package com.akshar.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.backup.BackupCodec
import com.akshar.data.db.AppDatabase
import com.akshar.data.db.SeedDefaultAccountCallback
import com.akshar.data.model.Account
import com.akshar.data.model.Budget
import com.akshar.data.model.Debt
import com.akshar.data.model.Reconciliation
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.SavingsGoal
import com.akshar.data.model.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LedgerIntegrityTest {

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
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun leg(type: String, accountId: Long, amount: Double, transferId: String, date: Long = 1_000L) = Transaction(
        amount = amount, type = type, category = "Transfer", description = "Move", date = date,
        paymentMethod = "Transfer", accountId = accountId, transferId = transferId
    )

    @Test
    fun `fresh database has the default account that transactions point at`() = runBlocking {
        val accounts = repository.allAccounts.first()
        assertEquals(listOf(1L), accounts.map { it.id })
        assertEquals(Transaction(amount = 1.0, type = "EXPENSE", category = "Food", description = "", date = 0L, paymentMethod = "Cash").accountId, accounts.single().id)
    }

    @Test
    fun `editing a transfer replaces both legs instead of adding or losing any`() = runBlocking {
        repository.insertAccount(Account(id = 2, name = "Bank", type = "Bank"))
        repository.saveTransfer(leg("EXPENSE", 1, 100.0, "t1"), leg("INCOME", 2, 100.0, "t1"))

        // Edit: same transferId, new amount. Previously the screen deleted and re-saved in two
        // racing coroutines, which could drop the transfer entirely.
        repository.saveTransfer(leg("EXPENSE", 1, 75.0, "t1"), leg("INCOME", 2, 75.0, "t1"))

        val legs = repository.getTransferLegs("t1")
        assertEquals(2, legs.size)
        assertTrue(legs.all { it.amount == 75.0 })
        assertEquals("EXPENSE", legs.first().type)
        assertEquals(2, repository.allTransactions.first().size)
    }

    @Test
    fun `deleting a transfer removes only its legs`() = runBlocking {
        repository.insertAccount(Account(id = 2, name = "Bank", type = "Bank"))
        repository.saveTransfer(leg("EXPENSE", 1, 10.0, "a"), leg("INCOME", 2, 10.0, "a"))
        repository.saveTransfer(leg("EXPENSE", 2, 5.0, "b"), leg("INCOME", 1, 5.0, "b"))
        repository.deleteTransfer("a")
        assertEquals(setOf("b"), repository.allTransactions.first().map { it.transferId }.toSet())
    }

    @Test
    fun `transfers move balances between accounts but net worth is unchanged`() = runBlocking {
        repository.insertAccount(Account(id = 2, name = "Bank", type = "Bank", openingBalance = 500.0))
        repository.insertTransaction(Transaction(amount = 40.0, type = "EXPENSE", category = "Food", description = "", date = 1L, paymentMethod = "Cash", accountId = 1))
        repository.saveTransfer(leg("EXPENSE", 2, 200.0, "t"), leg("INCOME", 1, 200.0, "t"))

        val txs = repository.allTransactions.first()
        val accounts = repository.allAccounts.first()
        fun balance(id: Long) = accounts.first { it.id == id }.openingBalance +
            txs.filter { it.accountId == id }.sumOf { if (it.type == "INCOME") it.amount else -it.amount }
        assertEquals(160.0, balance(1), 0.0)
        assertEquals(300.0, balance(2), 0.0)
        // Income/expense totals (as the dashboard and analytics compute them) ignore transfers.
        val nonTransfer = txs.filter { it.transferId == null }
        assertEquals(0.0, nonTransfer.filter { it.type == "INCOME" }.sumOf { it.amount }, 0.0)
        assertEquals(40.0, nonTransfer.filter { it.type == "EXPENSE" }.sumOf { it.amount }, 0.0)
    }

    @Test
    fun `monthly recurring entries land on the same day each month`() = runBlocking {
        val cal = Calendar.getInstance().apply { clear(); set(2026, Calendar.JANUARY, 15, 9, 0) }
        val anchor = cal.timeInMillis
        repository.insertRecurringTransaction(
            RecurringTransaction(amount = 15.99, type = "EXPENSE", category = "Subscription", description = "Music", frequency = "MONTHLY", lastTriggered = anchor)
        )
        val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.DECEMBER, 20, 9, 0) }.timeInMillis

        repository.autoProcessRecurringTransactions(now)

        val generated = repository.allTransactions.first().sortedBy { it.date }
        assertEquals(11, generated.size) // Feb..Dec
        generated.forEach {
            val day = Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
            assertEquals(15, day)
        }
        val updated = repository.allRecurringTransactions.first().single()
        assertEquals(generated.last().date, updated.lastTriggered)

        // Running again at the same moment generates nothing new.
        repository.autoProcessRecurringTransactions(now)
        assertEquals(11, repository.allTransactions.first().size)
    }

    @Test
    fun `recurring entry never triggered starts its schedule without back-filling`() = runBlocking {
        repository.insertRecurringTransaction(
            RecurringTransaction(amount = 5.0, type = "EXPENSE", category = "Bills", description = "Phone", frequency = "WEEKLY", lastTriggered = 0L)
        )
        repository.autoProcessRecurringTransactions(1_000_000L)
        assertEquals(0, repository.allTransactions.first().size)
        assertEquals(1_000_000L, repository.allRecurringTransactions.first().single().lastTriggered)
    }

    @Test
    fun `json backup round trip restores every table with account links intact`() = runBlocking {
        repository.insertAccount(Account(id = 7, name = "Card", type = "Credit Card", openingBalance = -120.5))
        repository.insertTransaction(Transaction(amount = 19.99, type = "EXPENSE", category = "Food", description = "Lunch", date = 10L, paymentMethod = "Card", accountId = 7, importBatchId = "b", originalCsvRowHash = "h"))
        repository.saveTransfer(leg("EXPENSE", 1, 50.0, "tr"), leg("INCOME", 7, 50.0, "tr"))
        repository.insertBudget(Budget(category = "Food", limitAmount = 300.0, month = "2026-01", frequency = "MONTHLY", remindersEnabled = true, reminderThreshold = 80))
        repository.insertSavingsGoal(SavingsGoal(name = "Trip", targetAmount = 1000.0, currentAmount = 250.25, targetDate = "Dec 2026"))
        repository.insertRecurringTransaction(RecurringTransaction(amount = 9.0, type = "EXPENSE", category = "Bills", description = "Cloud", frequency = "MONTHLY", lastTriggered = 99L))
        repository.insertDebt(Debt(personName = "Sam", amount = 40.0, type = "LENT", date = 5L, description = "", dueDate = 50L))
        repository.insertReconciliation(Reconciliation(accountId = 7, statementBalance = -90.0, calculatedBalance = -90.51, date = 20L))

        val before = repository.exportSnapshot()
        val json = BackupCodec.encode(before)

        // Restore twice into a database that already has other data: the result must equal the
        // backup exactly, with no duplicates and no leftovers.
        repository.insertTransaction(Transaction(amount = 1.0, type = "INCOME", category = "Other", description = "stale", date = 1L, paymentMethod = "Cash"))
        repository.replaceAllData(BackupCodec.decode(json))
        repository.replaceAllData(BackupCodec.decode(json))

        val after = repository.exportSnapshot()
        assertEquals(before, after)
        val lunch = after.transactions.first { it.description == "Lunch" }
        assertEquals(7L, lunch.accountId)
        assertEquals(7L, after.reconciliations.single().accountId)
    }
}
