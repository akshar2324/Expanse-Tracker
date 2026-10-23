package com.akshar.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.akshar.ui.theme.*
import com.akshar.ui.components.*
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
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.data.model.Account
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransferScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    transferId: String? = null
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val activeAccounts = accounts.filter { !it.isArchived }

    // Default or existing state
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var fromAccountId by remember { mutableStateOf(activeAccounts.firstOrNull()?.id ?: 1L) }
    var toAccountId by remember { mutableStateOf(activeAccounts.getOrNull(1)?.id ?: activeAccounts.firstOrNull()?.id ?: 1L) }
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

    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader(
            title = if (transferId == null) "New transfer" else "Edit transfer",
            subtitle = "Move money between your own accounts",
            onBack = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            ExposedDropdownMenuBox(
                expanded = fromExpanded,
                onExpandedChange = { fromExpanded = !fromExpanded }
            ) {
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = activeAccounts.find { it.id == fromAccountId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("From account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = fromExpanded,
                    onDismissRequest = { fromExpanded = false }
                ) {
                    activeAccounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name) },
                            onClick = {
                                fromAccountId = acc.id
                                fromExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = toExpanded,
                onExpandedChange = { toExpanded = !toExpanded }
            ) {
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = activeAccounts.find { it.id == toAccountId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("To account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = toExpanded,
                    onDismissRequest = { toExpanded = false }
                ) {
                    activeAccounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.name) },
                            onClick = {
                                toAccountId = acc.id
                                toExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                colors = glassTextFieldColors(),
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount (${viewModel.getCurrencySymbol()})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.headlineSmall.money(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                colors = glassTextFieldColors(),
                value = description,
                onValueChange = { description = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                colors = glassTextFieldColors(),
                value = formatter.format(Date(selectedDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Choose transfer date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (parsedAmount > 0 && fromAccountId != toAccountId) {
                        if (transferId != null) {
                            viewModel.deleteTransfer(transferId)
                        }
                        viewModel.saveTransfer(
                            fromAccountId = fromAccountId,
                            toAccountId = toAccountId,
                            amount = parsedAmount,
                            description = description,
                            date = selectedDate,
                            transferId = transferId ?: UUID.randomUUID().toString()
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = amount.isNotBlank() && fromAccountId != toAccountId
            ) {
                Text("Save transfer", style = MaterialTheme.typography.titleMedium)
            }

            if (transferId != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteTransfer(transferId)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(Spacing.touchTarget),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete transfer", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
