package com.varsel.expensetracker.domain.usecase

import com.varsel.expensetracker.data.repository.FinancialEventAllocationRepository
import com.varsel.expensetracker.domain.model.FinancialEventAllocation
import com.varsel.expensetracker.domain.model.FinancialEventAllocationResult
import com.varsel.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlin.math.abs

/**
 * Creates an allocation of a transaction to a Financial Event.
 *
 * A transaction can be allocated to multiple Financial Events.
 *
 * Example:
 *
 * Transaction = ₹1,000
 *
 * Event A = ₹600
 * Event B = ₹400
 *
 * Total allocated = ₹1,000
 *
 * The transaction is therefore fully represented by
 * Financial Events and contributes ₹0 to ordinary
 * category reporting.
 *
 * Partial allocation is also supported:
 *
 * Transaction = ₹1,000
 * Event A = ₹600
 *
 * Remaining ordinary transaction amount = ₹400
 */
class CreateFinancialEventAllocationUseCase @Inject constructor(
    private val allocationRepository:
        FinancialEventAllocationRepository,

    private val transactionRepository:
        TransactionRepository
) {

    suspend operator fun invoke(
        transactionId: Long,
        transactionLinkId: String,
        allocatedAmount: Double
    ): FinancialEventAllocationResult {

        // --------------------------------------------------
        // Basic validation
        // --------------------------------------------------

        if (allocatedAmount <= 0.0) {
            return FinancialEventAllocationResult
                .InvalidAmount(
                    message =
                        "Allocated amount must be greater than zero."
                )
        }

        // --------------------------------------------------
        // Load original transaction
        // --------------------------------------------------

        val transaction =
            transactionRepository
                .getTransactionById(
                    transactionId
                )
                ?: return FinancialEventAllocationResult
                    .InvalidAmount(
                        message =
                            "Transaction could not be found."
                    )

        /*
         * Transaction.amount may use a negative sign for
         * expenses depending on the existing Varsel model.
         *
         * Allocation itself represents magnitude, therefore
         * compare using the absolute transaction amount.
         */
        val transactionAmount =
            abs(
                transaction.amount
            )

        // --------------------------------------------------
        // Existing allocations
        // --------------------------------------------------

        val alreadyAllocated =
            allocationRepository
                .getAllocatedAmountForTransaction(
                    transactionId
                )

        val remainingAmount =
            transactionAmount -
                alreadyAllocated

        // --------------------------------------------------
        // Allocation cannot exceed remaining amount
        // --------------------------------------------------

        if (
            allocatedAmount >
            remainingAmount + ALLOCATION_EPSILON
        ) {

            return FinancialEventAllocationResult
                .ExceedsTransactionAmount(
                    transactionAmount =
                        transactionAmount,

                    alreadyAllocated =
                        alreadyAllocated,

                    requestedAmount =
                        allocatedAmount,

                    remainingAmount =
                        maxOf(
                            0.0,
                            remainingAmount
                        )
                )
        }

        // --------------------------------------------------
        // Create allocation
        // --------------------------------------------------

        val createdAt =
            System.currentTimeMillis()

        val allocationId =
            allocationRepository
                .insertAllocation(
                    transactionId =
                        transactionId,

                    transactionLinkId =
                        transactionLinkId,

                    allocatedAmount =
                        allocatedAmount,

                    createdAt =
                        createdAt
                )

        // --------------------------------------------------
        // Return domain result
        // --------------------------------------------------

        return FinancialEventAllocationResult
            .Success(
                allocation =
                    FinancialEventAllocation(
                        id =
                            allocationId,

                        transactionId =
                            transactionId,

                        transactionLinkId =
                            transactionLinkId,

                        allocatedAmount =
                            allocatedAmount,

                        createdAt =
                            createdAt
                    )
            )
    }

    private companion object {

        /**
         * Small tolerance for Double arithmetic.
         *
         * Example:
         *
         * 1000.00 - 600.00
         *
         * can theoretically produce a value such as
         * 399.99999999999994.
         */
        const val ALLOCATION_EPSILON =
            0.000001
    }
}
