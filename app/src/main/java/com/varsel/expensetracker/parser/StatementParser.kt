package com.varsel.expensetracker.parser

import com.varsel.expensetracker.domain.model.Transaction

interface StatementParser {

    /**
     * Returns true if this parser understands the supplied statement.
     */
    fun canParse(rawText: String): Boolean

    /**
     * Converts the statement into transactions.
     */
    fun parse(rawText: String): List<Transaction>

    /**
     * Optional hook for parsers that compute opening/ending balances directly from row ledgers (e.g. ICICI Bank).
     */
    fun extractSummary(rawText: String, transactions: List<Transaction>): StatementSummary? = null
}
