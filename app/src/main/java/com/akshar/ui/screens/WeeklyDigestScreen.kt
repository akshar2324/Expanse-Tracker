package com.akshar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akshar.insights.InsightEngine
import com.akshar.insights.WeeklyDigestSummary
import com.akshar.ui.theme.ExpenseRed
import com.akshar.ui.theme.IncomeGreen
import com.akshar.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyDigestScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allBudgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val allRecurring by viewModel.allRecurringTransactions.collectAsStateWithLifecycle()

    var summary by remember { mutableStateOf<WeeklyDigestSummary?>(null) }
    
    val engine = remember { InsightEngine() }

    LaunchedEffect(allTransactions, allBudgets, allRecurring) {
        summary = engine.generateWeeklyDigest(
            allTransactions,
            allBudgets,
            allRecurring
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Digest", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val data = summary
            if (data == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (data.hasInsufficientData) {
                WeeklyDigestEmptyState()
            } else {
                WeeklyDigestContent(
                    summary = data,
                    formatCurrency = viewModel::formatCurrencyValue
                )
            }
        }
    }
}

@Composable
private fun WeeklyDigestEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Not enough data for this week.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add some transactions to see your weekly digest.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklyDigestContent(
    summary: WeeklyDigestSummary,
    formatCurrency: (Double) -> String
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("This Week's Cash Flow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Income", style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatCurrency(summary.totalIncome),
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Spent", style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatCurrency(summary.totalSpent),
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    val netColor = if (summary.netCashFlow >= 0) IncomeGreen else ExpenseRed
                    Text("Net: ${formatCurrency(summary.netCashFlow)}", fontWeight = FontWeight.Bold, color = netColor)
                }
            }
        }

        if (summary.anomalies.isNotEmpty()) {
            item {
                Text("Anomalies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(summary.anomalies) { anomaly ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(anomaly.description, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        Text("+${formatCurrency(anomaly.anomalyAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        if (summary.topCategories.isNotEmpty()) {
            item {
                Text("Top Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(summary.topCategories) { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat.categoryName, fontWeight = FontWeight.Medium)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatCurrency(cat.amountSpent), fontWeight = FontWeight.Bold)
                        Text(String.format("%.1f%%", cat.percentageOfTotal), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        
        if (summary.budgetProgress.isNotEmpty()) {
            item {
                Text("Budget Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(summary.budgetProgress) { budget ->
                val progress = if (budget.budgetLimit > 0) (budget.spentSoFar / budget.budgetLimit).toFloat() else 0f
                val clampedProgress = progress.coerceIn(0f, 1f)
                val color = if (budget.isOverBudget) ExpenseRed else IncomeGreen
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(budget.categoryName, fontWeight = FontWeight.Bold)
                        Text("${formatCurrency(budget.spentSoFar)} / ${formatCurrency(budget.budgetLimit)}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { clampedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    if (budget.isOverBudget) {
                        Text("Over budget!", color = ExpenseRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        summary.outlook?.let { outlook ->
            item {
                Text("Next Week Outlook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (outlook.expectedRecurringSpend > 0) {
                            Text(
                                formatCurrency(outlook.expectedRecurringSpend),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(outlook.suggestion, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
    }
}
