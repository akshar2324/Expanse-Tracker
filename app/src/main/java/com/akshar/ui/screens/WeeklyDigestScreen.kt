package com.akshar.ui.screens

import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.akshar.ui.components.*
import com.akshar.ui.theme.*
import com.akshar.ui.theme.AkSpendTheme
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

    Column(modifier = Modifier.fillMaxSize()) {
        val data = summary
        when {
            data == null -> {
                ScreenHeader(title = "Weekly digest", onBack = onBack)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            data.hasInsufficientData -> {
                ScreenHeader(title = "Weekly digest", onBack = onBack)
                WeeklyDigestEmptyState()
            }
            else -> WeeklyDigestContent(
                summary = data,
                formatCurrency = viewModel::formatCurrencyValue,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun WeeklyDigestEmptyState() {
    Box(Modifier.fillMaxWidth().padding(Spacing.gutter)) {
        EmptyState(
            icon = Icons.Rounded.AutoAwesome,
            title = "Your digest is warming up",
            message = "Log a few transactions this week and your summary will appear here."
        )
    }
}

@Composable
private fun DigestSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(top = Spacing.sm)
            .semantics { heading() }
    )
}

@Composable
private fun WeeklyDigestContent(
    summary: WeeklyDigestSummary,
    formatCurrency: (Double) -> String,
    onBack: () -> Unit
) {
    val finance = AkSpendTheme.finance
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier.navigationBarsPadding()
    ) {
        item {
            ScreenHeader(
                title = "Weekly digest",
                subtitle = "Your week in review, worked out on this device",
                onBack = onBack,
                insetHorizontally = false
            )
        }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                fill = AkSpendTheme.glass.fillStrong
            ) {
                val netColor = if (summary.netCashFlow >= 0) finance.income else finance.expense
                Text("NET THIS WEEK", style = MaterialTheme.typography.labelMedium.eyebrow(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xs))
                Text(formatCurrency(summary.netCashFlow), style = MaterialTheme.typography.displaySmall.money(), color = netColor)
                Spacer(Modifier.height(Spacing.lg))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Earned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(summary.totalIncome), style = MaterialTheme.typography.titleMedium.money(), color = finance.income)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(summary.totalSpent), style = MaterialTheme.typography.titleMedium.money(), color = finance.expense)
                    }
                }
            }
        }

        if (summary.anomalies.isNotEmpty()) {
            item { DigestSection("Worth a look") }
            items(summary.anomalies) { anomaly ->
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.WarningAmber, tint = finance.warning, contentDescription = "Unusual spend")
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(anomaly.description, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                            Text("+${formatCurrency(anomaly.anomalyAmount)}", style = MaterialTheme.typography.titleSmall.money(), color = finance.expense)
                        }
                    }
                }
            }
        }

        if (summary.topCategories.isNotEmpty()) {
            item { DigestSection("Top categories") }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = Spacing.xs)) {
                    summary.topCategories.forEachIndexed { index, cat ->
                        if (index > 0) GlassDivider()
                        GlassListRow(
                            title = cat.categoryName,
                            subtitle = String.format("%.1f%% of spending", cat.percentageOfTotal),
                            leading = { IconBadge(icon = categoryIcon(cat.categoryName), tint = categoryTint(cat.categoryName)) },
                            trailing = {
                                Text(formatCurrency(cat.amountSpent), style = MaterialTheme.typography.titleSmall.money(), color = MaterialTheme.colorScheme.onSurface)
                            }
                        )
                    }
                }
            }
        }

        if (summary.budgetProgress.isNotEmpty()) {
            item { DigestSection("Budgets") }
            items(summary.budgetProgress) { budget ->
                val progress = if (budget.budgetLimit > 0) (budget.spentSoFar / budget.budgetLimit).toFloat() else 0f
                val clampedProgress = progress.coerceIn(0f, 1f)
                val color = if (budget.isOverBudget) finance.expense else finance.income

                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(Spacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(budget.categoryName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "${formatCurrency(budget.spentSoFar)} / ${formatCurrency(budget.budgetLimit)}",
                            style = MaterialTheme.typography.bodySmall.money(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    LinearProgressIndicator(
                        progress = { clampedProgress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(PillShape),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        strokeCap = StrokeCap.Round,
                        drawStopIndicator = {}
                    )
                    if (budget.isOverBudget) {
                        Text("Over budget", color = finance.expense, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = Spacing.xs))
                    }
                }
            }
        }

        summary.outlook?.let { outlook ->
            item { DigestSection("Next week") }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), fill = AkSpendTheme.glass.fillStrong) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = Icons.Rounded.AutoAwesome, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(Spacing.md))
                        Column {
                            if (outlook.expectedRecurringSpend > 0) {
                                Text("Expected bills", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatCurrency(outlook.expectedRecurringSpend),
                                    style = MaterialTheme.typography.titleMedium.money(),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(Spacing.xs))
                            }
                            Text(outlook.suggestion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}
