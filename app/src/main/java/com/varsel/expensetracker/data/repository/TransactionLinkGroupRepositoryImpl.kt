package com.varsel.expensetracker.data.repository

import com.varsel.expensetracker.data.local.dao.TransactionLinkGroupDao
import com.varsel.expensetracker.data.local.entity.TransactionLinkGroupEntity
import com.varsel.expensetracker.domain.model.TransactionLinkGroup
import com.varsel.expensetracker.domain.repository.TransactionLinkGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionLinkGroupRepositoryImpl @Inject constructor(
    private val dao: TransactionLinkGroupDao
) : TransactionLinkGroupRepository {

    override fun getAllGroups(): Flow<List<TransactionLinkGroup>> {

        return dao
            .getAllGroups()
            .map { entities ->

                entities.map {
                    it.toDomain()
                }
            }
    }

    override suspend fun getGroup(
        transactionLinkId: String
    ): TransactionLinkGroup? {

        return dao
            .getGroup(transactionLinkId)
            ?.toDomain()
    }

    override suspend fun saveGroup(
        group: TransactionLinkGroup
    ) {

        dao.insertGroup(
            group.toEntity()
        )
    }

    override suspend fun deleteGroup(
        transactionLinkId: String
    ) {

        dao.deleteGroupByLinkId(
            transactionLinkId
        )
    }
}

private fun TransactionLinkGroupEntity.toDomain() =
    TransactionLinkGroup(

        transactionLinkId =
            transactionLinkId,

        groupName =
            groupName,

        category =
            category,

        createdAt =
            createdAt
    )

private fun TransactionLinkGroup.toEntity() =
    TransactionLinkGroupEntity(

        transactionLinkId =
            transactionLinkId,

        groupName =
            groupName,

        category =
            category,

        createdAt =
            createdAt
    )
