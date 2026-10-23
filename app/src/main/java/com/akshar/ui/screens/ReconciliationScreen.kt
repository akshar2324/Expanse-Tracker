package com.akshar.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.akshar.ui.components.*
import com.akshar.ui.theme.*
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

    val formatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val currencySymbol = viewModel.getCurrencySymbol()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader(title = "Reconcile ${account.name}", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            OutlinedTextField(
                colors = glassTextFieldColors(),
                value = formatter.format(Date(selectedDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Statement date") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Choose statement date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                colors = glassTextFieldColors(),
                value = statementBalance,
                onValueChange = { statementBalance = it },
                label = { Text("Statement balance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            GlassCard(modifier = Modifier.fillMaxWidth(), fill = AkSpendTheme.glass.fillStrong) {
                val matched = abs(difference) < 0.01
                ReconcileLine("Calculated balance", "$currencySymbol${String.format("%,.2f", calculatedBalance)}")
                Spacer(modifier = Modifier.height(Spacing.sm))
                ReconcileLine("Statement balance", "$currencySymbol${String.format("%,.2f", parsedStatementBalance)}")
                Spacer(modifier = Modifier.height(Spacing.md))
                GlassDivider(Modifier.padding(horizontal = 0.dp))
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = if (matched) "Balances match" else "Difference",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$currencySymbol${String.format("%,.2f", abs(difference))}",
                    color = if (matched) AkSpendTheme.finance.income else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineSmall.money()
                )
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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = statementBalance.isNotBlank()
            ) {
                Text("Confirm reconciliation", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ReconcileLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall.money(), color = MaterialTheme.colorScheme.onSurface)
    }
}
