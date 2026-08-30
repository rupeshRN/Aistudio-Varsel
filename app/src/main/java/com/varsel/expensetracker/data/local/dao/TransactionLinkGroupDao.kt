package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.varsel.expensetracker.data.local.entity.TransactionLinkGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionLinkGroupDao {

    /**
     * Observe all report groups.
     */
    @Query(
        """
        SELECT *
        FROM transaction_link_groups
        ORDER BY createdAt DESC
        """
    )
    fun getAllGroups(): Flow<List<TransactionLinkGroupEntity>>

    /**
     * Get one group synchronously.
     */
    @Query(
        """
        SELECT *
        FROM transaction_link_groups
        WHERE transactionLinkId = :transactionLinkId
        LIMIT 1
        """
    )
    suspend fun getGroup(
        transactionLinkId: String
    ): TransactionLinkGroupEntity?

    /**
     * Insert a new report group.
     */
    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertGroup(
        group: TransactionLinkGroupEntity
    )

    /**
     * Update existing report-group metadata.
     */
    @Update
    suspend fun updateGroup(
        group: TransactionLinkGroupEntity
    )

    /**
     * Delete a report group.
     *
     * Underlying transactions are NOT deleted.
     */
    @Delete
    suspend fun deleteGroup(
        group: TransactionLinkGroupEntity
    )

    /**
     * Delete a group directly by its transaction link ID.
     *
     * Underlying transactions remain untouched.
     */
    @Query(
        """
        DELETE FROM transaction_link_groups
        WHERE transactionLinkId = :transactionLinkId
        """
    )
    suspend fun deleteGroupByLinkId(
        transactionLinkId: String
    )
}
