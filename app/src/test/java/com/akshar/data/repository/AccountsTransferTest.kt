package com.akshar.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.db.AppDatabase
import com.akshar.data.model.Account
import com.akshar.data.model.Reconciliation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(sdk = [32])
class AccountsTransferTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(database.financeDao(), database)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `test account insertion and retrieval`() = runBlocking {
        val account = Account(
            name = "Test Bank",
            type = "Bank",
            openingBalance = 1000.0,
            isArchived = false
        )
        repository.insertAccount(account)

        val accounts = repository.allAccounts.first()
        assertEquals(1, accounts.size)
        assertEquals("Test Bank", accounts[0].name)
        assertEquals(1000.0, accounts[0].openingBalance, 0.0)
    }

    @Test
    fun `test transfer atomicity`() = runBlocking {
        val fromAccount = Account(name = "Wallet 1", type = "Wallet", openingBalance = 500.0)
        val toAccount = Account(name = "Wallet 2", type = "Wallet", openingBalance = 200.0)
        repository.insertAccount(fromAccount)
        repository.insertAccount(toAccount)

        // In real usage, id is auto-generated. We fake 1 and 2 here for test
        val transferId = UUID.randomUUID().toString()
        val fromTx = com.akshar.data.model.Transaction(
            amount = 100.0,
            type = "EXPENSE",
            category = "Transfer",
            description = "Test Transfer",
            date = System.currentTimeMillis(),
            paymentMethod = "Transfer",
            accountId = 1L,
            transferId = transferId
        )
        val toTx = com.akshar.data.model.Transaction(
            amount = 100.0,
            type = "INCOME",
            category = "Transfer",
            description = "Test Transfer",
            date = System.currentTimeMillis(),
            paymentMethod = "Transfer",
            accountId = 2L,
            transferId = transferId
        )

        repository.saveTransfer(fromTx, toTx)

        val transactions = repository.allTransactions.first()
        assertEquals(2, transactions.size)
        val expenseTx = transactions.find { it.type == "EXPENSE" }
        val incomeTx = transactions.find { it.type == "INCOME" }

        assertNotNull(expenseTx)
        assertNotNull(incomeTx)
        assertEquals(transferId, expenseTx!!.transferId)
        assertEquals(transferId, incomeTx!!.transferId)
        assertEquals(1L, expenseTx.accountId)
        assertEquals(2L, incomeTx.accountId)
    }

    @Test
    fun `test transfer deletion`() = runBlocking {
        val transferId = UUID.randomUUID().toString()
        val fromTx = com.akshar.data.model.Transaction(
            amount = 100.0, type = "EXPENSE", category = "Transfer", description = "Test",
            date = 0, paymentMethod = "Transfer", accountId = 1L, transferId = transferId
        )
        val toTx = fromTx.copy(type = "INCOME", accountId = 2L)

        repository.saveTransfer(fromTx, toTx)
        assertEquals(2, repository.allTransactions.first().size)

        repository.deleteTransfer(transferId)
        assertEquals(0, repository.allTransactions.first().size)
    }

    @Test
    fun `test reconciliation insertion`() = runBlocking {
        val rec = Reconciliation(
            accountId = 1L,
            statementBalance = 1500.0,
            calculatedBalance = 1450.0,
            date = System.currentTimeMillis()
        )
        repository.insertReconciliation(rec)

        val recs = repository.allReconciliations.first()
        assertEquals(1, recs.size)
        assertEquals(1500.0, recs[0].statementBalance, 0.0)
    }
}
