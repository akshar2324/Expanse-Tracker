package com.akshar.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.akshar.ui.components.*
import com.akshar.ui.theme.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.akshar.data.model.Account
import com.akshar.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    accountId: Long? = null
) {
    val accounts by viewModel.accounts.collectAsState()
    val existingAccount = accounts.find { it.id == accountId }

    var name by remember { mutableStateOf(existingAccount?.name ?: "") }
    var type by remember { mutableStateOf(existingAccount?.type ?: "Cash") }
    var openingBalance by remember { mutableStateOf(existingAccount?.openingBalance?.toString() ?: "") }
    var isArchived by remember { mutableStateOf(existingAccount?.isArchived ?: false) }
    var typeExpanded by remember { mutableStateOf(false) }
    val types = listOf("Cash", "Bank", "Wallet", "Savings", "Credit Card")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
    ) {
        ScreenHeader(title = if (existingAccount == null) "New account" else "Edit account", onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            OutlinedTextField(
                colors = glassTextFieldColors(),
                value = name,
                onValueChange = { name = it },
                label = { Text("Account name") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    types.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = {
                                type = t
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                colors = glassTextFieldColors(),
                value = openingBalance,
                onValueChange = { openingBalance = it },
                label = { Text("Opening balance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (existingAccount != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Archive (hide from new transactions)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = isArchived, onCheckedChange = { isArchived = it })
                }
            }

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val amount = openingBalance.toDoubleOrNull() ?: 0.0
                        val account = Account(
                            id = existingAccount?.id ?: 0L,
                            name = name,
                            type = type,
                            openingBalance = amount,
                            isArchived = isArchived
                        )
                        viewModel.addOrUpdateAccount(account)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save account", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
