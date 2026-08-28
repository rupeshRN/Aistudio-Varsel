package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.varsel.expensetracker.data.local.entity.FinancialEventAllocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialEventAllocationDao {

    /**
     * Observe every allocation.
     */
    @Query(
        """
        SELECT *
        FROM financial_event_allocations
        ORDER BY createdAt ASC
        """
    )
    fun getAllAllocations():
        Flow<List<FinancialEventAllocationEntity>>

    /**
     * Get allocations for one transaction.
     */
    @Query(
        """
        SELECT *
        FROM financial_event_allocations
        WHERE transactionId = :transactionId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getAllocationsForTransaction(
        transactionId: Long
    ): List<FinancialEventAllocationEntity>

    /**
     * Get allocations belonging to one Financial Event.
     */
    @Query(
        """
        SELECT *
        FROM financial_event_allocations
        WHERE transactionLinkId = :transactionLinkId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getAllocationsForFinancialEvent(
        transactionLinkId: String
    ): List<FinancialEventAllocationEntity>

    /**
     * Insert one allocation.
     */
    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insertAllocation(
        allocation:
            FinancialEventAllocationEntity
    ): Long

    /**
     * Insert multiple allocations.
     */
    @Insert(
        onConflict =
            OnConflictStrategy.REPLACE
    )
    suspend fun insertAllocations(
        allocations:
            List<FinancialEventAllocationEntity>
    )

    /**
     * Remove one allocation.
     */
    @Delete
    suspend fun deleteAllocation(
        allocation:
            FinancialEventAllocationEntity
    )

    /**
     * Remove an allocation by ID.
     */
    @Query(
        """
        DELETE FROM financial_event_allocations
        WHERE id = :allocationId
        """
    )
    suspend fun deleteAllocationById(
        allocationId: Long
    )

    /**
     * Remove every allocation belonging to a
     * transaction.
     */
    @Query(
        """
        DELETE FROM financial_event_allocations
        WHERE transactionId = :transactionId
        """
    )
    suspend fun deleteAllocationsForTransaction(
        transactionId: Long
    )

    /**
     * Remove every allocation belonging to a
     * Financial Event.
     */
    @Query(
        """
        DELETE FROM financial_event_allocations
        WHERE transactionLinkId = :transactionLinkId
        """
    )
    suspend fun deleteAllocationsForFinancialEvent(
        transactionLinkId: String
    )

    /**
     * Total amount allocated from a transaction.
     */
    @Query(
        """
        SELECT COALESCE(
            SUM(allocatedAmount),
            0.0
        )
        FROM financial_event_allocations
        WHERE transactionId = :transactionId
        """
    )
    suspend fun getAllocatedAmountForTransaction(
        transactionId: Long
    ): Double

    /**
     * Get all transaction IDs allocated to a Financial Event.
     */
    @Query(
        """
        SELECT transactionId
        FROM financial_event_allocations
        WHERE transactionLinkId = :transactionLinkId
        ORDER BY createdAt ASC
        """
    )
    suspend fun getTransactionIdsForFinancialEvent(
        transactionLinkId: String
    ): List<Long>

    /**
     * Observe allocations for one transaction.
     */
    @Query(
        """
        SELECT *
        FROM financial_event_allocations
        WHERE transactionId = :transactionId
        ORDER BY createdAt ASC
        """
    )
    fun observeAllocationsForTransaction(
        transactionId: Long
    ): Flow<List<FinancialEventAllocationEntity>>

    /**
     * Observe allocations for one Financial Event.
     */
    @Query(
        """
        SELECT *
        FROM financial_event_allocations
        WHERE transactionLinkId = :transactionLinkId
        ORDER BY createdAt ASC
        """
    )
    fun observeAllocationsForFinancialEvent(
        transactionLinkId: String
    ): Flow<List<FinancialEventAllocationEntity>>

    /**
     * Update allocated amount for a specific transaction and event.
     */
    @Query(
        """
        UPDATE financial_event_allocations
        SET allocatedAmount = :newAmount
        WHERE transactionId = :transactionId AND transactionLinkId = :transactionLinkId
        """
    )
    suspend fun updateAllocationAmount(
        transactionId: Long,
        transactionLinkId: String,
        newAmount: Double
    )

    /**
     * Remove allocation for a specific transaction and event.
     */
    @Query(
        """
        DELETE FROM financial_event_allocations
        WHERE transactionId = :transactionId AND transactionLinkId = :transactionLinkId
        """
    )
    suspend fun deleteAllocationForTransactionAndEvent(
        transactionId: Long,
        transactionLinkId: String
    )
}
