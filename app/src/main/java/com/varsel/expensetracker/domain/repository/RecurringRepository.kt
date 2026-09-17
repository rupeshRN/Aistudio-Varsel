package com.varsel.expensetracker.domain.repository

import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.model.recurring.RecurringType
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun getAllRecurringItems(): Flow<List<RecurringItem>>
    fun getActiveRecurringItems(): Flow<List<RecurringItem>>
    fun getRecurringItemsByType(type: RecurringType): Flow<List<RecurringItem>>
    suspend fun getRecurringItemById(id: Long): RecurringItem?
    fun getRecurringItemByIdFlow(id: Long): Flow<RecurringItem?>
    suspend fun getDueRecurringItems(timestamp: Long): List<RecurringItem>
    suspend fun insertRecurringItem(item: RecurringItem): Long
    suspend fun updateRecurringItem(item: RecurringItem)
    suspend fun deleteRecurringItem(item: RecurringItem)
    suspend fun deleteRecurringItemById(id: Long)
    suspend fun setRecurringItemActive(id: Long, isActive: Boolean)
    suspend fun updateOccurrence(id: Long, nextTimestamp: Long, lastGenTimestamp: Long)
}
