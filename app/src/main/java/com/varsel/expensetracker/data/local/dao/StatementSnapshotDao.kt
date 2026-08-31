package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatementSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(
        snapshot: StatementSnapshotEntity
    )

    @Query(
        """
        SELECT *
        FROM statement_snapshots
        WHERE accountId = :accountId
        ORDER BY
            statementEndDate DESC,
            importedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestSnapshot(
        accountId: String
    ): StatementSnapshotEntity?

    @Query(
        """
        SELECT *
        FROM statement_snapshots
        ORDER BY
            importedAt DESC
        """
    )
    suspend fun getAllSnapshots(): List<StatementSnapshotEntity>

    @Query(
        """
        SELECT *
        FROM statement_snapshots
        ORDER BY
            importedAt DESC
        """
    )
    fun observeAllSnapshots(): Flow<List<StatementSnapshotEntity>>

    @Query(
        """
        DELETE FROM statement_snapshots
        WHERE id = :id
        """
    )
    suspend fun deleteSnapshotById(id: Long)

    @Query(
        """
        DELETE FROM transactions
        WHERE accountId = :accountId
        AND dateTimestamp >= :startDate
        AND dateTimestamp <= :endDate
        """
    )
    suspend fun deleteTransactionsForAccountPeriod(
        accountId: String,
        startDate: Long,
        endDate: Long
    )
}
