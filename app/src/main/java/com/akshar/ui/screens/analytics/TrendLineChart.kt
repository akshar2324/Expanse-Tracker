package com.akshar.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.akshar.ui.theme.Spacing
import com.akshar.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

internal fun calculateTrendPoints(
    transactions: List<Transaction>,
    calendar: Calendar = Calendar.getInstance()
): List<Pair<String, Double>> {
    val formatter = SimpleDateFormat("dd", Locale.getDefault())
    val totals = TreeMap<String, Double>()

    repeat(7) {
        totals[formatter.format(calendar.time)] = 0.0
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }

    val date = Date()
    transactions.forEach { transaction ->
        date.time = transaction.date
        val key = formatter.format(date)
        totals[key] = (totals[key] ?: 0.0) + transaction.amount
    }

    return totals.toList().takeLast(10)
}

// --- Custom Animated Line Trend Chart ---
@Composable
fun TrendLineChart(transactions: List<Transaction>, lineColor: Color) {
    // 30 day timeseries
    val points = remember(transactions) {
        calculateTrendPoints(transactions)
    }

    val maxVal = points.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val displayMax = if (maxVal == 0f) 1000f else maxVal
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val dotFill = MaterialTheme.colorScheme.surface
    val reveal = rememberChartReveal()
    val summary = remember(points) {
        "Daily totals. " + points.joinToString("; ") { "day ${it.first}: ${String.format("%,.0f", it.second)}" }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                .semantics { contentDescription = summary }
        ) {
            val stepX = size.width / (points.size - 1).coerceAtLeast(1)
            val progress = reveal()

            val gridLines = 4
            for (i in 0..gridLines) {
                val y = size.height * i / gridLines
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (i == gridLines) null else PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                )
            }

            // Smooth the series with horizontal cubic tangents so it reads like a calm curve.
            val coords = points.mapIndexed { index, pair ->
                Offset(index * stepX, size.height - (pair.second.toFloat() / displayMax) * size.height * progress)
            }
            val path = Path()
            coords.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    val prev = coords[index - 1]
                    val midX = (prev.x + point.x) / 2
                    path.cubicTo(midX, prev.y, midX, point.y, point.x, point.y)
                }
            }

            val fillPath = Path()
            fillPath.addPath(path)
            fillPath.lineTo(size.width, size.height)
            fillPath.lineTo(0f, size.height)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
                )
            )
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            coords.forEach { point ->
                drawCircle(color = dotFill, radius = 4.5.dp.toPx(), center = point)
                drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = point, style = Stroke(2.dp.toPx()))
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach {
                Text(
                    text = it.first,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
