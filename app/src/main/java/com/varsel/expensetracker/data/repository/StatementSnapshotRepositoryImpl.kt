package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.data.local.dao.StatementSnapshotDao
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import com.varsel.expensetracker.domain.repository.StatementSnapshotRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StatementSnapshotRepositoryImpl @Inject constructor(
    private val statementSnapshotDao: StatementSnapshotDao
) : StatementSnapshotRepository {

    override suspend fun saveSnapshot(
        snapshot: StatementSnapshotEntity
    ) {
        statementSnapshotDao.insertSnapshot(snapshot)
    }

    override suspend fun getLatestSnapshot(
        accountId: String
    ): StatementSnapshotEntity? {
        return statementSnapshotDao.getLatestSnapshot(accountId)
    }

    override suspend fun getAllSnapshots():
        List<StatementSnapshotEntity> {
        return statementSnapshotDao.getAllSnapshots()
    }

    override fun observeAllSnapshots(): Flow<List<StatementSnapshotEntity>> {
        return statementSnapshotDao.observeAllSnapshots()
    }

    override suspend fun deleteSnapshot(snapshotId: Long) {
        statementSnapshotDao.deleteSnapshotById(snapshotId)
    }

    override suspend fun deleteSnapshotWithTransactions(snapshot: StatementSnapshotEntity) {
        statementSnapshotDao.deleteSnapshotById(snapshot.id)
        val accId = snapshot.accountId
        val start = snapshot.statementStartDate
        val end = snapshot.statementEndDate
        if (!accId.isNullOrBlank() && start != null && end != null) {
            statementSnapshotDao.deleteTransactionsForAccountPeriod(accId, start, end)
        }
    }
}
