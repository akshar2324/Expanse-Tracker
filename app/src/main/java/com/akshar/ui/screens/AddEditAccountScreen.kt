package com.akshar.ui.screens

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingAccount == null) "Add Account" else "Edit Account") },
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Account Name") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account Type") },
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
                value = openingBalance,
                onValueChange = { openingBalance = it },
                label = { Text("Opening Balance") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (existingAccount != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Archived (hide from new transactions)")
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
