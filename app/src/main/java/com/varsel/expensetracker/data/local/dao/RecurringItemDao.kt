package com.varsel.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.varsel.expensetracker.data.local.entity.RecurringItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringItemDao {

    @Query("SELECT * FROM recurring_items ORDER BY nextOccurrenceTimestamp ASC")
    fun getAllRecurringItems(): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE isActive = 1 ORDER BY nextOccurrenceTimestamp ASC")
    fun getActiveRecurringItems(): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE id = :id")
    suspend fun getRecurringItemById(id: Long): RecurringItemEntity?

    @Query("SELECT * FROM recurring_items WHERE id = :id")
    fun getRecurringItemByIdFlow(id: Long): Flow<RecurringItemEntity?>

    @Query("SELECT * FROM recurring_items WHERE type = :type ORDER BY nextOccurrenceTimestamp ASC")
    fun getRecurringItemsByType(type: String): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE isActive = 1 AND nextOccurrenceTimestamp <= :timestamp ORDER BY nextOccurrenceTimestamp ASC")
    suspend fun getDueRecurringItems(timestamp: Long): List<RecurringItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringItem(item: RecurringItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringItems(items: List<RecurringItemEntity>): List<Long>

    @Update
    suspend fun updateRecurringItem(item: RecurringItemEntity)

    @Delete
    suspend fun deleteRecurringItem(item: RecurringItemEntity)

    @Query("DELETE FROM recurring_items WHERE id = :id")
    suspend fun deleteRecurringItemById(id: Long)

    @Query("UPDATE recurring_items SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setRecurringItemActive(id: Long, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE recurring_items SET nextOccurrenceTimestamp = :nextTimestamp, lastGeneratedTimestamp = :lastGenTimestamp, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOccurrence(id: Long, nextTimestamp: Long, lastGenTimestamp: Long, updatedAt: Long = System.currentTimeMillis())
}
