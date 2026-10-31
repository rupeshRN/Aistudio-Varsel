package com.varsel.expensetracker.util

/**
 * Maps [AppError] variants to clean, empathetic, action-oriented messages suitable for
 * Compose UI (snackbars, banners, dialogs). Never leaks internal traces, SQL syntax, or file paths.
 */
object AppErrorMessageMapper {

    /**
     * Returns the primary user-facing error message for an [AppError].
     */
    fun getUserMessage(error: AppError): String {
        return when (error) {
            is AppError.Database -> {
                if (error.isConstraintViolation) {
                    "A duplicate entry or dependency conflict occurred. Please check your data."
                } else {
                    "Unable to save changes right now. Please try again."
                }
            }
            is AppError.PermissionDenied -> {
                "Storage permission is required to access this file."
            }
            is AppError.FileNotFound -> {
                "The selected file was not found. Please choose the file again."
            }
            is AppError.InvalidFile -> {
                "This file is unreadable or corrupted. Please try a different copy."
            }
            is AppError.UnsupportedFile -> {
                "Unsupported file format. Please upload a valid PDF or CSV statement."
            }
            is AppError.PasswordRequired -> {
                "This PDF is password-protected. Please enter the password to open it."
            }
            is AppError.InvalidPassword -> {
                "Incorrect password. Please verify the document password and try again."
            }
            is AppError.PdfExtractionFailed -> {
                "Unable to read text from this PDF. Try using OCR mode or another statement copy."
            }
            is AppError.OcrFailed -> {
                "OCR could not recognize text on this document. Please ensure the scan is clear."
            }
            is AppError.ParsingFailed -> {
                "Could not recognize transactions in this bank statement format."
            }
            is AppError.UnsupportedBank -> {
                if (!error.detectedBankName.isNullOrBlank()) {
                    "Statements from ${error.detectedBankName} are not currently supported. Supported banks are Indian Bank, ICICI Bank, and HDFC Bank."
                } else {
                    "This bank is not currently supported. Supported banks are Indian Bank, ICICI Bank, and HDFC Bank."
                }
            }
            is AppError.UnsupportedStatementFormat -> {
                if (!error.bankName.isNullOrBlank()) {
                    "This statement format for ${error.bankName} is not supported yet."
                } else {
                    "This statement format is not supported yet."
                }
            }
            is AppError.AmbiguousBankStatement -> {
                if (error.matchedBanks.isNotEmpty()) {
                    "The statement matches multiple bank formats (${error.matchedBanks.joinToString(", ")}). Please provide a clearer statement PDF."
                } else {
                    "The statement matches multiple bank formats. Please provide a clearer statement PDF."
                }
            }
            is AppError.NoTransactionsFound -> {
                "No transactions were detected in the selected statement."
            }
            is AppError.InvalidStatement -> {
                "Statement details could not be validated. Please check the date range and format."
            }
            is AppError.InvalidInput -> {
                if (!error.field.isNullOrBlank()) {
                    "Please check the ${error.field} field and enter a valid value."
                } else {
                    error.reason ?: "Please check the entered values and try again."
                }
            }
            is AppError.DuplicateData -> {
                val itemType = error.itemType ?: "item"
                "This $itemType has already been imported or saved."
            }
            is AppError.OperationCancelled -> {
                "Operation was cancelled."
            }
            is AppError.StorageFull -> {
                "Device storage is full. Please free up some space and try again."
            }
            is AppError.KeyStoreError -> {
                "Security hardware error. Please try restarting the application."
            }
            is AppError.Network -> {
                "Network connection issue. Please check your internet connection."
            }
            is AppError.Unknown -> {
                "Something went wrong. Please try again."
            }
        }
    }

    /**
     * Returns an optional actionable suggestion for the user (e.g. for banners or dialog buttons).
     */
    fun getActionSuggestion(error: AppError): String? {
        return when (error) {
            is AppError.Database -> "Retry saving"
            is AppError.PermissionDenied -> "Grant permission in Settings"
            is AppError.FileNotFound, is AppError.InvalidFile -> "Select another file"
            is AppError.UnsupportedFile -> "Use PDF format"
            is AppError.PasswordRequired, is AppError.InvalidPassword -> "Enter password"
            is AppError.PdfExtractionFailed -> "Enable OCR mode"
            is AppError.UnsupportedBank -> "Use supported bank"
            is AppError.UnsupportedStatementFormat -> "Use standard statement layout"
            is AppError.AmbiguousBankStatement -> "Select bank or upload clearer PDF"
            is AppError.ParsingFailed -> "Use developer parser tool or manual entry"
            is AppError.NoTransactionsFound -> "Check statement dates"
            is AppError.DuplicateData -> "View existing items"
            is AppError.StorageFull -> "Free up storage"
            else -> "Try again"
        }
    }
}
