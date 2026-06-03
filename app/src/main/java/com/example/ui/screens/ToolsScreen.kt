package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.SmsTemplate
import com.example.data.model.ParsedSmsLog
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

    val backupStatus by viewModel.backupStatus.collectAsState()

    val recurringList by viewModel.allRecurringTransactions.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()

    var activeSubTool by remember { mutableStateOf("RECURRING") } // "RECURRING", "REPORTS", "BACKUP"

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
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = { Text("Tools & Reminders", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets(0.dp)
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
                        "RECURRING" to "Reminders",
                        "UPISMS" to "UPI SMS",
                        "VAULT" to "Vault",
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
                // --- 1. Recurring Transactions ---
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

                // --- SMS UPI Auto Parsing configs ---
                "UPISMS" -> {
                    val smsTemplates by viewModel.allSmsTemplates.collectAsState()
                    val parsedSmsLogs by viewModel.allParsedSmsLogs.collectAsState()
                    
                    var creditKeywordsInput by remember { mutableStateOf("") }
                    var creditExampleInput by remember { mutableStateOf("") }
                    var debitKeywordsInput by remember { mutableStateOf("") }
                    var debitExampleInput by remember { mutableStateOf("") }

                    // Pre-fill fields when loaded
                    LaunchedEffect(smsTemplates) {
                        smsTemplates.find { it.id == "CREDIT" }?.let {
                            creditKeywordsInput = it.keywords
                            creditExampleInput = it.exampleText
                        }
                        smsTemplates.find { it.id == "DEBIT" }?.let {
                            debitKeywordsInput = it.keywords
                            debitExampleInput = it.exampleText
                        }
                    }

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
                                        imageVector = Icons.Default.Sms,
                                        contentDescription = null,
                                        tint = AlertOrangeDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "UPI SMS Auto-Reader",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Define words and bank formatted SMS lines to automatically parse transaction details in the background. Tap 'Save Configurations' to update.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Credit Card Form
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(IncomeGreen.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Credit / Income SMS Patterns", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Text("Keywords (comma-separated)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = creditKeywordsInput,
                                    onValueChange = { creditKeywordsInput = it },
                                    placeholder = { Text("credited, deposited, added, received") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Text("Example SMS Text (to reference format)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = creditExampleInput,
                                    onValueChange = { creditExampleInput = it },
                                    placeholder = { Text("Your account XX2432 is credited with INR 500.00") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                            }
                        }

                        // Debit Card Form
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(ExpenseRed.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Debit / Expense SMS Patterns", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Text("Keywords (comma-separated)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = debitKeywordsInput,
                                    onValueChange = { debitKeywordsInput = it },
                                    placeholder = { Text("debited, spent, paid, charged") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Text("Example SMS Text (to reference format)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = debitExampleInput,
                                    onValueChange = { debitExampleInput = it },
                                    placeholder = { Text("Your account XX2432 is debited by Rs.1500.00") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val creditKw = creditKeywordsInput.ifBlank { "credited, deposited, added, received" }
                                val creditEx = creditExampleInput.ifBlank { "Your a/c XX2432 is credited with INR 500.00 on 03-Jun" }
                                val debitKw = debitKeywordsInput.ifBlank { "debited, paid, spent, sent, charged, deduction" }
                                val debitEx = debitExampleInput.ifBlank { "Your a/c XX2432 is debited by Rs.1500.00 on 03-Jun" }
                                
                                viewModel.updateSmsTemplate("CREDIT", creditEx, creditKw)
                                viewModel.updateSmsTemplate("DEBIT", debitEx, debitKw)
                                Toast.makeText(context, "Configurations saved! Background parser updated.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Configurations", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SMS Parse History Log",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            if (parsedSmsLogs.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearAllSmsLogs() }) {
                                    Text("Clear All Logs", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        if (parsedSmsLogs.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmsFailed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No parsed notifications recorded yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        } else {
                            parsedSmsLogs.forEach { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Sender: ${log.smsSender}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            
                                            // Status Badge
                                            val badgeColor = when (log.status) {
                                                "CONFIRMED" -> IncomeGreen.copy(alpha = 0.15f)
                                                "IGNORED" -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> AlertOrangeDark.copy(alpha = 0.15f)
                                            }
                                            val badgeTextCol = when (log.status) {
                                                "CONFIRMED" -> IncomeGreenDark
                                                "IGNORED" -> MaterialTheme.colorScheme.onSurfaceVariant
                                                else -> AlertOrangeDark
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(badgeColor)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = log.status,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = badgeTextCol
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = log.smsBody,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val formattedDate = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date(log.date))
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (log.type == "EXPENSE") "Debit" else "Credit",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (log.type == "EXPENSE") ExpenseRed else IncomeGreen
                                                )
                                                Text(
                                                    text = "₹${String.format(Locale.getDefault(), "%.2f", log.amount)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (log.type == "EXPENSE") ExpenseRed else IncomeGreen
                                                )
                                            }
                                        }
                                        
                                        if (log.status == "CONFIRMED" && log.category.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Assigned Category: ${log.category}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- security settings (Finance Vault) ---
                "VAULT" -> {
                    val sharedPrefs = remember { context.getSharedPreferences("vault_settings", android.content.Context.MODE_PRIVATE) }
                    var vaultEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("vault_enabled", true)) }
                    var vaultBiometrics by remember { mutableStateOf(sharedPrefs.getBoolean("vault_biometrics", true)) }
                    var actualPin by remember { mutableStateOf(sharedPrefs.getString("vault_pin", "1234") ?: "1234") }

                    var pinChangeInput by remember { mutableStateOf("") }
                    var confirmPinInput by remember { mutableStateOf("") }

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
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Advanced Security & Vault",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Maintain privacy for your transactions and bank statement records. Toggle access checks and set custom authentication PIN codes below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Authentication Methods",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text("Enable App Secure Lock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Requires lock screens check on opening the app.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Switch(
                                        checked = vaultEnabled,
                                        onCheckedChange = { checked ->
                                            vaultEnabled = checked
                                            sharedPrefs.edit().putBoolean("vault_enabled", checked).apply()
                                            Toast.makeText(context, if (checked) "Secure lock enabled!" else "Secure lock disabled!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }

                                Divider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.0f)) {
                                        Text("Enable Biometric Fingerprint", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Accept fingerprint scanner verification as instant passcode shortcut.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Switch(
                                        checked = vaultBiometrics,
                                        enabled = vaultEnabled,
                                        onCheckedChange = { checked ->
                                            vaultBiometrics = checked
                                            sharedPrefs.edit().putBoolean("vault_biometrics", checked).apply()
                                            Toast.makeText(context, if (checked) "Biometrics integrated!" else "Biometrics omitted.", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = vaultEnabled) {
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "Reset Vault passcode",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Text(
                                        text = "Current PIN: ****",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )

                                    OutlinedTextField(
                                        value = pinChangeInput,
                                        onValueChange = { input ->
                                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                                pinChangeInput = input
                                            }
                                        },
                                        label = { Text("New 4-Digit Passcode") },
                                        placeholder = { Text("Enter 4 numbers") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = confirmPinInput,
                                        onValueChange = { input ->
                                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                                confirmPinInput = input
                                            }
                                        },
                                        label = { Text("Confirm New Passcode") },
                                        placeholder = { Text("Re-enter 4 numbers") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            if (pinChangeInput.length != 4) {
                                                Toast.makeText(context, "PIN must be exactly 4 digits!", Toast.LENGTH_SHORT).show()
                                            } else if (pinChangeInput != confirmPinInput) {
                                                Toast.makeText(context, "PINs do not match!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                actualPin = pinChangeInput
                                                sharedPrefs.edit().putString("vault_pin", pinChangeInput).apply()
                                                pinChangeInput = ""
                                                confirmPinInput = ""
                                                Toast.makeText(context, "Security passcode updated successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save New passcode")
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
