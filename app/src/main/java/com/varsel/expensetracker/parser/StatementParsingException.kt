package com.varsel.expensetracker.parser

import com.varsel.expensetracker.util.AppError

/**
 * Domain exception thrown when statement parsing or bank detection fails with a typed [AppError].
 */
open class StatementParsingException(
    val appError: AppError,
    message: String? = null,
    cause: Throwable? = null
) : IllegalArgumentException(message ?: (appError as? AppError.UnsupportedBank)?.detectedBankName, cause)

class UnsupportedBankException(val bankName: String? = null) :
    StatementParsingException(
        appError = AppError.UnsupportedBank(bankName),
        message = if (!bankName.isNullOrBlank()) "Unsupported bank: $bankName" else "Bank not supported"
    )

class UnsupportedStatementFormatException(val bankName: String? = null, val reason: String? = null) :
    StatementParsingException(
        appError = AppError.UnsupportedStatementFormat(bankName, reason),
        message = reason ?: "Unsupported statement format for ${bankName ?: "bank"}"
    )

class AmbiguousBankException(val matchedBanks: List<String> = emptyList()) :
    StatementParsingException(
        appError = AppError.AmbiguousBankStatement(matchedBanks),
        message = "Ambiguous bank match: ${matchedBanks.joinToString(", ")}"
    )
