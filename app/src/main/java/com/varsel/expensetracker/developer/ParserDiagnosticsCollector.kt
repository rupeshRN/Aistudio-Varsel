package com.varsel.expensetracker.developer

import com.varsel.expensetracker.parser.ReconciliationResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collects parser diagnostics.
 *
 * StatementParserEngine should never update
 * ParserDiagnosticsManager directly.
 *
 * All diagnostic updates flow through this class.
 */
@Singleton
class ParserDiagnosticsCollector @Inject constructor() {

    //--------------------------------------------------
    // Reset
    //--------------------------------------------------

    fun reset() {

        ParserDiagnosticsManager.reset()

    }

    //--------------------------------------------------
    // Normalization
    //--------------------------------------------------

    /**
     * Records normalization statistics directly from raw
     * and normalized statement text.
     */
    fun recordNormalization(

        rawText: String,

        normalizedText: String

    ) {

        val rawLines =

            rawText
                .lines()
                .count { it.isNotBlank() }

        val normalizedLines =

            normalizedText
                .lines()
                .count { it.isNotBlank() }

        ParserDiagnosticsManager.latest =

            ParserDiagnosticsManager.latest.copy(

                rawLines = rawLines,

                normalizedLines = normalizedLines

            )

    }

    //--------------------------------------------------
    // Date detection
    //--------------------------------------------------

    /**
     * Records the number of detected transaction dates.
     */
    fun recordDetectedDates(

        normalizedText: String

    ) {

        val detectedDates =

            Regex(
                "\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}"
            )
                .findAll(normalizedText)
                .count()

        ParserDiagnosticsManager.latest =

            ParserDiagnosticsManager.latest.copy(

                datesDetected = detectedDates

            )

    }

    //--------------------------------------------------
    // Parser output
    //--------------------------------------------------

    /**
     * Records the number of successfully parsed transactions
     * and the most recent transaction date.
     */
    fun recordTransactions(

        transactionCount: Int,

        lastTimestamp: Long?

    ) {

        ParserDiagnosticsManager.latest =

            ParserDiagnosticsManager.latest.copy(

                transactionsParsed = transactionCount,

                lastParsedDate =

                    lastTimestamp?.let {

                        SimpleDateFormat(

                            "dd MMM yyyy",

                            Locale.ENGLISH

                        ).format(Date(it))

                    } ?: "—"

            )

    }

    //--------------------------------------------------
    // Reconciliation diagnostics
    //--------------------------------------------------

    /**
     * Records reconciliation values for developer diagnostics.
     *
     * This keeps reconciliation diagnostics inside the
     * ParserDiagnosticsCollector architecture rather than
     * allowing StatementParserEngine to update
     * ParserDiagnosticsManager directly.
     */
    fun recordReconciliation(

        reconciliation: ReconciliationResult,

        statementCredits: Double?,

        statementDebits: Double?

    ) {

        ParserDiagnosticsManager.latest =

            ParserDiagnosticsManager.latest.copy(

                calculatedCredits =
                    reconciliation.calculatedCredits,

                statementCredits =
                    statementCredits,

                calculatedDebits =
                    reconciliation.calculatedDebits,

                statementDebits =
                    statementDebits,

                creditDifference =
                    reconciliation.creditDifference,

                debitDifference =
                    reconciliation.debitDifference

            )

    }

}
