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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.akshar.data.model.Transaction
import com.akshar.ui.theme.AkSpendTheme
import com.akshar.ui.theme.Spacing
import com.akshar.ui.theme.money

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

    val colors = AkSpendTheme.finance.chart
    val reveal = rememberChartReveal()
    val sliceGapDegrees = 2f

    if (totalSum == 0.0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp), contentAlignment = Alignment.Center
        ) {
            Text(
                "Nothing logged here yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val summary = remember(categoryTotals, totalSum) {
            "Category breakdown. " + categoryTotals.take(5).joinToString("; ") {
                "${it.first} ${(it.second / totalSum * 100).toInt()} percent"
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .size(148.dp)
                        .padding(Spacing.sm)
                        .semantics { contentDescription = summary }
                ) {
                    val stroke = 18.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val progress = reveal()
                    var startAngle = -90f
                    categoryTotals.forEachIndexed { idx, pair ->
                        val sweep = ((pair.second / totalSum) * 360f).toFloat() * progress
                        val visibleSweep = (sweep - sliceGapDegrees).coerceAtLeast(0.5f)
                        drawArc(
                            color = colors[idx % colors.size],
                            startAngle = startAngle,
                            sweepAngle = visibleSweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${categoryTotals.size}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (categoryTotals.size == 1) "category" else "categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                categoryTotals.take(5).forEachIndexed { idx, pair ->
                    val pct = (pair.second / totalSum * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors[idx % colors.size])
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = pair.first,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.labelLarge.money(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (categoryTotals.size > 5) {
                    Text(
                        text = "+ ${categoryTotals.size - 5} more",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
