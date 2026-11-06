package com.akshar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.akshar.data.model.Account
import com.akshar.ui.components.*
import com.akshar.ui.theme.*
import com.akshar.ui.viewmodel.FinanceViewModel

@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel,
    navController: NavController
) {
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val currencySymbol = viewModel.getCurrencySymbol()

    val balances = accounts.associate { account ->
        val accountTransactions = transactions.filter { it.accountId == account.id }
        account.id to account.openingBalance + accountTransactions.sumOf { tx ->
            if (tx.type == "INCOME") tx.amount else -tx.amount
        }
    }
    val total = balances.values.sum()

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
                    title = "Accounts",
                    onBack = { navController.popBackStack() },
                    insetHorizontally = false,
                    actions = {
                        GlassIconButton(
                            icon = Icons.Rounded.SwapHoriz,
                            contentDescription = "New transfer",
                            onClick = { navController.navigate("addTransfer") }
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
                    Text("TOTAL ACROSS ACCOUNTS", style = MaterialTheme.typography.labelMedium.eyebrow(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "$currencySymbol${String.format("%,.2f", total)}",
                        style = MaterialTheme.typography.displaySmall.money(),
                        color = if (total >= 0) MaterialTheme.colorScheme.onSurface else AkSpendTheme.finance.expense
                    )
                    Text(
                        "${accounts.size} ${if (accounts.size == 1) "account" else "accounts"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (accounts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "No accounts yet",
                        message = "Add your bank, cash or card accounts to see each balance separately."
                    )
                }
            } else {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
                        accounts.forEachIndexed { index, account ->
                            if (index > 0) GlassDivider()
                            AccountCard(
                                account = account,
                                balance = balances[account.id] ?: account.openingBalance,
                                currencySymbol = currencySymbol,
                                onClick = { navController.navigate("accountDetail/${account.id}") }
                            )
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { navController.navigate("addEditAccount") },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Add account", style = MaterialTheme.typography.labelLarge) },
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

internal fun accountIcon(type: String): ImageVector = when (type.lowercase()) {
    "cash" -> Icons.Rounded.Payments
    "savings" -> Icons.Rounded.Savings
    "credit card", "card" -> Icons.Rounded.CreditCard
    "wallet" -> Icons.Rounded.AccountBalanceWallet
    else -> Icons.Rounded.AccountBalance
}

@Composable
fun AccountCard(account: Account, balance: Double, onClick: () -> Unit, currencySymbol: String = "₹") {
    val balanceText = "$currencySymbol${String.format("%,.2f", balance)}"
    GlassListRow(
        title = account.name,
        subtitle = account.type,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${account.name}, ${account.type}, balance $balanceText"
        },
        leading = { IconBadge(icon = accountIcon(account.type), tint = MaterialTheme.colorScheme.primary, size = 44.dp) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = balanceText,
                    style = MaterialTheme.typography.titleSmall.money(),
                    color = if (balance >= 0) MaterialTheme.colorScheme.onSurface else AkSpendTheme.finance.expense
                )
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        onClick = onClick
    )
}
