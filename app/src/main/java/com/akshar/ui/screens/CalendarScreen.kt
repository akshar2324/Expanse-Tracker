package com.akshar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akshar.data.model.Bill
import com.akshar.data.model.BillOccurrence
import com.akshar.ui.theme.*
import com.akshar.ui.viewmodel.FinanceViewModel
import com.akshar.utils.BillForecastEngine
import java.text.SimpleDateFormat
import java.util.*
import android.os.Build
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.akshar.utils.NotificationHelper

@Composable
fun CalendarScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // We just request it, if denied, they don't get notifications.
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val bills by viewModel.allBills.collectAsStateWithLifecycle()
    val occurrences by viewModel.allBillOccurrences.collectAsStateWithLifecycle()
    val recurringTransactions by viewModel.allRecurringTransactions.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    val currentBalance = transactions.sumOf { if (it.type == "EXPENSE") -it.amount else it.amount }

    var selectedTab by remember { mutableStateOf(0) } // 0: Agenda, 1: Forecast
    val tabs = listOf("Agenda & Bills", "6-Month Forecast")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        if (selectedTab == 0) {
            MonthCalendarView(
                bills = bills,
                occurrences = occurrences,
                onDayClick = { /* Can show specific day details if needed */ }
            )
            AgendaView(
                bills = bills,
                occurrences = occurrences,
                viewModel = viewModel
            )
        } else {
            ForecastView(
                currentBalance = currentBalance,
                bills = bills,
                occurrences = occurrences,
                recurringTransactions = recurringTransactions,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun AgendaView(
    bills: List<Bill>,
    occurrences: List<BillOccurrence>,
    viewModel: FinanceViewModel
) {
    var showAddBillDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddBillDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Bill")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (bills.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No bills scheduled.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val activeBills = bills.filter { it.isActive }
                    val pausedBills = bills.filter { !it.isActive }

                    if (activeBills.isNotEmpty()) {
                        item {
                            Text("Active Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(activeBills, key = { it.id }) { bill ->
                            BillItem(bill = bill, occurrences = occurrences, viewModel = viewModel)
                        }
                    }

                    if (pausedBills.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Paused Bills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(pausedBills, key = { it.id }) { bill ->
                            BillItem(bill = bill, occurrences = occurrences, viewModel = viewModel)
                        }
                    }
                }
            }
        }

        if (showAddBillDialog) {
            AddEditBillDialog(
                onDismiss = { showAddBillDialog = false },
                onSave = {
                    viewModel.addBill(it)
                    showAddBillDialog = false
                }
            )
        }
    }
}

@Composable
fun BillItem(bill: Bill, occurrences: List<BillOccurrence>, viewModel: FinanceViewModel) {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Determine status of the next occurrence
    val engine = BillForecastEngine()
    val now = engine.getCurrentTimeMillis()

    // Find relevant occurrences
    val billOccs = occurrences.filter { it.billId == bill.id }
    val paidOrSkipped = billOccs.map { it.dueDate }.toSet()

    // Expand to find next unpaid occurrence
    val projected = engine.expandRecurrence(bill, bill.startDate, 12)
    val nextDue = projected.firstOrNull { it !in paidOrSkipped } ?: bill.startDate

    // Next due is overdue if before today
    val isOverdue = nextDue < now
    // Due soon if within 3 days
    val isDueSoon = nextDue >= now && nextDue <= now + (3 * 24 * 60 * 60 * 1000L)

    val statusColor = if (!bill.isActive) {
        Color.Gray
    } else if (isOverdue) {
        MaterialTheme.colorScheme.error
    } else if (isDueSoon) {
        Color(0xFFFFA000) // Amber
    } else {
        MaterialTheme.colorScheme.primary
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(bill.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Next due: ${df.format(Date(nextDue))}", fontSize = 14.sp, color = statusColor)
                    Text(bill.recurrence, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "${viewModel.getCurrencySymbol()}${bill.amount}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (bill.type == "EXPENSE") ExpenseRed else IncomeGreen
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (bill.isActive) {
                        IconButton(onClick = {
                            viewModel.addBillOccurrence(BillOccurrence(billId = bill.id, dueDate = nextDue, state = "PAID"))
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Mark Paid", tint = IncomeGreen)
                        }
                        IconButton(onClick = {
                            viewModel.addBillOccurrence(BillOccurrence(billId = bill.id, dueDate = nextDue, state = "SKIPPED"))
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Skip", tint = Color.Gray)
                        }
                        IconButton(onClick = {
                            viewModel.updateBill(bill.copy(isActive = false))
                        }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = {
                            viewModel.updateBill(bill.copy(isActive = true))
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = {
                        viewModel.deleteBill(bill.id)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastView(
    currentBalance: Double,
    bills: List<Bill>,
    occurrences: List<BillOccurrence>,
    recurringTransactions: List<com.akshar.data.model.RecurringTransaction>,
    viewModel: FinanceViewModel
) {
    val engine = remember { BillForecastEngine() }
    val forecast = remember(currentBalance, bills, occurrences, recurringTransactions) {
        engine.generateSixMonthForecast(currentBalance, bills, occurrences, recurringTransactions)
    }
    val df = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val dfDay = SimpleDateFormat("dd MMM", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Current Balance: ${viewModel.getCurrencySymbol()}${String.format(Locale.US, "%.2f", currentBalance)}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        val firstNegative = forecast.find { it.isFirstNegative }
        if (firstNegative != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Warning: Negative balance projected!", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    Text("On ${dfDay.format(Date(firstNegative.date))}, balance will be ${viewModel.getCurrencySymbol()}${String.format(Locale.US, "%.2f", firstNegative.balance)}", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Group by month
        val byMonth = forecast.groupBy { df.format(Date(it.date)) }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            byMonth.forEach { (month, days) ->
                item {
                    Text(month, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }

                // Show start, middle and end of month to keep it compact, plus any negatives
                val toShow = buildList {
                    add(days.first())
                    if (days.size > 15) add(days[15])
                    add(days.last())
                    addAll(days.filter { it.isFirstNegative })
                }.distinctBy { it.date }.sortedBy { it.date }

                items(toShow) { day ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(dfDay.format(Date(day.date)))
                        Text(
                            "${viewModel.getCurrencySymbol()}${String.format(Locale.US, "%.2f", day.balance)}",
                            color = if (day.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 8.dp, end = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillDialog(
    onDismiss: () -> Unit,
    onSave: (Bill) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var recurrence by remember { mutableStateOf("MONTHLY") }

    val cal = Calendar.getInstance()
    var startDate by remember { mutableStateOf(cal.timeInMillis) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bill / Income") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Income") }
                    )
                }

                Text("Recurrence")
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY").forEach { rec ->
                        FilterChip(
                            selected = recurrence == rec,
                            onClick = { recurrence = rec },
                            label = { Text(rec) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        onSave(
                            Bill(
                                title = title,
                                amount = amt,
                                type = type,
                                category = "Bill", // default
                                startDate = startDate,
                                recurrence = recurrence,
                                reminderLeadTimeDays = 1,
                                isActive = true
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
