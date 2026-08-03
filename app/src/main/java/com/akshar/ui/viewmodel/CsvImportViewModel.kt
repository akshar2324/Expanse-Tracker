package com.akshar.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akshar.ExpenseTrackerApp
import com.akshar.data.model.CsvImportProfile
import com.akshar.data.model.Transaction
import com.akshar.data.repository.FinanceRepository
import com.akshar.utils.CsvParseError
import com.akshar.utils.CsvParseResult
import com.akshar.utils.CsvParser
import com.akshar.utils.ParsedCsvRow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

enum class ImportStep {
    SELECT_FILE,
    CONFIGURE_MAPPING,
    PREVIEW,
    SUMMARY
}

data class ValidationRow(
    val parsedRow: ParsedCsvRow,
    val isDuplicate: Boolean,
    var skip: Boolean = false
)

class CsvImportViewModel(
    application: Application,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val repository: FinanceRepository = (application as ExpenseTrackerApp).repository

    val savedProfiles = repository.allCsvImportProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentStep = MutableStateFlow(ImportStep.SELECT_FILE)
    val currentStep: StateFlow<ImportStep> = _currentStep.asStateFlow()

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()

    private val _fileName = MutableStateFlow<String?>(null)
    val fileName: StateFlow<String?> = _fileName.asStateFlow()

    private val _rawHeaders = MutableStateFlow<List<String>>(emptyList())
    val rawHeaders: StateFlow<List<String>> = _rawHeaders.asStateFlow()

    // Mapping config
    val delimiter = MutableStateFlow(",")
    val hasHeader = MutableStateFlow(true)
    val dateFormat = MutableStateFlow("yyyy-MM-dd")
    val dateColumn = MutableStateFlow("")
    val amountColumn = MutableStateFlow("")
    val debitColumn = MutableStateFlow("")
    val creditColumn = MutableStateFlow("")
    val descriptionColumn = MutableStateFlow("")
    val categoryColumn = MutableStateFlow("")
    val localeStr = MutableStateFlow("en_US")
    val invertAmountSigns = MutableStateFlow(false)
    val useDebitCredit = MutableStateFlow(false)

    // Preview
    private val _previewRows = MutableStateFlow<List<ValidationRow>>(emptyList())
    val previewRows: StateFlow<List<ValidationRow>> = _previewRows.asStateFlow()

    private val _parseErrors = MutableStateFlow<List<CsvParseError>>(emptyList())
    val parseErrors: StateFlow<List<CsvParseError>> = _parseErrors.asStateFlow()

    // Import result
    private val _importSummary = MutableStateFlow("")
    val importSummary: StateFlow<String> = _importSummary.asStateFlow()

    private val _lastBatchId = MutableStateFlow<String?>(null)
    val lastBatchId: StateFlow<String?> = _lastBatchId.asStateFlow()


    fun handleFileSelected(uri: Uri) {
        _selectedUri.value = uri
        _fileName.value = queryName(uri)
        extractHeaders(uri)
        _currentStep.value = ImportStep.CONFIGURE_MAPPING
    }

    private fun queryName(uri: Uri): String {
        val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
        var name = "Unknown File"
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    private fun extractHeaders(uri: Uri) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                inputStream?.use { stream ->
                    val del = delimiter.value
                    val reader = com.github.doyaaaaaken.kotlincsv.dsl.csvReader {
                        this.delimiter = if (del.isNotEmpty()) del[0] else ','
                    }
                    val firstRow = reader.readAll(stream).firstOrNull()
                    if (firstRow != null) {
                        _rawHeaders.value = firstRow
                    }
                }
            } catch (e: Exception) {
                // Ignore, just won't show headers
            }
        }
    }

    fun generatePreview() {
        viewModelScope.launch(ioDispatcher) {
            val uri = _selectedUri.value ?: return@launch
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _parseErrors.value = listOf(CsvParseError(0, emptyList(), "Could not open file."))
                    _previewRows.value = emptyList()
                    _currentStep.value = ImportStep.PREVIEW
                    return@launch
                }
                inputStream.use { stream ->
                    val del = delimiter.value
                    val result = CsvParser.parse(
                        inputStream = stream,
                        delimiter = if (del.isNotEmpty()) del[0] else ',',
                        hasHeader = hasHeader.value,
                        dateColumnName = dateColumn.value,
                        dateFormatString = dateFormat.value,
                        amountColumnName = if (useDebitCredit.value) null else amountColumn.value,
                        debitColumnName = if (useDebitCredit.value) debitColumn.value else null,
                        creditColumnName = if (useDebitCredit.value) creditColumn.value else null,
                        descriptionColumnName = descriptionColumn.value,
                        categoryColumnName = categoryColumn.value.takeIf { it.isNotBlank() },
                        localeString = localeStr.value,
                        invertAmountSigns = invertAmountSigns.value
                    )

                    _parseErrors.value = result.errors

                    val hashes = result.rows.map { it.hash }
                    val existingHashes = repository.getExistingCsvHashes(hashes).toSet()

                    val validationRows = result.rows.map { row ->
                        val isDuplicate = existingHashes.contains(row.hash)
                        ValidationRow(parsedRow = row, isDuplicate = isDuplicate, skip = isDuplicate)
                    }

                    _previewRows.value = validationRows
                    _currentStep.value = ImportStep.PREVIEW
                }
            } catch (e: Exception) {
                _parseErrors.value = listOf(CsvParseError(0, emptyList(), e.message ?: "Unknown preview error"))
                _previewRows.value = emptyList()
                _currentStep.value = ImportStep.PREVIEW
            }
        }
    }

    fun toggleRowSkip(index: Int) {
        val currentList = _previewRows.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = currentList[index].copy(skip = !currentList[index].skip)
            _previewRows.value = currentList
        }
    }

    fun executeImport() {
        viewModelScope.launch(ioDispatcher) {
            val rowsToImport = _previewRows.value.filter { !it.skip }.map { it.parsedRow }
            if (rowsToImport.isEmpty()) {
                _importSummary.value = "No rows imported."
                _currentStep.value = ImportStep.SUMMARY
                return@launch
            }

            val batchId = UUID.randomUUID().toString()

            val transactions = rowsToImport.map {
                Transaction(
                    amount = it.amount,
                    type = it.type,
                    category = it.category,
                    description = it.description,
                    date = it.date,
                    paymentMethod = "Imported",
                    timestamp = System.currentTimeMillis(),
                    importBatchId = batchId,
                    originalCsvRowHash = it.hash
                )
            }

            try {
                repository.insertTransactions(transactions)
                _lastBatchId.value = batchId
                _importSummary.value = "Successfully imported ${transactions.size} records."
                _currentStep.value = ImportStep.SUMMARY
            } catch (e: Exception) {
                _importSummary.value = "Import failed: ${e.message}"
                _currentStep.value = ImportStep.SUMMARY
            }
        }
    }

    fun undoLastBatch() {
        viewModelScope.launch(ioDispatcher) {
            val batchId = _lastBatchId.value
            if (batchId != null) {
                repository.deleteTransactionsByBatchId(batchId)
                _importSummary.value = "Undid import batch $batchId."
                _lastBatchId.value = null
            }
        }
    }

    fun loadProfile(profile: CsvImportProfile) {
        delimiter.value = profile.delimiter
        hasHeader.value = profile.hasHeader
        dateFormat.value = profile.dateFormat
        dateColumn.value = profile.dateColumn
        descriptionColumn.value = profile.descriptionColumn
        categoryColumn.value = profile.categoryColumn ?: ""
        localeStr.value = profile.locale
        invertAmountSigns.value = profile.invertAmountSigns

        if (profile.amountColumn != null) {
            useDebitCredit.value = false
            amountColumn.value = profile.amountColumn
        } else {
            useDebitCredit.value = true
            debitColumn.value = profile.debitColumn ?: ""
            creditColumn.value = profile.creditColumn ?: ""
        }

        // Re-extract headers if file is selected
        _selectedUri.value?.let { extractHeaders(it) }
    }

    fun saveProfile(name: String) {
        viewModelScope.launch(ioDispatcher) {
            val profile = CsvImportProfile(
                name = name,
                delimiter = delimiter.value,
                hasHeader = hasHeader.value,
                dateColumn = dateColumn.value,
                dateFormat = dateFormat.value,
                amountColumn = if (!useDebitCredit.value) amountColumn.value else null,
                debitColumn = if (useDebitCredit.value) debitColumn.value else null,
                creditColumn = if (useDebitCredit.value) creditColumn.value else null,
                descriptionColumn = descriptionColumn.value,
                categoryColumn = categoryColumn.value.takeIf { it.isNotBlank() },
                locale = localeStr.value,
                invertAmountSigns = invertAmountSigns.value
            )
            repository.insertCsvImportProfile(profile)
        }
    }

    fun reset() {
        _currentStep.value = ImportStep.SELECT_FILE
        _selectedUri.value = null
        _fileName.value = null
        _rawHeaders.value = emptyList()
        _previewRows.value = emptyList()
        _parseErrors.value = emptyList()
        _importSummary.value = ""
        _lastBatchId.value = null
    }
}
