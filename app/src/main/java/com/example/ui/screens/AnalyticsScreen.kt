package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsState()

    // --- State Toggles ---
    var selectedTab by remember { mutableStateOf("EXPENSES") } // "EXPENSES", "INCOME", "CASHFLOW"

    val expenses = transactions.filter { it.type == "EXPENSE" }
    val incomes = transactions.filter { it.type == "INCOME" }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = { Text("Financial Analytics", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )

                // Tab selectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("EXPENSES", "INCOME", "CASHFLOW").forEach { tab ->
                        val active = selectedTab == tab
                        val textCol = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        val bg = if (active) MaterialTheme.colorScheme.primary else Color.Transparent

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = tab }
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (tab == "CASHFLOW") "Cash Flow" else tab.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textCol
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "No data",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Log transactions to populate charts!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            } else {
                when (selectedTab) {
                    "EXPENSES" -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Expense By Category (Pie)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PieSliceChart(transactions = expenses)
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Monthly Spending Trend",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TrendLineChart(transactions = expenses, lineColor = ExpenseRedDark)
                                }
                            }
                        }
                    }

                    "INCOME" -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Income Sources Share",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PieSliceChart(transactions = incomes)
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Income Accumulation Trend",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    TrendLineChart(transactions = incomes, lineColor = IncomeGreenDark)
                                }
                            }
                        }
                    }

                    "CASHFLOW" -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Cash Flow (Income vs Expense)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CashFlowBarChart(transactions = transactions)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        Color(0xFF26A69A), Color(0xFFEF5350), Color(0xFF42A5F5),
        Color(0xFFFFB74D), Color(0xFFAB47BC), Color(0xFF26C6DA),
        Color(0xFF9CCC65), Color(0xFFFF7043), Color(0xFF78909C),
        Color(0xFFEC407A)
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
