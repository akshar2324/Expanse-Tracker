package com.akshar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.akshar.ui.components.*
import com.akshar.ui.theme.*
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.ui.screens.TransactionItem

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
    val currencySymbol = viewModel.getCurrencySymbol()

    if (account == null) {
        return
    }

    val balance = account.openingBalance + accountTransactions.sumOf { tx ->
        if (tx.type == "INCOME") tx.amount else -tx.amount
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                bottom = Spacing.xxxl + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                ScreenHeader(
                    title = account.name,
                    eyebrow = account.type,
                    onBack = { navController.popBackStack() },
                    insetHorizontally = false,
                    actions = {
                        GlassIconButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = "Edit account",
                            onClick = { navController.navigate("addEditAccount/${account.id}") }
                        )
                    }
                )
            }
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    fill = AkSpendTheme.glass.fillStrong
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = accountIcon(account.type), tint = MaterialTheme.colorScheme.primary, size = 48.dp)
                        Spacer(Modifier.width(Spacing.md))
                        Text("CURRENT BALANCE", style = MaterialTheme.typography.labelMedium.eyebrow(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "$currencySymbol${String.format("%,.2f", balance)}",
                        style = MaterialTheme.typography.displaySmall.money(),
                        color = if (balance >= 0) MaterialTheme.colorScheme.onSurface else AkSpendTheme.finance.expense
                    )
                    Text(
                        "Opening balance $currencySymbol${String.format("%,.2f", account.openingBalance)}",
                        style = MaterialTheme.typography.bodySmall.money(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { SectionHeader(title = "Transactions") }
            if (accountTransactions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.ReceiptLong,
                        title = "No transactions yet",
                        message = "Anything you log against this account shows up here."
                    )
                }
            } else {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
                        accountTransactions.forEachIndexed { index, tx ->
                            if (index > 0) GlassDivider()
                            TransactionItem(
                                transaction = tx,
                                currencySymbol = currencySymbol,
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

        ExtendedFloatingActionButton(
            onClick = { navController.navigate("reconcileAccount/${account.id}") },
            icon = { Icon(Icons.Rounded.FactCheck, contentDescription = null) },
            text = { Text("Reconcile", style = MaterialTheme.typography.labelLarge) },
            shape = PillShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(Spacing.gutter)
        )
    }
}
