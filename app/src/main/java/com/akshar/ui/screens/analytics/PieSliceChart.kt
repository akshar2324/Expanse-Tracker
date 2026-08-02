package com.akshar.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.akshar.data.model.Transaction

// --- Custom Animated Pie Chart ---
@Composable
fun PieSliceChart(transactions: List<Transaction>) {
    // Group and aggregate
    val categoryTotals = remember(transactions) {
        transactions.groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val totalSum = categoryTotals.sumOf { it.second }

    // Color definitions
    val colors = listOf(
        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.inversePrimary,
        MaterialTheme.colorScheme.inverseSurface
    )

    if (totalSum == 0.0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp), contentAlignment = Alignment.Center
        ) {
            Text("No transactions in this category.")
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Draw Pie
            Canvas(
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp)
            ) {
                var startAngle = 0f
                categoryTotals.forEachIndexed { idx, pair ->
                    val sweep = ((pair.second / totalSum) * 360f).toFloat()
                    drawArc(
                        color = colors[idx % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        size = size
                    )
                    startAngle += sweep
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Draw Legends
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categoryTotals.take(5).forEachIndexed { idx, pair ->
                    val pct = (pair.second / totalSum * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colors[idx % colors.size])
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${pair.first} ($pct%)",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
                if (categoryTotals.size > 5) {
                    Text(
                        text = "+ ${categoryTotals.size - 5} others",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
