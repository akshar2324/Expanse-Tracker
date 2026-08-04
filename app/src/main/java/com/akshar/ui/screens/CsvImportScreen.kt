package com.akshar.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akshar.ui.viewmodel.CsvImportViewModel
import com.akshar.ui.viewmodel.ImportStep

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvImportScreen(
    viewModel: CsvImportViewModel,
    onBack: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Import CSV", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentStep) {
                ImportStep.SELECT_FILE -> SelectFileStep(viewModel)
                ImportStep.CONFIGURE_MAPPING -> ConfigureMappingStep(viewModel)
                ImportStep.PREVIEW -> PreviewStep(viewModel)
                ImportStep.SUMMARY -> SummaryStep(viewModel)
            }
        }
    }
}

@Composable
fun SelectFileStep(viewModel: CsvImportViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handleFileSelected(uri)
        } else {
            Toast.makeText(context, "No file selected", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FileUpload,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Import Bank Statements (CSV)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Select a CSV file from your device to import transactions. Your data stays completely offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { launcher.launch("text/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Select CSV File")
        }

        Spacer(modifier = Modifier.height(32.dp))
        val profiles by viewModel.savedProfiles.collectAsStateWithLifecycle()
        if (profiles.isNotEmpty()) {
            Text("Or start with a saved profile:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(profiles) { profile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.loadProfile(profile)
                                launcher.launch("text/*")
                            }
                    ) {
                        Text(profile.name, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigureMappingStep(viewModel: CsvImportViewModel) {
    val fileName by viewModel.fileName.collectAsStateWithLifecycle()
    val headers by viewModel.rawHeaders.collectAsStateWithLifecycle()

    val delimiter by viewModel.delimiter.collectAsStateWithLifecycle()
    val hasHeader by viewModel.hasHeader.collectAsStateWithLifecycle()
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle()
    val dateColumn by viewModel.dateColumn.collectAsStateWithLifecycle()
    val amountColumn by viewModel.amountColumn.collectAsStateWithLifecycle()
    val debitColumn by viewModel.debitColumn.collectAsStateWithLifecycle()
    val creditColumn by viewModel.creditColumn.collectAsStateWithLifecycle()
    val descriptionColumn by viewModel.descriptionColumn.collectAsStateWithLifecycle()
    val categoryColumn by viewModel.categoryColumn.collectAsStateWithLifecycle()
    val localeStr by viewModel.localeStr.collectAsStateWithLifecycle()
    val invertAmountSigns by viewModel.invertAmountSigns.collectAsStateWithLifecycle()
    val useDebitCredit by viewModel.useDebitCredit.collectAsStateWithLifecycle()

    var showSaveProfileDialog by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("File: $fileName", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("File Format", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = delimiter,
                    onValueChange = { if (it.length <= 1) viewModel.delimiter.value = it },
                    label = { Text("Delimiter (e.g. , or ;)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasHeader, onCheckedChange = { viewModel.hasHeader.value = it })
                    Text("First row is header")
                }

                OutlinedTextField(
                    value = localeStr,
                    onValueChange = { viewModel.localeStr.value = it },
                    label = { Text("Number Locale (e.g. en_US, de_DE)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Column Mapping", fontWeight = FontWeight.Bold)
                Text("Use exact column name (if header exists) or 0-based index.", fontSize = 12.sp)

                OutlinedTextField(
                    value = dateColumn,
                    onValueChange = { viewModel.dateColumn.value = it },
                    label = { Text("Date Column") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateFormat,
                    onValueChange = { viewModel.dateFormat.value = it },
                    label = { Text("Date Format (e.g. yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descriptionColumn,
                    onValueChange = { viewModel.descriptionColumn.value = it },
                    label = { Text("Description Column") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoryColumn,
                    onValueChange = { viewModel.categoryColumn.value = it },
                    label = { Text("Category Column (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useDebitCredit, onCheckedChange = { viewModel.useDebitCredit.value = it })
                    Text("Use separate Debit/Credit columns")
                }

                if (useDebitCredit) {
                    OutlinedTextField(
                        value = debitColumn,
                        onValueChange = { viewModel.debitColumn.value = it },
                        label = { Text("Debit Column") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = creditColumn,
                        onValueChange = { viewModel.creditColumn.value = it },
                        label = { Text("Credit Column") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = amountColumn,
                        onValueChange = { viewModel.amountColumn.value = it },
                        label = { Text("Amount Column") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = invertAmountSigns, onCheckedChange = { viewModel.invertAmountSigns.value = it })
                        Text("Invert signs (Treat positive as expense)")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showSaveProfileDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Profile")
            }
            Button(
                onClick = { viewModel.generatePreview() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Preview")
            }
        }
    }

    if (showSaveProfileDialog) {
        AlertDialog(
            onDismissRequest = { showSaveProfileDialog = false },
            title = { Text("Save Import Profile") },
            text = {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (profileName.isNotBlank()) {
                        viewModel.saveProfile(profileName)
                        showSaveProfileDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveProfileDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PreviewStep(viewModel: CsvImportViewModel) {
    val previewRows by viewModel.previewRows.collectAsStateWithLifecycle()
    val parseErrors by viewModel.parseErrors.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (parseErrors.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${parseErrors.size} Parsing Errors", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    parseErrors.take(3).forEach { err ->
                        Text("Row ${err.rowIndex}: ${err.errorMessage}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    if (parseErrors.size > 3) {
                        Text("...and ${parseErrors.size - 3} more", fontSize = 12.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Preview (${previewRows.size} valid rows)", fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.executeImport() }, enabled = previewRows.any { !it.skip }) {
                Text("Import")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(previewRows) { index, row ->
                val cardColor = if (row.skip) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = !row.skip,
                            onCheckedChange = { viewModel.toggleRowSkip(index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.parsedRow.description, fontWeight = FontWeight.Bold)
                            Text("Amt: ${row.parsedRow.amount} | Type: ${row.parsedRow.type}", fontSize = 12.sp)
                            if (row.isDuplicate) {
                                Text("DUPLICATE (Already in database)", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStep(viewModel: CsvImportViewModel) {
    val summary by viewModel.importSummary.collectAsStateWithLifecycle()
    val lastBatchId by viewModel.lastBatchId.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Import Complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(summary, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(32.dp))

        if (lastBatchId != null) {
            OutlinedButton(
                onClick = { viewModel.undoLastBatch() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Undo Import Batch")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = { viewModel.reset() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import Another File")
        }
    }
}
