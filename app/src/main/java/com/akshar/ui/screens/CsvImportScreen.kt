package com.akshar.ui.screens

import com.akshar.utils.launchFilePicker
import com.akshar.ui.theme.*
import com.akshar.ui.components.*
import android.net.Uri
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
import com.akshar.ui.components.AlertDialog
import com.akshar.ui.components.Card
import com.akshar.ui.components.OutlinedTextField
import com.akshar.ui.components.Checkbox
import com.akshar.ui.components.TextButton
import com.akshar.ui.components.OutlinedButton
import com.akshar.ui.components.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ScreenHeader(
            title = "Import CSV",
            subtitle = when (currentStep) {
                ImportStep.SELECT_FILE -> "Step 1 of 3 · Choose a file"
                ImportStep.CONFIGURE_MAPPING -> "Step 2 of 3 · Match the columns"
                ImportStep.PREVIEW -> "Step 3 of 3 · Review and import"
                ImportStep.SUMMARY -> "All done"
            },
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
    val feedback = LocalAppFeedback.current
    val profiles by viewModel.savedProfiles.collectAsStateWithLifecycle()
    val pickFileError: (String) -> Unit = { feedback?.show(it, SnackbarDuration.Long) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handleFileSelected(uri)
        } else {
            feedback?.show("No file selected")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = Spacing.xs,
            bottom = Spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    IconBadge(icon = Icons.Default.FileUpload, tint = MaterialTheme.colorScheme.primary, size = 64.dp)
                    Text(
                        "Bring in a bank statement",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text(
                        "Choose a CSV export from your bank. It's read on this device and never uploaded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Button(
                        onClick = { launcher.launchFilePicker("text/*", pickFileError) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget),
                    ) {
                        Text("Choose CSV file")
                    }
                }
            }
        }

        if (profiles.isNotEmpty()) {
            item { SectionHeader("Or reuse a saved column setup") }
            items(profiles) { profile ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.loadProfile(profile)
                        launcher.launchFilePicker("text/*", pickFileError)
                    },
                    onClickLabel = "Use ${profile.name} and choose a file",
                    contentPadding = PaddingValues(Spacing.lg)
                ) {
                    Text(profile.name, style = MaterialTheme.typography.titleSmall)
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
            .verticalScroll(scrollState)
            .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xs, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("File: $fileName", fontWeight = AkSpendTheme.emphasis, color = MaterialTheme.colorScheme.primary)

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("File Format", fontWeight = AkSpendTheme.emphasis)

                OutlinedTextField(
                    colors = glassTextFieldColors(),
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
                    colors = glassTextFieldColors(),
                    value = localeStr,
                    onValueChange = { viewModel.localeStr.value = it },
                    label = { Text("Number Locale (e.g. en_US, de_DE)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Column Mapping", fontWeight = AkSpendTheme.emphasis)
                Text("Use the exact column name (if the file has a header row) or a 0-based column number.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = dateColumn,
                    onValueChange = { viewModel.dateColumn.value = it },
                    label = { Text("Date Column") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = dateFormat,
                    onValueChange = { viewModel.dateFormat.value = it },
                    label = { Text("Date Format (e.g. yyyy-MM-dd)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    colors = glassTextFieldColors(),
                    value = descriptionColumn,
                    onValueChange = { viewModel.descriptionColumn.value = it },
                    label = { Text("Description Column") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    colors = glassTextFieldColors(),
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
                        colors = glassTextFieldColors(),
                        value = debitColumn,
                        onValueChange = { viewModel.debitColumn.value = it },
                        label = { Text("Debit Column") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        colors = glassTextFieldColors(),
                        value = creditColumn,
                        onValueChange = { viewModel.creditColumn.value = it },
                        label = { Text("Credit Column") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        colors = glassTextFieldColors(),
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
                    colors = glassTextFieldColors(),
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

    Column(Modifier.fillMaxSize()) {
        LabelValueRow(
            modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.xs),
            label = {
                Text("${previewRows.size} rows ready", style = MaterialTheme.typography.titleSmall)
            },
            value = {
                Button(
                    onClick = { viewModel.executeImport() },
                    enabled = previewRows.any { !it.skip },
                    modifier = Modifier.heightIn(min = Spacing.touchTarget)
                ) {
                    Text("Import selected", style = MaterialTheme.typography.labelLarge)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(
                start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xs, bottom = Spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (parseErrors.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${parseErrors.size} rows couldn't be read", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            parseErrors.take(3).forEach { err ->
                                Text("Row ${err.rowIndex}: ${err.errorMessage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            if (parseErrors.size > 3) {
                                Text("…and ${parseErrors.size - 3} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            itemsIndexed(previewRows) { index, row ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    fill = if (row.skip) AkSpendTheme.glass.fill.copy(alpha = AkSpendTheme.glass.fill.alpha * 0.5f) else AkSpendTheme.glass.fill,
                    contentPadding = PaddingValues(0.dp)
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
                            Text(row.parsedRow.description, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${row.parsedRow.type.lowercase().replaceFirstChar { it.uppercase() }} · ${String.format("%,.2f", row.parsedRow.amount)} · ${row.parsedRow.category}" +
                                    if (row.learnedCategory) " (learned)" else "",
                                style = MaterialTheme.typography.bodySmall.money(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (row.isDuplicate) {
                                Text(
                                    (row.duplicateNote ?: "Possible duplicate: already saved") + ". Tick to import anyway.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
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
            .verticalScroll(rememberScrollState())
            .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xs, bottom = Spacing.xxl),
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AkSpendTheme.finance.income
                )
                Text("Import complete", style = MaterialTheme.typography.titleLarge)
                Text(summary, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

                if (lastBatchId != null) {
                    OutlinedButton(
                        onClick = { viewModel.undoLastBatch() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)
                    ) {
                        Text("Undo import batch")
                    }
                }

                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)
                ) {
                    Text("Import another file")
                }
            }
        }
    }
}
