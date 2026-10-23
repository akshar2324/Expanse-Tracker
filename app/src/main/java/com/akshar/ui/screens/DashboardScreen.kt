package com.akshar.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.WbSunny
import androidx.navigation.NavController
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.akshar.data.model.Transaction
import com.akshar.ui.components.*
import com.akshar.ui.theme.*
import com.akshar.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToHistory: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val transactions = allTransactions.filter { it.transferId == null }
    val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val recurringList by viewModel.allRecurringTransactions.collectAsStateWithLifecycle()

    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()

    // --- State Calculations ---
    val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val currentBalance = totalIncome - totalExpense

    // Today boundaries
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Month boundaries
    val startOfMonth = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayIncome = transactions.filter { it.type == "INCOME" && it.date >= startOfToday }.sumOf { it.amount }
    val todayExpense = transactions.filter { it.type == "EXPENSE" && it.date >= startOfToday }.sumOf { it.amount }

    val monthIncome = transactions.filter { it.type == "INCOME" && it.date >= startOfMonth }.sumOf { it.amount }
    val monthExpense = transactions.filter { it.type == "EXPENSE" && it.date >= startOfMonth }.sumOf { it.amount }

    // --- Quick Stats Calculations ---
    val recentTxs = transactions.take(10)

    // Average spending (daily average in the current month)
    val expensesThisMonth = transactions.filter { it.type == "EXPENSE" && it.date >= startOfMonth }
    val daysInMonthElapsed = remember {
        val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        todayDay.coerceAtLeast(1)
    }
    val avgDailySpending = if (expensesThisMonth.isEmpty()) 0.0 else (expensesThisMonth.sumOf { it.amount } / daysInMonthElapsed)

    // Highest Expense Day
    val expenseByDay = com.akshar.utils.TransactionUtils.expencesGroupedByDay(transactions)
    val highestExpenseDayVal = expenseByDay.maxByOrNull { it.value }?.value ?: 0.0

    // Highest Income Day
    val incomeByDay = com.akshar.utils.TransactionUtils.incomesGroupedByDay(transactions)
    val highestIncomeDayVal = incomeByDay.maxByOrNull { it.value }?.value ?: 0.0

    // Remaining Budget
    val currentMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val totalBudgetLimit = budgets.filter { it.month == currentMonthStr }.sumOf { it.limitAmount }
    val remainingBudget = (totalBudgetLimit - monthExpense).coerceAtLeast(0.0)

    val entrance = rememberEntranceState(itemCount = 7)
    val bottomInset = LocalFloatingBarInset.current

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.sm,
                bottom = bottomInset + 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item(key = "header") {
                DashboardHeader(
                    onOpenAccounts = { navController.navigate("accounts") },
                    modifier = Modifier.entrance(entrance, 0)
                )
            }

            item(key = "hero") {
                BalanceHero(
                    currentBalance = currentBalance,
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    totalBudgetLimit = totalBudgetLimit,
                    currencySymbol = currencySymbol,
                    onAddTransfer = { navController.navigate("addTransfer") },
                    onOpenAccounts = { navController.navigate("accounts") },
                    modifier = Modifier.entrance(entrance, 1)
                )
            }

            item(key = "today") {
                CashflowBanner(
                    todayIncome = todayIncome,
                    todayExpense = todayExpense,
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.entrance(entrance, 2)
                )
            }

            item(key = "stats") {
                QuickStatistics(
                    avgDailySpending = avgDailySpending,
                    highestExpenseDayVal = highestExpenseDayVal,
                    highestIncomeDayVal = highestIncomeDayVal,
                    remainingBudget = remainingBudget,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.entrance(entrance, 3)
                )
            }

            item(key = "digest") {
                WeeklyDigestTeaser(
                    onOpen = { navController.navigate("weeklyDigest") },
                    modifier = Modifier.entrance(entrance, 4)
                )
            }

            if (recurringList.isNotEmpty()) {
                item(key = "reminders") {
                    UpcomingRemindersSection(
                        recurringList = recurringList,
                        currencySymbol = currencySymbol,
                        onPay = { rec -> viewModel.triggerRecurringPayment(rec) },
                        modifier = Modifier.entrance(entrance, 5)
                    )
                }
            }

            recentTransactionsSection(
                recentTxs = recentTxs,
                currencySymbol = currencySymbol,
                onNavigateToHistory = onNavigateToHistory,
                navController = navController,
                onDelete = { tx -> viewModel.deleteTransaction(tx) },
                entrance = entrance
            )
        }

        // Primary action floats just above the tab bar, within thumb reach.
        ExtendedFloatingActionButton(
            onClick = onNavigateToAddTransaction,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Add", style = MaterialTheme.typography.labelLarge) },
            shape = PillShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.gutter, bottom = bottomInset + Spacing.xs)
                .semantics { contentDescription = "Add transaction" }
        )
    }
}

private fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Hello"
}

