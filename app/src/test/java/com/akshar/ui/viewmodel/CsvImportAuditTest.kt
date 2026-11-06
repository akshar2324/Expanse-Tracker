package com.akshar.ui.viewmodel

import android.net.Uri
import com.akshar.ExpenseTrackerApp
import com.akshar.data.db.AppDatabase
import com.akshar.data.model.Transaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CsvImportAuditTest {
    private val app = ApplicationProvider.getApplicationContext<ExpenseTrackerApp>()
    private val file by lazy { File(app.cacheDir, "audit-import.csv") }

    @After fun close() {
        AppDatabase.closeAndResetInstance()
        file.delete()
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue("Timed out waiting for import", condition())
    }

    private fun preview(csv: String): CsvImportViewModel {
        file.writeText(csv)
        return CsvImportViewModel(app).apply {
            handleFileSelected(Uri.fromFile(file))
            dateColumn.value = "Date"
            amountColumn.value = "Amount"
            descriptionColumn.value = "Description"
            generatePreview()
            await { currentStep.value == ImportStep.PREVIEW }
            assertTrue(parseErrors.value.toString(), parseErrors.value.isEmpty())
        }
    }

    @Test fun duplicateRowsWithinOneFileAreSkippedByDefault() {
        val vm = preview("Date,Amount,Description\n2026-01-05,-10,Tea\n2026-01-05,-10,Tea\n")
        assertEquals(listOf(false, true), vm.previewRows.value.map { it.isDuplicate })
        assertEquals(listOf(false, true), vm.previewRows.value.map { it.skip })
    }

    @Test fun reimportDetectsExistingRowsAndUndoRemovesOnlyItsBatch() = runBlocking {
        val csv = "Date,Amount,Description\n2026-01-05,-10,Tea\n"
        val vm = preview(csv)
        vm.executeImport()
        await { vm.currentStep.value == ImportStep.SUMMARY }
        val batchId = vm.lastBatchId.value
        assertNotNull(batchId)
        assertEquals(batchId, app.repository.allTransactions.first().single().importBatchId)
        val again = preview(csv)
        assertTrue(again.previewRows.value.single().skip)
        app.repository.insertTransaction(Transaction(amount = 25.0, type = "INCOME", category = "Other", description = "Keep", date = 1L, paymentMethod = "Cash", importBatchId = "other-batch"))
        vm.undoLastBatch()
        await { vm.lastBatchId.value == null }
        assertEquals(listOf("Keep"), app.repository.allTransactions.first().map { it.description })
    }
}
