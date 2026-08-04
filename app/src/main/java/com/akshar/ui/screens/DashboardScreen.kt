package com.akshar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.navigation.NavController
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshar.data.model.Transaction
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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { navController.navigate("addTransfer") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp, end = 16.dp)
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = "Add Transfer")
                }
                FloatingActionButton(
                    onClick = onNavigateToAddTransaction,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                DashboardHeader(
                    currentBalance = currentBalance,
                    currencySymbol = currencySymbol
                )
            }

            item {
                CashflowBanner(
                    todayIncome = todayIncome,
                    todayExpense = todayExpense,
                    monthIncome = monthIncome,
                    monthExpense = monthExpense,
                    currencySymbol = currencySymbol
                )
            }

            item {
                QuickStatistics(
                    avgDailySpending = avgDailySpending,
                    highestExpenseDayVal = highestExpenseDayVal,
                    highestIncomeDayVal = highestIncomeDayVal,
                    remainingBudget = remainingBudget,
                    currencySymbol = currencySymbol
                )
            }

            if (recurringList.isNotEmpty()) {
                item {
                    UpcomingRemindersSection(
                        recurringList = recurringList,
                        currencySymbol = currencySymbol,
                        onPay = { rec -> viewModel.triggerRecurringPayment(rec) }
                    )
                }
            }

            recentTransactionsSection(
                recentTxs = recentTxs,
                currencySymbol = currencySymbol,
                onNavigateToHistory = onNavigateToHistory,
                navController = navController,
                onDelete = { tx -> viewModel.deleteTransaction(tx) }
            )
        }

    }
}



// --- Extracted Components ---

@Composable
fun DashboardHeader(
    currentBalance: Double,
    currencySymbol: String
) {
    Column {
        // Minimal Hello Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GREETINGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Hello, Investor!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "Wallet Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Clean Balanced Net Worth Display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "TOTAL NET BALANCE",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currencySymbol${String.format("%,.2f", currentBalance)}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 38.sp,
                    color = if (currentBalance >= 0) IncomeGreenDark else ExpenseRedDark
                )
            )
        }
    }
}

@Composable
fun CashflowBanner(
    todayIncome: Double,
    todayExpense: Double,
    monthIncome: Double,
    monthExpense: Double,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Flows",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "+$currencySymbol${String.format("%,.0f", todayIncome)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = IncomeGreenDark
                    )
                    Text(
                        text = "-$currencySymbol${String.format("%,.0f", todayExpense)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ExpenseRedDark
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                thickness = 1.dp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This Month's Volume",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "+$currencySymbol${String.format("%,.0f", monthIncome)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = IncomeGreenDark
                    )
                    Text(
                        text = "-$currencySymbol${String.format("%,.0f", monthExpense)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ExpenseRedDark
                    )
                }
            }
        }
    }
}

@Composable
fun QuickStatistics(
    avgDailySpending: Double,
    highestExpenseDayVal: Double,
    highestIncomeDayVal: Double,
    remainingBudget: Double,
    currencySymbol: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "QUICK STATISTICS",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            ),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactStatPill(
                label = "Daily Avg",
                value = "$currencySymbol${avgDailySpending.toInt()}",
                icon = Icons.Default.AvTimer,
                color = AnalyticsBlueDark
            )
            CompactStatPill(
                label = "Max spend",
                value = "$currencySymbol${highestExpenseDayVal.toInt()}",
                icon = Icons.Default.ArrowUpward,
                color = ExpenseRedDark
            )
            CompactStatPill(
                label = "Peak Income",
                value = "$currencySymbol${highestIncomeDayVal.toInt()}",
                icon = Icons.Default.Star,
                color = IncomeGreenDark
            )
            CompactStatPill(
                label = "Remaining Budget",
                value = "$currencySymbol${remainingBudget.toInt()}",
                icon = Icons.Default.PieChart,
                color = AlertOrangeDark
            )
        }
    }
}

@Composable
fun UpcomingRemindersSection(
    recurringList: List<com.akshar.data.model.RecurringTransaction>,
    currencySymbol: String,
    onPay: (com.akshar.data.model.RecurringTransaction) -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UPCOMING REMINDERS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            )
            Text(
                text = "${sortedReminders.size} Active",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
             )
        }

        sortedReminders.take(2).forEach { (rec, dueTimeAndDays) ->
            val (nextDue, diffDays) = dueTimeAndDays
            val formattedDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(nextDue))

            val (statusText, badgeBg, badgeText) = when {
                diffDays < 0 -> Triple("Overdue", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                diffDays == 0 -> Triple("Today", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                diffDays <= 3 -> Triple("${diffDays}d left", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                else -> Triple("${diffDays}d", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (rec.category.uppercase()) {
                                    "RECHARGE", "MOBILE" -> Icons.Default.PhoneAndroid
                                    "RENT" -> Icons.Default.Home
                                    "ELECTRICITY", "BILLS" -> Icons.Default.ReceiptLong
                                    "SUBSCRIPTION" -> Icons.Default.PlayCircle
                                    else -> Icons.Default.DateRange
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = rec.description,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeBg,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = badgeText,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = "Due $formattedDate",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$currencySymbol${rec.amount.toInt()}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(
                            onClick = { onPay(rec) },
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Pay",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun LazyListScope.recentTransactionsSection(
    recentTxs: List<Transaction>,
    currencySymbol: String,
    onNavigateToHistory: () -> Unit,
    navController: NavController,
    onDelete: (Transaction) -> Unit
) {
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT TRANSACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            )
            TextButton(
                onClick = onNavigateToHistory,
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("View All", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Arrow Icon",
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }

    if (recentTxs.isEmpty()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = "No Transactions Found",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No transactions logged yet.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                )
            }
        }
    } else {
        items(recentTxs) { transaction ->
            TransactionItemRow(
                transaction = transaction,
                onDelete = { onDelete(transaction) },
                currencySymbol = currencySymbol
            )
        }
    }
}

// --- Minimalist Inner Components ---

@Composable
fun CompactStatPill(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = Modifier
            .height(52.dp)
            .widthIn(min = 120.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
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
    val isExpense = transaction.type == "EXPENSE"
    val badgeColor = if (isExpense) ExpenseRed else IncomeGreen
    val characterBadge = remember(transaction.category) {
        if (transaction.category.length >= 2) {
            transaction.category.substring(0, 2).uppercase()
        } else {
            transaction.category.uppercase()
        }
    }

    val formattedDate = remember(transaction.date) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.date))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = characterBadge,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.description.ifEmpty { transaction.category },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isExpense) "-" else "+"}$currencySymbol${String.format("%,.2f", transaction.amount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) ExpenseRedDark else IncomeGreenDark
                        )
                    )
                    Text(
                        text = transaction.paymentMethod,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete transaction",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    }
}

