package com.varsel.expensetracker.ui.settings.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.export.BackupInspectionResult
import com.varsel.expensetracker.data.export.BackupRestoreEngine
import com.varsel.expensetracker.data.export.CsvExportEngine
import com.varsel.expensetracker.data.export.PdfReportGenerator
import com.varsel.expensetracker.data.local.entity.TransactionEntity
import com.varsel.expensetracker.data.repository.toEntity
import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class ExportPeriod(val label: String) {
    CURRENT_MONTH("Current Month"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    ALL_TIME("All Time")
}

data class DataManagementUiState(
    val selectedPeriod: ExportPeriod = ExportPeriod.CURRENT_MONTH,
    val totalTransactionsCount: Int = 0,
    val filteredTransactionsCount: Int = 0,
    val isGenerating: Boolean = false,
    val isRestoring: Boolean = false,
    val pendingShareIntent: Intent? = null,
    val inspectedBackup: BackupInspectionResult.Valid? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val csvExportEngine: CsvExportEngine,
    private val pdfReportGenerator: PdfReportGenerator,
    private val backupRestoreEngine: BackupRestoreEngine,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataManagementUiState())
    val uiState: StateFlow<DataManagementUiState> = _uiState.asStateFlow()

    private var allTransactions: List<TransactionEntity> = emptyList()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { list ->
                val entities = list.map { it.toEntity() }
                allTransactions = entities
                updateCounts()
            }
        }
    }

    fun onSelectPeriod(period: ExportPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        updateCounts()
    }

    private fun updateCounts() {
        val filtered = filterTransactionsByPeriod(allTransactions, _uiState.value.selectedPeriod)
        _uiState.update {
            it.copy(
                totalTransactionsCount = allTransactions.size,
                filteredTransactionsCount = filtered.size
            )
        }
    }

    private fun filterTransactionsByPeriod(
        list: List<TransactionEntity>,
        period: ExportPeriod
    ): List<TransactionEntity> {
        if (period == ExportPeriod.ALL_TIME) return list

        val now = LocalDate.now()
        val zone = ZoneId.systemDefault()

        val startInclusive = when (period) {
            ExportPeriod.CURRENT_MONTH -> now.withDayOfMonth(1)
            ExportPeriod.LAST_3_MONTHS -> now.minusMonths(2).withDayOfMonth(1)
            ExportPeriod.LAST_6_MONTHS -> now.minusMonths(5).withDayOfMonth(1)
            ExportPeriod.ALL_TIME -> LocalDate.MIN
        }

        val startTimestamp = startInclusive.atStartOfDay(zone).toInstant().toEpochMilli()

        return list.filter { it.dateTimestamp >= startTimestamp }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                val targets = filterTransactionsByPeriod(allTransactions, _uiState.value.selectedPeriod)
                if (targets.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = "No transactions found for the selected period."
                        )
                    }
                    return@launch
                }

                val exportResult = csvExportEngine.exportToCacheAndGetUri(context, targets)
                val shareIntent = csvExportEngine.createShareIntent(context, exportResult)

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        pendingShareIntent = shareIntent,
                        successMessage = "CSV generated: ${exportResult.fileName} (${exportResult.recordCount} rows)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Failed to export CSV: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun exportPdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                val period = _uiState.value.selectedPeriod
                val targets = filterTransactionsByPeriod(allTransactions, period)
                if (targets.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = "No transactions found for the selected period."
                        )
                    }
                    return@launch
                }

                val exportResult = pdfReportGenerator.generateReportToCacheAndGetUri(context, targets, period.label)
                val shareIntent = pdfReportGenerator.createShareIntent(context, exportResult)

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        pendingShareIntent = shareIntent,
                        successMessage = "PDF statement generated: ${exportResult.fileName}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Failed to generate PDF: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                val exportResult = backupRestoreEngine.exportBackupToCacheAndGetUri(context)
                val shareIntent = backupRestoreEngine.createShareIntent(context, exportResult)

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        pendingShareIntent = shareIntent,
                        successMessage = "Backup generated: ${exportResult.fileName}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Failed to create backup: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onBackupFileSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = "Unable to open selected file."
                        )
                    }
                    return@launch
                }

                when (val result = backupRestoreEngine.inspectBackupStream(inputStream)) {
                    is BackupInspectionResult.Valid -> {
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                inspectedBackup = result
                            )
                        }
                    }
                    is BackupInspectionResult.Invalid -> {
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                errorMessage = "Invalid backup: ${result.reason}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = "Error inspecting backup: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun confirmRestore() {
        val inspection = _uiState.value.inspectedBackup ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, errorMessage = null) }
            try {
                val success = backupRestoreEngine.restoreDatabase(inspection.rawJson)
                if (success) {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            inspectedBackup = null,
                            successMessage = "Database restored successfully! (${inspection.transactionCount} transactions)"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isRestoring = false,
                            errorMessage = "Restore failed. Existing database was left unchanged."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        errorMessage = "Restore error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun dismissRestoreDialog() {
        _uiState.update { it.copy(inspectedBackup = null) }
    }

    fun onShareIntentHandled() {
        _uiState.update { it.copy(pendingShareIntent = null) }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
