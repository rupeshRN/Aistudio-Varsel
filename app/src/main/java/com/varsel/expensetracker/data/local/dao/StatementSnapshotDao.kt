package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.varsel.expensetracker.data.local.entity.StatementSnapshotEntity

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
            statementEndDate DESC,
            importedAt DESC
        """
    )
    suspend fun getAllSnapshots(): List<StatementSnapshotEntity>
}
