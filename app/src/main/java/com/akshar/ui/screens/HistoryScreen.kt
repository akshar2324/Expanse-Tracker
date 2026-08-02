package com.akshar.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshar.data.model.Transaction
import com.akshar.ui.theme.ExpenseRed
import com.akshar.ui.theme.IncomeGreen
import com.akshar.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    // --- Search & Filter States ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "EXPENSE", "INCOME"
    var selectedPaymentFilter by remember { mutableStateOf("ALL") } // "ALL", "Cash", "UPI", "Credit Card", "Debit Card", "Bank Transfer", "Wallet"
    var selectedTimeFilter by remember { mutableStateOf("ALL") } // "ALL", "WEEK", "MONTH", "YEAR"
    var sortBy by remember { mutableStateOf("NEWEST") } // "NEWEST", "OLDEST", "HIGHEST", "LOWEST"

    var showFilters by remember { mutableStateOf(false) }

    // Constants
    val paymentMethods = remember { listOf("ALL", "Cash", "UPI", "Credit Card", "Debit Card", "Bank Transfer", "Wallet") }
    val timeFilters = remember { listOf("ALL", "WEEK", "MONTH", "YEAR") }

    // --- Filter logic ---
    val filteredTransactions = remember(
        transactions, searchQuery, selectedTypeFilter, selectedPaymentFilter, selectedTimeFilter, sortBy
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

            // Time filter match
            val matchesTime = when (selectedTimeFilter) {
                "WEEK" -> it.date >= weekAgo
                "MONTH" -> it.date >= monthAgo
                "YEAR" -> it.date >= yearAgo
                else -> true
            }

            matchesQuery && matchesType && matchesPayment && matchesTime
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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("Financial Ledger") },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Toggle Filters"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            // --- Elegant Search Field ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }

            // --- Expandable Filters Drawer Area ---
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // --- 1. Type filter chips (All, Expense, Income) ---
                        Text("Transaction Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL", "EXPENSE", "INCOME").forEach { type ->
                                FilterChip(
                                    selected = selectedTypeFilter == type,
                                    onClick = { selectedTypeFilter = type },
                                    label = { Text(type) }
                                )
                            }
                        }

                        // --- 2. Sorting select ---
                        Text("Sort Results", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("NEWEST", "OLDEST", "HIGHEST", "LOWEST").forEach { sortOption ->
                                FilterChip(
                                    selected = sortBy == sortOption,
                                    onClick = { sortBy = sortOption },
                                    label = { Text(sortOption) }
                                )
                            }
                        }

                        // --- 3. Payment Filter Segment ---
                        Text("Payment Method", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Render a custom smaller selection block
                            ScrollableFilterRow(
                                items = paymentMethods,
                                selectedItem = selectedPaymentFilter,
                                onSelect = { selectedPaymentFilter = it }
                            )
                        }

                        // --- 4. Date range filter ---
                        Text("Date Range", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            timeFilters.forEach { tf ->
                                FilterChip(
                                    selected = selectedTimeFilter == tf,
                                    onClick = { selectedTimeFilter = tf },
                                    label = {
                                        Text(
                                            when (tf) {
                                                "WEEK" -> "This Week"
                                                "MONTH" -> "This Month"
                                                "YEAR" -> "This Year"
                                                else -> "All Time"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- Header Statistics Info ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${filteredTransactions.size} transactions",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                )
                if (selectedTypeFilter != "ALL" || selectedPaymentFilter != "ALL" || selectedTimeFilter != "ALL" || searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectedTypeFilter = "ALL"
                            selectedPaymentFilter = "ALL"
                            selectedTimeFilter = "ALL"
                            searchQuery = ""
                            sortBy = "NEWEST"
                        }
                    ) {
                        Text("Reset Filters", fontSize = 12.sp)
                    }
                }
            }

            // --- History ledger listings ---
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterNone,
                            contentDescription = "No Matches Found",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching transactions.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = "Try clearing search keywords or filters.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTransactions) { transaction ->
                        TransactionItemRow(
                            transaction = transaction,
                            onDelete = { viewModel.deleteTransaction(transaction) }
                        )
                    }
                }
            }
        }
    }
}

// Custom horizontal filter helper
@Composable
fun ScrollableFilterRow(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { it ->
            val active = it == selectedItem
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                            alpha = 0.6f
                        )
                    )
                    .clickable { onSelect(it) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (it == "ALL") "All Payments" else it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// Helpers
fun List<Transaction>.sortedByByAmountDescending() = sortedByDescending { it.amount }
