package com.akshar

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.akshar.alerts.BudgetAlertStore
import com.akshar.alerts.BudgetAlertWorker
import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReleaseIntegrationTest {
    @Test
    fun budgetWorkerPostsBothThresholdsOnceAndPreservesLedger() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ExpenseTrackerApp
        val repository = app.repository
        val store = BudgetAlertStore(app)
        val wasEnabled = store.enabled.first()
        val before = withContext(Dispatchers.IO) { repository.exportSnapshot() }
        val notificationManager = app.getSystemService(NotificationManager::class.java)
        val workManager = WorkManager.getInstance(app)
        val category = "Release QA ${UUID.randomUUID()}"
        val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val claimsBefore = store.sent()
        try {
            assertTrue("Grant POST_NOTIFICATIONS before this test", BudgetAlertWorker.canNotify(app))
            withContext(Dispatchers.IO) {
                repository.insertBudgets(listOf(Budget(category = category, limitAmount = 100.0, month = month)))
                repository.insertTransaction(Transaction(amount = 100.0, type = "EXPENSE", category = category,
                    description = "Release QA", date = System.currentTimeMillis(), paymentMethod = "Cash"))
            }
            store.setEnabled(true)
            suspend fun checkAlerts() {
                val request = OneTimeWorkRequestBuilder<BudgetAlertWorker>().build()
                workManager.enqueue(request).result.get()
                val state = withTimeout(20_000) {
                    var info = workManager.getWorkInfoById(request.id).get()
                    while (info == null || !info.state.isFinished) {
                        delay(100)
                        info = workManager.getWorkInfoById(request.id).get()
                    }
                    info.state
                }
                assertEquals(WorkInfo.State.SUCCEEDED, state)
            }
            checkAlerts()
            val firstClaims = store.sent() - claimsBefore
            assertTrue("Both 80% and 100% must be claimed", firstClaims.size >= 2)
            val tags = notificationManager.activeNotifications.filter { it.tag in firstClaims }.map { it.tag }.toSet()
            assertTrue("Claims must correspond to posted notifications", tags.size >= 2)
            checkAlerts()
            assertEquals(firstClaims, store.sent() - claimsBefore)
            store.setEnabled(false)
            checkAlerts()
            assertEquals(firstClaims, store.sent() - claimsBefore)
        } finally {
            store.setEnabled(false)
            workManager.cancelUniqueWork(com.akshar.alerts.BudgetAlertScheduler.DAILY).result.get()
            workManager.cancelUniqueWork(com.akshar.alerts.BudgetAlertScheduler.AFTER_SAVE).result.get()
            withContext(Dispatchers.IO) {
                repository.replaceAllData(before)
                assertEquals(before, repository.exportSnapshot())
            }
            (store.sent() - claimsBefore).forEach { notificationManager.cancel(it, 0) }
            store.setEnabled(wasEnabled)
        }
    }

    @Test
    fun transferDeleteAndUndoRestoreTheExactSnapshot() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ExpenseTrackerApp
        val repository = app.repository
        withContext(Dispatchers.IO) {
            val before = repository.exportSnapshot()
            try {
                val transferId = UUID.randomUUID().toString()
                val from = Transaction(amount = 42.35, type = "EXPENSE", category = "Transfer",
                    description = "Release QA transfer", date = System.currentTimeMillis(),
                    paymentMethod = "Transfer", accountId = 1, transferId = transferId)
                repository.saveTransfer(from, from.copy(type = "INCOME", accountId = 2))
                val saved = repository.exportSnapshot()
                val rows = saved.transactions.filter { it.transferId == transferId }
                assertEquals(2, rows.size)
                val deleted = repository.deleteActivityTransaction(rows.first().id)
                assertEquals(rows.toSet(), deleted.toSet())
                assertTrue(repository.exportSnapshot().transactions.none { it.transferId == transferId })
                repository.restoreActivityTransactions(deleted)
                assertEquals(saved, repository.exportSnapshot())
            } finally {
                repository.replaceAllData(before)
                assertEquals(before, repository.exportSnapshot())
            }
        }
    }
}
