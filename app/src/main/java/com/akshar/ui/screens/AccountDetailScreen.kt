package com.akshar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.data.model.Account
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.akshar.ui.screens.TransactionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    accountId: Long
) {
    val accounts by viewModel.accounts.collectAsState()
    val account = accounts.find { it.id == accountId }
    val transactions by viewModel.allTransactions.collectAsState()
    val accountTransactions = transactions.filter { it.accountId == accountId }.sortedByDescending { it.date }

    if (account == null) {
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("addEditAccount/${account.id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("reconcileAccount/${account.id}") }
            ) {
                Text("Reconcile")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val balance = account.openingBalance + accountTransactions.sumOf { tx ->
                if (tx.type == "INCOME") tx.amount else -tx.amount
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Balance", style = MaterialTheme.typography.titleMedium)
                    Text("₹${"%.2f".format(balance)}", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accountTransactions) { tx ->
                    TransactionItem(
                        transaction = tx,
                        onClick = {
                            if (tx.transferId != null) {
                                navController.navigate("addTransfer/${tx.transferId}")
                            } else {
                                navController.navigate("addTransaction/${tx.id}")
                            }
                        }
                    )
                }
            }
        }
    }
}
