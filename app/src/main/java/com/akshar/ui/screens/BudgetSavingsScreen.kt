package com.akshar.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.ui.graphics.StrokeCap
import com.akshar.ui.components.*
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

    val activeBudgets = budgets.filter {
        (it.frequency == "WEEKLY" && it.month == currentWeekStr) ||
        (it.frequency == "MONTHLY" && it.month == currentMonthStr) ||
        (it.frequency.isBlank() && it.month == currentMonthStr)
    }
    val budgetSpent: (Budget) -> Double = { budget ->
        transactions.filter {
            it.type == "EXPENSE" &&
                    it.category.equals(budget.category, ignoreCase = true) &&
                    (if (budget.frequency == "WEEKLY") isSameWeek(it.date, budget.month) else isSameMonth(it.date, budget.month))
        }.sumOf { it.amount }
    }
    val bottomInset = LocalFloatingBarInset.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                bottom = bottomInset + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                ScreenHeader(
                    title = "Budgets",
                    subtitle = "Spending limits and the goals you're saving for",
                    insetHorizontally = false
                )
            }
            item {
                GlassSegmentedControl(
                    options = listOf("Budgets", "Savings goals"),
                    selectedIndex = if (activeTab == "BUDGETS") 0 else 1,
                    onSelect = { activeTab = if (it == 0) "BUDGETS" else "SAVINGS" }
                )
            }

            if (activeTab == "BUDGETS") {
                if (activeBudgets.isEmpty()) {
                    item {
                        EmptyVisualState(
                            message = "No budgets yet",
                            subtext = "Set a monthly or weekly limit for a category and we'll show how much is left."
                        )
                    }
                } else {
                    item {
                        val totalLimit = activeBudgets.sumOf { it.limitAmount }
                        val totalSpent = activeBudgets.sumOf { budgetSpent(it) }
                        BudgetSummaryCard(totalSpent = totalSpent, totalLimit = totalLimit, currencySymbol = currencySymbol)
                    }
                    items(activeBudgets, key = { it.id }) { budget ->
                        BudgetRow(
                            budget = budget,
                            spent = budgetSpent(budget),
                            onDelete = { viewModel.deleteBudgetById(budget.id) },
                            currencySymbol = currencySymbol
                        )
                    }
                }
            } else {
                if (savingsGoals.isEmpty()) {
                    item {
                        EmptyVisualState(
                            message = "No savings goals yet",
                            subtext = "Name something you're saving for, like a trip or a new laptop, and watch it fill up."
                        )
                    }
                } else {
                    items(savingsGoals, key = { it.id }) { goal ->
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

        ExtendedFloatingActionButton(
            onClick = {
                if (activeTab == "BUDGETS") showAddBudgetDialog = true else showAddGoalDialog = true
            },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(if (activeTab == "BUDGETS") "New budget" else "New goal", style = MaterialTheme.typography.labelLarge) },
            shape = PillShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.gutter, bottom = bottomInset + Spacing.xs)
        )
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
private fun BudgetSummaryCard(totalSpent: Double, totalLimit: Double, currencySymbol: String) {
    val finance = AkSpendTheme.finance
    val used = if (totalLimit > 0) (totalSpent / totalLimit).toFloat().coerceIn(0f, 1f) else 0f
    val left = (totalLimit - totalSpent).coerceAtLeast(0.0)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        fill = AkSpendTheme.glass.fillStrong
    ) {
        Text("LEFT TO SPEND", style = MaterialTheme.typography.labelMedium.eyebrow(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "$currencySymbol${String.format("%,.0f", left)}",
            style = MaterialTheme.typography.displaySmall.money(),
            color = if (used >= 1f) finance.expense else MaterialTheme.colorScheme.onSurface
        )
        Text(
            "of $currencySymbol${String.format("%,.0f", totalLimit)} across all active budgets",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))
        GlassProgressBar(fraction = used, color = if (used >= 0.9f) finance.expense else MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun GlassProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(fraction, motionSpec(Durations.emphasized * 2), label = "progress")
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(PillShape),
        color = color,
        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {}
    )
}

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
        pct >= 1.0 -> AkSpendTheme.finance.expense
        pct >= (budget.reminderThreshold / 100.0) -> AkSpendTheme.finance.warning
        else -> AkSpendTheme.finance.income
    }

    val statusLabel = when {
        pct >= 1.0 -> "Over budget by $currencySymbol${String.format("%,.0f", spent - budget.limitAmount)}"
        budget.remindersEnabled && (pct >= (budget.reminderThreshold / 100.0)) -> "Passed your ${budget.reminderThreshold}% alert"
        pct >= 0.8 -> "Getting close: ${(pct * 100).toInt()}% used"
        else -> "$currencySymbol${String.format("%,.0f", budget.limitAmount - spent)} left"
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = Spacing.lg, top = Spacing.md, end = Spacing.xs, bottom = Spacing.lg)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = categoryIcon(budget.category), tint = categoryTint(budget.category), size = 44.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = budget.category.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = budget.frequency.ifEmpty { "MONTHLY" }.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(PillShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                    )
                }
                Text(text = statusLabel, style = MaterialTheme.typography.bodySmall, color = barColor)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete ${budget.category} budget",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        GlassProgressBar(fraction = progressFraction, color = barColor, modifier = Modifier.padding(end = Spacing.md))
        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", spent)} spent",
                style = MaterialTheme.typography.bodySmall.money(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Limit $currencySymbol${String.format("%,.0f", budget.limitAmount)}",
                style = MaterialTheme.typography.labelLarge.money(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (budget.remindersEnabled) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Alert at ${budget.reminderThreshold}% of the limit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    val estimatedDate = goal.targetDate.ifEmpty { "No target date" }

    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = Spacing.lg, top = Spacing.md, end = Spacing.xs, bottom = Spacing.lg)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon = Icons.Rounded.Savings, tint = AkSpendTheme.finance.info, size = 44.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(text = goal.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Target: $estimatedDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAddSavings) {
                Icon(
                    imageVector = Icons.Rounded.AddCircleOutline,
                    contentDescription = "Add money to ${goal.name}",
                    tint = AkSpendTheme.finance.income
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete ${goal.name} goal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        Row(Modifier.fillMaxWidth().padding(end = Spacing.md), verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$currencySymbol${String.format("%,.0f", goal.currentAmount)}",
                style = MaterialTheme.typography.headlineSmall.money(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = "of $currencySymbol${String.format("%,.0f", goal.targetAmount)}",
                style = MaterialTheme.typography.bodyMedium.money(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(bottom = 2.dp)
            )
            Text(percentString, style = MaterialTheme.typography.labelLarge.money(), color = AkSpendTheme.finance.info)
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        GlassProgressBar(fraction = progressFraction, color = AkSpendTheme.finance.info, modifier = Modifier.padding(end = Spacing.md))
    }
}

@Composable
fun EmptyVisualState(message: String, subtext: String) {
    EmptyState(icon = Icons.Rounded.Flag, title = message, message = subtext)
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
                    Text(text = category, style = MaterialTheme.typography.bodyLarge)
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
                    colors = glassTextFieldColors(),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    colors = glassTextFieldColors(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name (e.g. MacBook Study fund)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Goal Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = currentStr,
                    onValueChange = { currentStr = it },
                    label = { Text("Initial Saved Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    colors = glassTextFieldColors(),
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
                colors = glassTextFieldColors(),
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
