package com.akshar.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akshar.reports.reportMoney
import com.akshar.ui.viewmodel.MonthlyReportViewModel
import com.akshar.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthlyReportScreen(viewModel: MonthlyReportViewModel = viewModel()) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currency = CurrencyUtils.symbolFor(CurrencyUtils.selectedCountry(context))
    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Choose a month, then build its report. Nothing is saved to your files automatically.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(modifier = Modifier.weight(1f), enabled = !state.busy, onClick = { viewModel.selectMonth(adjacentReportMonth(month, -1)) }) { Text("Previous month") }
            TextButton(modifier = Modifier.weight(1f), enabled = !state.busy, onClick = { viewModel.selectMonth(adjacentReportMonth(month, 1)) }) { Text("Next month") }
        }
        Text(month)
        Button(enabled = !state.busy, onClick = viewModel::build) { Text(if (state.busy) "Working..." else "Build report") }
        state.report?.let { report ->
            Text("In: ${reportMoney(report.income, currency)}")
            Text("Out: ${reportMoney(report.expense, currency)}")
            Text("Net: ${reportMoney(report.net, currency)}")
            Text("Includes spending by category, a bar chart, top 10 expenses and monthly budgets against actual spending. Transfers are excluded.")
            Button(enabled = !state.busy, onClick = {
                viewModel.share { uri ->
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData = ClipData.newRawUri("Monthly report", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, "Share monthly report"))
                }
            }) { Text("Share") }
        }
        state.error?.let { Text(it) }
    }
}

internal fun adjacentReportMonth(month: String, offset: Int): String {
    val format = SimpleDateFormat("yyyy-MM", Locale.US)
    val calendar = Calendar.getInstance().apply { time = requireNotNull(format.parse(month)); add(Calendar.MONTH, offset) }
    return format.format(calendar.time)
}
