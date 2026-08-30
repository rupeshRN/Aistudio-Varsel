package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.data.local.dao.FinancialEventAllocationDao
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinancialEventAllocationRepository @Inject constructor(
    private val dao: FinancialEventAllocationDao
) {

    fun observeAllAllocations():
        Flow<List<FinancialEventAllocationEntity>> =
        dao.getAllAllocations()

    fun observeAllocationsForTransaction(
        transactionId: Long
    ): Flow<List<FinancialEventAllocationEntity>> =
        dao.observeAllocationsForTransaction(transactionId)

    fun observeAllocationsForFinancialEvent(
        transactionLinkId: String
    ): Flow<List<FinancialEventAllocationEntity>> =
        dao.observeAllocationsForFinancialEvent(transactionLinkId)

    suspend fun getAllocationsForTransaction(
        transactionId: Long
    ): List<FinancialEventAllocationEntity> =
        dao.getAllocationsForTransaction(
            transactionId
        )

    /**
     * Get all transaction IDs allocated to a
     * Financial Event.
     */
    suspend fun getTransactionIdsForFinancialEvent(
        transactionLinkId: String
    ): List<Long> =
        dao.getTransactionIdsForFinancialEvent(
            transactionLinkId
        )

    /**
     * Get all allocation rows belonging to a
     * Financial Event.
     *
     * Each row contains:
     *
     * transactionId
     * transactionLinkId
     * allocatedAmount
     */
    suspend fun getAllocationsForFinancialEvent(
        transactionLinkId: String
    ): List<FinancialEventAllocationEntity> =
        dao.getAllocationsForFinancialEvent(
            transactionLinkId
        )

    /**
     * Get the total amount of a transaction that
     * has already been allocated to Financial Events.
     */
    suspend fun getAllocatedAmountForTransaction(
        transactionId: Long
    ): Double =
        dao.getAllocatedAmountForTransaction(
            transactionId
        )

    /**
     * Insert one Financial Event allocation.
     *
     * Amount validation against the original
     * transaction is intentionally performed by
     * CreateFinancialEventAllocationUseCase.
     */
    suspend fun insertAllocation(
        transactionId: Long,
        transactionLinkId: String,
        allocatedAmount: Double,
        createdAt: Long =
            System.currentTimeMillis()
    ): Long {

        require(
            allocatedAmount > 0.0
        ) {
            "Allocated amount must be greater than zero."
        }

        return dao.insertAllocation(
            FinancialEventAllocationEntity(
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

    /**
     * Insert multiple allocations.
     */
    suspend fun insertAllocations(
        allocations:
            List<FinancialEventAllocationEntity>
    ) {

        require(
            allocations.all {
                it.allocatedAmount > 0.0
            }
        ) {
            "All allocated amounts must be greater than zero."
        }

        dao.insertAllocations(
            allocations
        )
    }

    /**
     * Delete one allocation.
     */
    suspend fun deleteAllocation(
        allocation:
            FinancialEventAllocationEntity
    ) {
        dao.deleteAllocation(
            allocation
        )
    }

    /**
     * Delete allocation by ID.
     */
    suspend fun deleteAllocationById(
        allocationId: Long
    ) {
        dao.deleteAllocationById(
            allocationId
        )
    }

    /**
     * Delete every allocation belonging to a
     * transaction.
     */
    suspend fun deleteAllocationsForTransaction(
        transactionId: Long
    ) {
        dao.deleteAllocationsForTransaction(
            transactionId
        )
    }

    /**
     * Update allocated amount for a specific transaction and event.
     */
    suspend fun updateAllocationAmount(
        transactionId: Long,
        transactionLinkId: String,
        newAmount: Double
    ) {
        require(
            newAmount > 0.0
        ) {
            "Allocated amount must be greater than zero."
        }

        dao.updateAllocationAmount(
            transactionId = transactionId,
            transactionLinkId = transactionLinkId,
            newAmount = newAmount
        )
    }

    /**
     * Delete allocation for a specific transaction and event.
     */
    suspend fun deleteAllocationForTransactionAndEvent(
        transactionId: Long,
        transactionLinkId: String
    ) {
        dao.deleteAllocationForTransactionAndEvent(
            transactionId = transactionId,
            transactionLinkId = transactionLinkId
        )
    }

    /**
     * Delete every allocation belonging to a
     * Financial Event.
     */
    suspend fun deleteAllocationsForFinancialEvent(
        transactionLinkId: String
    ) {
        dao.deleteAllocationsForFinancialEvent(
            transactionLinkId
        )
    }
}
