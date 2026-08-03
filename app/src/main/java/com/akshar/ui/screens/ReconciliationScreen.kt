package com.akshar.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.akshar.data.model.Reconciliation
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.data.model.Account
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    accountId: Long
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    val account = accounts.find { it.id == accountId }
    if (account == null) {
        return
    }

    var statementBalance by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            selectedDate = cal.timeInMillis
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    // Calculate balance up to the selected date
    val accountTransactions = transactions.filter { it.accountId == accountId && it.date <= selectedDate }
    val calculatedBalance = account.openingBalance + accountTransactions.sumOf { tx ->
        if (tx.type == "INCOME") tx.amount else -tx.amount
    }

    val parsedStatementBalance = statementBalance.toDoubleOrNull() ?: 0.0
    val difference = parsedStatementBalance - calculatedBalance

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reconcile ${account.name}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = formatter.format(Date(selectedDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Statement Date") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = statementBalance,
                onValueChange = { statementBalance = it },
                label = { Text("Statement Balance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Calculated Balance: ₹${"%.2f".format(calculatedBalance)}")
                    Text("Statement Balance: ₹${"%.2f".format(parsedStatementBalance)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Difference: ₹${"%.2f".format(abs(difference))}",
                        color = if (abs(difference) < 0.01) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Button(
                onClick = {
                    val rec = Reconciliation(
                        accountId = accountId,
                        statementBalance = parsedStatementBalance,
                        calculatedBalance = calculatedBalance,
                        date = selectedDate
                    )
                    viewModel.addReconciliation(rec)
                    // If difference is significant, we could optionally prompt to create an adjustment transaction.
                    // For now, just save the reconciliation checkpoint and pop.
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = statementBalance.isNotBlank()
            ) {
                Text("Confirm Reconciliation")
            }
        }
    }
}
