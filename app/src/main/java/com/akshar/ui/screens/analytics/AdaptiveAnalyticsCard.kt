package com.akshar.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.akshar.ui.components.GlassCard
import com.akshar.ui.components.IconBadge
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Spacing
import com.akshar.ui.theme.money
import com.akshar.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdaptiveAnalyticsCard(transactions: List<Transaction>, currencySymbol: String = "₹") {
    val expenses = remember(transactions) { transactions.filter { it.type == "EXPENSE" } }
    if (expenses.isEmpty()) return

    // Calculate current day of month and total elapsed
    val cal = Calendar.getInstance()
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Group current month transactions
    val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    val currentMonthExpenses = expenses.filter {
        val df = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        df.format(Date(it.date)) == currentMonthKey
    }

    val totalSpentThisMonth = currentMonthExpenses.sumOf { it.amount }
    val avgDailyBurn = if (dayOfMonth > 0) totalSpentThisMonth / dayOfMonth else 0.0
    val projectedSpent = avgDailyBurn * totalDaysInMonth

    // Anomalies: find categories where a single transaction exceeds 2.0x of that category's historical average
    val categoryHistograms = expenses.groupBy { it.category }
    val anomalies = remember(expenses) {
        val list = mutableListOf<String>()
        categoryHistograms.forEach { (cat, txs) ->
            if (txs.size >= 3) {
                val sorted = txs.sortedBy { it.date }
                val latest = sorted.last()
                val previousOnes = sorted.dropLast(1)
                val averageHistoric = previousOnes.map { it.amount }.average()
                if (latest.amount > 2.0 * averageHistoric && averageHistoric > 0) {
                    val percentIncrease = ((latest.amount - averageHistoric) / averageHistoric * 100).toInt()
                    list.add("Sudden ${percentIncrease}% spike in $cat on ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(latest.date))} ($currencySymbol${latest.amount.toInt()} vs avg $currencySymbol${averageHistoric.toInt()})")
                }
            }
        }
        list
    }

    val finance = AkSpendTheme.finance
    GlassCard(modifier = Modifier.fillMaxWidth(), fill = AkSpendTheme.glass.fillStrong) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.AutoMirrored.Rounded.ShowChart, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text("Spending forecast", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("Projected from this month so far", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Daily burn", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "$currencySymbol${String.format("%,.0f", avgDailyBurn)}",
                    style = MaterialTheme.typography.headlineSmall.money(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("per day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Month forecast", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "$currencySymbol${String.format("%,.0f", projectedSpent)}",
                    style = MaterialTheme.typography.headlineSmall.money(),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text("by month end", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        val advice = when {
            projectedSpent > 30000 -> "Spending is running higher than usual. Shopping and entertainment are good places to ease off."
            projectedSpent > 15000 -> "Spending is on a moderate path. Your savings rate looks safe."
            else -> "Nicely done. You're on track to beat your saving goals."
        }
        Text(
            text = advice,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                .padding(Spacing.md)
        )

        if (anomalies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text("Unusual spends", style = MaterialTheme.typography.labelLarge, color = finance.expense)
            Spacer(modifier = Modifier.height(Spacing.xs))
            anomalies.forEach { anomaly ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xxs),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        Icons.Rounded.WarningAmber,
                        contentDescription = "Warning",
                        tint = finance.expense,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = anomaly,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
