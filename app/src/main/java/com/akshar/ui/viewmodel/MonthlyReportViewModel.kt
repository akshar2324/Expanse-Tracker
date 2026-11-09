package com.akshar.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.akshar.ExpenseTrackerApp
import com.akshar.reports.MonthlyReport
import com.akshar.reports.MonthlyReportPdf
import com.akshar.utils.CurrencyUtils
import com.akshar.utils.PeriodUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MonthlyReportViewModel internal constructor(
    application: Application,
    private val savedState: SavedStateHandle,
    private val ioDispatcher: CoroutineDispatcher,
    private val loadReport: suspend (String) -> MonthlyReport,
) : AndroidViewModel(application) {
    constructor(application: Application, savedState: SavedStateHandle) : this(
        application, savedState, Dispatchers.IO,
        { month -> (application as ExpenseTrackerApp).repository.monthlyReport(month) },
    )
    val month = savedState.getStateFlow("reportMonth", PeriodUtils.monthKey(System.currentTimeMillis()))
    private val mutableState = MutableStateFlow(ReportState())
    val state = mutableState.asStateFlow()

    fun selectMonth(value: String) {
        if (mutableState.value.busy) return
        savedState["reportMonth"] = value
        mutableState.value = ReportState()
    }

    /** Preview stays in memory. A private temporary PDF is created only after tapping Share. */
    fun build() {
        if (mutableState.value.busy) return
        mutableState.value = ReportState(busy = true)
        viewModelScope.launch {
            try {
                val report = withContext(ioDispatcher) {
                    loadReport(month.value)
                }
                mutableState.value = ReportState(report = report)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { mutableState.value = ReportState(error = "Could not build this report. Try again.") }
        }
    }

    fun share(onReady: (Uri) -> Unit) {
        val report = mutableState.value.report ?: return
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val uri = withContext(ioDispatcher) {
                    val application = getApplication<Application>()
                    val directory = File(application.cacheDir, "monthly_reports").apply { check(isDirectory || mkdirs()) }
                    directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000L }?.forEach { it.delete() }
                    val target = File.createTempFile("monthly-report-${report.month}-", ".pdf", directory)
                    try {
                        target.outputStream().use { MonthlyReportPdf.write(report, CurrencyUtils.symbolFor(CurrencyUtils.selectedCountry(application)), it) }
                    } catch (failure: Exception) { target.delete(); throw failure }
                    FileProvider.getUriForFile(application, "${application.packageName}.fileprovider", target)
                }
                mutableState.value = mutableState.value.copy(busy = false)
                onReady(uri)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { mutableState.value = mutableState.value.copy(busy = false, error = "Could not share the report. Try again.") }
        }
    }
}

data class ReportState(val report: MonthlyReport? = null, val busy: Boolean = false, val error: String? = null)
