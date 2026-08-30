package com.varsel.expensetracker.domain.repository

import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import kotlinx.coroutines.flow.Flow

interface TransactionLinkGroupRepository {

    /**
     * Observe all report groups.
     */
    fun getAllGroups(): Flow<List<TransactionLinkGroup>>

    /**
     * Get a report group for a transaction link.
     */
    suspend fun getGroup(
        transactionLinkId: String
    ): TransactionLinkGroup?

    /**
     * Create or replace a report group.
     */
    suspend fun saveGroup(
        group: TransactionLinkGroup
    )

    /**
     * Delete report metadata without deleting transactions.
     */
    suspend fun deleteGroup(
        transactionLinkId: String
    )
}
