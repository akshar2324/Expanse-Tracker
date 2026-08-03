package com.akshar.ui.screens

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshar.data.model.Budget
import com.akshar.data.model.SavingsGoal
import com.akshar.ui.theme.*
import com.akshar.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSavingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val transactions = allTransactions.filter { it.transferId == null }
    val savingsGoals by viewModel.allSavingsGoals.collectAsStateWithLifecycle()

    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()

    var activeTab by remember { mutableStateOf("BUDGETS") } // "BUDGETS", "SAVINGS"

    // Dialog state controllers
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddSavingsFundsDialog by remember { mutableStateOf<SavingsGoal?>(null) }

    // Month strings
    val currentMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val currentWeekStr = remember { getCurrentWeekKey() }

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
                        .clip(MaterialTheme.shapes.medium)
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
                                .clip(MaterialTheme.shapes.medium)
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
                val activeBudgets = budgets.filter {
                    (it.frequency == "WEEKLY" && it.month == currentWeekStr) ||
                    (it.frequency == "MONTHLY" && it.month == currentMonthStr) ||
                    (it.frequency.isBlank() && it.month == currentMonthStr)
                }

                if (activeBudgets.isEmpty()) {
                    item {
                        EmptyVisualState(
                            message = "No active budgets found.",
                            subtext = "Tap + to allocate category limits and control expenses!"
                        )
                    }
                } else {
                    items(activeBudgets) { budget ->
                        // Calculate total spent for this category in the current period
                        val spent = transactions.filter {
                            it.type == "EXPENSE" &&
                                    it.category.equals(budget.category, ignoreCase = true) &&
                                    (if (budget.frequency == "WEEKLY") isSameWeek(it.date, budget.month) else isSameMonth(it.date, budget.month))
                        }.sumOf { it.amount }

                        BudgetRow(
                            budget = budget,
                            spent = spent,
                            onDelete = { viewModel.deleteBudgetById(budget.id) },
                            currencySymbol = currencySymbol
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
                            onDelete = { viewModel.deleteSavingsGoalById(goal.id) },
                            currencySymbol = currencySymbol
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG: New Budget Entry ---
    if (showAddBudgetDialog) {
        val currentWeekKey = remember { getCurrentWeekKey() }
        AddBudgetDialog(
            currentMonth = currentMonthStr,
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { cat, lim, freq, reminderOn, threshold ->
                val periodKey = if (freq == "WEEKLY") currentWeekKey else currentMonthStr
                viewModel.addBudget(
                    category = cat,
                    limitAmount = lim,
                    month = periodKey,
                    frequency = freq,
                    remindersEnabled = reminderOn,
                    reminderThreshold = threshold
                )
                showAddBudgetDialog = false
            },
            currencySymbol = currencySymbol
        )
    }

    // --- DIALOG: New Savings Goal Entry ---
    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, tgt, cur, dt ->
                viewModel.addSavingsGoal(name, tgt, cur, dt)
                showAddGoalDialog = false
            },
            currencySymbol = currencySymbol
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
            },
            currencySymbol = currencySymbol
        )
    }
}

// --- Dynamic Row Components ---

