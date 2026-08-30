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

        val statementCredits =
            summary.totalCredits ?: 0.0

        val statementDebits =
            summary.totalDebits ?: 0.0

        val creditDifference =
            calculatedCredits - statementCredits

        val debitDifference =
            calculatedDebits - statementDebits

        val isBalanced =
            abs(creditDifference) <= tolerance &&
            abs(debitDifference) <= tolerance

        return ReconciliationResult(
            calculatedCredits = calculatedCredits,
            calculatedDebits = calculatedDebits,
            creditDifference = creditDifference,
            debitDifference = debitDifference,
            isBalanced = isBalanced
        )
    }
}
