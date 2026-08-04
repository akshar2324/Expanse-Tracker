package com.akshar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.akshar.data.model.Account
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.ui.theme.ExpenseRed
import com.akshar.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel,
    navController: NavController
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addEditAccount") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accounts) { account ->
                val accountTransactions = transactions.filter { it.accountId == account.id }
                val balance = account.openingBalance + accountTransactions.sumOf { tx ->
                    if (tx.type == "INCOME") tx.amount else -tx.amount
                }

                AccountCard(
                    account = account,
                    balance = balance,
                    onClick = { navController.navigate("accountDetail/${account.id}") }
                )
            }
        }
    }
}

@Composable
fun AccountCard(account: Account, balance: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = account.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = account.type, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = "₹${"%.2f".format(balance)}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (balance >= 0) IncomeGreen else ExpenseRed
            )
        }
    }
}