@Composable
fun BudgetRow(
    budget: Budget,
    spent: Double,
    onDelete: () -> Unit,
    currencySymbol: String = "₹"
) {
    val pct = if (budget.limitAmount > 0) spent / budget.limitAmount else 0.0
    val progressFraction = pct.coerceIn(0.0, 1.0).toFloat()

    // Threshold coloring
    val barColor = when {
        pct >= 1.0 -> ExpenseRedDark
        pct >= (budget.reminderThreshold / 100.0) -> AlertOrangeDark
        else -> IncomeGreenDark
    }

    val warningLabel = when {
        pct >= 1.0 -> "LIMIT EXCEEDED ALERT! ⚠️"
        budget.remindersEnabled && (pct >= (budget.reminderThreshold / 100.0)) -> "Alert: ${budget.reminderThreshold}% boundary crossed! 🔔"
        pct >= 0.8 -> "Warning! 80% threshold exceeded"
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = budget.category.uppercase(),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        // Frequency Badge
                        val freqText = budget.frequency.uppercase().ifEmpty { "MONTHLY" }
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = freqText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
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
                    .clip(MaterialTheme.shapes.medium),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: $currencySymbol${String.format(Locale.getDefault(), "%.1f", spent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Limit: $currencySymbol${budget.limitAmount.toInt()}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (budget.remindersEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Watcher Active: Multi-channel warning logs armed below ${budget.reminderThreshold}% spent limit",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsGoalRow(
    goal: SavingsGoal,
    onAddSavings: () -> Unit,
    onDelete: () -> Unit,
    currencySymbol: String = "₹"
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
                    .clip(MaterialTheme.shapes.medium),
                color = AnalyticsBlueDark,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Saved: $currencySymbol${goal.currentAmount.toInt()} ($percentString)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Goal Target: $currencySymbol${goal.targetAmount.toInt()}",
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
    onConfirm: (category: String, limit: Double, frequency: String, remindersEnabled: Boolean, reminderThreshold: Int) -> Unit,
    currencySymbol: String = "₹"
) {
    var category by remember { mutableStateOf("Food") }
    var limitStr by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("MONTHLY") } // "MONTHLY" or "WEEKLY"
    var remindersEnabled by remember { mutableStateOf(false) }
    var reminderThreshold by remember { mutableStateOf(90) }

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
        title = { Text("Create Limit Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                
                // Frequency Toggle Selector
                Text("Budget Cycle Period", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    listOf("MONTHLY", "WEEKLY").forEach { freq ->
                        val active = frequency == freq
                        val label = if (freq == "MONTHLY") "Monthly" else "Weekly"
                        val bg = if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        val contentCol = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(bg)
                                .clickable { frequency = freq }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label, 
                                style = MaterialTheme.typography.labelLarge,
                                color = contentCol
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text("Select Category", style = MaterialTheme.typography.labelSmall)
                // Dropdown category
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDropdownExpanded = true }
                        .padding(vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
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

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Limit Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Reminders Toggle Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Threshold Alerts", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Get local notification indicators on crossing limits", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { remindersEnabled = it }
                    )
                }

                if (remindersEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Warning Threshold Limit", style = MaterialTheme.typography.labelSmall)
                            Text("${reminderThreshold}% Spent", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Slider(
                            value = reminderThreshold.toFloat(),
                            onValueChange = { reminderThreshold = it.toInt() },
                            valueRange = 50f..100f,
                            steps = 9
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = com.akshar.utils.MathUtils.evaluateMathExpression(limitStr) ?: 0.0
                    if (limit > 0) {
                        onConfirm(category, limit, frequency, remindersEnabled, reminderThreshold)
                    }
                }
            ) {
                Text("Set Budget Limit")
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
    onConfirm: (name: String, target: Double, current: Double, date: String) -> Unit,
    currencySymbol: String = "₹"
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
                    label = { Text("Target Goal Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentStr,
                    onValueChange = { currentStr = it },
                    label = { Text("Initial Saved Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                    val target = com.akshar.utils.MathUtils.evaluateMathExpression(targetStr) ?: 0.0
                    val current = com.akshar.utils.MathUtils.evaluateMathExpression(currentStr) ?: 0.0
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
    onConfirm: (funds: Double) -> Unit,
    currencySymbol: String = "₹"
) {
    var fundStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contribute to $goalName") },
        text = {
            OutlinedTextField(
                value = fundStr,
                onValueChange = { fundStr = it },
                label = { Text("Contribution Amount ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val funds = com.akshar.utils.MathUtils.evaluateMathExpression(fundStr) ?: 0.0
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

private fun getCurrentWeekKey(): String {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val week = cal.get(Calendar.WEEK_OF_YEAR)
    return "$year-W$week"
}

private fun isSameWeek(timestamp: Long, weekKey: String): Boolean {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timestamp
    val year = cal.get(Calendar.YEAR)
    val week = cal.get(Calendar.WEEK_OF_YEAR)
    return "$year-W$week" == weekKey
}
