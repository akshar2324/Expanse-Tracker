package com.akshar.ui.screens.analytics

import com.akshar.ui.theme.AkSpendTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.akshar.ui.theme.Spacing
import com.akshar.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

// --- Custom Animated Bar Chart ---
@Composable
fun CashFlowBarChart(transactions: List<Transaction>) {
    // 5 day summary comparison
    val data = remember(transactions) {
        val df = SimpleDateFormat("dd MMM", Locale.getDefault())
        val cal = Calendar.getInstance()
        val flow = ArrayList<Triple<String, Double, Double>>() // Days, Income, Expense

        class DayWindow(val label: String, val start: Long, val end: Long) {
            var income = 0.0
            var expense = 0.0
        }

        val windows = Array(5) {
            val dayStr = df.format(cal.time)
            val startOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endOfDay = startOfDay + 86400000L
            cal.add(Calendar.DAY_OF_YEAR, -1)
            DayWindow(dayStr, startOfDay, endOfDay)
        }

        transactions.forEach { transaction ->
            val window = windows.firstOrNull { transaction.date in it.start until it.end }
                ?: return@forEach
            when (transaction.type) {
                "INCOME" -> window.income += transaction.amount
                "EXPENSE" -> window.expense += transaction.amount
            }
        }

        for (index in windows.lastIndex downTo 0) {
            val window = windows[index]
            flow.add(Triple(window.label, window.income, window.expense))
        }
        flow
    }

    val maxVal = data.maxOf { it.second.coerceAtLeast(it.third) }.toFloat()
    val axisMax = if (maxVal == 0f) 5000f else maxVal
    val incomeColor = AkSpendTheme.finance.income
    val expenseColor = AkSpendTheme.finance.expense
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val reveal = rememberChartReveal()
    val summary = remember(data) {
        "Income and expenses for the last 5 days. " + data.joinToString("; ") {
            "${it.first}: earned ${String.format("%,.0f", it.second)}, spent ${String.format("%,.0f", it.third)}"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            ChartLegendDot(color = incomeColor, label = "Income")
            ChartLegendDot(color = expenseColor, label = "Expense")
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = Spacing.xs)
                .semantics { contentDescription = summary }
        ) {
            val barWidth = 12.dp.toPx()
            val gap = 4.dp.toPx()
            val spacing = size.width / data.size
            val gridLines = 4
            val radius = CornerRadius(barWidth / 2, barWidth / 2)
            val progress = reveal()

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

            data.forEachIndexed { index, triple ->
                val centerX = index * spacing + (spacing / 2)
                val incHeight = (triple.second.toFloat() / axisMax) * size.height * progress
                val expHeight = (triple.third.toFloat() / axisMax) * size.height * progress
                if (incHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(incomeColor, incomeColor.copy(alpha = 0.55f)), startY = size.height - incHeight, endY = size.height),
                        topLeft = Offset(centerX - barWidth - gap / 2, size.height - incHeight),
                        size = Size(barWidth, incHeight),
                        cornerRadius = radius
                    )
                }
                if (expHeight > 0f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(expenseColor, expenseColor.copy(alpha = 0.55f)), startY = size.height - expHeight, endY = size.height),
                        topLeft = Offset(centerX + gap / 2, size.height - expHeight),
                        size = Size(barWidth, expHeight),
                        cornerRadius = radius
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach {
                Text(
                    text = it.first,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