@Composable
fun DashboardHeader(
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { Date() }
    val greeting = remember { greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    val dateLabel = remember(today) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(today) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = dateLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium.eyebrow(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
        }
        GlassIconButton(
            icon = Icons.Rounded.AccountBalanceWallet,
            contentDescription = "Open accounts",
            onClick = onOpenAccounts,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BalanceHero(
    currentBalance: Double,
    monthIncome: Double,
    monthExpense: Double,
    totalBudgetLimit: Double,
    currencySymbol: String,
    onAddTransfer: () -> Unit,
    onOpenAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finance = AkSpendTheme.finance
    val shown = rememberCountUp(currentBalance)
    val finalText = "$currencySymbol${String.format("%,.2f", currentBalance)}"
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        fill = AkSpendTheme.glass.fillStrong,
        contentPadding = PaddingValues(Spacing.xxl)
    ) {
        Text(
            text = "NET BALANCE",
            style = MaterialTheme.typography.labelMedium.eyebrow(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "$currencySymbol${String.format("%,.2f", shown)}",
            style = MaterialTheme.typography.displayMedium.money(),
            color = if (currentBalance >= 0) MaterialTheme.colorScheme.onSurface else finance.expense,
            maxLines = 1,
            modifier = Modifier.semantics { contentDescription = "Net balance $finalText" }
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FlowPill(
                icon = Icons.Rounded.SouthWest,
                label = "In",
                value = "$currencySymbol${String.format("%,.0f", monthIncome)}",
                tint = finance.income,
                modifier = Modifier.weight(1f)
            )
            FlowPill(
                icon = Icons.Rounded.NorthEast,
                label = "Out",
                value = "$currencySymbol${String.format("%,.0f", monthExpense)}",
                tint = finance.expense,
                modifier = Modifier.weight(1f)
            )
        }
        if (totalBudgetLimit > 0) {
            val used = (monthExpense / totalBudgetLimit).toFloat().coerceIn(0f, 1f)
            val animatedUsed by animateFloatAsState(used, motionSpec(Durations.emphasized * 2), label = "budgetUsed")
            Spacer(modifier = Modifier.height(Spacing.lg))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly budget", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${(used * 100).toInt()}% used",
                    style = MaterialTheme.typography.labelMedium.money(),
                    color = if (used >= 0.9f) finance.expense else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            LinearProgressIndicator(
                progress = { animatedUsed },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(PillShape),
                color = if (used >= 0.9f) finance.expense else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {}
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilledTonalButton(
                onClick = onAddTransfer,
                modifier = Modifier.weight(1f).heightIn(min = Spacing.touchTarget),
                shape = PillShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (AkSpendTheme.isDark) 0.18f else 0.10f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Transfer", style = MaterialTheme.typography.labelLarge)
            }
            FilledTonalButton(
                onClick = onOpenAccounts,
                modifier = Modifier.weight(1f).heightIn(min = Spacing.touchTarget),
                shape = PillShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (AkSpendTheme.isDark) 0.18f else 0.10f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Rounded.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Accounts", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FlowPill(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(tint.copy(alpha = if (AkSpendTheme.isDark) 0.14f else 0.09f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm))
        Column {
            Text("$label this month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall.money(), color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CashflowBanner(
    todayIncome: Double,
    todayExpense: Double,
    monthIncome: Double,
    monthExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val finance = AkSpendTheme.finance
    val net = todayIncome - todayExpense
    val headline = when {
        todayIncome == 0.0 && todayExpense == 0.0 -> "A calm day so far"
        net >= 0 -> "You're ahead today"
        else -> "Spent today"
    }
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Rounded.WbSunny, tint = finance.warning, size = 40.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("TODAY", style = MaterialTheme.typography.labelSmall.eyebrow(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(headline, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Row(Modifier.fillMaxWidth()) {
            SummaryFigure("Earned", "+$currencySymbol${String.format("%,.0f", todayIncome)}", finance.income, Modifier.weight(1f))
            SummaryFigure("Spent", "-$currencySymbol${String.format("%,.0f", todayExpense)}", finance.expense, Modifier.weight(1f))
            SummaryFigure(
                "Month net",
                "${if (monthIncome - monthExpense >= 0) "+" else "-"}$currencySymbol${String.format("%,.0f", kotlin.math.abs(monthIncome - monthExpense))}",
                MaterialTheme.colorScheme.onSurface,
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryFigure(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xxs))
        Text(value, style = MaterialTheme.typography.titleMedium.money(), color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun QuickStatistics(
    avgDailySpending: Double,
    highestExpenseDayVal: Double,
    highestIncomeDayVal: Double,
    remainingBudget: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val finance = AkSpendTheme.finance
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SectionHeader(title = "At a glance")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            StatTile(
                label = "Daily average",
                value = "$currencySymbol${String.format("%,.0f", avgDailySpending)}",
                icon = Icons.Rounded.Timelapse,
                tint = finance.info,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Budget left",
                value = "$currencySymbol${String.format("%,.0f", remainingBudget)}",
                icon = Icons.Rounded.Savings,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            StatTile(
                label = "Biggest spend day",
                value = "$currencySymbol${String.format("%,.0f", highestExpenseDayVal)}",
                icon = Icons.Rounded.NorthEast,
                tint = finance.expense,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Best income day",
                value = "$currencySymbol${String.format("%,.0f", highestIncomeDayVal)}",
                icon = Icons.Rounded.SouthWest,
                tint = finance.income,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeeklyDigestTeaser(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen,
        onClickLabel = "Open weekly digest",
        contentPadding = PaddingValues(Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Rounded.AutoAwesome, tint = MaterialTheme.colorScheme.tertiary, size = 44.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text("Your weekly digest", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Top categories, unusual spends and next week's outlook",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UpcomingRemindersSection(
    recurringList: List<com.akshar.data.model.RecurringTransaction>,
    currencySymbol: String,
    onPay: (com.akshar.data.model.RecurringTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedReminders = recurringList.map { rec ->
        val cal = Calendar.getInstance().apply { timeInMillis = rec.lastTriggered }
        when (rec.frequency.uppercase()) {
            "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> cal.add(Calendar.MONTH, 1)
            "YEARLY" -> cal.add(Calendar.YEAR, 1)
            else -> cal.add(Calendar.MONTH, 1)
        }
        val nextDue = cal.timeInMillis
        val diffDays = ((nextDue - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt()
        rec to (nextDue to diffDays)
    }.sortedBy { it.second.first }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = "Coming up · ${sortedReminders.size}")
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
            sortedReminders.take(3).forEachIndexed { index, (rec, dueTimeAndDays) ->
                val (nextDue, diffDays) = dueTimeAndDays
                val formattedDate = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(nextDue))
                val (statusText, statusColor) = when {
                    diffDays < 0 -> "Overdue" to MaterialTheme.colorScheme.error
                    diffDays == 0 -> "Due today" to AkSpendTheme.finance.warning
                    diffDays <= 3 -> "In ${diffDays}d" to AkSpendTheme.finance.warning
                    else -> "In ${diffDays}d" to MaterialTheme.colorScheme.onSurfaceVariant
                }
                if (index > 0) GlassDivider()
                GlassListRow(
                    title = rec.description,
                    subtitle = "$statusText · $formattedDate",
                    leading = { IconBadge(icon = categoryIcon(rec.category), tint = categoryTint(rec.category)) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$currencySymbol${String.format("%,.0f", rec.amount)}",
                                style = MaterialTheme.typography.titleSmall.money(),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            FilledTonalIconButton(
                                onClick = { onPay(rec) },
                                modifier = Modifier.size(Spacing.touchTarget)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Mark ${rec.description} as paid",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
                if (statusColor == MaterialTheme.colorScheme.error) {
                    Spacer(Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = Spacing.lg),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    )
}

fun LazyListScope.recentTransactionsSection(
    recentTxs: List<Transaction>,
    currencySymbol: String,
    onNavigateToHistory: () -> Unit,
    navController: NavController,
    onDelete: (Transaction) -> Unit,
    entrance: EntranceState? = null
) {
    item(key = "recentHeader") {
        SectionHeader(
            title = "Recent activity",
            actionLabel = "See all",
            onAction = onNavigateToHistory,
            modifier = if (entrance != null) Modifier.entrance(entrance, 6) else Modifier
        )
    }

    if (recentTxs.isEmpty()) {
        item(key = "recentEmpty") {
            EmptyState(
                icon = Icons.Rounded.Inbox,
                title = "No transactions yet",
                message = "Tap Add to log your first expense or income. Everything stays on this device."
            )
        }
    } else {
        item(key = "recentList") {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (entrance != null) Modifier.entrance(entrance, 7) else Modifier),
                contentPadding = PaddingValues(vertical = Spacing.xs)
            ) {
                recentTxs.forEachIndexed { index, transaction ->
                    if (index > 0) GlassDivider()
                    TransactionItemRow(
                        transaction = transaction,
                        onDelete = { onDelete(transaction) },
                        currencySymbol = currencySymbol
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: Transaction,
    onDelete: () -> Unit,
    currencySymbol: String = "₹"
) {
    val finance = AkSpendTheme.finance
    val isExpense = transaction.type == "EXPENSE"
    val formattedDate = remember(transaction.date) {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(transaction.date))
    }
    val amountText = "${if (isExpense) "-" else "+"}$currencySymbol${String.format("%,.2f", transaction.amount)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.sm, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(
            icon = categoryIcon(transaction.category),
            tint = categoryTint(transaction.category),
            size = 44.dp
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {}
        ) {
            Text(
                text = transaction.description.ifEmpty { transaction.category },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = "${transaction.category} · $formattedDate · ${transaction.paymentMethod}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleSmall.money(),
            color = if (isExpense) finance.expense else finance.income,
            maxLines = 1
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Delete ${transaction.description.ifEmpty { transaction.category }}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
