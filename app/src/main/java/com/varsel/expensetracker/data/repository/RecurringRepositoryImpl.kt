package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.data.local.dao.RecurringItemDao
import com.varsel.expensetracker.data.local.entity.RecurringItemEntity
import com.varsel.expensetracker.domain.model.recurring.RecurringItem
import com.varsel.expensetracker.domain.model.recurring.RecurringType
import com.varsel.expensetracker.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val recurringItemDao: RecurringItemDao
) : RecurringRepository {

    override fun getAllRecurringItems(): Flow<List<RecurringItem>> {
        return recurringItemDao.getAllRecurringItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getActiveRecurringItems(): Flow<List<RecurringItem>> {
        return recurringItemDao.getActiveRecurringItems().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecurringItemsByType(type: RecurringType): Flow<List<RecurringItem>> {
        return recurringItemDao.getRecurringItemsByType(type.name).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getRecurringItemById(id: Long): RecurringItem? {
        return recurringItemDao.getRecurringItemById(id)?.toDomain()
    }

    override fun getRecurringItemByIdFlow(id: Long): Flow<RecurringItem?> {
        return recurringItemDao.getRecurringItemByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun getDueRecurringItems(timestamp: Long): List<RecurringItem> {
        return recurringItemDao.getDueRecurringItems(timestamp).map { it.toDomain() }
    }

    override suspend fun insertRecurringItem(item: RecurringItem): Long {
        return recurringItemDao.insertRecurringItem(RecurringItemEntity.fromDomain(item))
    }

    override suspend fun updateRecurringItem(item: RecurringItem) {
        recurringItemDao.updateRecurringItem(RecurringItemEntity.fromDomain(item))
    }

    override suspend fun deleteRecurringItem(item: RecurringItem) {
        recurringItemDao.deleteRecurringItem(RecurringItemEntity.fromDomain(item))
    }

    override suspend fun deleteRecurringItemById(id: Long) {
        recurringItemDao.deleteRecurringItemById(id)
    }

    override suspend fun setRecurringItemActive(id: Long, isActive: Boolean) {
        recurringItemDao.setRecurringItemActive(id, isActive)
    }

    override suspend fun updateOccurrence(id: Long, nextTimestamp: Long, lastGenTimestamp: Long) {
        recurringItemDao.updateOccurrence(id, nextTimestamp, lastGenTimestamp)
    }
}
