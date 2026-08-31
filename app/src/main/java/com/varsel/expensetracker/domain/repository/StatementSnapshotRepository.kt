package com.varsel.expensetracker.domain.repository

import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import kotlinx.coroutines.flow.Flow

interface StatementSnapshotRepository {

    suspend fun saveSnapshot(
        snapshot: StatementSnapshotEntity
    )

    suspend fun getLatestSnapshot(
        accountId: String
    ): StatementSnapshotEntity?

    suspend fun getAllSnapshots(): List<StatementSnapshotEntity>

    fun observeAllSnapshots(): Flow<List<StatementSnapshotEntity>>

    suspend fun deleteSnapshot(snapshotId: Long)

    suspend fun deleteSnapshotWithTransactions(snapshot: StatementSnapshotEntity)
}
