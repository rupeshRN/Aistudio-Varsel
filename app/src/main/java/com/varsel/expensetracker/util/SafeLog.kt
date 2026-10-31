package com.varsel.expensetracker.util

import android.util.Log
import com.varsel.expensetracker.BuildConfig

/**
 * PII-safe logging utility.
 *
 * Logging must never interrupt the application operation being performed.
 * This is especially important when logging an exception from an error handler.
 */
object SafeLog {

    private const val MAX_LOG_LENGTH = 3000

    private val ACCOUNT_NUMBER_REGEX =
        Regex("(?<!\\d)\\d{9,18}(?!\\d)")

    private val CARD_NUMBER_REGEX =
        Regex(
            "(?<!\\d)(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})(?!\\d)"
        )

    private val EMAIL_REGEX =
        Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")

    private val PASSWORD_FIELD_REGEX =
        Regex(
            "(?i)(password|passphrase|pin|cvv|secret)\\s*[:=]\\s*[^\\s,;]+",
            RegexOption.IGNORE_CASE
        )

    fun d(
        tag: String,
        message: String
    ) {
        if (!BuildConfig.DEBUG) return

        val sanitizedMessage = sanitize(message)

        safeAndroidLog {
            Log.d(
                sanitizeTag(tag),
                sanitizedMessage
            )
        }
    }

    fun i(
        tag: String,
        message: String
    ) {
        val sanitizedMessage = sanitize(message)

        safeAndroidLog {
            Log.i(
                sanitizeTag(tag),
                sanitizedMessage
            )
        }
    }

    fun w(
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        val sanitizedMessage = sanitize(message)
        val sanitizedThrowable = throwable?.let { filterStackTrace(it) }

        safeAndroidLog {
            if (sanitizedThrowable != null) {
                Log.w(
                    sanitizeTag(tag),
                    sanitizedMessage,
                    sanitizedThrowable
                )
            } else {
                Log.w(
                    sanitizeTag(tag),
                    sanitizedMessage
                )
            }
        }
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        val sanitizedMessage = sanitize(message)
        val sanitizedThrowable = throwable?.let { filterStackTrace(it) }

        safeAndroidLog {
            if (sanitizedThrowable != null) {
                Log.e(
                    sanitizeTag(tag),
                    sanitizedMessage,
                    sanitizedThrowable
                )
            } else {
                Log.e(
                    sanitizeTag(tag),
                    sanitizedMessage
                )
            }
        }
    }

    /**
     * Sanitizes sensitive personal and financial information.
     */
    fun sanitize(input: String?): String {
        if (input.isNullOrBlank()) return ""

        var sanitized = input
            .replace(
                PASSWORD_FIELD_REGEX,
                "$1=***REDACTED***"
            )
            .replace(CARD_NUMBER_REGEX) { matchResult ->
                val number = matchResult.value
                "****-****-****-${number.takeLast(4)}"
            }
            .replace(ACCOUNT_NUMBER_REGEX) { matchResult ->
                val number = matchResult.value
                "XX${number.takeLast(4)}"
            }
            .replace(EMAIL_REGEX) { matchResult ->
                val email = matchResult.value
                val parts = email.split("@")

                if (parts.size == 2) {
                    val name = parts[0]
                    val domain = parts[1]
                    "${name.take(1)}***@$domain"
                } else {
                    "***@***"
                }
            }

        if (sanitized.length > MAX_LOG_LENGTH) {
            sanitized =
                sanitized.take(MAX_LOG_LENGTH) + "... [TRUNCATED]"
        }

        return sanitized
    }

    private fun sanitizeTag(tag: String): String {
        return tag.take(23)
    }

    /**
     * Android logging must never crash the application or interrupt
     * error handling.
     *
     * JVM unit tests may not provide a working Android Log implementation.
     */
    private inline fun safeAndroidLog(
        logAction: () -> Unit
    ) {
        try {
            logAction()
        } catch (_: RuntimeException) {
            // Logging failure must never replace the original application error.
        }
    }

    /**
     * Keeps the current throwable for diagnostic logging.
     *
     * Sensitive details must not be included in the explicit log message.
     */
    private fun filterStackTrace(
        throwable: Throwable
    ): Throwable {
        return throwable
    }
}
