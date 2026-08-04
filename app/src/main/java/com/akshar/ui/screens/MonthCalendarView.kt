package com.akshar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akshar.data.model.Bill
import com.akshar.data.model.BillOccurrence
import com.akshar.ui.theme.ExpenseRed
import com.akshar.ui.theme.IncomeGreen
import com.akshar.utils.BillForecastEngine
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthCalendarView(
    bills: List<Bill>,
    occurrences: List<BillOccurrence>,
    onDayClick: (Long) -> Unit
) {
    var currentMonthCal by remember { mutableStateOf(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }) }

    val dfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    // Generate dates for current month
    val daysInMonth = currentMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = currentMonthCal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 2=Monday, etc.

    // Engine for events
    val engine = remember { BillForecastEngine() }

    val currentMonthStart = currentMonthCal.timeInMillis
    val currentMonthEnd = Calendar.getInstance().apply {
        timeInMillis = currentMonthStart
        add(Calendar.MONTH, 1)
        add(Calendar.MILLISECOND, -1)
    }.timeInMillis

    // Collect all events for this month
    val eventsByDay = remember(bills, occurrences, currentMonthStart) {
        val map = mutableMapOf<Int, MutableList<Pair<Bill, String>>>() // String is state: PAID, SKIPPED, PENDING

        bills.filter { it.isActive }.forEach { bill ->
            val billOccs = occurrences.filter { it.billId == bill.id }
            val paidOrSkipped = billOccs.associateBy { it.dueDate }

            // Generate up to end of this month
            val projectedDates = engine.expandRecurrence(bill, bill.startDate, 30)

            projectedDates.filter { it in currentMonthStart..currentMonthEnd }.forEach { date ->
                val dayCal = Calendar.getInstance().apply { timeInMillis = date }
                val day = dayCal.get(Calendar.DAY_OF_MONTH)

                val state = if (paidOrSkipped.containsKey(date)) {
                    paidOrSkipped[date]!!.state
                } else if (date < System.currentTimeMillis()) {
                    "OVERDUE"
                } else {
                    "PENDING"
                }

                map.getOrPut(day) { mutableListOf() }.add(bill to state)
            }
        }
        map
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentMonthCal = Calendar.getInstance().apply {
                    timeInMillis = currentMonthCal.timeInMillis
                    add(Calendar.MONTH, -1)
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month")
            }

            Text(dfMonthYear.format(currentMonthCal.time), fontWeight = FontWeight.Bold, fontSize = 18.sp)

            IconButton(onClick = {
                currentMonthCal = Calendar.getInstance().apply {
                    timeInMillis = currentMonthCal.timeInMillis
                    add(Calendar.MONTH, 1)
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid
        val totalCells = daysInMonth + (firstDayOfWeek - 1)
        val rows = (totalCells + 6) / 7

        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - (firstDayOfWeek - 1) + 1

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (day in 1..daysInMonth) {
                                val dateMillis = Calendar.getInstance().apply {
                                    timeInMillis = currentMonthCal.timeInMillis
                                    set(Calendar.DAY_OF_MONTH, day)
                                }.timeInMillis

                                val events = eventsByDay[day] ?: emptyList()
                                val isToday = isSameDay(dateMillis, System.currentTimeMillis())

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (events.isNotEmpty()) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onDayClick(dateMillis) }
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontWeight = if (isToday || events.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )

                                    if (events.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        // Dots for events
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            events.take(3).forEach { (_, state) ->
                                                val dotColor = when (state) {
                                                    "PAID" -> IncomeGreen
                                                    "SKIPPED" -> Color.Gray
                                                    "OVERDUE" -> ExpenseRed
                                                    else -> MaterialTheme.colorScheme.primary // PENDING
                                                }
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor).padding(horizontal = 1.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
