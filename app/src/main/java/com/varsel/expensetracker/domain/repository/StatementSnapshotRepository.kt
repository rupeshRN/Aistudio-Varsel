package com.varsel.expensetracker.domain.repository

import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity

interface StatementSnapshotRepository {

    suspend fun saveSnapshot(
        snapshot: StatementSnapshotEntity
    )

    suspend fun getLatestSnapshot(
        accountId: String
    ): StatementSnapshotEntity?

    suspend fun getAllSnapshots(): List<StatementSnapshotEntity>
}
