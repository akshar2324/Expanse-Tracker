package com.akshar.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.unit.Dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.akshar.ui.components.*
import com.akshar.data.model.RecurringTransaction
import com.akshar.data.model.Transaction
import com.akshar.data.model.Debt
import androidx.compose.foundation.horizontalScroll
import com.akshar.ui.theme.*
import com.akshar.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier,
    navController: NavController? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val dbFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.restoreDatabaseFromDbFile(context, uri)
            } else {
                Toast.makeText(context, "No backup file selected", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    val recurringList by viewModel.allRecurringTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val transactions = allTransactions.filter { it.transferId == null }

    // null shows the hub; otherwise one of the tool ids below.
    var activeSubTool by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = activeSubTool != null) { activeSubTool = null }
    val bottomInset = LocalFloatingBarInset.current

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

    Column(modifier = modifier.fillMaxSize()) {
        if (activeSubTool == null) {
            MoreHub(
                bottomInset = bottomInset,
                onOpen = { toolId ->
                    when {
                        toolId == "ACCOUNTS" && navController != null -> navController.navigate("accounts")
                        toolId == "WEEKLY_DIGEST" && navController != null -> navController.navigate("weeklyDigest")
                        toolId == "CSV_IMPORT" && navController != null -> navController.navigate("csvImport")
                        else -> activeSubTool = toolId
                    }
                }
            )
        } else {
        val tool = moreTools.firstOrNull { it.id == activeSubTool }
        ScreenHeader(
            title = tool?.title ?: "More",
            subtitle = tool?.subtitle,
            onBack = { activeSubTool = null }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = bottomInset)
        ) {

            when (activeSubTool) {
                "WEEKLY_DIGEST" -> {
                    WeeklyDigestScreen(viewModel = viewModel, onBack = { activeSubTool = null })
                }
                "CALCULATORS" -> {
                    CalculatorsTab()
                }
                "CSV_IMPORT" -> {
                    val csvViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.akshar.ui.viewmodel.CsvImportViewModel>()
                    CsvImportScreen(viewModel = csvViewModel, onBack = { activeSubTool = null })
                }
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        colors = glassCardColors()
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
                                                    text = "${if (recurring.type == "EXPENSE") "-" else "+"}${viewModel.getCurrencySymbol()}${String.format("%,.0f", recurring.amount)}",
                                                    color = if (recurring.type == "EXPENSE") AkSpendTheme.finance.expense else AkSpendTheme.finance.income,
                                                    style = MaterialTheme.typography.titleMedium.money()
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
                            colors = glassCardColors(AkSpendTheme.glass.fillStrong)
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

                        Card(modifier = Modifier.fillMaxWidth(), colors = glassCardColors(), border = glassBorder()) {
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
                                        Text("App lock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Ask for your passcode every time AkSpend opens.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        Text("Fingerprint unlock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("Use your fingerprint instead of typing the passcode.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Card(modifier = Modifier.fillMaxWidth(), colors = glassCardColors(), border = glassBorder()) {
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
                                        colors = glassTextFieldColors(),
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
                                        colors = glassTextFieldColors(),
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
                            colors = glassCardColors(AkSpendTheme.glass.fillStrong)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = AkSpendTheme.finance.warning,
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
                            Card(
                                colors = glassCardColors(),
                                border = glassBorder(),
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

                // --- 4. Offline Backups (Physical .db database file) ---
                "BACKUP" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Card(
                            colors = glassCardColors(AkSpendTheme.glass.fillStrong),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "NATIVE SQLITE BACKUPS",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your financial records are 100% yours, stored locally on your device's protected memory. You can now perform full binary exports of the underlying Room SQLite database directly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Export Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = glassCardColors(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Export Database File",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Exporting creates a production-grade, uncompressed binary SQLite snapshot of your database (.db). You can save this file securely in Google Drive, local Folders, or send it to another device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        viewModel.exportDatabaseToDbFile(context)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Export .db Backup File", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Import/Restore Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = glassCardColors(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Restore Database File",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select a previously exported database .db backup. Note: Restoring overrides your existing local database file and reboots the application to securely swap the data engine.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        dbFilePickerLauncher.launch("application/octet-stream")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.outlineVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Upload & Restore .db File", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                "SETTINGS" -> {
                    val selectedCountryCode by viewModel.selectedCountry.collectAsStateWithLifecycle()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Global Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            colors = glassCardColors(AkSpendTheme.glass.fillStrong),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Select Country & Currency Region",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "This automatically adjusts payment methods, hides region-specific features (like UPI parsing in non-Indian regions), and updates formatting symbols.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val countriesList = listOf(
                            Triple("IN", "India", "Rupee (₹)"),
                            Triple("US", "United States", "US Dollar ($)"),
                            Triple("JP", "Japan", "Japanese Yen (¥)"),
                            Triple("EU", "European Union", "Euro (€)")
                        )

                        countriesList.forEach { (code, cName, details) ->
                            val active = selectedCountryCode == code
                            Card(
                                onClick = { viewModel.updateCountry(code) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(if (active) 1.5.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$cName ($code)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RadioButton(
                                        selected = active,
                                        onClick = { viewModel.updateCountry(code) }
                                    )
                                }
                            }
                        }
                    }
                }

                "DEBTS" -> {
                    val debtsList by viewModel.allDebts.collectAsStateWithLifecycle()
                    val selectedCountryCode by viewModel.selectedCountry.collectAsStateWithLifecycle()
                    val currencySymbol = viewModel.getCurrencySymbol()

                    var showAddDebtDialog by remember { mutableStateOf(false) }
                    var debtsFilter by remember { mutableStateOf("ALL") } // "ALL", "LENT", "BORROWED", "RESOLVED"

                    val filteredDebts = remember(debtsList, debtsFilter) {
                        when (debtsFilter) {
                            "LENT" -> debtsList.filter { it.type == "LENT" && !it.isResolved }
                            "BORROWED" -> debtsList.filter { it.type == "BORROWED" && !it.isResolved }
                            "RESOLVED" -> debtsList.filter { it.isResolved }
                            else -> debtsList
                        }
                    }

                    val totalLent = remember(debtsList) {
                        debtsList.filter { it.type == "LENT" && !it.isResolved }.sumOf { it.amount }
                    }
                    val totalBorrowed = remember(debtsList) {
                        debtsList.filter { it.type == "BORROWED" && !it.isResolved }.sumOf { it.amount }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Debts Balance",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Overview of pending settlements",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    val netScore = totalLent - totalBorrowed
                                    val netFormatted = viewModel.formatCurrencyValue(kotlin.math.abs(netScore))
                                    Text(
                                        text = if (netScore >= 0) "+$netFormatted" else "-$netFormatted",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netScore >= 0) AkSpendTheme.finance.income else AkSpendTheme.finance.expense
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Others Owe You (Lent)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = viewModel.formatCurrencyValue(totalLent),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AkSpendTheme.finance.income
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "You Owe Others (Borrowed)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = viewModel.formatCurrencyValue(totalBorrowed),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AkSpendTheme.finance.expense
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showAddDebtDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("New Borrow / Lent Record")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                "ALL" to "All",
                                "LENT" to "Lent",
                                "BORROWED" to "Borrowed",
                                "RESOLVED" to "Settled"
                            ).forEach { (fCode, fName) ->
                                val active = debtsFilter == fCode
                                val textCol = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                val bg = if (active) MaterialTheme.colorScheme.primary else Color.Transparent

                                Box(
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .fillMaxHeight()
                                        .clickable { debtsFilter = fCode }
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = fName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = textCol
                                        )
                                    )
                                }
                            }
                        }

                        if (filteredDebts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1.0f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No pending records found under this filter.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1.0f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredDebts, key = { it.id }) { debt ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (debt.isResolved) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                              else MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val typeBadgeBg = if (debt.type == "LENT") AkSpendTheme.finance.income.copy(alpha = 0.15f)
                                                                     else AkSpendTheme.finance.expense.copy(alpha = 0.15f)
                                                    val typeBadgeTc = if (debt.type == "LENT") AkSpendTheme.finance.income else AkSpendTheme.finance.expense
                                                    val typeText = if (debt.type == "LENT") "Lent" else "Borrowed"
                                                    
                                                    Text(
                                                        text = debt.personName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = if (debt.isResolved) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(typeBadgeBg)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = typeText,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = typeBadgeTc
                                                        )
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = debt.description.ifBlank { "Personal record" },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(debt.date))
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "Opened: $dateStr",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(start = 12.dp)
                                            ) {
                                                Text(
                                                    text = viewModel.formatCurrencyValue(debt.amount),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (debt.isResolved) MaterialTheme.colorScheme.outline
                                                            else if (debt.type == "LENT") AkSpendTheme.finance.income
                                                            else AkSpendTheme.finance.expense
                                                )
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    FilledTonalIconButton(
                                                        onClick = {
                                                            if (debt.isResolved) {
                                                                viewModel.unresolveDebt(debt)
                                                            } else {
                                                                viewModel.resolveDebt(debt)
                                                            }
                                                        },
                                                        modifier = Modifier.size(36.dp),
                                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                            containerColor = if (debt.isResolved) MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                                                            else MaterialTheme.colorScheme.primaryContainer
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = if (debt.isResolved) Icons.Default.Refresh else Icons.Default.Check,
                                                            contentDescription = "Resolve",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.deleteDebt(debt) },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
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

                    if (showAddDebtDialog) {
                        AddDebtDialog(
                            onDismiss = { showAddDebtDialog = false },
                            onConfirm = { person, amt, tp, desc, date, due ->
                                viewModel.addDebt(person, amt, tp, desc, date, due)
                                showAddDebtDialog = false
                            },
                            currencySymbol = currencySymbol
                        )
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
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
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

private data class MoreTool(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val moreTools = listOf(
    MoreTool("WEEKLY_DIGEST", "Weekly digest", "Your week in review, computed on this device", Icons.Rounded.AutoAwesome),
    MoreTool("CSV_IMPORT", "Import CSV", "Bring in a bank statement export", Icons.Rounded.UploadFile),
    MoreTool("ACCOUNTS", "Accounts", "Balances, transfers, reconciling", Icons.Rounded.AccountBalance),
    MoreTool("RECURRING", "Reminders", "Bills and income that repeat", Icons.Rounded.EventRepeat),
    MoreTool("DEBTS", "Borrow & lend", "Money owed to and by you", Icons.Rounded.Handshake),
    MoreTool("REPORTS", "Reports", "Daily to yearly summaries", Icons.Rounded.Assessment),
    MoreTool("CALCULATORS", "Calculators", "EMI, SIP and quick maths", Icons.Rounded.Calculate),
    MoreTool("BACKUP", "Backup & restore", "Export or restore your data file", Icons.Rounded.Backup),
    MoreTool("VAULT", "Vault & passcode", "App lock and fingerprint", Icons.Rounded.Shield),
    MoreTool("SETTINGS", "Region & currency", "Currency symbol and payment methods", Icons.Rounded.Public),
)

@Composable
private fun MoreHub(bottomInset: Dp, onOpen: (String) -> Unit) {
    val tints = listOf(
        MaterialTheme.colorScheme.tertiary,
        AkSpendTheme.finance.info,
        MaterialTheme.colorScheme.primary,
        AkSpendTheme.finance.warning,
        MaterialTheme.colorScheme.secondary,
        AkSpendTheme.finance.income,
    )
    val featured = moreTools.take(2)
    val rest = moreTools.drop(2)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = bottomInset + Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader(
                title = "More",
                subtitle = "Tools that keep your money organised. Everything runs offline.",
                insetHorizontally = false
            )
        }
        featured.forEachIndexed { index, tool ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onOpen(tool.id) },
                    onClickLabel = "Open ${tool.title}",
                    fill = AkSpendTheme.glass.fillStrong,
                    contentPadding = PaddingValues(Spacing.lg)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon = tool.icon, tint = tints[index], size = 48.dp)
                        Spacer(Modifier.width(Spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(tool.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = "Everything else")
        }
        rest.forEachIndexed { index, tool ->
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp),
                    onClick = { onOpen(tool.id) },
                    onClickLabel = "Open ${tool.title}",
                    contentPadding = PaddingValues(Spacing.lg)
                ) {
                    IconBadge(icon = tool.icon, tint = tints[(index + 2) % tints.size], size = 40.dp)
                    Spacer(Modifier.height(Spacing.md))
                    Text(tool.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(tool.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
                    colors = glassTextFieldColors(),
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Impact Sum (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    colors = glassTextFieldColors(),
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
                    Text(text = frequency, style = MaterialTheme.typography.bodyLarge)
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
                    Text(text = categorySelected, style = MaterialTheme.typography.bodyLarge)
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
                    val amt = com.akshar.utils.MathUtils.evaluateMathExpression(amountStr) ?: 0.0
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (personName: String, amount: Double, type: String, description: String, date: Long, dueDate: Long?) -> Unit,
    currencySymbol: String
) {
    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("LENT") } // "LENT" or "BORROWED"
    var description by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Borrow / Lent Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type Choice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("LENT" to "Lent", "BORROWED" to "Borrowed").forEach { (tCode, tLabel) ->
                        val active = type == tCode
                        val activeColor = if (tCode == "LENT") AkSpendTheme.finance.income else AkSpendTheme.finance.expense

                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight()
                                .clickable { type = tCode }
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) activeColor else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tLabel,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Name
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text("Person's Name") },
                    placeholder = { Text("e.g. John Doe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    placeholder = { Text("e.g. For dinner bills, project, etc.") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amtVal = com.akshar.utils.MathUtils.evaluateMathExpression(amountText)
                    if (personName.isBlank()) {
                        Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                    } else if (amtVal == null || amtVal <= 0.0) {
                        Toast.makeText(context, "Please enter a valid positive amount", Toast.LENGTH_SHORT).show()
                    } else {
                        onConfirm(
                            personName.trim(),
                            amtVal,
                            type,
                            description.trim(),
                            System.currentTimeMillis(),
                            null
                        )
                    }
                }
            ) {
                Text("Save Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CalculatorsTab() {
    var principal by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var emiResult by remember { mutableStateOf<Double?>(null) }
    var ciResult by remember { mutableStateOf<Double?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Financial Calculators", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = glassCardColors(AkSpendTheme.glass.fillStrong)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = principal,
                    onValueChange = { principal = it },
                    label = { Text("Principal Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Interest Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (Years)") },
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val p = com.akshar.utils.MathUtils.evaluateMathExpression(principal) ?: 0.0
                            val r = com.akshar.utils.MathUtils.evaluateMathExpression(rate) ?: 0.0
                            val t = com.akshar.utils.MathUtils.evaluateMathExpression(time) ?: 0.0
                            if (p > 0 && r > 0 && t > 0) {
                                val rMonthly = r / (12 * 100)
                                val nMonths = t * 12
                                val emi = (p * rMonthly * Math.pow(1 + rMonthly, nMonths)) / (Math.pow(1 + rMonthly, nMonths) - 1)
                                emiResult = emi
                                ciResult = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Calculate EMI")
                    }
                    Button(
                        onClick = {
                            val p = com.akshar.utils.MathUtils.evaluateMathExpression(principal) ?: 0.0
                            val r = com.akshar.utils.MathUtils.evaluateMathExpression(rate) ?: 0.0
                            val t = com.akshar.utils.MathUtils.evaluateMathExpression(time) ?: 0.0
                            if (p > 0 && r > 0 && t > 0) {
                                val amount = p * Math.pow(1 + (r / 100), t)
                                ciResult = amount
                                emiResult = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Calculate CI")
                    }
                }

                if (emiResult != null) {
                    Text("Monthly EMI: ${String.format("%.2f", emiResult)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (ciResult != null) {
                    Text("Total Amount (CI): ${String.format("%.2f", ciResult)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
