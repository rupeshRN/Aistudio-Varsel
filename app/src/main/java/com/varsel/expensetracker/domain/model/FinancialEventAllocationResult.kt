package com.varsel.expensetracker.domain.model

/**
 * Result of validating an allocation request.
 */
sealed interface FinancialEventAllocationResult {

    data class Success(
        val allocation:
            FinancialEventAllocation
    ) : FinancialEventAllocationResult

    data class InvalidAmount(
        val message: String
    ) : FinancialEventAllocationResult

    data class ExceedsTransactionAmount(
        val transactionAmount: Double,
        val alreadyAllocated: Double,
        val requestedAmount: Double,
        val remainingAmount: Double
    ) : FinancialEventAllocationResult
}
