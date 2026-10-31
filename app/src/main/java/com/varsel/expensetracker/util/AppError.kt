package com.varsel.expensetracker.util

/**
 * Unified application-wide error model representing domain, data, file I/O,
 * and processing failure categories.
 */
sealed interface AppError {

    /** Generic or unclassified runtime failure */
    data class Unknown(val technicalMessage: String? = null) : AppError

    /** Network connectivity or timeout failure */
    data object Network : AppError

    /** Local database (Room/SQLite) read or write error */
    data class Database(
        val operation: String? = null,
        val isConstraintViolation: Boolean = false
    ) : AppError

    /** Requested file or URI was not found */
    data object FileNotFound : AppError

    /** Missing file or system permission (e.g., storage access) */
    data object PermissionDenied : AppError

    /** File is corrupted, 0 bytes, or unreadable */
    data class InvalidFile(val reason: String? = null) : AppError

    /** File format is not supported (e.g. non-PDF/CSV) */
    data object UnsupportedFile : AppError

    /** Failed to extract text or render pages from PDF document */
    data class PdfExtractionFailed(val reason: String? = null) : AppError

    /** PDF requires a password to open */
    data object PasswordRequired : AppError

    /** Supplied PDF password was incorrect */
    data object InvalidPassword : AppError

    /** On-device OCR processing failed or returned empty text */
    data class OcrFailed(val reason: String? = null) : AppError

    /** Statement parsing could not identify transaction table or bank structure */
    data class ParsingFailed(val reason: String? = null) : AppError

    /** The bank identified in the statement is not currently supported */
    data class UnsupportedBank(val detectedBankName: String? = null) : AppError

    /** The bank is supported, but this specific statement format or layout is not yet supported */
    data class UnsupportedStatementFormat(val bankName: String? = null, val reason: String? = null) : AppError

    /** The statement matches signatures for multiple banks ambiguously */
    data class AmbiguousBankStatement(val matchedBanks: List<String> = emptyList()) : AppError

    /** Statement was parsed but contains 0 valid transactions */
    data object NoTransactionsFound : AppError

    /** Statement was parsed with invalid dates, totals, or account structure */
    data object InvalidStatement : AppError

    /** User input validation failure (e.g. invalid amount, empty title, invalid date) */
    data class InvalidInput(val field: String? = null, val reason: String? = null) : AppError

    /** Duplicate record detected (e.g. imported statement snapshot already exists) */
    data class DuplicateData(val itemType: String? = null) : AppError

    /** User or system deliberately cancelled the in-flight operation */
    data object OperationCancelled : AppError

    /** Insufficient storage on device for cache or exports */
    data object StorageFull : AppError

    /** Android KeyStore hardware or biometric encryption failure */
    data class KeyStoreError(val reason: String? = null) : AppError
}

/**
 * One-off UI event representing an error that should be shown via Snackbar/Toast.
 */
data class UiErrorEvent(
    val error: AppError,
    val retryAction: (() -> Unit)? = null
)
