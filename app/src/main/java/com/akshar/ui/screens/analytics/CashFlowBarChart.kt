package com.akshar.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshar.data.model.Transaction
import com.akshar.ui.theme.ExpenseRedDark
import com.akshar.ui.theme.IncomeGreenDark
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

        for (i in 0..4) {
            val dayStr = df.format(cal.time)
            val startOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endOfDay = startOfDay + 86400000L

            val dayTxs = transactions.filter { it.date in startOfDay until endOfDay }
            val inc = dayTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val exp = dayTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            flow.add(0, Triple(dayStr, inc, exp))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        flow
    }

    val maxVal = data.maxOf { it.second.coerceAtLeast(it.third) }.toFloat()
    val axisMax = if (maxVal == 0f) 5000f else maxVal

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(horizontal = 8.dp)
        ) {
            val barWidth = 14.dp.toPx()
            val spacing = size.width / data.size
            val gridLines = 4

            // Background grid
            for (i in 0..gridLines) {
                val y = size.height * i / gridLines
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            // Columns Draw
            data.forEachIndexed { index, triple ->
                val centerX = index * spacing + (spacing / 2)

                // Income Column
                val incPct = triple.second.toFloat() / axisMax
                val incHeight = incPct * size.height
                drawRect(
                    color = IncomeGreenDark,
                    topLeft = Offset(centerX - barWidth - 2.dp.toPx(), size.height - incHeight),
                    size = Size(barWidth, incHeight)
                )

                // Expense Column
                val expPct = triple.third.toFloat() / axisMax
                val expHeight = expPct * size.height
                drawRect(
                    color = ExpenseRedDark,
                    topLeft = Offset(centerX + 2.dp.toPx(), size.height - expHeight),
                    size = Size(barWidth, expHeight)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach {
                Text(
                    text = it.first,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
