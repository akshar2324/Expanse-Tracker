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
import androidx.compose.ui.unit.sp
import com.akshar.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

// --- Custom Animated Line Trend Chart ---
@Composable
fun TrendLineChart(transactions: List<Transaction>, lineColor: Color) {
    // 30 day timeseries
    val points = remember(transactions) {
        val df = SimpleDateFormat("dd", Locale.getDefault())
        val totals = TreeMap<String, Double>()

        // Initialize last 7 days of the month with zero
        val testCal = Calendar.getInstance()
        for (i in 0..6) {
            val key = df.format(testCal.time)
            totals[key] = 0.0
            testCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Fill stats
        transactions.forEach {
            val key = df.format(Date(it.date))
            totals[key] = (totals[key] ?: 0.0) + it.amount
        }

        totals.toList().takeLast(10)
    }

    val maxVal = points.maxOfOrNull { it.second }?.toFloat() ?: 1f
    val displayMax = if (maxVal == 0f) 1000f else maxVal

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp)
        ) {
            val stepX = size.width / (points.size - 1).coerceAtLeast(1)
            val path = Path()

            // Draw Background grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = size.height * i / gridLines
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            // Map and Draw Line Path
            points.forEachIndexed { index, pair ->
                val x = index * stepX
                val pctHeight = pair.second.toFloat() / displayMax
                val y = size.height - (pctHeight * size.height)

                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }

                // Draw node points
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Render path stroke
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx())
            )

            // Fill gradient underneath line
            val fillPath = Path()
            fillPath.addPath(path)
            fillPath.lineTo(size.width, size.height)
            fillPath.lineTo(0f, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach {
                Text(
                    text = it.first,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
