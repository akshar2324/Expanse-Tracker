package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Budget
import com.example.data.model.SavingsGoal
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSavingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.allBudgets.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val savingsGoals by viewModel.allSavingsGoals.collectAsState()

    var activeTab by remember { mutableStateOf("BUDGETS") } // "BUDGETS", "SAVINGS"

    // Dialog state controllers
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddSavingsFundsDialog by remember { mutableStateOf<SavingsGoal?>(null) }

    // Month strings
    val currentMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = { Text("Financial Goals & Budgets", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0.dp)
                )

                // Sub-tab selectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("BUDGETS", "SAVINGS").forEach { tab ->
                        val active = activeTab == tab
                        val textCol = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        val bg = if (active) MaterialTheme.colorScheme.primary else Color.Transparent

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = tab }
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tab == "BUDGETS") "Monthly Budgets" else "Savings Goals",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textCol
                                )
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeTab == "BUDGETS") showAddBudgetDialog = true else showAddGoalDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal or Budget")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if (activeTab == "BUDGETS") {
                // --- CATEGORY BUDGETS LIST ---
                if (budgets.isEmpty()) {
                    item {
                        EmptyVisualState(
                            message = "No monthly budgets set.",
                            subtext = "Tap + to allocate category limits and control expenses!"
                        )
                    }
                } else {
                    val filteredBudgets = budgets.filter { it.month == currentMonthStr }
                    items(filteredBudgets) { budget ->
                        // Calculate total spent for this category in the current month
                        val spent = transactions.filter {
                            it.type == "EXPENSE" &&
                                    it.category.equals(budget.category, ignoreCase = true) &&
                                    isSameMonth(it.date, currentMonthStr)
                        }.sumOf { it.amount }

                        BudgetRow(
                            budget = budget,
                            spent = spent,
                            onDelete = { viewModel.deleteBudgetById(budget.id) }
                        )
                    }
                }
            } else {
                // --- SAVINGS GOALS LIST ---
                if (savingsGoals.isEmpty()) {
                    item {
                        EmptyVisualState(
                            message = "No savings tracking goals yet.",
                            subtext = "Start saving! Map a target like 'MacBook Fund' to stay driven."
                        )
                    }
                } else {
                    items(savingsGoals) { goal ->
                        SavingsGoalRow(
                            goal = goal,
                            onAddSavings = { showAddSavingsFundsDialog = goal },
                            onDelete = { viewModel.deleteSavingsGoalById(goal.id) }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG: New Budget Entry ---
    if (showAddBudgetDialog) {
        AddBudgetDialog(
            currentMonth = currentMonthStr,
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { cat, lim ->
                viewModel.addBudget(cat, lim, currentMonthStr)
                showAddBudgetDialog = false
            }
        )
    }

    // --- DIALOG: New Savings Goal Entry ---
    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, tgt, cur, dt ->
                viewModel.addSavingsGoal(name, tgt, cur, dt)
                showAddGoalDialog = false
            }
        )
    }

    // --- DIALOG: Contribute Savings Funds Entry ---
    if (showAddSavingsFundsDialog != null) {
        AddSavingsFundsDialog(
            goalName = showAddSavingsFundsDialog!!.name,
            onDismiss = { showAddSavingsFundsDialog = null },
            onConfirm = { funds ->
                viewModel.addSavingsProgress(showAddSavingsFundsDialog!!, funds)
                showAddSavingsFundsDialog = null
            }
        )
    }
}

// --- Dynamic Row Components ---

@Composable
fun BudgetRow(
    budget: Budget,
    spent: Double,
    onDelete: () -> Unit
) {
    val pct = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
    val progressFraction = pct.coerceIn(0.0, 1.0).toFloat()

    // Threshold coloring
    val barColor = when {
        pct >= 1.0 -> ExpenseRedDark
        pct >= 0.8 -> AlertOrangeDark
        else -> IncomeGreenDark
    }

    val warningLabel = when {
        pct >= 1.0 -> "OVER BUDGET ALERT! ⚠️"
        pct >= 0.8 -> "Warning! 80% threshold exceeded 🔔"
        else -> "Remaining Budget is stable"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = budget.category.uppercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = warningLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove Budget",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: ₹${spent.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Limit: ₹${budget.limitAmount.toInt()}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun SavingsGoalRow(
    goal: SavingsGoal,
    onAddSavings: () -> Unit,
    onDelete: () -> Unit
) {
    val pct = if (goal.targetAmount > 0) goal.currentAmount / goal.targetAmount else 0.0
    val progressFraction = pct.coerceIn(0.0, 1.0).toFloat()
    val percentString = "${(pct * 100).toInt()}%"

    // Estimated completion date rule
    val estimatedDate = goal.targetDate.ifEmpty { "Estimated: 6 months or based on income rate" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = goal.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Estimate completion: $estimatedDate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Row {
                    IconButton(onClick = onAddSavings) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add Funds",
                            tint = IncomeGreenDark
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Goal",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = AnalyticsBlueDark,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saved: ₹${goal.currentAmount.toInt()} ($percentString)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Goal Target: ₹${goal.targetAmount.toInt()}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun EmptyVisualState(message: String, subtext: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.GolfCourse,
            contentDescription = "Goal Target",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        )
    }
}

// --- Inner Dialog Implementations ---

@Composable
fun AddBudgetDialog(
    currentMonth: String,
    onDismiss: () -> Unit,
    onConfirm: (category: String, limit: Double) -> Unit
) {
    var category by remember { mutableStateOf("Food") }
    var limitStr by remember { mutableStateOf("") }

    val categories = remember {
        listOf(
            "Food", "Grocery", "Transportation", "Fuel", "Shopping",
            "Entertainment", "Bills", "EMI", "Rent", "Health",
            "Education", "Travel", "Gifts", "Subscription", "Other"
        )
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Category Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Category", style = MaterialTheme.typography.labelSmall)
                // Dropdown category
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDropdownExpanded = true }
                        .padding(vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = category, fontSize = 16.sp)
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Limit Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitStr.toDoubleOrNull() ?: 0.0
                    if (limit > 0) {
                        onConfirm(category, limit)
                    }
                }
            ) {
                Text("Set Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: Double, current: Double, date: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var currentStr by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Target Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. MacBook Study fund)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Goal Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentStr,
                    onValueChange = { currentStr = it },
                    label = { Text("Initial Saved Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Estimated Completion Date (Dec 2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetStr.toDoubleOrNull() ?: 0.0
                    val current = currentStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotEmpty() && target > 0) {
                        onConfirm(name, target, current, targetDate)
                    }
                }
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSavingsFundsDialog(
    goalName: String,
    onDismiss: () -> Unit,
    onConfirm: (funds: Double) -> Unit
) {
    var fundStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contribute to $goalName") },
        text = {
            OutlinedTextField(
                value = fundStr,
                onValueChange = { fundStr = it },
                label = { Text("Contribution Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val funds = fundStr.toDoubleOrNull() ?: 0.0
                    if (funds > 0) {
                        onConfirm(funds)
                    }
                }
            ) {
                Text("Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper: check if transaction timestamp is in the specified yyyy-MM
private fun isSameMonth(timestamp: Long, monthYYYYMM: String): Boolean {
    val df = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    return df.format(Date(timestamp)) == monthYYYYMM
}
