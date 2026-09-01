package com.varsel.expensetracker.parser

import com.varsel.expensetracker.domain.model.Transaction
import com.varsel.expensetracker.domain.model.TransactionType
import javax.inject.Inject
import kotlin.math.abs

class ReconciliationEngine @Inject constructor() {

    private val tolerance = 0.01

    fun reconcile(
        summary: StatementSummary,
        transactions: List<Transaction>
    ): ReconciliationResult {

        val calculatedCredits =
            transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

        val calculatedDebits =
            transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

        val hasSummaryTotals = summary.totalCredits != null || summary.totalDebits != null

        val statementCredits =
            summary.totalCredits ?: calculatedCredits

        val statementDebits =
            summary.totalDebits ?: calculatedDebits

        val creditDifference =
            calculatedCredits - statementCredits

        val debitDifference =
            calculatedDebits - statementDebits

        val isBalanced = if (hasSummaryTotals) {
            abs(creditDifference) <= tolerance &&
            abs(debitDifference) <= tolerance
        } else {
            // For banks like ICICI that don't provide a grand total credit/debit box,
            // the statement is considered balanced if transactions were parsed.
            transactions.isNotEmpty()
        }

        return ReconciliationResult(
            calculatedCredits = calculatedCredits,
            calculatedDebits = calculatedDebits,
            creditDifference = creditDifference,
            debitDifference = debitDifference,
            isBalanced = isBalanced,
            hasSummaryTotals = hasSummaryTotals
        )
    }
}
