package com.akshar.reports

import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akshar.data.model.Budget
import com.akshar.data.model.Transaction
import com.akshar.ExpenseTrackerApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class MonthlyReportPdfTest {
    @Test fun writesDemoReportAndPaginatesLargeTables() = runBlocking {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ExpenseTrackerApp
        val repository = application.repository
        val backup = withContext(Dispatchers.IO) { repository.exportSnapshot() }
        try {
            val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
            val zone = TimeZone.getTimeZone("UTC")
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.parse("2026-11-15")!!.time
            val categories = listOf("Food", "Transport", "Home", "Health", "Leisure", "Learning")
            val rows = categories.mapIndexed { index, category -> Transaction(amount = (index + 1) * 125.25, type = "EXPENSE", category = category,
                description = "Demo only", date = date, paymentMethod = "Cash") } + Transaction(amount = 5000.0, type = "INCOME", category = "Income", description = "Demo only", date = date, paymentMethod = "Cash")
            val budgets = categories.map { Budget(category = it, limitAmount = 800.0, month = "2026-11") }
            val report = MonthlyReports.build("2026-11", rows, budgets, zone)
            val demo = File(cache, "monthly-report-demo.pdf")
            demo.outputStream().use { MonthlyReportPdf.write(report, "₹", it) }
            assertTrue(demo.readBytes().take(5).toByteArray().toString(Charsets.US_ASCII).startsWith("%PDF-"))
            ParcelFileDescriptor.open(demo, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { assertTrue(it.pageCount >= 2) }
            }
            val large = File(cache, "monthly-report-pagination.pdf")
            large.outputStream().use { output -> MonthlyReportPdf.write(report.copy(categories = (1..90).map { ReportCategory("Category $it " + "long name ".repeat(15), report.expense) }), "$", output) }
            ParcelFileDescriptor.open(large, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { assertTrue(it.pageCount >= 5) }
            }
        } finally {
            withContext(Dispatchers.IO) {
                repository.replaceAllData(backup)
                assertEquals(backup, repository.exportSnapshot())
            }
        }
    }
}
