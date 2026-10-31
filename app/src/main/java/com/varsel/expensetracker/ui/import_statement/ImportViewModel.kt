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
import com.varsel.expensetracker.domain.engine.RecurringMatcherEngine
import com.varsel.expensetracker.domain.model.TransactionType
import com.varsel.expensetracker.domain.repository.RecurringRepository
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import com.varsel.expensetracker.domain.repository.TransactionRepository
import com.varsel.expensetracker.parser.StatementImportResult
import com.varsel.expensetracker.ui.util.UiErrorEvent
import com.varsel.expensetracker.util.AppError
import com.varsel.expensetracker.util.AppErrorMessageMapper
import com.varsel.expensetracker.util.OcrManager
import com.varsel.expensetracker.util.PdfExtractionResult
import com.varsel.expensetracker.util.PdfTextExtractor
import com.varsel.expensetracker.util.SafeErrorHandler
import com.varsel.expensetracker.util.SafeLog
import com.varsel.expensetracker.util.StatementParserEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
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
        val error: AppError = AppError.Unknown(),
        val message: String = AppErrorMessageMapper.getUserMessage(error)
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

    private val recurringRepository: RecurringRepository,

    private val recurringMatcherEngine: RecurringMatcherEngine,

    @ApplicationContext private val context: Context

) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ImportUiState>(
            ImportUiState.Idle
        )

    val uiState: StateFlow<ImportUiState> =
        _uiState.asStateFlow()

    private val _errorEvents = Channel<UiErrorEvent>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()

    private var pendingStatementResult: StatementImportResult? = null

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
            try {
                statementSnapshotRepository.deleteSnapshot(snapshotId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val appError = SafeErrorHandler.handle("ImportViewModel", e, "Delete Snapshot")
                _errorEvents.send(UiErrorEvent(error = appError))
            }
        }
    }

    fun deleteSnapshotWithTransactions(snapshot: StatementSnapshotEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statementSnapshotRepository.deleteSnapshotWithTransactions(snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val appError = SafeErrorHandler.handle("ImportViewModel", e, "Delete Snapshot with Transactions")
                _errorEvents.send(UiErrorEvent(error = appError))
            }
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
                                error = AppError.PdfExtractionFailed(pdfResult.message),
                                message = pdfResult.message ?: AppErrorMessageMapper.getUserMessage(AppError.PdfExtractionFailed())
                            )
                            return@launch
                        }
                    }
                } else {
                    val textFromImage = ocrManager.extractTextFromImage(context, uri)
                    if (textFromImage.isNullOrBlank()) {
                        _uiState.value = ImportUiState.Error(
                            error = AppError.OcrFailed("No text detected in image"),
                            message = AppErrorMessageMapper.getUserMessage(AppError.OcrFailed())
                        )
                        return@launch
                    }
                    textFromImage
                }

                if (rawText.isBlank()) {
    val isPdf =
        resolvedMimeType == "application/pdf" ||
            uri.toString().endsWith(".pdf", true)

    val error = if (isPdf) {
        AppError.PdfExtractionFailed("Document contains no readable text")
    } else {
        AppError.OcrFailed("No readable text detected")
    }

    _uiState.value =
        ImportUiState.Error(
            error = error,
            message = AppErrorMessageMapper.getUserMessage(error)
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
                            error = AppError.NoTransactionsFound,
                            message = AppErrorMessageMapper.getUserMessage(AppError.NoTransactionsFound)
                        )
                    return@launch
                }

                // Hold the parsed result in memory; statement snapshot will only
                // be committed to the database once the user confirms and saves.
                pendingStatementResult = result

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
                // Recurring & Subscription Auto-Matching
                // --------------------------------------------------

                val activeRecurringItems = try {
    recurringRepository.getActiveRecurringItems().first()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    SafeErrorHandler.handle(
        tag = "ImportViewModel",
        throwable = e,
        operationContext = "Load Recurring Items"
    )
    emptyList()
                }

                // --------------------------------------------------
                // Build preview transactions
                // --------------------------------------------------

                var recurringMatchedCount = 0
                val selectableTransactions =
                    result.transactions.map { transaction ->

                        val isDuplicate =
                            transaction
                                .transactionFingerprint
                                ?.let {
                                    it in existingFingerprints
                                }
                                ?: false

                        val matchResult = if (!isDuplicate && activeRecurringItems.isNotEmpty()) {
                            recurringMatcherEngine.findBestMatch(transaction, activeRecurringItems)
                        } else {
                            null
                        }

                        if (matchResult != null) {
                            recurringMatchedCount++
                        }

                        // Attach the matched recurringItemId to the transaction if matched
                        val enrichedTransaction = if (matchResult != null) {
                            transaction.copy(recurringItemId = matchResult.recurringItem.id)
                        } else {
                            transaction
                        }

                        SelectableTransaction(
                            transaction = enrichedTransaction,
                            selected = !isDuplicate,
                            isDuplicate = isDuplicate,
                            matchedRecurringItem = matchResult
                        )
                    }

                val finalSummary = summary.copy(
                    recurringMatchedCount = recurringMatchedCount
                )

                _uiState.value =
                    ImportUiState.ParsedTransactions(
                        summary =
                            finalSummary,

                        parsedTransactions =
                            selectableTransactions
                    )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val appError = SafeErrorHandler.handle("ImportViewModel", e, "Process Statement File")
                _uiState.value =
                    ImportUiState.Error(
                        error = appError,
                        message = AppErrorMessageMapper.getUserMessage(appError)
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
                            error = AppError.InvalidInput(reason = "Please select at least one transaction to import."),
                            message = "Please select at least one transaction to import."
                        )
                    return@launch
                }

                // 1. Commit the statement snapshot now that user has confirmed
                pendingStatementResult?.let { snapshotResult ->
                    saveStatementSnapshot(snapshotResult)
                }

                // 2. Perform atomic batch insert of selected transactions
                transactionRepository.insertTransactions(
                    selectedTransactions.map { it.transaction }
                )

                // 3. Advance occurrence on matched recurring items so user won't get prompted again for the same cycle
                val matchedItems = selectedTransactions.mapNotNull { sel ->
                    sel.matchedRecurringItem?.let { match ->
                        match.recurringItem to sel.transaction.dateTimestamp
                    }
                }
                for ((item, txDateTimestamp) in matchedItems) {
                    try {
                        val advancedItem = recurringMatcherEngine.advanceOccurrenceAfterMatch(item, txDateTimestamp)
                        recurringRepository.updateRecurringItem(advancedItem)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
    SafeErrorHandler.handle(
        tag = "ImportViewModel",
        throwable = e,
        operationContext = "Advance Recurring Item After Match"
    )
                    }
                }

                // 4. Clear pending result
                pendingStatementResult = null

                // 5. Automatically reconcile and link transfers across accounts
                try {
                    autoTransferReconciliationEngine.reconcileTransfers()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
    SafeErrorHandler.handle(
        tag = "ImportViewModel",
        throwable = e,
        operationContext = "Auto-Reconciliation After Import"
    )
                }

                _uiState.value =
                    ImportUiState.Saved(
                        selectedTransactions.size
                    )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val appError = SafeErrorHandler.handle("ImportViewModel", e, "Save Transactions")
                _uiState.value =
                    ImportUiState.Error(
                        error = appError,
                        message = AppErrorMessageMapper.getUserMessage(appError)
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
        pendingStatementResult = null
        _uiState.value =
            ImportUiState.Idle
    }
}
