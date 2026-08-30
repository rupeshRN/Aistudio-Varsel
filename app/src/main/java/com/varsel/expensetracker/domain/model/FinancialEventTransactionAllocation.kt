package com.varsel.expensetracker.domain.model

/**
 * A transaction together with the amount of that
 * transaction allocated to a Financial Event.
 */
data class FinancialEventTransactionAllocation(
    val transaction: Transaction,
    val allocatedAmount: Double
)
