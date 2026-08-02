package com.akshar.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Adaptive AI Analysis",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Daily Burn Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currencySymbol${avgDailyBurn.toInt()}/day", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Month Forecast", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$currencySymbol${projectedSpent.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Forecast advice
            val advice = when {
                projectedSpent > 30000 -> "Projected burn rate is higher than normal. Consider capping Shopping/Entertainment."
                projectedSpent > 15000 -> "Spend trajectory is moderate. Your savings rate is safe."
                else -> "Excellent! Highly efficient discipline. You are set to exceed your saving goals."
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = advice,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Anomalies
            if (anomalies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Spike & Anomaly Watch", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(4.dp))
                anomalies.forEach { anomaly ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("⚠️", fontSize = 12.sp)
                        Text(
                            text = anomaly,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
