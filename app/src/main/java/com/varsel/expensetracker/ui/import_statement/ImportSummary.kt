package com.varsel.expensetracker.ui.import_statement

/**
 * High-level summary shown immediately after
 * statement parsing completes.
 *
 * This model is purely for the UI.
 */
data class ImportSummary(

    /**
     * Bank detected by the parser.
     *
     * Example:
     * Indian Bank, ICICI Bank
     */
    val bankName: String,

    /**
     * Statement period.
     *
     * Example:
     * 01 Jul 2026 → 31 Jul 2026
     */
    val statementPeriod: String,

    /**
     * Total transaction blocks detected.
     */
    val transactionsDetected: Int,

    /**
     * Successfully parsed transactions.
     */
    val transactionsParsed: Int,

    /**
     * Credit transaction count.
     */
    val credits: Int,

    /**
     * Debit transaction count.
     */
    val debits: Int,

    /**
     * Duplicate transactions skipped.
     */
    val duplicates: Int,

    /**
     * Transactions automatically improved
     * using learned knowledge.
     */
    val learnedMatches: Int,

    /**
     * Transactions requiring manual review.
     */
    val needsReview: Int,

    /**
     * Whether reconciliation succeeded.
     */
    val reconciliationPassed: Boolean,

    /**
     * Detailed status label for reconciliation.
     */
    val reconciliationStatusText: String = "Opening + Credits − Debits = Closing"

)
