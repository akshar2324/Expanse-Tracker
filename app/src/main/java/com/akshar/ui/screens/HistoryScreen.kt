package com.akshar.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterListOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.akshar.ui.components.*
import com.akshar.ui.theme.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akshar.data.model.Transaction
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.data.model.Account
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: FinanceViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    // --- Search & Filter States ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "EXPENSE", "INCOME"
    var selectedPaymentFilter by remember { mutableStateOf("ALL") } // "ALL", "Cash", "UPI", "Credit Card", "Debit Card", "Bank Transfer", "Wallet"
    var selectedTimeFilter by remember { mutableStateOf("ALL") } // "ALL", "WEEK", "MONTH", "YEAR"
    var selectedAccountFilter by remember { mutableStateOf<Long?>(null) } // null means "ALL"
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var sortBy by remember { mutableStateOf("NEWEST") } // "NEWEST", "OLDEST", "HIGHEST", "LOWEST"

    var showFilters by remember { mutableStateOf(false) }

    // Constants
    val paymentMethods = remember { listOf("ALL", "Cash", "UPI", "Credit Card", "Debit Card", "Bank Transfer", "Wallet") }
    val timeFilters = remember { listOf("ALL", "WEEK", "MONTH", "YEAR") }

    // --- Filter logic ---
    val filteredTransactions = remember(
        transactions, searchQuery, selectedTypeFilter, selectedPaymentFilter, selectedTimeFilter, selectedAccountFilter, sortBy
    ) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val weekAgo = calendar.run {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -7)
            timeInMillis
        }
        val monthAgo = calendar.run {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1) // start of currently loaded month
            timeInMillis
        }
        val yearAgo = calendar.run {
            timeInMillis = now
            set(Calendar.DAY_OF_YEAR, 1) // start of currently loaded year
            timeInMillis
        }

        var list = transactions.filter {
            // Search query match
            val matchesQuery = it.description.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)

            // Type filter match
            val matchesType = selectedTypeFilter == "ALL" || it.type == selectedTypeFilter

            // Payment method match
            val matchesPayment = selectedPaymentFilter == "ALL" || it.paymentMethod == selectedPaymentFilter

            // Account filter match
            val matchesAccount = if (selectedAccountFilter == null) true else it.accountId == selectedAccountFilter || (it.transferId != null && (it.accountId == selectedAccountFilter || transactions.any { relatedTx -> relatedTx.transferId == it.transferId && relatedTx.accountId == selectedAccountFilter }))

            // Time filter match
            val matchesTime = when (selectedTimeFilter) {
                "WEEK" -> it.date >= weekAgo
                "MONTH" -> it.date >= monthAgo
                "YEAR" -> it.date >= yearAgo
                else -> true
            }

            matchesQuery && matchesType && matchesPayment && matchesTime && matchesAccount
        }

        // Sorting
        list = when (sortBy) {
            "OLDEST" -> list.sortedBy { it.date }
            "HIGHEST" -> list.sortedByDescending { it.amount }
            "LOWEST" -> list.sortedBy { it.amount }
            else -> list.sortedByDescending { it.date } // "NEWEST" Default
        }

        list
    }

    val currencySymbol = viewModel.getCurrencySymbol()
    val filtersActive = selectedTypeFilter != "ALL" || selectedPaymentFilter != "ALL" || selectedTimeFilter != "ALL" || searchQuery.isNotEmpty()
    val groupByDay = sortBy == "NEWEST" || sortBy == "OLDEST"
    val groups = remember(filteredTransactions, groupByDay) {
        if (groupByDay) filteredTransactions.groupBy { dayKey(it.date) }.toList()
        else listOf("" to filteredTransactions)
    }
    val finance = AkSpendTheme.finance
    val totalIn = filteredTransactions.filter { it.type == "INCOME" && it.transferId == null }.sumOf { it.amount }
    val totalOut = filteredTransactions.filter { it.type == "EXPENSE" && it.transferId == null }.sumOf { it.amount }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            bottom = LocalFloatingBarInset.current + Spacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "header") {
            ScreenHeader(
                title = "Activity",
                subtitle = "Every expense, income and transfer",
                insetHorizontally = false,
                actions = {
                    GlassIconButton(
                        icon = if (showFilters) Icons.Rounded.FilterListOff else Icons.Rounded.Tune,
                        contentDescription = if (showFilters) "Hide filters" else "Show filters",
                        onClick = { showFilters = !showFilters },
                        tint = if (filtersActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        item(key = "search") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by description or category") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = PillShape,
                colors = glassTextFieldColors()
            )
        }

        item(key = "filters") {
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(motionSpec()) + fadeIn(motionSpec()),
                exit = shrinkVertically(motionSpec(Durations.quick)) + fadeOut(motionSpec(Durations.quick))
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.lg)) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FilterGroupLabel("Type")
                        ScrollableFilterRow(
                            items = listOf("ALL", "EXPENSE", "INCOME"),
                            selectedItem = selectedTypeFilter,
                            onSelect = { selectedTypeFilter = it },
                            labelFor = { if (it == "ALL") "All" else it.lowercase().replaceFirstChar { c -> c.uppercase() } }
                        )
                        FilterGroupLabel("Sort by")
                        ScrollableFilterRow(
                            items = listOf("NEWEST", "OLDEST", "HIGHEST", "LOWEST"),
                            selectedItem = sortBy,
                            onSelect = { sortBy = it },
                            labelFor = {
                                when (it) {
                                    "NEWEST" -> "Newest"
                                    "OLDEST" -> "Oldest"
                                    "HIGHEST" -> "Highest amount"
                                    else -> "Lowest amount"
                                }
                            }
                        )
                        FilterGroupLabel("Payment method")
                        ScrollableFilterRow(
                            items = paymentMethods,
                            selectedItem = selectedPaymentFilter,
                            onSelect = { selectedPaymentFilter = it }
                        )
                        FilterGroupLabel("Date range")
                        ScrollableFilterRow(
                            items = timeFilters,
                            selectedItem = selectedTimeFilter,
                            onSelect = { selectedTimeFilter = it },
                            labelFor = {
                                when (it) {
                                    "WEEK" -> "Last 7 days"
                                    "MONTH" -> "This month"
                                    "YEAR" -> "This year"
                                    else -> "All time"
                                }
                            }
                        )
                    }
                }
            }
        }

        item(key = "summary") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.touchTarget),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${filteredTransactions.size} ${if (filteredTransactions.size == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "+$currencySymbol${String.format("%,.0f", totalIn)}  ·  -$currencySymbol${String.format("%,.0f", totalOut)}",
                        style = MaterialTheme.typography.bodySmall.money(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (filtersActive) {
                    TextButton(
                        onClick = {
                            selectedTypeFilter = "ALL"
                            selectedPaymentFilter = "ALL"
                            selectedTimeFilter = "ALL"
                            searchQuery = ""
                            sortBy = "NEWEST"
                        }
                    ) {
                        Text("Reset filters", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = if (transactions.isEmpty()) "Nothing here yet" else "No matches",
                    message = if (transactions.isEmpty()) "Transactions you add will appear here, newest first."
                    else "Try a different search or clear the filters."
                )
            }
        } else {
            groups.forEach { (day, dayTransactions) ->
                if (day.isNotEmpty()) {
                    item(key = "day-$day") {
                        val net = dayTransactions.filter { it.transferId == null }.sumOf { if (it.type == "EXPENSE") -it.amount else it.amount }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.sm, start = Spacing.xs, end = Spacing.xs),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = "${if (net >= 0) "+" else "-"}$currencySymbol${String.format("%,.0f", kotlin.math.abs(net))}",
                                style = MaterialTheme.typography.labelLarge.money(),
                                color = if (net >= 0) finance.income else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item(key = "group-$day-${dayTransactions.first().id}") {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = Spacing.xs)
                    ) {
                        dayTransactions.forEachIndexed { index, transaction ->
                            if (index > 0) GlassDivider()
                            TransactionItem(
                                transaction = transaction,
                                currencySymbol = currencySymbol,
                                onClick = {
                                    if (transaction.transferId != null) {
                                        navController.navigate("addTransfer/${transaction.transferId}")
                                    } else {
                                        navController.navigate("addTransaction/${transaction.id}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun dayKey(timestamp: Long): String {
    val today = Calendar.getInstance()
    val day = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameYear = today.get(Calendar.YEAR) == day.get(Calendar.YEAR)
    val dayDiff = today.get(Calendar.DAY_OF_YEAR) - day.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "Today"
        sameYear && dayDiff == 1 -> "Yesterday"
        sameYear -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
private fun FilterGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.xs)
    )
}

// Horizontal row of selectable capsules; each one is a full 48dp touch target.
@Composable
fun ScrollableFilterRow(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit,
    labelFor: (String) -> String = { if (it == "ALL") "All payments" else it }
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(items) { item ->
            FilterChip(
                selected = item == selectedItem,
                onClick = { onSelect(item) },
                label = { Text(labelFor(item), style = MaterialTheme.typography.labelLarge) },
                shape = PillShape,
                modifier = Modifier.heightIn(min = Spacing.touchTarget),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = AkSpendTheme.glass.fill,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

// Helpers
fun List<Transaction>.sortedByByAmountDescending() = sortedByDescending { it.amount }

@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit, currencySymbol: String = "₹") {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val isExpense = transaction.type == "EXPENSE"
    val isTransfer = transaction.transferId != null
    val finance = AkSpendTheme.finance

    GlassListRow(
        title = transaction.description.ifBlank { transaction.category },
        subtitle = listOfNotNull(
            if (isTransfer) "Transfer" else transaction.category,
            transaction.paymentMethod.takeIf { it.isNotBlank() },
            formatter.format(Date(transaction.date))
        ).joinToString(" · "),
        leading = {
            IconBadge(
                icon = if (isTransfer) Icons.Rounded.SwapHoriz else categoryIcon(transaction.category),
                tint = if (isTransfer) finance.transfer else categoryTint(transaction.category),
                size = 44.dp
            )
        },
        trailing = {
            Text(
                text = "${if (isExpense) "-" else "+"}$currencySymbol${String.format("%,.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleSmall.money(),
                color = when {
                    isTransfer -> MaterialTheme.colorScheme.onSurfaceVariant
                    isExpense -> finance.expense
                    else -> finance.income
                }
            )
        },
        onClick = onClick
    )
}
