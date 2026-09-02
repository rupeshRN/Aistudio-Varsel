package com.varsel.expensetracker.ui.import_statement

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.developer.DeveloperRepository
import com.varsel.expensetracker.developer.ParserDiagnostics
import com.varsel.expensetracker.developer.ParserDiagnosticsManager
import com.varsel.expensetracker.domain.engine.AutoTransferReconciliationEngine
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.parser.StatementImportResult
import com.varsel.expensetracker.util.OcrManager
import com.varsel.expensetracker.util.PdfExtractionResult
import com.varsel.expensetracker.util.PdfTextExtractor
import com.varsel.expensetracker.util.StatementParserEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ImportUiState {

    object Idle : ImportUiState

    object Loading : ImportUiState

    object Processing : ImportUiState

    data class ParsedTransactions(
        val summary: ImportSummary,
        val parsedTransactions: List<SelectableTransaction>
    ) : ImportUiState

    data class PasswordRequired(
        val isInvalidPasswordError: Boolean = false,
        val pendingUri: Uri? = null,
        val pendingMimeType: String? = null
    ) : ImportUiState

    data class Saved(
        val count: Int
    ) : ImportUiState

    data class Error(
        val message: String
    ) : ImportUiState
}

@HiltViewModel
class ImportViewModel @Inject constructor(

    private val transactionRepository: TransactionRepository,

    private val statementSnapshotRepository: StatementSnapshotRepository,

    private val statementParserEngine: StatementParserEngine,

    private val pdfTextExtractor: PdfTextExtractor,

    private val ocrManager: OcrManager,

    private val developerRepository: DeveloperRepository,

    private val autoTransferReconciliationEngine: AutoTransferReconciliationEngine,

    @ApplicationContext private val context: Context

) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ImportUiState>(
            ImportUiState.Idle
        )

    val uiState: StateFlow<ImportUiState> =
        _uiState.asStateFlow()

    // --------------------------------------------------
    // Developer diagnostics
    // --------------------------------------------------

    private val _diagnostics =
        MutableStateFlow(
            ParserDiagnostics()
        )

    val diagnostics: StateFlow<ParserDiagnostics> =
        _diagnostics.asStateFlow()

    val parserDiagnosticsEnabled =
        developerRepository
            .parserDiagnosticsEnabled
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    // --------------------------------------------------
    // Import history
    // --------------------------------------------------

    val importHistory: StateFlow<List<StatementSnapshotEntity>> =
        statementSnapshotRepository
            .observeAllSnapshots()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun deleteSnapshot(snapshotId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            statementSnapshotRepository.deleteSnapshot(snapshotId)
        }
    }

    fun deleteSnapshotWithTransactions(snapshot: StatementSnapshotEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            statementSnapshotRepository.deleteSnapshotWithTransactions(snapshot)
        }
    }

    // --------------------------------------------------
    // Process selected statement
    // --------------------------------------------------

    fun processSelectedFile(
        uri: Uri,
        mimeType: String? = null,
        password: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            _uiState.value =
                ImportUiState.Loading

            try {

                val resolvedMimeType =
                    mimeType
                        ?: context.contentResolver.getType(uri)

                val rawText: String = if (
                    resolvedMimeType == "application/pdf" ||
                    uri.toString().endsWith(
                        ".pdf",
                        true
                    )
                ) {
                    when (val pdfResult = pdfTextExtractor.extractTextFromPdf(context, uri, password)) {
                        is PdfExtractionResult.Success -> pdfResult.text
                        is PdfExtractionResult.PasswordRequired -> {
                            _uiState.value = ImportUiState.PasswordRequired(
                                isInvalidPasswordError = false,
                                pendingUri = uri,
                                pendingMimeType = mimeType
                            )
                            return@launch
                        }
                        is PdfExtractionResult.InvalidPassword -> {
                            _uiState.value = ImportUiState.PasswordRequired(
                                isInvalidPasswordError = true,
                                pendingUri = uri,
                                pendingMimeType = mimeType
                            )
                            return@launch
                        }
                        is PdfExtractionResult.Error -> {
                            _uiState.value = ImportUiState.Error(
                                pdfResult.message ?: "Could not extract text from document."
                            )
                            return@launch
                        }
                    }
                } else {
                    val textFromImage = ocrManager.extractTextFromImage(context, uri)
                    if (textFromImage.isNullOrBlank()) {
                        _uiState.value = ImportUiState.Error("Could not extract any text from the selected document.")
                        return@launch
                    }
                    textFromImage
                }

                if (rawText.isBlank()) {

                    _uiState.value =
                        ImportUiState.Error(
                            "Could not extract any text from the selected document."
                        )

                    return@launch
                }

                // --------------------------------------------------
                // Parse statement
                // --------------------------------------------------

                val result =
                    statementParserEngine
                        .parseStatement(rawText)

                // --------------------------------------------------
                // Update developer diagnostics
                // --------------------------------------------------

                _diagnostics.value =
                    ParserDiagnosticsManager.latest

                if (result.transactions.isEmpty()) {

                    _uiState.value =
                        ImportUiState.Error(
                            "No transactions found."
                        )

                    return@launch
                }

                // --------------------------------------------------
                // Save statement snapshot
                //
                // This represents the bank statement itself,
                // independent of which transactions the user
                // eventually selects for saving.
                // --------------------------------------------------

                saveStatementSnapshot(result)

                // --------------------------------------------------
                // Build UI summary
                // --------------------------------------------------

                val credits =
                    result.transactions.count {

                        it.type ==
                            TransactionType.INCOME
                    }

                val debits =
                    result.transactions.count {

                        it.type ==
                            TransactionType.EXPENSE
                    }

                // --------------------------------------------------
                // Existing duplicate detection
                // --------------------------------------------------

                val fingerprints =
                    result.transactions
                        .mapNotNull {
                            it.transactionFingerprint
                        }
                        .distinct()

                val existingFingerprints =
                    transactionRepository
                        .findExistingFingerprints(
                            fingerprints
                        )

                val duplicateCount =
                    result.transactions.count {

                        val fingerprint =
                            it.transactionFingerprint

                        fingerprint != null &&
                            fingerprint in existingFingerprints
                    }

                // --------------------------------------------------
                // Import summary
                // --------------------------------------------------

                val reconStatus = if (!result.reconciliation.hasSummaryTotals) {
                    "Transactions verified from ${result.bankName}"
                } else if (result.reconciliation.isBalanced) {
                    "Opening + Credits − Debits = Closing"
                } else {
                    "Statement totals do not reconcile."
                }

                val summary =
                    ImportSummary(

                        bankName =
                            result.bankName,

                        statementPeriod =
                            formatStatementPeriod(
                                result.summary.statementStartDate,
                                result.summary.statementEndDate
                            ),

                        transactionsDetected =
                            if (result.bankName.contains("ICICI", ignoreCase = true)) result.transactions.size else ParserDiagnosticsManager.latest.blocksBuilt,

                        transactionsParsed =
                            result.transactions.size,

                        credits =
                            credits,

                        debits =
                            debits,

                        duplicates =
                            duplicateCount,

                        learnedMatches =
                            0,

                        needsReview =
                            0,

                        reconciliationPassed =
                            result.reconciliation.isBalanced,

                        reconciliationStatusText =
                            reconStatus
                    )

                // --------------------------------------------------
                // Build preview transactions
                // --------------------------------------------------

                val selectableTransactions =
                    result.transactions.map { transaction ->

                        val isDuplicate =
                            transaction
                                .transactionFingerprint
                                ?.let {
                                    it in existingFingerprints
                                }
                                ?: false

                        SelectableTransaction(
                            transaction = transaction,
                            selected = !isDuplicate,
                            isDuplicate = isDuplicate
                        )
                    }

                _uiState.value =
                    ImportUiState.ParsedTransactions(

                        summary =
                            summary,

                        parsedTransactions =
                            selectableTransactions
                    )

            } catch (e: Exception) {

                _uiState.value =
                    ImportUiState.Error(
                        e.message
                            ?: e.stackTraceToString()
                    )
            }
        }
    }

    // --------------------------------------------------
    // Save statement snapshot
    // --------------------------------------------------

    private suspend fun saveStatementSnapshot(
        result: StatementImportResult
    ) {

        val summary =
            result.summary

        val snapshot =
            StatementSnapshotEntity(
                accountId = result.accountId,
                accountLast4 = result.accountLast4,
                bankName = result.bankName,
                ifscCode = result.ifscCode,

                statementStartDate =
                    summary.statementStartDate,

                statementEndDate =
                    summary.statementEndDate,

                openingBalance =
                    summary.openingBalance,

                totalCredits =
                    summary.totalCredits,

                totalDebits =
                    summary.totalDebits,

                endingBalance =
                    summary.endingBalance,

                importedAt =
                    System.currentTimeMillis()
            )

        statementSnapshotRepository
            .saveSnapshot(snapshot)
    }

    // --------------------------------------------------
    // Save selected transactions
    // --------------------------------------------------

    fun confirmAndSaveTransactions(
        transactions: List<SelectableTransaction>
    ) {
        viewModelScope.launch(Dispatchers.IO) {

            try {

                val selectedTransactions =
                    transactions.filter {
                        it.selected
                    }

                if (selectedTransactions.isEmpty()) {

                    _uiState.value =
                        ImportUiState.Error(
                            "Please select at least one transaction."
                        )

                    return@launch
                }

                selectedTransactions.forEach {

                    transactionRepository
                        .insertTransaction(
                            it.transaction
                        )
                }

                // Automatically reconcile and link transfers across accounts
                autoTransferReconciliationEngine.reconcileTransfers()

                _uiState.value =
                    ImportUiState.Saved(
                        selectedTransactions.size
                    )

            } catch (e: Exception) {

                _uiState.value =
                    ImportUiState.Error(
                        e.localizedMessage
                            ?: "Failed to save transactions."
                    )
            }
        }
    }

    // --------------------------------------------------
    // Format statement period
    // --------------------------------------------------

    private fun formatStatementPeriod(
        startDate: Long?,
        endDate: Long?
    ): String {

        if (
            startDate == null ||
            endDate == null
        ) {
            return "Statement Period Unavailable"
        }

        val formatter =
            java.text.SimpleDateFormat(
                "dd MMM ''yy",
                java.util.Locale.ENGLISH
            )

        return "${formatter.format(startDate)} – ${
            formatter.format(endDate)
        }"
    }

    // --------------------------------------------------
    // Submit PDF Password
    // --------------------------------------------------

    fun submitPassword(password: String) {
        val currentState = _uiState.value as? ImportUiState.PasswordRequired ?: return
        val uri = currentState.pendingUri ?: return
        processSelectedFile(
            uri = uri,
            mimeType = currentState.pendingMimeType,
            password = password
        )
    }

    // --------------------------------------------------
    // Reset import state
    // --------------------------------------------------

    fun resetState() {

        _uiState.value =
            ImportUiState.Idle
    }
}
