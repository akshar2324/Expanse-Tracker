package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RecurringTransaction
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val backupStatus by viewModel.backupStatus.collectAsState()

    val recurringList by viewModel.allRecurringTransactions.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var activeSubTool by remember { mutableStateOf("AI_RECOMMEND") } // "AI_RECOMMEND", "RECURRING", "REPORTS", "BACKUP"

    // Dialog & Form states
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var showReportDetailType by remember { mutableStateOf<String?>(null) } // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    var restoreJsonInput by remember { mutableStateOf("") }

    // Backup status toasts
    LaunchedEffect(backupStatus) {
        if (backupStatus.isNotEmpty()) {
            Toast.makeText(context, backupStatus, Toast.LENGTH_LONG).show()
            viewModel.clearBackupStatus()
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = { Text("Financial Toolkit & AI", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )

                // Tool selection row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        "AI_RECOMMEND" to "AI",
                        "RECURRING" to "Recurring",
                        "REPORTS" to "Reports",
                        "BACKUP" to "Backup"
                    ).forEach { (toolId, tabLabel) ->
                        val active = activeSubTool == toolId
                        val textCol = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        val bg = if (active) MaterialTheme.colorScheme.primary else Color.Transparent

                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .clickable { activeSubTool = toolId }
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            when (activeSubTool) {
                // --- 1. AI Insights Hub ---
                "AI_RECOMMEND" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI",
                                        tint = AnalyticsBlueDark,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "AI Spending Advisor",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Analyze your expense leaks, get savings predictions, and active financial counseling directly via Gemini AI model.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.runAiSpendingAnalysis() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = !aiLoading
                        ) {
                            if (aiLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Psychology, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Smart Insights")
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Analysis Statement Output",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (aiAnalysis.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Press generate to run analysis.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                } else {
                                    SelectionContainer {
                                        Text(
                                            text = aiAnalysis,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 2. Recurring Transactions ---
                "RECURRING" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { showAddRecurringDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New Auto-Recurring Entry")
                        }

                        Text(
                            text = "Active Recurring Logs",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (recurringList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1.0f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.EventRepeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No recurring transactions set up.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1.0f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(recurringList) { recurring ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = recurring.description,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    SuggestionChip(
                                                        onClick = {},
                                                        label = { Text(recurring.frequency) }
                                                    )
                                                    SuggestionChip(
                                                        onClick = {},
                                                        label = { Text(recurring.category) }
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${if (recurring.type == "EXPENSE") "-" else "+"}₹${recurring.amount.toInt()}",
                                                    fontWeight = FontWeight.Black,
                                                    color = if (recurring.type == "EXPENSE") ExpenseRedDark else IncomeGreenDark,
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                IconButton(onClick = { viewModel.deleteRecurringTransactionById(recurring.id) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 3. Spreadsheet Reports Generator ---
                "REPORTS" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = AlertOrangeDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Generate Summary Ledger",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Compile full breakdowns into clean data files to share or backup in sheets.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Text("Select Compilation Scope", fontWeight = FontWeight.Bold)

                        listOf(
                            "DAILY" to "Daily Financial Sheet",
                            "WEEKLY" to "Weekly Income vs Expense Summary",
                            "MONTHLY" to "Monthly category Breakdowns",
                            "YEARLY" to "Yearly Audit Ledger Sheet"
                        ).forEach { (typeKey, rowLabel) ->
                            OutlinedCard(
                                onClick = { showReportDetailType = typeKey },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = rowLabel, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                // --- 4. Offline Backups (JSON Data block) ---
                "BACKUP" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Backup, contentDescription = null, tint = IncomeGreenDark)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Full Offline Backup Controls", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Your data is 100% yours and saved securely on your device. Export to a JSON text block or paste back a JSON to restore.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val json = viewModel.exportDataToJson()
                                if (json != null) {
                                    clipboardManager.setText(AnnotatedString(json))
                                    Toast.makeText(context, "Full Backup copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Export process failed.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Database to Clipboard")
                        }

                        HorizontalDivider()

                        Text("Restore Database", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = restoreJsonInput,
                            onValueChange = { restoreJsonInput = it },
                            label = { Text("Paste JSON Backup string here") },
                            placeholder = { Text("{ \"transactions\": [...] }") },
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )

                        Button(
                            onClick = {
                                if (restoreJsonInput.isNotEmpty()) {
                                    val success = viewModel.restoreDataFromJson(restoreJsonInput)
                                    if (success) {
                                        restoreJsonInput = ""
                                        Toast.makeText(context, "Restore Process Complete!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please paste an exported backup block first.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validate & Restore Database")
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: New Recurring Transaction Dialog ---
    if (showAddRecurringDialog) {
        AddRecurringDialog(
            onDismiss = { showAddRecurringDialog = false },
            onConfirm = { amt, tp, cat, desc, freq, pay ->
                viewModel.addRecurringTransaction(amt, tp, cat, desc, freq, pay)
                showAddRecurringDialog = false
            }
        )
    }

    // --- DIALOG: Compile Report Details ---
    if (showReportDetailType != null) {
        val sheetType = showReportDetailType!!
        val compileReportString = remember(sheetType, transactions) {
            compileFinancialReport(transactions, sheetType)
        }

        AlertDialog(
            onDismissRequest = { showReportDetailType = null },
            title = { Text("$sheetType Financial Sheet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Report compiled successfully:", style = MaterialTheme.typography.labelSmall)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Text(
                            text = compileReportString,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(compileReportString))
                        Toast.makeText(context, "Report CSV copied as Text!", Toast.LENGTH_SHORT).show()
                        showReportDetailType = null
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDetailType = null }) {
                    Text("Done")
                }
            }
        )
    }
}

// --- Report Compiler Helper ---
private fun compileFinancialReport(list: List<Transaction>, type: String): String {
    val output = StringBuilder()
    output.append("--- Expense Tracker Pro CSV Report ---\n")
    output.append("Scope: $type\n")
    output.append("Generated On: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}\n\n")

    val filtered = when (type) {
        "DAILY" -> {
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            list.filter { it.date >= startOfToday }
        }
        "WEEKLY" -> {
            val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            list.filter { it.date >= weekAgo }
        }
        "MONTHLY" -> {
            val monthAgo = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            list.filter { it.date >= monthAgo }
        }
        else -> list // Yearly default / All
    }

    val totalIncome = filtered.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = filtered.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    output.append("SUMMARY STATS:\n")
    output.append("Total Income,₹${totalIncome}\n")
    output.append("Total Expense,₹${totalExpense}\n")
    output.append("Net Cash Flow,₹${totalIncome - totalExpense}\n\n")

    output.append("DETAILED LOGS:\n")
    output.append("Date,Type,Category,Description,Amount,Payment Method\n")

    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    filtered.forEach {
        val escapedDesc = it.description.replace(",", ";")
        output.append("${df.format(Date(it.date))},${it.type},${it.category},$escapedDesc,₹${it.amount},${it.paymentMethod}\n")
    }

    return output.toString()
}

// --- DIALOG: New Recurring Add ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: String, category: String, description: String, frequency: String, payment: String) -> Unit
) {
    var isExpense by remember { mutableStateOf(true) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var paymentMethod by remember { mutableStateOf("UPI") }

    val categories = remember {
        listOf(
            "Rent", "EMI", "Subscription", "Bills", "Salary", "Freelance", "Investment", "Other"
        )
    }
    var categorySelected by remember { mutableStateOf(categories.first()) }
    var catDropdownExpanded by remember { mutableStateOf(false) }

    val frequencies = remember { listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY") }
    var freqDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Recurring Rule") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Type Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("Expense (Debit)") }
                    )
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("Income (Credit)") }
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Impact Sum (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Billing Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Frequency Dropdown
                Text("Repetition Interval", style = MaterialTheme.typography.labelSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { freqDropdownExpanded = true }
                        .padding(vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = frequency, fontSize = 16.sp)
                    DropdownMenu(
                        expanded = freqDropdownExpanded,
                        onDismissRequest = { freqDropdownExpanded = false }
                    ) {
                        frequencies.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq) },
                                onClick = {
                                    frequency = freq
                                    freqDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category Dropdown
                Text("Log Category", style = MaterialTheme.typography.labelSmall)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { catDropdownExpanded = true }
                        .padding(vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = categorySelected, fontSize = 16.sp)
                    DropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    categorySelected = cat
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && description.isNotEmpty()) {
                        onConfirm(
                            amt,
                            if (isExpense) "EXPENSE" else "INCOME",
                            categorySelected,
                            description,
                            frequency,
                            paymentMethod
                        )
                    }
                }
            ) {
                Text("Enable Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
