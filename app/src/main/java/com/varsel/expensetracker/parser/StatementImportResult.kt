package com.varsel.expensetracker.parser

import com.varsel.expensetracker.domain.model.Transaction

data class StatementImportResult(

    val summary: StatementSummary,

    val reconciliation: ReconciliationResult,

    val transactions: List<Transaction>,

    val accountId: String? = null,

    val accountLast4: String? = null
)
