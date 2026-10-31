package com.varsel.expensetracker.util

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import com.varsel.expensetracker.parser.StatementParsingException
import kotlinx.coroutines.CancellationException
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Centralized exception handler that maps raw Android/JVM exceptions
 * to typed AppErrors while preserving useful diagnostic reasons.
 */
object SafeErrorHandler {


    /**
     * Inspects a throwable, logs the sanitized failure, and maps it to a safe [AppError].
     *
     * @param tag Logging tag (e.g. ViewModel name).
     * @param throwable The thrown exception.
     * @param operationContext Optional operation name (e.g. "Save Transaction", "Import Statement").
     * @return Safe [AppError] category.
     * @throws CancellationException Always re-thrown to avoid breaking coroutine lifecycles.
     */

    fun handle(
        tag: String,
        throwable: Throwable,
        operationContext: String? = null
    ): AppError {

        if (throwable is CancellationException) {
            SafeLog.d(
                tag,
                "Operation cancelled: ${operationContext ?: "job"}"
            )
            throw throwable
        }

        val contextPrefix =
            if (operationContext != null) {
                "[$operationContext] "
            } else {
                ""
            }

        SafeLog.e(
            tag = tag,
            message =
                "${contextPrefix}Failure occurred: " +
                        throwable.javaClass.simpleName,
            throwable = throwable
        )

        val exceptionName =
            throwable.javaClass.name.lowercase()

        val message =
            throwable.message?.lowercase() ?: ""

        return when {

            // Password-protected PDF / invalid PDF password
            exceptionName.contains("invalidpassword") ||
                    message.contains("password") -> {
                AppError.InvalidPassword
            }

            // Room / SQLite constraint errors
            throwable is SQLiteConstraintException -> {
                AppError.Database(
                    operation = operationContext,
                    isConstraintViolation = true
                )
            }

            // Generic SQLite errors
            throwable is SQLiteException -> {
                AppError.Database(
                    operation = operationContext,
                    isConstraintViolation = false
                )
            }

            // Security / permission errors
            throwable is SecurityException -> {
                AppError.PermissionDenied
            }

            // File not found
            throwable is FileNotFoundException -> {
                AppError.FileNotFound
            }

    //OCR Extractor exception       
throwable is OcrExtractionException -> {
    AppError.OcrFailed(
        reason = throwable.message
            ?.takeIf { it.isNotBlank() }
            ?: "OCR processing failed"
    )
}


            // File I/O errors
            throwable is IOException -> {

                if (
                    message.contains("enospc") ||
                    message.contains("no space left")
                ) {
                    AppError.StorageFull
                } else {
                    AppError.InvalidFile(
                        reason = "File unreadable or damaged"
                    )
                }
            }

            // KeyStore / cryptographic errors
            exceptionName.contains("keystore") ||
                    exceptionName.contains("crypto") -> {
                AppError.KeyStoreError(
                    reason = "Security provider failure"
                )
            }

            // Domain-specific statement parsing or bank detection error
            throwable is StatementParsingException -> {
                throwable.appError
            }

            // Preserve the actual validation or parser reason
            throwable is IllegalArgumentException -> {
                val rawMsg = throwable.message.orEmpty()
                when {
                    rawMsg.contains("matches multiple bank formats", ignoreCase = true) -> {
                        AppError.AmbiguousBankStatement()
                    }
                    rawMsg.contains("No supported bank", ignoreCase = true) -> {
                        AppError.UnsupportedStatementFormat(reason = rawMsg)
                    }
                    else -> {
                        val reason =
                            throwable.message
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?: "Invalid input values provided"

                        AppError.InvalidInput(
                            reason = reason
                        )
                    }
                }
            }

            // Default unclassified error
            else -> {
                AppError.Unknown(
                    technicalMessage =
                        throwable.javaClass.simpleName
                )
            }
        }
    }
}
